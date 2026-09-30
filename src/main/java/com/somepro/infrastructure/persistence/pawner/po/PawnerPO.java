package com.somepro.infrastructure.persistence.pawner.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.somepro.infrastructure.persistence.base.BasePO;
import lombok.Getter;
import lombok.Setter;

/**
 * t_pawner 当户档案表的 PO（基础设施层）。只描述表结构，不含业务规则。
 *
 * 注意：t_pawner 上没有「未注销身份证唯一」的数据库约束（列上只有普通索引 idx_id_card），
 * 因为同一身份证允许同时存在多条历史 CLOSED 档案。该不变量由应用侧在
 * MySQL 命名锁 + 事务内校验保证（见 PawnerRepositoryImpl#register/modify）。
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

    // 电话/地址允许被清空成 NULL：默认 NOT_NULL 更新策略会跳过 null 字段，这里改为始终参与 UPDATE
    @TableField(value = "phone", updateStrategy = FieldStrategy.ALWAYS)
    private String phone;

    @TableField(value = "address", updateStrategy = FieldStrategy.ALWAYS)
    private String address;

    /** NORMAL / FROZEN / CLOSED，库里是 VARCHAR，领域侧用枚举。 */
    @TableField("status")
    private String status;
}
