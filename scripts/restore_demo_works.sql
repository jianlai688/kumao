USE kumao_music;

UPDATE musician_work SET name = '晚风告白', status = 'APPROVED' WHERE id = 14002;
UPDATE mv SET name = '55', audit_status = 'APPROVED', status = 1 WHERE id = 2119452714343837698;

SELECT id, name, status FROM musician_work WHERE id = 14002;
SELECT id, name, status, audit_status FROM mv WHERE id = 2119452714343837698;
