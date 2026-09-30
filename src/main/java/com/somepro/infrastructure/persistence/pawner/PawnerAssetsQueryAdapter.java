package com.somepro.infrastructure.persistence.pawner;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.somepro.application.pawner.port.PawnerAssetsQueryPort;
import com.somepro.domain.pawner.model.PawnerAssets;
import com.somepro.infrastructure.config.ReactiveOperatorContext;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import com.somepro.infrastructure.persistence.pawner.query.CollateralStatMapper;
import com.somepro.infrastructure.persistence.pawner.query.CollateralStatPO;
import com.somepro.infrastructure.persistence.pawner.query.PawnTicketStatMapper;
import com.somepro.infrastructure.persistence.pawner.query.PawnTicketStatPO;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.function.Supplier;

/**
 * 当户对账查询端口的适配器（基础设施层）。
 *
 * 数字直接从当物、当票两表实时 COUNT，@TableLogic 自动追加 del_flag = 0，
 * 保证与那两个模块里的记录对得上，不会出现「详情显示干净、那边还压着一堆东西」。
 *
 * 口径（与 PawnerAssets 注释一致）：
 * - 还押在行里（未了结）：IN_STOCK（登记未开票，躺在库里）+ PAWNED（开了在当当票）
 * - 还躺在库里：仅 IN_STOCK
 * - 没走完的当票：ACTIVE
 */
@Repository
public class PawnerAssetsQueryAdapter implements PawnerAssetsQueryPort {

    private final CollateralStatMapper collateralStatMapper;
    private final PawnTicketStatMapper pawnTicketStatMapper;

    public PawnerAssetsQueryAdapter(CollateralStatMapper collateralStatMapper,
                                   PawnTicketStatMapper pawnTicketStatMapper) {
        this.collateralStatMapper = collateralStatMapper;
        this.pawnTicketStatMapper = pawnTicketStatMapper;
    }

    @Override
    public Mono<PawnerAssets> countByPawner(Long pawnerId) {
        return blocking(() -> {
            long inStock = collateralStatMapper.selectCount(
                    Wrappers.<CollateralStatPO>lambdaQuery()
                            .eq(CollateralStatPO::getPawnerId, pawnerId)
                            .eq(CollateralStatPO::getStatus, "IN_STOCK"));
            long pawned = collateralStatMapper.selectCount(
                    Wrappers.<CollateralStatPO>lambdaQuery()
                            .eq(CollateralStatPO::getPawnerId, pawnerId)
                            .eq(CollateralStatPO::getStatus, "PAWNED"));
            long activeTickets = pawnTicketStatMapper.selectCount(
                    Wrappers.<PawnTicketStatPO>lambdaQuery()
                            .eq(PawnTicketStatPO::getPawnerId, pawnerId)
                            .eq(PawnTicketStatPO::getStatus, "ACTIVE"));
            return new PawnerAssets(inStock + pawned, inStock, activeTickets);
        });
    }

    private <T> Mono<T> blocking(Supplier<T> supplier) {
        return Mono.deferContextual(ctx -> {
            String operator = ReactiveOperatorContext.getOperator(ctx);
            return Mono.fromCallable(() -> {
                AuditContextHolder.setOperator(operator);
                try {
                    return supplier.get();
                } finally {
                    AuditContextHolder.clear();
                }
            }).subscribeOn(Schedulers.boundedElastic());
        });
    }
}
