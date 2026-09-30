package com.somepro.interfaces.rest.pawner.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 当户对外档案（接口层出参，不可变 record）。名册每一行与详情共用。
 * 每行都带 pawnerNo，方便柜台跟纸面登记本对号。
 * 不含 delFlag / createBy / updateBy / updateTime 等内部字段。
 */
public record PawnerVO(
        Long id,
        String pawnerNo,
        String name,
        String idCard,
        String phone,
        String address,
        String status,
        LocalDateTime createTime
) implements Serializable {
}
