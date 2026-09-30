package com.somepro.interfaces.rest.pawner.converter;

import com.somepro.domain.pawner.model.Pawner;
import com.somepro.domain.pawner.model.PawnerDetail;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.interfaces.rest.common.vo.PageVO;
import com.somepro.interfaces.rest.pawner.vo.PawnerDetailVO;
import com.somepro.interfaces.rest.pawner.vo.PawnerVO;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 当户领域对象 → 对外 VO 的唯一转换入口（接口层），Controller 不直接序列化领域对象。
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
                domain.getStatus() == null ? null : domain.getStatus().name(),
                domain.getCreateTime());
    }

    public static PawnerDetailVO toDetailVo(PawnerDetail detail) {
        Pawner p = detail.pawner();
        return new PawnerDetailVO(
                p.getId(),
                p.getPawnerNo(),
                p.getName(),
                p.getIdCard(),
                p.getPhone(),
                p.getAddress(),
                p.getStatus() == null ? null : p.getStatus().name(),
                p.getCreateTime(),
                detail.assets().pledgedCount(),
                detail.assets().inStockCount(),
                detail.assets().activeTicketCount());
    }

    public static PageVO<PawnerVO> toPageVo(PageResult<Pawner> page) {
        List<PawnerVO> content = page.content().stream()
                .map(PawnerVoConverter::toVo)
                .collect(Collectors.toList());
        return new PageVO<>(content, page.total(), page.pageNum(), page.pageSize(), page.totalPages());
    }
}
