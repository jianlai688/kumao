-- ============================================================
-- 演示账号密码初始化
-- 说明：应用支持 {noop} 前缀的明文密码，同时 02-data.sql 中的
--       演示账号会以 {noop} 形式保存，便于直接登录演示。
--       如需改为 BCrypt，请启动后调用 /auth/password/change 修改。
-- 账号：
--   admin      / admin123   管理员
--   zhangsan   / 123456     普通用户
--   anchor01   / 123456     主播
--   musician01 / 123456     音乐人
--   adman01    / 123456     广告制作人
--   vipuser    / 123456     VIP 会员
-- ============================================================
USE `kumao_music`;

UPDATE `sys_user` SET `password` = '{noop}123456' WHERE `username` IN
    ('zhangsan', 'anchor01', 'musician01', 'adman01', 'vipuser');
UPDATE `sys_user` SET `password` = '{noop}admin123' WHERE `username` = 'admin';
