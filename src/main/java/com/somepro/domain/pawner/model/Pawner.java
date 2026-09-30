package com.somepro.domain.pawner.model;

import com.somepro.common.exception.BizException;
import com.somepro.domain.shared.model.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 当户档案聚合根（典当行的「人」）。
 *
 * 纯领域对象：只承载档案字段与业务不变量，不带任何持久化/接口注解。
 *
 * 关键业务规则（本模块最容易出错的一处）：
 * 身份证认人，但「唯一」只针对**未注销（status != CLOSED）**的档案：
 * - 同一张身份证同时存在两条未注销档案 —— 不允许（新建/改身份证时挡回）；
 * - 历史上注销过的档案不算数 —— 注销后拿同一张身份证重新建档必须放行；
 * - 注销是「留账除名」：状态置 CLOSED，物理记录保留，名册查询隐去但账还在。
 *
 * 注销另有前置条件（名下无未了结当物、无在当当票），规则在用例编排里校验。
 */
@Getter
@Setter
public class Pawner extends BaseEntity {

    private Long id;

    /** 当户编号，样式 DH-2026-0001，全局唯一。 */
    private String pawnerNo;

    private String name;

    /** 身份证号：未注销档案内唯一；注销档案不占名额。 */
    private String idCard;

    private String phone;

    private String address;

    private PawnerStatus status;

    /**
     * 工厂方法：新录档案。编号由仓储侧按年份序列分配后回填，状态固定默认 NORMAL
     * （不允许新建时直接传 FROZEN / CLOSED）。
     */
    public static Pawner create(String name, String idCard, String phone, String address) {
        Pawner pawner = new Pawner();
        pawner.applyProfile(name, idCard, phone, address);
        pawner.status = PawnerStatus.NORMAL;
        return pawner;
    }

    /**
     * 领域行为：改资料（姓名/身份证/电话/地址）。
     * 身份证是否与他人未注销档案冲突，由用例层在仓储侧加锁校验（同身份证并发只落一份）。
     */
    public void applyProfile(String name, String idCard, String phone, String address) {
        if (name == null || name.isBlank()) {
            throw new BizException("姓名不能为空");
        }
        if (idCard == null || idCard.isBlank()) {
            throw new BizException("身份证号不能为空");
        }
        if (name.trim().length() > 64) {
            throw new BizException("姓名长度不能超过 64");
        }
        if (idCard.trim().length() > 18) {
            throw new BizException("身份证号长度不能超过 18");
        }
        if (phone != null && phone.trim().length() > 20) {
            throw new BizException("联系电话长度不能超过 20");
        }
        if (address != null && address.trim().length() > 255) {
            throw new BizException("地址长度不能超过 255");
        }
        this.name = name.trim();
        this.idCard = idCard.trim();
        this.phone = emptyToNull(phone);
        this.address = emptyToNull(address);
    }

    /**
     * 领域行为：日常状态调整，只允许在 NORMAL / FROZEN 之间切换。
     * CLOSED 只能由 {@link #close()} 走出注销流程，不能随手改出来。
     */
    public void changeStatus(PawnerStatus target) {
        if (target == null) {
            throw new BizException("状态不能为空");
        }
        if (this.status == PawnerStatus.CLOSED) {
            throw new BizException("已注销档案不能再调整状态");
        }
        if (target == PawnerStatus.CLOSED) {
            throw new BizException("注销必须走注销流程，请调用注销接口");
        }
        this.status = target;
    }

    /** 领域行为：结清校验通过后正式注销（留账除名）。 */
    public void close() {
        if (this.status == PawnerStatus.CLOSED) {
            throw new BizException("该当户已注销，无需重复注销");
        }
        this.status = PawnerStatus.CLOSED;
    }

    private static String emptyToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
