package com.somepro.infrastructure.persistence.pawner.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.somepro.infrastructure.persistence.base.BasePO;
import lombok.Getter;
import lombok.Setter;

/**
 * t_pawner 表的持久化对象（PO，基础设施层）。只描述表结构，不放业务规则。
 *
 * 表已由 doc/schema/pawn.sql 建好，列名即契约，本类不做任何建表/改表动作。
 * status 列直接存状态枚举名（NORMAL/FROZEN/CLOSED），由 Converter 与领域枚举互转。
 */
@Getter
@Setter
@TableName("t_pawner")
public class PawnerPO extends BasePO {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("pawner_no")
    private String pawnerNo;

    @TableField("name")
    private String name;

    @TableField("id_card")
    private String idCard;

    @TableField("phone")
    private String phone;

    @TableField("address")
    private String address;

    @TableField("status")
    private String status;
}
