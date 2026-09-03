-- 先执行mall-auth建表脚本和schema-admin.sql，再执行本脚本。
-- 仅初始化角色/权限目录与角色授权，不创建账号，不给任何账号分配管理员。
-- 固定ID为本脚本保留；若库中相同ID属于其他编码，请先调整ID，禁止覆盖现有身份。
-- 按编码判断是否已存在，已存在记录保持原ID及状态，不重置已有自定义角色。
START TRANSACTION;
INSERT INTO auth_permission(id, permission_code, permission_name, resource)
SELECT 910001, 'admin:access', '后台准入', '/admin/**'
WHERE NOT EXISTS (SELECT 1 FROM auth_permission WHERE permission_code = 'admin:access');
INSERT INTO auth_permission(id, permission_code, permission_name, resource)
SELECT 910002, 'dashboard:read', '运营数据查询', '/admin/dashboard/**'
WHERE NOT EXISTS (SELECT 1 FROM auth_permission WHERE permission_code = 'dashboard:read');
INSERT INTO auth_permission(id, permission_code, permission_name, resource)
SELECT 910003, 'product:manage', '分类品牌商品管理', '/admin/products/**'
WHERE NOT EXISTS (SELECT 1 FROM auth_permission WHERE permission_code = 'product:manage');
INSERT INTO auth_permission(id, permission_code, permission_name, resource)
SELECT 910004, 'inventory:manage', '库存查询与入库', '/admin/inventory/**'
WHERE NOT EXISTS (SELECT 1 FROM auth_permission WHERE permission_code = 'inventory:manage');
INSERT INTO auth_permission(id, permission_code, permission_name, resource)
SELECT 910005, 'order:manage', '订单查询与履约', '/admin/orders/**'
WHERE NOT EXISTS (SELECT 1 FROM auth_permission WHERE permission_code = 'order:manage');
INSERT INTO auth_permission(id, permission_code, permission_name, resource)
SELECT 910006, 'coupon:manage', '优惠券模板管理', '/admin/coupon-templates/**'
WHERE NOT EXISTS (SELECT 1 FROM auth_permission WHERE permission_code = 'coupon:manage');
INSERT INTO auth_permission(id, permission_code, permission_name, resource)
SELECT 910007, 'after-sale:manage', '售后审核退款', '/admin/after-sales/**'
WHERE NOT EXISTS (SELECT 1 FROM auth_permission WHERE permission_code = 'after-sale:manage');
INSERT INTO auth_permission(id, permission_code, permission_name, resource)
SELECT 910008, 'user:manage', '用户查询与启停', '/admin/users/**'
WHERE NOT EXISTS (SELECT 1 FROM auth_permission WHERE permission_code = 'user:manage');
INSERT INTO auth_permission(id, permission_code, permission_name, resource)
SELECT 910009, 'rbac:manage', '角色权限管理', '/admin/rbac/**'
WHERE NOT EXISTS (SELECT 1 FROM auth_permission WHERE permission_code = 'rbac:manage');
INSERT INTO auth_permission(id, permission_code, permission_name, resource)
SELECT 910010, 'audit:read', '后台审计查询', '/admin/operation-logs/**'
WHERE NOT EXISTS (SELECT 1 FROM auth_permission WHERE permission_code = 'audit:read');
INSERT INTO auth_role(id, role_code, role_name, status)
SELECT 920001, 'SYSTEM_ADMIN', '系统管理员', 1
WHERE NOT EXISTS (SELECT 1 FROM auth_role WHERE role_code = 'SYSTEM_ADMIN');
INSERT INTO auth_role(id, role_code, role_name, status)
SELECT 920002, 'OPERATOR', '运营管理员', 1
WHERE NOT EXISTS (SELECT 1 FROM auth_role WHERE role_code = 'OPERATOR');

-- 补齐当前版本明确列出的权限，不使用通配符，也不把未来新权限自动分配出去。
INSERT INTO auth_role_permission(role_id, permission_id)
SELECT role.id, permission.id
FROM auth_role role CROSS JOIN auth_permission permission
WHERE role.role_code = 'SYSTEM_ADMIN'
  AND permission.permission_code IN ('admin:access', 'dashboard:read', 'product:manage', 'inventory:manage', 'order:manage', 'coupon:manage', 'after-sale:manage', 'user:manage', 'rbac:manage', 'audit:read')
  AND NOT EXISTS (SELECT 1 FROM auth_role_permission link WHERE link.role_id = role.id AND link.permission_id = permission.id);
INSERT INTO auth_role_permission(role_id, permission_id)
SELECT role.id, permission.id
FROM auth_role role CROSS JOIN auth_permission permission
WHERE role.role_code = 'OPERATOR'
  AND permission.permission_code IN ('admin:access', 'dashboard:read', 'product:manage', 'inventory:manage', 'order:manage', 'coupon:manage', 'after-sale:manage')
  AND NOT EXISTS (SELECT 1 FROM auth_role_permission link WHERE link.role_id = role.id AND link.permission_id = permission.id);
COMMIT;
