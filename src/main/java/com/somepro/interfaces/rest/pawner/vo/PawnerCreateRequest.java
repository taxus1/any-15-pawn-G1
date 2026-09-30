package com.somepro.interfaces.rest.pawner.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 当户录入请求（接口层入参，不可变 record）。
 * 状态不接受前端指定：新录一律 NORMAL；编号由后端按年生成。
 */
public record PawnerCreateRequest(
        @NotBlank(message = "姓名不能为空")
        @Size(max = 64, message = "姓名长度不能超过 64")
        String name,

        @NotBlank(message = "身份证号不能为空")
        @Size(max = 18, message = "身份证号长度不能超过 18")
        String idCard,

        @Size(max = 20, message = "联系电话长度不能超过 20")
        String phone,

        @Size(max = 255, message = "地址长度不能超过 255")
        String address
) implements Serializable {
}
