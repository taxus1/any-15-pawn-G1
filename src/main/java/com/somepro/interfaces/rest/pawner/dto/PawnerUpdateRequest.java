package com.somepro.interfaces.rest.pawner.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 修改当户入参（用户接口层）。
 *
 * 字段按全量传；status 可选，只允许 NORMAL/FROZEN（冻结/解冻），
 * 传 CLOSED 或别的写法一律挡回 —— 注销有专门的 /close 用例。
 */
@Getter
@Setter
public class PawnerUpdateRequest {

    private Long id;

    private String name;

    private String idCard;

    private String phone;

    private String address;

    private String status;
}
