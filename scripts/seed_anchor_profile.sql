USE kumao_music;

INSERT INTO anchor_apply (id, user_id, real_name, id_card, live_category, intro, cover_url, status, anchor_name, avatar)
VALUES (16001, 3, '李甜甜', '4403**********1234', '弹唱', '每天晚上八点，一起听歌聊天', 'https://picsum.photos/seed/anchorcover/800', 'APPROVED', '甜心主播', NULL)
ON DUPLICATE KEY UPDATE status = 'APPROVED';

SELECT id, user_id, anchor_name, live_category, status, intro FROM anchor_apply WHERE id = 16001;
