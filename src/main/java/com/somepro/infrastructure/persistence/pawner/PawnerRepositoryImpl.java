package com.somepro.infrastructure.persistence.pawner;

import cn.hutool.core.util.IdUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageHelper;
import com.somepro.common.exception.BizException;
import com.somepro.domain.pawner.model.Pawner;
import com.somepro.domain.pawner.model.PawnerAssets;
import com.somepro.domain.pawner.model.PawnerCondition;
import com.somepro.domain.pawner.model.PawnerDetail;
import com.somepro.domain.pawner.model.PawnerStatus;
import com.somepro.domain.pawner.repository.PawnerRepository;
import com.somepro.domain.shared.model.PageResult;
import com.somepro.infrastructure.config.ReactiveOperatorContext;
import com.somepro.infrastructure.persistence.audit.AuditContextHolder;
import com.somepro.infrastructure.persistence.pawner.converter.PawnerPoConverter;
import com.somepro.infrastructure.persistence.pawner.po.PawnerPO;
import com.somepro.infrastructure.persistence.pawner.query.CollateralStatMapper;
import com.somepro.infrastructure.persistence.pawner.query.CollateralStatPO;
import com.somepro.infrastructure.persistence.pawner.query.PawnTicketStatMapper;
import com.somepro.infrastructure.persistence.pawner.query.PawnTicketStatPO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Year;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 当户仓储适配器（基础设施层）——本模块并发正确性的核心。
 *
 * 三件必须和存储层强绑定的事：
 *
 * 1. 身份证「未注销唯一」+ 并发登记只落一份
 *    表上只有普通索引（同身份证允许有多条历史 CLOSED），唯一性靠应用侧双保险：
 *    a) MySQL 命名锁（GET_LOCK）按身份证串行化同一身份证的登记/改证 —— 覆盖「连接快进快出、
 *       事务还没提交」的窗口（命名锁随锁名全局生效，不依赖某个事务的行锁）；
 *    b) 事务内再用 SELECT ... FOR UPDATE 当前读：有未注销行则直接拿行锁，没有则由 idx_id_card
 *       上的间隙锁挡住后来者的相同身份证插入。
 *    两层都在，同一身份证连点两下、高并发涌入，最终只有一条 NORMAL/FROZEN 档案。
 *    已注销（CLOSED）档案不参与判定，注销后拿同一身份证重开必须放行。
 *
 * 2. 当户编号 DH-年-四位序号：年度内 MAX(pawner_no)+1，连 CLOSED / del_flag=1 的号一起算，
 *    号永不复用；uk_pawner_no 再兜底，撞了重取重试。
 *
 * 3. 注销：事务内复查名下当物/当票结清，并以「当前状态 <> CLOSED」为条件做更新，
 *    防住复查后、落库前又被开出当票/已被并发注销的窗口。注销只改状态，物理行保留（留账除名）。
 */
@Slf4j
@Repository
public class PawnerRepositoryImpl implements PawnerRepository {

    /** 命名锁等待秒数：同身份证前一个登记正常很快完成，给 10 秒足够，超过即按系统繁忙挡回。 */
    private static final int LOCK_TIMEOUT_SECONDS = 10;
    private static final String LOCK_PREFIX = "pawner:idcard:";

    private final PawnerMapper pawnerMapper;
    private final CollateralStatMapper collateralStatMapper;
    private final PawnTicketStatMapper pawnTicketStatMapper;
    private final TransactionTemplate transactionTemplate;

