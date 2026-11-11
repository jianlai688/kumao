USE kumao_music;

ALTER TABLE sys_user
  ADD COLUMN gender TINYINT NOT NULL DEFAULT 0 COMMENT '0未知 1男 2女',
  ADD COLUMN birthday DATE NULL COMMENT '生日',
  ADD COLUMN region VARCHAR(64) NULL COMMENT '地区',
  ADD COLUMN email VARCHAR(128) NULL COMMENT '邮箱';

SELECT id, username, nickname, gender, birthday, region, email FROM sys_user LIMIT 3;
