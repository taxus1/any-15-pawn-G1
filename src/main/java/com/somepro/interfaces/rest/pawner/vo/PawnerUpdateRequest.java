package com.somepro.interfaces.rest.pawner.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 当户修改请求（接口层入参，不可变 record）。
 * status 不传表示保持原状；只接受 NORMAL / FROZEN，CLOSED 必须走注销接口（应用层再校验一次）。
 */
public record PawnerUpdateRequest(
        @NotBlank(message = "姓名不能为空")
        @Size(max = 64, message = "姓名长度不能超过 64")
        String name,

        @NotBlank(message = "身份证号不能为空")
        @Size(max = 18, message = "身份证号长度不能超过 18")
        String idCard,

        @Size(max = 20, message = "联系电话长度不能超过 20")
        String phone,

        @Size(max = 255, message = "地址长度不能超过 255")
        String address,

        String status
) implements Serializable {
}
