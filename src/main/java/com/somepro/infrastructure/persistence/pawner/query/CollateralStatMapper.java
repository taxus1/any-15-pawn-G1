package com.somepro.infrastructure.persistence.pawner.query;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 当物计数只读 Mapper：用于按当户统计未了结/在库件数，禁止在此扩展写入。
 */
@Mapper
public interface CollateralStatMapper extends BaseMapper<CollateralStatPO> {

    /**
     * 注销事务内的当前读：锁定该当户名下全部当物行；一条都没有时由 idx_pawner 间隙锁
     * 挡住「注销复查刚数完、别人又给这个当户登记当物」的并发插入。锁随事务释放。
     */
    @Select("SELECT * FROM t_collateral WHERE pawner_id = #{pawnerId} AND del_flag = 0 FOR UPDATE")
    List<CollateralStatPO> selectByPawnerForUpdate(@Param("pawnerId") Long pawnerId);
}
