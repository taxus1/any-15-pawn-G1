package com.somepro.infrastructure.persistence.pawner.converter;

import com.somepro.domain.pawner.model.Pawner;
import com.somepro.domain.pawner.model.PawnerStatus;
import com.somepro.infrastructure.persistence.pawner.po.PawnerPO;

/**
 * PawnerPO（表）↔ Pawner（领域）唯一转换入口（基础设施层）。
 * status 在库里是 VARCHAR，领域侧是枚举，枚举非法值属于数据腐败，读到直接失败比静默吞掉安全。
 */
public final class PawnerPoConverter {

    private PawnerPoConverter() {
    }

    public static PawnerPO toPo(Pawner domain) {
        PawnerPO po = new PawnerPO();
        po.setId(domain.getId());
        po.setPawnerNo(domain.getPawnerNo());
        po.setName(domain.getName());
        po.setIdCard(domain.getIdCard());
        po.setPhone(domain.getPhone());
        po.setAddress(domain.getAddress());
        po.setStatus(domain.getStatus() == null ? null : domain.getStatus().name());
        po.setDelFlag(domain.getDelFlag());
        po.setCreateBy(domain.getCreateBy());
        po.setCreateTime(domain.getCreateTime());
        po.setUpdateBy(domain.getUpdateBy());
        po.setUpdateTime(domain.getUpdateTime());
        return po;
    }

    public static Pawner toDomain(PawnerPO po) {
        Pawner domain = new Pawner();
        domain.setId(po.getId());
        domain.setPawnerNo(po.getPawnerNo());
        domain.setName(po.getName());
        domain.setIdCard(po.getIdCard());
        domain.setPhone(po.getPhone());
        domain.setAddress(po.getAddress());
        domain.setStatus(po.getStatus() == null ? null : PawnerStatus.valueOf(po.getStatus()));
        domain.setDelFlag(po.getDelFlag());
        domain.setCreateBy(po.getCreateBy());
        domain.setCreateTime(po.getCreateTime());
        domain.setUpdateBy(po.getUpdateBy());
        domain.setUpdateTime(po.getUpdateTime());
        return domain;
    }
}
