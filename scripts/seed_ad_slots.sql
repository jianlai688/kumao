USE kumao_music;

INSERT INTO ad_plan (id, advertiser_id, user_id, material_id, material_name, slot, start_time, end_time, target_audience, days, daily_price, amount, payment_status, paid_time, status)
VALUES
 (19003, 17001, 5, 18002, '耳机产品视频广告', 'LIVE', '2026-11-01 00:00:00', '2026-12-31 23:59:59', '全年龄', 30, 300.00, 9000.00, 'PAID', '2026-11-01 00:00:00', 'RUNNING'),
 (19004, 17001, 5, 18003, '夏日音乐节推荐', 'PLAYER', '2026-11-01 00:00:00', '2026-12-31 23:59:59', '全年龄', 30, 200.00, 6000.00, 'PAID', '2026-11-01 00:00:00', 'RUNNING')
ON DUPLICATE KEY UPDATE status='RUNNING', start_time=VALUES(start_time), end_time=VALUES(end_time), payment_status='PAID';

SELECT id, slot, material_name, status, payment_status FROM ad_plan WHERE id IN (19003, 19004);
