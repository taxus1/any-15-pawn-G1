package com.somepro.infrastructure.persistence.pawner;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.somepro.infrastructure.persistence.pawner.po.PawnerPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 当户档案 Mapper（基础设施层）。
 *
 * 除 BaseMapper 能力外，两个自定义查询都不是普通 CRUD 能表达的：
 * - GET_LOCK / RELEASE_LOCK：MySQL 命名锁，用来把「同一张身份证」的并发登记串行化，
 *   锁名取身份证 MD5（身份证原文最长 18 位，MD5 后固定 32 位，锁名不超 64 字符上限）；
 * - 取年度最大编号：MAX(pawner_no) 不分 del_flag / status，连已删除、已注销的号也算，
 *   保证编号永不复用、永不落到两个人头上。
 *
 * 阻塞 JDBC API，只能在 boundedElastic 线程上调用（见 PawnerRepositoryImpl）。
 */
@Mapper
public interface PawnerMapper extends BaseMapper<PawnerPO> {

    /** 申请命名锁：1 成功，0 超时，null 异常。命名锁与连接/会话绑定，必须同连接释放。 */
    @Select("SELECT GET_LOCK(#{lockKey}, #{timeoutSeconds})")
    Integer acquireNamedLock(@Param("lockKey") String lockKey,
                             @Param("timeoutSeconds") int timeoutSeconds);

    /** 释放命名锁：1 成功，0 当前连接并未持有（亦视为已释放），null 异常。 */
    @Select("SELECT RELEASE_LOCK(#{lockKey})")
    Integer releaseNamedLock(@Param("lockKey") String lockKey);

    /** 取某年度编号前缀下已用过的最大编号（如 DH-2026-0007），含 CLOSED / del_flag=1 的行。 */
    @Select("SELECT MAX(pawner_no) FROM t_pawner WHERE pawner_no LIKE #{pattern}")
    String selectMaxPawnerNo(@Param("pattern") String pattern);

    /**
     * 以「当前读」方式锁定该身份证名下的未注销档案行：
     * - 已存在未注销行时直接拿到行锁；
     * - 不存在时在 idx_id_card 索引区间加间隙锁，后来的相同身份证插入会被阻塞，
     *   与命名锁形成双保险，保证同身份证并发登记只有一份落库（含「连点两下」）。
     * 必须在事务内调用，锁随事务提交/回滚释放。
     */
    @Select("SELECT * FROM t_pawner WHERE id_card = #{idCard} AND status <> 'CLOSED' AND del_flag = 0 FOR UPDATE")
    List<PawnerPO> selectActiveByIdCardForUpdate(@Param("idCard") String idCard);
}
