package com.somepro.interfaces.rest.pawner.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 录入当户入参（用户接口层）。
 *
 * 用可变 bean + @ModelAttribute：Spring WebFlux 下 application/x-www-form-urlencoded 表单、
 * query string 都能直接绑定（@RequestParam 在 WebFlux 不解析表单体）。
 * 状态不接受外部传入，新录固定 NORMAL。
 */
@Getter
@Setter
public class PawnerCreateRequest {

    private String name;

    private String idCard;

    private String phone;

    private String address;
}
