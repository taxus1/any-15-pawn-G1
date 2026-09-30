package com.somepro.infrastructure.persistence.pawner.query;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 当票计数只读 Mapper：用于按当户统计在当当票数，禁止在此扩展写入。
 */
@Mapper
public interface PawnTicketStatMapper extends BaseMapper<PawnTicketStatPO> {

    /**
     * 注销事务内的当前读：锁定该当户名下全部当票行；没有时由 idx_pawner 间隙锁
     * 挡住并发开新当票。锁随事务释放。
     */
    @Select("SELECT * FROM t_pawn_ticket WHERE pawner_id = #{pawnerId} AND del_flag = 0 FOR UPDATE")
    List<PawnTicketStatPO> selectByPawnerForUpdate(@Param("pawnerId") Long pawnerId);
}
