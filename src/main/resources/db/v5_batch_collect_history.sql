CREATE TABLE IF NOT EXISTS batch_deal_collect_history (
    deal_ymd            VARCHAR(6)  NOT NULL PRIMARY KEY,
    status              VARCHAR(20) NOT NULL,
    collected_count     INT         NULL,
    failed_lawd_count   INT         NULL,
    started_at          DATETIME    NULL,
    ended_at            DATETIME    NULL
);

CREATE TABLE IF NOT EXISTS batch_deal_collect_failed_lawd (
    deal_ymd VARCHAR(6)  NOT NULL,
    lawd_cd  VARCHAR(10) NOT NULL,
    PRIMARY KEY (deal_ymd, lawd_cd)
);

CREATE INDEX idx_collect_failed_lawd_deal
    ON batch_deal_collect_failed_lawd (deal_ymd);
