package com.somepro.interfaces.rest.pawner.converter;

import com.somepro.application.pawner.PawnerAppService.PawnerDetail;
import com.somepro.domain.pawner.model.Pawner;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.pawner.vo.PawnerDetailVO;
import com.somepro.interfaces.rest.pawner.vo.PawnerVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 当户领域对象 → VO 转换器（用户接口层）。Controller 不直接把领域对象塞进 Result。
 */
public final class PawnerVoConverter {

    private PawnerVoConverter() {
    }

    public static PawnerVO toVo(Pawner domain) {
        return new PawnerVO(
                domain.getId(),
                domain.getPawnerNo(),
                domain.getName(),
                domain.getIdCard(),
                domain.getPhone(),
                domain.getAddress(),
                domain.getStatus() == null ? null : domain.getStatus().code(),
                domain.getCreateTime());
    }

    public static PawnerDetailVO toDetailVo(PawnerDetail detail) {
        return new PawnerDetailVO(
                toVo(detail.pawner()),
                detail.ledger().heldItemCount(),
                detail.ledger().inStockCount(),
                detail.ledger().activeTicketCount());
    }

    public static PageVO<PawnerVO> toPageVo(PageResult<Pawner> page) {
        List<PawnerVO> content = page.content().stream()
                .map(PawnerVoConverter::toVo)
                .collect(Collectors.toList());
        return new PageVO<>(content, page.total(), page.pageNum(), page.pageSize(), page.totalPages());
    }
}
