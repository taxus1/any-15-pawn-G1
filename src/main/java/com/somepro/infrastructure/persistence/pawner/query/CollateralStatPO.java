package com.somepro.infrastructure.persistence.pawner.query;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.somepro.infrastructure.persistence.base.BasePO;
import lombok.Getter;
import lombok.Setter;

/**
 * t_collateral（当物登记）的只读投影 PO（基础设施层）。
 *
 * 当物模块本身尚未落地，当户模块的注销校验 / 详情对账需要从这张表实时计数，
 * 这里只映射计数用得到的列（id / pawner_id / status），不做任何写入。
 * 后续当物模块落地自己的完整持久化时，可把本类迁走。
 * del_flag 仍由 BasePO 上的 @TableLogic 自动过滤，计数只算未删除行。
 */
@Getter
@Setter
@TableName("t_collateral")
public class CollateralStatPO extends BasePO {

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    @TableField("pawner_id")
    private Long pawnerId;

    /** IN_STOCK 在库 / PAWNED 已典当 / REDEEMED 已赎回 / FORFEITED 已绝当 / RELEASED 已退还 */
    @TableField("status")
    private String status;
}
