USE kumao_music;

ALTER TABLE anchor_apply
  ADD COLUMN anchor_name VARCHAR(64) NULL COMMENT '主播艺名',
  ADD COLUMN avatar VARCHAR(500) NULL COMMENT '主播头像';

SELECT id, user_id, real_name, anchor_name, avatar, live_category, status FROM anchor_apply LIMIT 5;