    public PawnerRepositoryImpl(PawnerMapper pawnerMapper,
                                CollateralStatMapper collateralStatMapper,
                                PawnTicketStatMapper pawnTicketStatMapper,
                                TransactionTemplate transactionTemplate) {
        this.pawnerMapper = pawnerMapper;
        this.collateralStatMapper = collateralStatMapper;
        this.pawnTicketStatMapper = pawnTicketStatMapper;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public Mono<Pawner> register(Pawner pawner) {
        return blocking(() -> {
            String lockKey = idCardLockKey(pawner.getIdCard());
            acquireLock(lockKey);
            try {
                // uk_pawner_no 理论上只会在极端并发取号时冲突；每次尝试都在新事务里重取号，最多 3 次
                DuplicateKeyException last = null;
                for (int attempt = 1; attempt <= 3; attempt++) {
                    try {
                        return transactionTemplate.execute(status -> doRegisterInTx(pawner));
                    } catch (DuplicateKeyException e) {
                        last = e;
                    }
                }
                throw new BizException("当户编号生成冲突，请重试");
            } finally {
                releaseLock(lockKey);
            }
        });
    }

    @Override
    public Mono<Pawner> modify(Pawner pawner) {
        return blocking(() -> {
            String lockKey = idCardLockKey(pawner.getIdCard());
            acquireLock(lockKey);
            try {
                return transactionTemplate.execute(status -> {
                    // 身份证未注销唯一复查（排除自己）；FOR UPDATE 与锁共同挡住并发改证
                    Long activeCount = pawnerMapper.selectCount(
                            Wrappers.<PawnerPO>lambdaQuery()
                                    .eq(PawnerPO::getIdCard, pawner.getIdCard())
                                    .ne(PawnerPO::getStatus, PawnerStatus.CLOSED.name())
                                    .ne(PawnerPO::getId, pawner.getId()));
                    if (activeCount != null && activeCount > 0) {
                        throw new BizException("该身份证已存在未注销的当户档案，不能重复建档");
                    }
                    pawnerMapper.selectActiveByIdCardForUpdate(pawner.getIdCard());
                    PawnerPO po = PawnerPoConverter.toPo(pawner);
                    int updated = pawnerMapper.update(po,
                            Wrappers.<PawnerPO>lambdaUpdate()
                                    .eq(PawnerPO::getId, pawner.getId())
                                    .ne(PawnerPO::getStatus, PawnerStatus.CLOSED.name()));
                    if (updated == 0) {
                        throw new BizException("当户已被注销或不存在，修改未生效，请刷新后重试");
                    }
                    PawnerPO fresh = pawnerMapper.selectById(pawner.getId());
                    return PawnerPoConverter.toDomain(fresh);
                });
            } finally {
                releaseLock(lockKey);
            }
        });
    }

    @Override
    public Mono<Void> close(Long id) {
        return blocking(() -> {
            transactionTemplate.executeWithoutResult(status -> {
                PawnerPO po = pawnerMapper.selectById(id);
                if (po == null) {
                    throw new BizException("当户不存在或已删除");
                }
                if (PawnerStatus.CLOSED.name().equals(po.getStatus())) {
                    throw new BizException("该当户已注销，无需重复注销");
                }
                // 当前读锁住该当户名下当物/当票行与间隙，再计数：
                // 防住「复查数到 0、提交前又被登记当物/开出在当当票」的并发空档
                collateralStatMapper.selectByPawnerForUpdate(id);
                pawnTicketStatMapper.selectByPawnerForUpdate(id);
                PawnerAssets assets = countAssets(id);
                if (!assets.allSettled()) {
                    throw new BizException("注销失败：名下尚有 " + assets.pledgedCount() + " 件未了结当物（其中在库 "
                            + assets.inStockCount() + " 件）、" + assets.activeTicketCount()
                            + " 张在当当票，结清后才能注销");
                }
                // 条件更新：只在仍非 CLOSED 时落刀；若复查后状态被并发改动，更新行数为 0，整事务回滚。
                // 用最小实体走 update(T, wrapper) 而不是 update(null, set...)：
                // 前者会触发 AutoFillMetaObjectHandler 填 updateBy/updateTime（审计约定），
                // 实体只给 status，其余列不在 SQL 里，不会误伤。
                PawnerPO closing = new PawnerPO();
                closing.setStatus(PawnerStatus.CLOSED.name());
                int updated = pawnerMapper.update(closing,
                        Wrappers.<PawnerPO>lambdaUpdate()
                                .eq(PawnerPO::getId, id)
                                .ne(PawnerPO::getStatus, PawnerStatus.CLOSED.name()));
                if (updated == 0) {
                    throw new BizException("当户状态已变动，注销未生效，请刷新后重试");
                }
            });
            return Boolean.TRUE;
        }).then();
    }

    @Override
    public Mono<Pawner> findById(Long id) {
        return blocking(() -> {
            PawnerPO po = pawnerMapper.selectById(id);
            return po == null ? null : PawnerPoConverter.toDomain(po);
        });
    }

    @Override
    public Mono<PawnerDetail> findDetailById(Long id) {
        return blocking(() -> {
            PawnerPO po = pawnerMapper.selectById(id);
            if (po == null) {
                return null;
            }
            return new PawnerDetail(PawnerPoConverter.toDomain(po), countAssets(id));
        });
    }

    @Override
    public Mono<PageResult<Pawner>> page(int pageNum, int pageSize, PawnerCondition condition) {
        return this.<PageResult<Pawner>>blocking(() -> {
            try {
                // 按 id 升序给名册一个稳定次序，配合 PageHelper 保证翻页不重不漏（两页不会出现同一个人）
                PageHelper.startPage(pageNum, pageSize);
                var wrapper = Wrappers.<PawnerPO>lambdaQuery()
                        // 注销即除名：名册恒不返回 CLOSED（历史账仍在表里、详情可查）
                        .ne(PawnerPO::getStatus, PawnerStatus.CLOSED.name())
                        .like(condition.name() != null, PawnerPO::getName, condition.name())
                        .eq(condition.idCard() != null, PawnerPO::getIdCard, condition.idCard())
                        .like(condition.phone() != null, PawnerPO::getPhone, condition.phone())
                        .eq(condition.status() != null, PawnerPO::getStatus, condition.status() == null
                                ? null : condition.status().name())
                        .orderByAsc(PawnerPO::getId);
                List<PawnerPO> rows = pawnerMapper.selectList(wrapper);
                long total = rows instanceof com.github.pagehelper.Page
                        ? ((com.github.pagehelper.Page<?>) rows).getTotal()
                        : rows.size();
                List<Pawner> content = rows.stream()
                        .map(PawnerPoConverter::toDomain)
                        .collect(Collectors.toList());
                return new PageResult<>(content, total, pageNum, pageSize);
            } finally {
                // PageHelper 靠 ThreadLocal 传分页参数，必须清理，否则污染线程池下一次调用
                PageHelper.clearPage();
            }
        });
    }

    // ------------------------------------------------------------------
    // 事务内部动作
    // ------------------------------------------------------------------

    private Pawner doRegisterInTx(Pawner pawner) {
        // 当前读锁定同身份证档案：未注销行非空 → 挡回；为空 → 间隙锁挡住并发的相同身份证插入
        List<PawnerPO> active = pawnerMapper.selectActiveByIdCardForUpdate(pawner.getIdCard());
        if (active != null && !active.isEmpty()) {
            throw new BizException("该身份证已存在未注销的当户档案，不能重复建档");
        }
        pawner.setPawnerNo(nextPawnerNo());
        PawnerPO po = PawnerPoConverter.toPo(pawner);
        po.setId(IdUtil.getSnowflakeNextId());
        pawnerMapper.insert(po);
        return PawnerPoConverter.toDomain(po);
    }

    /**
     * 生成年度序号编号：DH-2026-0001 起。MAX 取值包含已注销/已删除的行，号永不复用、
     * 也不会落到两个人头上；uk_pawner_no 是最后兜底。
     */
    private String nextPawnerNo() {
        int year = Year.now().getValue();
        String prefix = "DH-" + year + "-";
        String max = pawnerMapper.selectMaxPawnerNo(prefix + "%");
        long seq = 1L;
        if (max != null && max.startsWith(prefix)) {
            try {
                seq = Long.parseLong(max.substring(prefix.length())) + 1;
            } catch (NumberFormatException ignore) {
                seq = 1L;
            }
        }
        return prefix + String.format("%04d", seq);
    }

    /** 实时统计名下未了结当物/在库件数/在当当票；@TableLogic 自动只算 del_flag=0 的行。 */
    private PawnerAssets countAssets(Long pawnerId) {
        long inStock = collateralStatMapper.selectCount(
                Wrappers.<CollateralStatPO>lambdaQuery()
                        .eq(CollateralStatPO::getPawnerId, pawnerId)
                        .eq(CollateralStatPO::getStatus, "IN_STOCK"));
        long pawned = collateralStatMapper.selectCount(
                Wrappers.<CollateralStatPO>lambdaQuery()
                        .eq(CollateralStatPO::getPawnerId, pawnerId)
                        .eq(CollateralStatPO::getStatus, "PAWNED"));
        long activeTickets = pawnTicketStatMapper.selectCount(
                Wrappers.<PawnTicketStatPO>lambdaQuery()
                        .eq(PawnTicketStatPO::getPawnerId, pawnerId)
                        .eq(PawnTicketStatPO::getStatus, "ACTIVE"));
        return new PawnerAssets(inStock + pawned, inStock, activeTickets);
    }

    // ------------------------------------------------------------------
    // MySQL 命名锁（按身份证串行化）
    // ------------------------------------------------------------------

    private void acquireLock(String lockKey) {
        Integer result = pawnerMapper.acquireNamedLock(lockKey, LOCK_TIMEOUT_SECONDS);
        if (result == null || result != 1) {
            throw new BizException("系统繁忙：身份证登记锁暂不可用，请稍后重试");
        }
    }

    private void releaseLock(String lockKey) {
        try {
            pawnerMapper.releaseNamedLock(lockKey);
        } catch (Exception e) {
            // 释放失败只记日志不外抛：命名锁随连接归还/会话结束也会释放，不应因此盖掉业务结果
            log.warn("释放当户身份证命名锁失败 lockKey={}", lockKey, e);
        }
    }

    private String idCardLockKey(String idCard) {
        return LOCK_PREFIX + SecureUtil.md5(idCard);
    }

    // ------------------------------------------------------------------
    // 响应式 × 阻塞 JDBC 桥接（与 demo 模块同一约定）：
    // 先从 Reactor Context 取操作人，再切到 boundedElastic 执行 JDBC
    // ------------------------------------------------------------------

    private <T> Mono<T> blocking(Supplier<T> supplier) {
        return Mono.deferContextual(ctx -> {
            String operator = ReactiveOperatorContext.getOperator(ctx);
            return Mono.fromCallable(() -> {
                AuditContextHolder.setOperator(operator);
                try {
                    return supplier.get();
                } finally {
                    AuditContextHolder.clear();
                }
            }).subscribeOn(Schedulers.boundedElastic());
        });
    }
}
