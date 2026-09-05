-- MallX 本地开发账号初始化脚本（MySQL 8）。
-- 用途：删除2026-09-04验收创建的四个测试账号，然后初始化三个固定本地账号。
-- 执行前请备份mall数据库；请勿在生产环境使用固定弱密码。
-- 当前系统使用手机号登录，所以admin、user1、user2是昵称，登录账号是下面的手机号。
-- 执行前必须先执行mall-admin/src/main/resources/db/data-admin-rbac.sql，保证SYSTEM_ADMIN及其权限已初始化。

START TRANSACTION;

-- 四个旧账号ID来自《MallX_后端真实环境验收报告_20260904》；只按明确ID删除，避免误删其他账号。
-- 先删除身份附属数据和角色关系，再删除账号主表。
DELETE FROM auth_account_role
WHERE account_id IN (
    2095844983025197058,
    2095845061920055298,
    2095845063002185730,
    2095845063488724993
);

DELETE FROM user_address
WHERE user_id IN (
    2095844983025197058,
    2095845061920055298,
    2095845063002185730,
    2095845063488724993
);

DELETE FROM user_membership_log
WHERE user_id IN (
          2095844983025197058,
          2095845061920055298,
          2095845063002185730,
          2095845063488724993
      )
   OR operator_id IN (
          2095844983025197058,
          2095845061920055298,
          2095845063002185730,
          2095845063488724993
      );

DELETE FROM user_membership
WHERE user_id IN (
    2095844983025197058,
    2095845061920055298,
    2095845063002185730,
    2095845063488724993
);

DELETE FROM user_profile
WHERE user_id IN (
    2095844983025197058,
    2095845061920055298,
    2095845063002185730,
    2095845063488724993
);

DELETE FROM auth_account
WHERE id IN (
    2095844983025197058,
    2095845061920055298,
    2095845063002185730,
    2095845063488724993
);

-- 使用小号保留ID，避免与MyBatis-Plus生成的雪花ID冲突。
-- password_hash是由项目同款BCryptPasswordEncoder为明文123456生成的摘要，数据库不保存明文密码。
INSERT INTO auth_account (id, phone, password_hash, status)
SELECT 10001, '18800000000', '$2a$10$AthLfsQA8S8Ghc3wEX4rVeWslClhcDErb5jmYQSn6fUfQSWNRIbei', 1
WHERE NOT EXISTS (
    SELECT 1 FROM auth_account WHERE id = 10001 OR phone = '18800000000'
);

INSERT INTO auth_account (id, phone, password_hash, status)
SELECT 10002, '18800000001', '$2a$10$AthLfsQA8S8Ghc3wEX4rVeWslClhcDErb5jmYQSn6fUfQSWNRIbei', 1
WHERE NOT EXISTS (
    SELECT 1 FROM auth_account WHERE id = 10002 OR phone = '18800000001'
);

INSERT INTO auth_account (id, phone, password_hash, status)
SELECT 10003, '18800000002', '$2a$10$AthLfsQA8S8Ghc3wEX4rVeWslClhcDErb5jmYQSn6fUfQSWNRIbei', 1
WHERE NOT EXISTS (
    SELECT 1 FROM auth_account WHERE id = 10003 OR phone = '18800000002'
);

-- 重复执行时，只有ID和手机号均匹配才重置密码及状态；不会覆盖发生冲突的其他账号。
UPDATE auth_account
SET password_hash = '$2a$10$AthLfsQA8S8Ghc3wEX4rVeWslClhcDErb5jmYQSn6fUfQSWNRIbei', status = 1
WHERE (id = 10001 AND phone = '18800000000')
   OR (id = 10002 AND phone = '18800000001')
   OR (id = 10003 AND phone = '18800000002');

SET @admin_id = (SELECT id FROM auth_account WHERE id = 10001 AND phone = '18800000000');
SET @user1_id = (SELECT id FROM auth_account WHERE id = 10002 AND phone = '18800000001');
SET @user2_id = (SELECT id FROM auth_account WHERE id = 10003 AND phone = '18800000002');

INSERT INTO user_profile (user_id, nickname, gender)
SELECT @admin_id, 'admin', 0
WHERE @admin_id IS NOT NULL
ON DUPLICATE KEY UPDATE nickname = 'admin';

INSERT INTO user_profile (user_id, nickname, gender)
SELECT @user1_id, 'user1', 0
WHERE @user1_id IS NOT NULL
ON DUPLICATE KEY UPDATE nickname = 'user1';

INSERT INTO user_profile (user_id, nickname, gender)
SELECT @user2_id, 'user2', 0
WHERE @user2_id IS NOT NULL
ON DUPLICATE KEY UPDATE nickname = 'user2';

-- 三个固定账号先清空角色；普通用户保持无角色，admin只关联SYSTEM_ADMIN。
DELETE FROM auth_account_role
WHERE account_id IN (@admin_id, @user1_id, @user2_id);

INSERT INTO auth_account_role (account_id, role_id)
SELECT @admin_id, role.id
FROM auth_role role
WHERE @admin_id IS NOT NULL
  AND role.role_code = 'SYSTEM_ADMIN'
  AND role.status = 1;

COMMIT;

-- 执行结果检查：应返回三个账号；admin有SYSTEM_ADMIN，user1和user2角色为NULL。
SELECT account.id,
       account.phone,
       profile.nickname,
       account.status,
       GROUP_CONCAT(role.role_code ORDER BY role.role_code) AS role_codes
FROM auth_account account
LEFT JOIN user_profile profile ON profile.user_id = account.id
LEFT JOIN auth_account_role account_role ON account_role.account_id = account.id
LEFT JOIN auth_role role ON role.id = account_role.role_id
WHERE account.id IN (10001, 10002, 10003)
GROUP BY account.id, account.phone, profile.nickname, account.status
ORDER BY account.id;

-- 旧账号检查：应返回0。
SELECT COUNT(*) AS old_account_count
FROM auth_account
WHERE id IN (
    2095844983025197058,
    2095845061920055298,
    2095845063002185730,
    2095845063488724993
);
