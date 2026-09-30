package com.somepro.infrastructure.persistence.pawner;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 当物表只读 Mapper：当户模块只在「对账/注销前检查」时点当物数，不做任何写入。
 * 状态口径见 doc/schema/pawn.sql：IN_STOCK 在库 / PAWNED 已典当 / REDEEMED 已赎 / FORFEITED 已绝当 / RELEASED 已退还。
 */
@Mapper
public interface CollateralQueryMapper {

    /** 名下还押在行里（未了结）的当物：在库 + 已典当。 */
    @Select("SELECT COUNT(*) FROM t_collateral WHERE pawner_id = #{pawnerId} "
            + "AND status IN ('IN_STOCK', 'PAWNED') AND del_flag = 0")
    long countHeld(@Param("pawnerId") Long pawnerId);

    /** 名下还躺在库里的当物。 */
    @Select("SELECT COUNT(*) FROM t_collateral WHERE pawner_id = #{pawnerId} "
            + "AND status = 'IN_STOCK' AND del_flag = 0")
    long countInStock(@Param("pawnerId") Long pawnerId);
}
