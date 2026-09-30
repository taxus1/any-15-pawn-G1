package com.somepro.domain.pawner.repository;

import com.somepro.domain.pawner.model.PawnerLedger;
import reactor.core.publisher.Mono;

/**
 * 当户对账端口：当户模块需要向当物、当票两个模块要的数（领域层定义，基础设施层跨表实现）。
 *
 * 当户模块自己的表不存这些数 —— 存了就会和当物/当票模块对不上。每次详情、注销前实时去两张表点。
 */
public interface PawnerLedgerPort {

    Mono<PawnerLedger> load(Long pawnerId);
}
