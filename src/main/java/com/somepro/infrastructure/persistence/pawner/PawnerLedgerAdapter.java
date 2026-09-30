package com.somepro.infrastructure.persistence.pawner;

import com.somepro.domain.pawner.model.PawnerLedger;
import com.somepro.domain.pawner.repository.PawnerLedgerPort;
import com.somepro.infrastructure.config.ReactiveOperatorContext;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.function.Supplier;

/**
 * 当户对账端口的适配器（基础设施层）：实时去 t_collateral / t_pawn_ticket 点行，
 * 不在当户表里冗余这些数，保证与当物、当票两个模块永远对得上。
 *
 * 三次点行都在同一 boundedElastic 任务、同一审计上下文里完成（查询不写审计，取操作人仅为约定统一）。
 */
@Component
public class PawnerLedgerAdapter implements PawnerLedgerPort {

    private final CollateralQueryMapper collateralQueryMapper;
    private final PawnTicketQueryMapper pawnTicketQueryMapper;

    public PawnerLedgerAdapter(CollateralQueryMapper collateralQueryMapper,
                               PawnTicketQueryMapper pawnTicketQueryMapper) {
        this.collateralQueryMapper = collateralQueryMapper;
        this.pawnTicketQueryMapper = pawnTicketQueryMapper;
    }

    @Override
    public Mono<PawnerLedger> load(Long pawnerId) {
        return blocking(() -> new PawnerLedger(
                collateralQueryMapper.countHeld(pawnerId),
                collateralQueryMapper.countInStock(pawnerId),
                pawnTicketQueryMapper.countActive(pawnerId)));
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
