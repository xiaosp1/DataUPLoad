-- T1 Step 3: 模拟 dealClientAlarm 批量更新（除最新 1 条外全部置 SOLVED）
-- AlarmRecordServiceImpl.dealClientAlarm 字节码语义：
--   1) 查同 (lineNo, faceNo, type) + solve=UNSOLVED 的所有记录
--   2) 除第一条外，其余 updateBatchById 设 SOLVED
--   3) deal(第一条.uuid)
-- 等价 SQL（按 id DESC 取最新 1 条保留 UNSOLVED，其余置 SOLVED）：
WITH ranked AS (
  SELECT id,
         ROW_NUMBER() OVER (ORDER BY id DESC) AS rn
  FROM alarm_record
  WHERE line_no = 'line1B' AND face_no = 'B1' AND type = 3 AND solve = 2
)
UPDATE alarm_record SET solve = 1
WHERE id IN (SELECT id FROM ranked WHERE rn > 1)
RETURNING id, uuid, solve;
