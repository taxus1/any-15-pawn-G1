package com.somepro.application.pawner.port;

import com.somepro.domain.pawner.model.PawnerAssets;
import reactor.core.publisher.Mono;

/**
 * 当户对账数据查询端口（应用层定义）：从当物、当票两个模块的表里实时统计
 * 某个当户名下的在押/在库件数与在当当票数。
 *
 * 单独开一个端口（而不是塞进 PawnerRepository）：当物、当票模块后续落地时，
 * 由它们的读模型/仓储实现本端口即可，当户模块只认这份对账数据、不直接依赖对方的表细节。
 */
public interface PawnerAssetsQueryPort {

    Mono<PawnerAssets> countByPawner(Long pawnerId);
}
