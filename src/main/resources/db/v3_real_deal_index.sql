-- 실거래가 테이블 인덱스 및 제약조건
-- ddl-auto: validate 환경에서 명시적으로 관리

-- 지오코딩 상태 조회용 인덱스 (배치 처리 시 PENDING/RETRY 상태 조회)
CREATE INDEX idx_rd_geo_status ON real_deals (geo_status);

-- 주소 검색용 인덱스
CREATE INDEX idx_rd_address ON real_deals (address);

-- 중복 방지 유니크 제약 (동일 매물 중복 저장 방지)
ALTER TABLE real_deals
    ADD CONSTRAINT uk_real_deal_dedup
        UNIQUE (property_name, address, deal_amount, deal_date, floor);