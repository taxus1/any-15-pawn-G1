package com.somepro.application.pawner;

import com.somepro.application.pawner.port.PawnerAssetsQueryPort;
import com.somepro.common.exception.BizException;
import com.somepro.domain.pawner.model.Pawner;
import com.somepro.domain.pawner.model.PawnerAssets;
import com.somepro.domain.pawner.model.PawnerCondition;
import com.somepro.domain.pawner.model.PawnerDetail;
import com.somepro.domain.pawner.model.PawnerStatus;
import com.somepro.domain.pawner.repository.PawnerRepository;
import com.somepro.domain.shared.model.PageResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * 当户用例编排（应用层）：录入、修改、详情、注销、翻名册。
 *
 * 只做编排与前置校验，不写 SQL、不碰 PO/VO；真正与并发/事务强绑定的
 * 身份证唯一、编号取号、注销结清复查都收敛在仓储实现里。
 */
@Service
public class PawnerAppService {

    private final PawnerRepository pawnerRepository;
    private final PawnerAssetsQueryPort pawnerAssetsQueryPort;

    public PawnerAppService(PawnerRepository pawnerRepository,
                            PawnerAssetsQueryPort pawnerAssetsQueryPort) {
        this.pawnerRepository = pawnerRepository;
        this.pawnerAssetsQueryPort = pawnerAssetsQueryPort;
    }

    /** 录入当户：编号由仓储分配，状态默认 NORMAL；同身份证有未注销档案时仓储侧挡回。 */
    public Mono<Pawner> register(String name, String idCard, String phone, String address) {
        Pawner pawner = Pawner.create(name, idCard, phone, address);
        return pawnerRepository.register(pawner);
    }

    /**
     * 修改当户：姓名/身份证/电话/地址可改；status 仅允许 NORMAL / FROZEN（不传则保持原状）。
     * CLOSED 只能走注销用例。已注销档案拒绝任何修改。
     */
    public Mono<Pawner> modify(Long id, String name, String idCard, String phone, String address, String status) {
        return pawnerRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("当户不存在或已删除")))
                .flatMap(existing -> {
                    if (existing.getStatus() == PawnerStatus.CLOSED) {
                        return Mono.<Pawner>error(new BizException("已注销档案不能修改"));
                    }
                    existing.applyProfile(name, idCard, phone, address);
                    if (status != null && !status.isBlank()) {
                        existing.changeStatus(PawnerStatus.parse(status));
                    }
                    return pawnerRepository.modify(existing);
                });
    }

    /** 看详情：档案 + 名下还押几件、在库几件、在当当票几步（含已注销档案，账留着）。 */
    public Mono<PawnerDetail> detail(Long id) {
        return pawnerRepository.findDetailById(id)
                .switchIfEmpty(Mono.error(new BizException("当户不存在或已删除")));
    }

    /**
     * 注销（留账除名）：
     * 先在事务外给柜台一句明确的业务提示；仓储在事务内还会再复查一遍并做条件更新，
     * 防住「提示干净、实际落库前又被开了当票」的并发空档。
     */
    public Mono<Void> close(Long id) {
        return pawnerRepository.findById(id)
                .switchIfEmpty(Mono.error(new BizException("当户不存在或已删除")))
                .flatMap(existing -> {
                    if (existing.getStatus() == PawnerStatus.CLOSED) {
                        return Mono.<Void>error(new BizException("该当户已注销，无需重复注销"));
                    }
                    return pawnerAssetsQueryPort.countByPawner(id)
                            .flatMap(assets -> {
                                if (!assets.allSettled()) {
                                    return Mono.<Void>error(new BizException(buildBlockedMessage(assets)));
                                }
                                return pawnerRepository.close(id);
                            });
                });
    }

    /** 翻名册：任意条件组合，全空调全量；status 传 CLOSED 视为查不到（注销档案不在名册）。 */
    public Mono<PageResult<Pawner>> page(int pageNum, int pageSize,
                                         String name, String idCard, String phone, String status) {
        if (pageNum < 1) {
            return Mono.error(new BizException("pageNum 必须 >= 1"));
        }
        if (pageSize < 1) {
            return Mono.error(new BizException("pageSize 必须 >= 1"));
        }
        PawnerStatus statusFilter = (status == null || status.isBlank()) ? null : PawnerStatus.parse(status);
        if (statusFilter == PawnerStatus.CLOSED) {
            // 注销即除名，名册永远不返回 CLOSED
            return Mono.error(new BizException("已注销档案不在名册中，状态只可按 NORMAL / FROZEN 筛选"));
        }
        PawnerCondition condition = PawnerCondition.of(name, idCard, phone, statusFilter);
        return pawnerRepository.page(pageNum, pageSize, condition);
    }

    private String buildBlockedMessage(PawnerAssets assets) {
        return "注销失败：名下尚有 " + assets.pledgedCount() + " 件未了结当物（其中在库 "
                + assets.inStockCount() + " 件）、" + assets.activeTicketCount() + " 张在当当票，结清后才能注销";
    }
}
