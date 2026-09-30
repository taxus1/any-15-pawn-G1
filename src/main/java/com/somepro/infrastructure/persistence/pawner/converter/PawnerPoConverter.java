package com.somepro.infrastructure.persistence.pawner.converter;

import com.somepro.domain.pawner.model.Pawner;
import com.somepro.domain.pawner.model.PawnerStatus;
import com.somepro.infrastructure.persistence.pawner.po.PawnerPO;

/**
 * PawnerPO（表）↔ Pawner（领域）转换器（基础设施层），PO 不外泄。
 * status 列存枚举名，读出时用 valueOf 还原；库里的值受写入端约束，必为三个合法值之一。
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
        po.setStatus(domain.getStatus() == null ? null : domain.getStatus().code());
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
