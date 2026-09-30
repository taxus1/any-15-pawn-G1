package com.somepro.interfaces.rest.pawner.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 按 id 操作的入参（注销等）。POST 表单体在 WebFlux 下需经 @ModelAttribute 绑定。
 */
@Getter
@Setter
public class PawnerIdRequest {

    private Long id;
}
