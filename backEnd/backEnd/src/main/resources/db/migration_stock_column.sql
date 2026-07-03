-- =====================================================================
-- 재고 잔량 컬럼(item.stock_qty) + 낙관적 락(item.version) 도입 마이그레이션
-- =====================================================================
-- 실행 시점: 새 코드 배포 "전에" 각 DB(local MySQL / prod PostgreSQL)에 1회 수동 실행.
--
-- 배경:
--   - 기존에는 재고를 컬럼 없이 매번 집계(입고합 - 승인사용합)로 계산했다.
--   - 이제 item.stock_qty 를 "정답값"으로 두고 입고 +, 사용승인 - 로 갱신한다.
--   - ddl-auto=update 라 컬럼 자체는 앱 기동 시 자동 추가될 수 있으나,
--     그 경우 기존 품목의 stock_qty 가 0 으로만 채워지므로(백필 없음) 데이터가 틀어진다.
--     => 아래 백필 UPDATE 는 반드시 수동으로 실행해야 한다.
--
-- 권장 순서:
--   1) 아래 ALTER 로 컬럼 추가(이미 앱이 자동 추가했다면 건너뜀)
--   2) 아래 백필 UPDATE 로 기존 재고값 채움
--   3) 새 코드 배포/기동
-- =====================================================================


-- ############### MySQL (local) ###############

-- 1) 컬럼 추가 (이미 있으면 이 두 줄은 건너뛴다)
ALTER TABLE item ADD COLUMN stock_qty BIGINT NOT NULL DEFAULT 0;
ALTER TABLE item ADD COLUMN version   BIGINT NOT NULL DEFAULT 0;

-- 2) 백필: stock_qty = 누적 입고수량 - 승인된 사용량
UPDATE item i
LEFT JOIN (
    SELECT pi.item_id AS item_id, COALESCE(SUM(rl.received_qty), 0) AS received
    FROM receipt_line rl
    JOIN po_item pi ON pi.po_item_id = rl.po_item_id
    GROUP BY pi.item_id
) r ON r.item_id = i.item_id
LEFT JOIN (
    SELECT su.item_id AS item_id, COALESCE(SUM(su.usage_qty), 0) AS used
    FROM stock_usage su
    WHERE su.status = 'APPROVED'
    GROUP BY su.item_id
) u ON u.item_id = i.item_id
SET i.stock_qty = COALESCE(r.received, 0) - COALESCE(u.used, 0);


-- ############### PostgreSQL (prod) ###############

-- 1) 컬럼 추가 (IF NOT EXISTS 지원)
ALTER TABLE item ADD COLUMN IF NOT EXISTS stock_qty BIGINT NOT NULL DEFAULT 0;
ALTER TABLE item ADD COLUMN IF NOT EXISTS version   BIGINT NOT NULL DEFAULT 0;

-- 2) 백필: stock_qty = 누적 입고수량 - 승인된 사용량
UPDATE item i
SET stock_qty = COALESCE(r.received, 0) - COALESCE(u.used, 0)
FROM item base
LEFT JOIN (
    SELECT pi.item_id AS item_id, SUM(rl.received_qty) AS received
    FROM receipt_line rl
    JOIN po_item pi ON pi.po_item_id = rl.po_item_id
    GROUP BY pi.item_id
) r ON r.item_id = base.item_id
LEFT JOIN (
    SELECT su.item_id AS item_id, SUM(su.usage_qty) AS used
    FROM stock_usage su
    WHERE su.status = 'APPROVED'
    GROUP BY su.item_id
) u ON u.item_id = base.item_id
WHERE i.item_id = base.item_id;


-- =====================================================================
-- 대사(검증) 쿼리: 컬럼값과 집계값이 어긋나는 품목 찾기 (양쪽 DB 공통)
-- 결과가 0행이면 정상. (앱의 GET /items/{id}/stock/reconcile 과 동일 개념)
-- =====================================================================
-- SELECT i.item_id, i.item_code, i.stock_qty,
--        COALESCE(r.received,0) - COALESCE(u.used,0) AS aggregated
-- FROM item i
-- LEFT JOIN (SELECT pi.item_id, SUM(rl.received_qty) received
--            FROM receipt_line rl JOIN po_item pi ON pi.po_item_id = rl.po_item_id
--            GROUP BY pi.item_id) r ON r.item_id = i.item_id
-- LEFT JOIN (SELECT su.item_id, SUM(su.usage_qty) used
--            FROM stock_usage su WHERE su.status='APPROVED'
--            GROUP BY su.item_id) u ON u.item_id = i.item_id
-- WHERE i.stock_qty <> COALESCE(r.received,0) - COALESCE(u.used,0);
