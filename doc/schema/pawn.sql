-- any-15-pawn · 典当行单业务线 · 建表 SQL
-- 字符集 utf8mb4，时区 Asia/Shanghai。create 阶段建好，模型只写业务代码，不碰建表。
-- 列名即契约：del_flag 由 @TableLogic 自动拼接（查询带 del_flag=0，删除置 1），
-- create_by/update_by/create_time/update_time 由 AutoFillMetaObjectHandler 自动填充，业务代码不要手写。
-- 主键 id 由应用侧雪花分配（IdType.INPUT），不依赖自增。

-- 1) 当户档案
CREATE TABLE IF NOT EXISTS t_pawner (
    id          BIGINT      NOT NULL PRIMARY KEY COMMENT '雪花 ID，应用层分配',
    pawner_no   VARCHAR(32) NOT NULL COMMENT '当户编号，全局唯一（如 DH-2026-0001）',
    name        VARCHAR(64) NOT NULL COMMENT '当户姓名',
    id_card     VARCHAR(18) NOT NULL COMMENT '身份证号，未注销档案内唯一',
    phone       VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
    address     VARCHAR(255) DEFAULT NULL COMMENT '联系地址',
    status      VARCHAR(16) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL 正常 / FROZEN 冻结 / CLOSED 注销',
    del_flag    TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    create_by   VARCHAR(64) DEFAULT NULL,
    create_time DATETIME    DEFAULT NULL,
    update_by   VARCHAR(64) DEFAULT NULL,
    update_time DATETIME    DEFAULT NULL,
    UNIQUE KEY uk_pawner_no (pawner_no),
    KEY idx_id_card (id_card),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='当户档案';

-- 2) 当物登记
CREATE TABLE IF NOT EXISTS t_collateral (
    id              BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花 ID，应用层分配',
    item_no         VARCHAR(32)   NOT NULL COMMENT '当物编号，全局唯一（如 DW-2026-0001）',
    pawner_id       BIGINT        NOT NULL COMMENT '所属当户 id（t_pawner.id）',
    category        VARCHAR(16)   NOT NULL COMMENT '类别 JEWELRY 珠宝首饰 / WATCH 名表 / ELECTRONICS 电子产品 / VEHICLE 机动车 / OTHER 其他',
    item_name       VARCHAR(128)  NOT NULL COMMENT '当物名称',
    brand           VARCHAR(64)   DEFAULT NULL COMMENT '品牌或成色说明',
    condition_level VARCHAR(16)   NOT NULL DEFAULT 'GOOD' COMMENT '品相 NEW 全新 / GOOD 良好 / FAIR 一般 / POOR 较差',
    appraised_value DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '评估价值（元）',
    status          VARCHAR(16)   NOT NULL DEFAULT 'IN_STOCK' COMMENT 'IN_STOCK 在库 / PAWNED 已典当 / REDEEMED 已赎回 / FORFEITED 已绝当 / RELEASED 已退还',
    del_flag        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    create_by       VARCHAR(64)   DEFAULT NULL,
    create_time     DATETIME      DEFAULT NULL,
    update_by       VARCHAR(64)   DEFAULT NULL,
    update_time     DATETIME      DEFAULT NULL,
    UNIQUE KEY uk_item_no (item_no),
    KEY idx_pawner (pawner_id),
    KEY idx_status (status),
    KEY idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='当物登记';

-- 3) 当票
CREATE TABLE IF NOT EXISTS t_pawn_ticket (
    id              BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花 ID，应用层分配',
    ticket_no       VARCHAR(32)   NOT NULL COMMENT '当票号，全局唯一（如 DP-2026-0001）',
    pawner_id       BIGINT        NOT NULL COMMENT '当户 id（t_pawner.id）',
    collateral_id   BIGINT        NOT NULL COMMENT '当物 id（t_collateral.id）',
    category        VARCHAR(16)   NOT NULL COMMENT '类别快照，随当物带出',
    pawn_amount     DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '当金（元）',
    appraised_value DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '折当时估值快照（元）',
    monthly_rate    DECIMAL(8,5)  NOT NULL DEFAULT 0.00000 COMMENT '月利率快照',
    service_rate    DECIMAL(8,5)  NOT NULL DEFAULT 0.00000 COMMENT '月综合费率快照',
    start_date      DATE          NOT NULL COMMENT '起当日期',
    due_date        DATE          NOT NULL COMMENT '到期日期',
    term_months     INT           NOT NULL DEFAULT 1 COMMENT '当期月数',
    status          VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE 在当 / REDEEMED 已赎 / FORFEITED 已绝当 / CANCELLED 已撤销',
    del_flag        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    create_by       VARCHAR(64)   DEFAULT NULL,
    create_time     DATETIME      DEFAULT NULL,
    update_by       VARCHAR(64)   DEFAULT NULL,
    update_time     DATETIME      DEFAULT NULL,
    UNIQUE KEY uk_ticket_no (ticket_no),
    KEY idx_pawner (pawner_id),
    KEY idx_collateral (collateral_id),
    KEY idx_status (status),
    KEY idx_due (due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='当票';

-- 4) 续当登记
CREATE TABLE IF NOT EXISTS t_pawn_renew (
    id             BIGINT      NOT NULL PRIMARY KEY COMMENT '雪花 ID，应用层分配',
    renew_no       VARCHAR(32) NOT NULL COMMENT '续当单号，全局唯一（如 XD-2026-0001）',
    ticket_id      BIGINT      NOT NULL COMMENT '当票 id（t_pawn_ticket.id）',
    old_due_date   DATE        DEFAULT NULL COMMENT '续当前到期日期',
    new_due_date   DATE        DEFAULT NULL COMMENT '续当后到期日期',
    extend_months  INT         NOT NULL DEFAULT 0 COMMENT '本次顺延月数',
    renewed_at     DATETIME    DEFAULT NULL COMMENT '续当办理时刻',
    del_flag       TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    create_by      VARCHAR(64) DEFAULT NULL,
    create_time    DATETIME    DEFAULT NULL,
    update_by      VARCHAR(64) DEFAULT NULL,
    update_time    DATETIME    DEFAULT NULL,
    UNIQUE KEY uk_renew_no (renew_no),
    KEY idx_ticket (ticket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='续当登记';

-- 5) 赎当结算
CREATE TABLE IF NOT EXISTS t_pawn_redeem (
    id           BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花 ID，应用层分配',
    redeem_no    VARCHAR(32)   NOT NULL COMMENT '赎当单号，全局唯一（如 SD-2026-0001）',
    ticket_id    BIGINT        NOT NULL COMMENT '当票 id（t_pawn_ticket.id）',
    redeemed_at  DATETIME      DEFAULT NULL COMMENT '赎当办理时刻',
    used_days    INT           NOT NULL DEFAULT 0 COMMENT '计费天数（起当日期到赎当日，至少 1）',
    fee_amount   DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '利息与综合费合计（元）',
    total_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '应还总额 = 当金 + 费用（元）',
    del_flag     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    create_by    VARCHAR(64)   DEFAULT NULL,
    create_time  DATETIME      DEFAULT NULL,
    update_by    VARCHAR(64)   DEFAULT NULL,
    update_time  DATETIME      DEFAULT NULL,
    UNIQUE KEY uk_redeem_no (redeem_no),
    KEY idx_ticket (ticket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='赎当结算';

-- 6) 绝当处置
CREATE TABLE IF NOT EXISTS t_pawn_forfeit (
    id             BIGINT        NOT NULL PRIMARY KEY COMMENT '雪花 ID，应用层分配',
    forfeit_no     VARCHAR(32)   NOT NULL COMMENT '绝当处置单号，全局唯一（如 JD-2026-0001）',
    ticket_id      BIGINT        NOT NULL COMMENT '当票 id（t_pawn_ticket.id）',
    collateral_id  BIGINT        NOT NULL COMMENT '当物 id（t_collateral.id）',
    forfeited_at   DATETIME      DEFAULT NULL COMMENT '绝当处置时刻',
    dispose_method VARCHAR(16)   NOT NULL DEFAULT 'AUCTION' COMMENT '处置方式 AUCTION 拍卖 / CONSIGN 变卖 / WRITE_OFF 核销',
    recover_amount DECIMAL(14,2) NOT NULL DEFAULT 0.00 COMMENT '处置回款（元）',
    del_flag       TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    create_by      VARCHAR(64)   DEFAULT NULL,
    create_time    DATETIME      DEFAULT NULL,
    update_by      VARCHAR(64)   DEFAULT NULL,
    update_time    DATETIME      DEFAULT NULL,
    UNIQUE KEY uk_forfeit_no (forfeit_no),
    KEY idx_ticket (ticket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绝当处置';

-- 7) 费率配置
CREATE TABLE IF NOT EXISTS t_pawn_rate (
    id             BIGINT       NOT NULL PRIMARY KEY COMMENT '雪花 ID，应用层分配',
    category       VARCHAR(16)  NOT NULL COMMENT '适用类别，一个类别一行',
    monthly_rate   DECIMAL(8,5) NOT NULL DEFAULT 0.00000 COMMENT '月利率',
    service_rate   DECIMAL(8,5) NOT NULL DEFAULT 0.00000 COMMENT '月综合费率',
    max_loan_ratio DECIMAL(5,4) NOT NULL DEFAULT 0.0000 COMMENT '折当率上限（0~1，当金不得超过估值×该值）',
    status         VARCHAR(16)  NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED 启用 / DISABLED 停用',
    del_flag       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 正常 / 1 已删除',
    create_by      VARCHAR(64)  DEFAULT NULL,
    create_time    DATETIME     DEFAULT NULL,
    update_by      VARCHAR(64)  DEFAULT NULL,
    update_time    DATETIME     DEFAULT NULL,
    UNIQUE KEY uk_category (category),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='费率配置';
