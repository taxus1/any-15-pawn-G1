package com.somepro.infrastructure.persistence.pawner;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 当票表只读 Mapper：当户模块只在「对账/注销前检查」时点在当当票数，不做任何写入。
 * 状态口径见 doc/schema/pawn.sql：ACTIVE 在当 / REDEEMED 已赎 / FORFEITED 已绝当 / CANCELLED 已撤销。
 */
@Mapper
public interface PawnTicketQueryMapper {

    /** 名下没走完的当票：在当 ACTIVE。 */
    @Select("SELECT COUNT(*) FROM t_pawn_ticket WHERE pawner_id = #{pawnerId} "
            + "AND status = 'ACTIVE' AND del_flag = 0")
    long countActive(@Param("pawnerId") Long pawnerId);
}
