package com.somepro.domain.pawner.repository;

import com.somepro.domain.pawner.model.Pawner;
import com.somepro.domain.pawner.model.PawnerCondition;
import com.somepro.domain.pawner.model.PawnerDetail;
import com.somepro.domain.shared.model.PageResult;
import reactor.core.publisher.Mono;

/**
 * 当户聚合的仓储端口（领域层定义，基础设施层实现）。
 *
 * 方法按业务动作命名，而不是裸 save/update —— 因为几个写动作都带必须由存储层
 * 与 SQL 强绑定才能守住的不变量：
 * - {@link #register}：身份证未注销唯一的「检查 + 落库」必须在同一把库级互斥锁 +
 *   同一事务内完成，否则同一身份证的并发登记会双双落库（柜台连点两下只准一份）；
 *   当户编号 DH-年份-序号 也在其中按年取号，保证号不重、不落到两个人头上。
 * - {@link #modify}：改身份证同样要在锁内复查未注销唯一。
 * - {@link #close}：注销前在同一事务内复查名下当物/当票是否结清，并做状态条件更新，
 *   防住「检查通过、落库前又被开了当票」的并发窗口。
 */
public interface PawnerRepository {

    /** 新录当户：锁内做身份证未注销唯一校验 + 编号生成 + 落库；重复身份证抛业务异常挡回。 */
    Mono<Pawner> register(Pawner pawner);

    /** 修改当户资料/日常状态：锁内复查身份证唯一（已注销档案不可改）。 */
    Mono<Pawner> modify(Pawner pawner);

    /** 注销：事务内复查名下结清后做条件更新；未结清或已注销抛业务异常。记录保留（留账除名）。 */
    Mono<Void> close(Long id);

    /** 按 id 取档案（含 CLOSED，账留着查得到）；不存在返回空信号。 */
    Mono<Pawner> findById(Long id);

    /** 取档案 + 名下当物/当票对账数（含 CLOSED）。 */
    Mono<PawnerDetail> findDetailById(Long id);

    /** 翻名册：任意条件组合分页，恒不含 CLOSED。 */
    Mono<PageResult<Pawner>> page(int pageNum, int pageSize, PawnerCondition condition);
}
