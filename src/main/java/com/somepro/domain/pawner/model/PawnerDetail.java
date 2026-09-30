package com.somepro.domain.pawner.model;

/**
 * 当户详情（领域值对象）：档案本体 + 名下联当业务对账数。
 *
 * 详情允许看 CLOSED 档案（账得留着、查得到），与名册查询（隐去 CLOSED）有意区分。
 */
public record PawnerDetail(Pawner pawner, PawnerAssets assets) {
}
