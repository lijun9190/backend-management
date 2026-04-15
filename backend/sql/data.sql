SET NAMES utf8mb4;

INSERT INTO `sys_dept` (`id`, `parent_id`, `dept_name`, `leader`, `phone`, `sort`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `remark`) VALUES
(1, 0, '集团总部', '董事长', '13800000001', 1, 1, 'system', NOW(), 'system', NOW(), 0, '总部部门'),
(2, 1, '技术中心', '技术负责人', '13800000002', 1, 1, 'system', NOW(), 'system', NOW(), 0, '负责系统研发'),
(3, 1, '运营中心', '运营负责人', '13800000003', 2, 1, 'system', NOW(), 'system', NOW(), 0, '负责日常运营'),
(4, 1, '审计中心', '审计负责人', '13800000004', 3, 1, 'system', NOW(), 'system', NOW(), 0, '负责审计与合规');

INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `remark`) VALUES
(1, '超级管理员', 'SUPER_ADMIN', 1, 'system', NOW(), 'system', NOW(), 0, '默认拥有全部权限'),
(2, '系统管理员', 'SYSTEM_ADMIN', 1, 'system', NOW(), 'system', NOW(), 0, '负责系统配置和基础数据'),
(3, '运营角色', 'OPERATOR', 1, 'system', NOW(), 'system', NOW(), 0, '负责运营用户与部门数据'),
(4, '审计角色', 'AUDITOR', 1, 'system', NOW(), 'system', NOW(), 0, '只读查看关键数据与日志');

INSERT INTO `sys_user` (`id`, `dept_id`, `username`, `password`, `nickname`, `real_name`, `phone`, `email`, `gender`, `avatar`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `remark`) VALUES
(1, 1, 'admin', '$2a$10$uve14A6g9.LY05a/WntrMuY3qV2F0wWfBLMp8d.LkoikDWxc43m7u', '超级管理员', '平台管理员', '13900000001', 'admin@example.com', 1, '', 1, 'system', NOW(), 'system', NOW(), 0, '默认超级管理员'),
(2, 2, 'sysadmin', '$2a$10$uve14A6g9.LY05a/WntrMuY3qV2F0wWfBLMp8d.LkoikDWxc43m7u', '系统管理员', '系统经理', '13900000002', 'sysadmin@example.com', 1, '', 1, 'system', NOW(), 'system', NOW(), 0, '系统管理员账号'),
(3, 3, 'operator', '$2a$10$uve14A6g9.LY05a/WntrMuY3qV2F0wWfBLMp8d.LkoikDWxc43m7u', '运营专员', '运营同学', '13900000003', 'operator@example.com', 2, '', 1, 'system', NOW(), 'system', NOW(), 0, '运营角色账号'),
(4, 4, 'auditor', '$2a$10$uve14A6g9.LY05a/WntrMuY3qV2F0wWfBLMp8d.LkoikDWxc43m7u', '审计专员', '审计同学', '13900000004', 'auditor@example.com', 1, '', 1, 'system', NOW(), 'system', NOW(), 0, '审计角色账号'),
(5, 2, 'devuser', '$2a$10$uve14A6g9.LY05a/WntrMuY3qV2F0wWfBLMp8d.LkoikDWxc43m7u', '研发测试员', '开发同学', '13900000005', 'devuser@example.com', 1, '', 1, 'system', NOW(), 'system', NOW(), 0, '辅助演示账号');

INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `path`, `component`, `route_name`, `icon`, `sort`, `permission_code`, `visible`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `deleted`, `remark`) VALUES
(100, 0, '工作台', 'CATALOG', '/dashboard', 'Layout', 'DashboardRoot', 'el-icon-data-analysis', 1, NULL, 1, 1, 'system', NOW(), 'system', NOW(), 0, '仪表盘目录'),
(110, 100, 'Dashboard', 'MENU', 'index', 'dashboard/index', 'Dashboard', 'el-icon-odometer', 1, 'dashboard:view', 1, 1, 'system', NOW(), 'system', NOW(), 0, '仪表盘页面'),

(200, 0, '系统管理', 'CATALOG', '/system', 'Layout', 'SystemRoot', 'el-icon-setting', 2, NULL, 1, 1, 'system', NOW(), 'system', NOW(), 0, '系统管理目录'),
(210, 200, '用户管理', 'MENU', 'user', 'system/user/index', 'SystemUser', 'el-icon-user', 1, 'system:user:view', 1, 1, 'system', NOW(), 'system', NOW(), 0, '用户管理页面'),
(211, 210, '用户查询', 'BUTTON', '', '', '', '', 1, 'system:user:list', 1, 1, 'system', NOW(), 'system', NOW(), 0, '用户列表权限'),
(212, 210, '用户新增', 'BUTTON', '', '', '', '', 2, 'system:user:add', 1, 1, 'system', NOW(), 'system', NOW(), 0, '用户新增权限'),
(213, 210, '用户编辑', 'BUTTON', '', '', '', '', 3, 'system:user:edit', 1, 1, 'system', NOW(), 'system', NOW(), 0, '用户编辑权限'),
(214, 210, '重置密码', 'BUTTON', '', '', '', '', 4, 'system:user:reset-password', 1, 1, 'system', NOW(), 'system', NOW(), 0, '重置密码权限'),
(215, 210, '分配角色', 'BUTTON', '', '', '', '', 5, 'system:user:assign-role', 1, 1, 'system', NOW(), 'system', NOW(), 0, '用户角色分配权限'),
(216, 210, '用户删除', 'BUTTON', '', '', '', '', 6, 'system:user:delete', 1, 1, 'system', NOW(), 'system', NOW(), 0, '用户删除权限'),

(220, 200, '角色管理', 'MENU', 'role', 'system/role/index', 'SystemRole', 'el-icon-s-custom', 2, 'system:role:view', 1, 1, 'system', NOW(), 'system', NOW(), 0, '角色管理页面'),
(221, 220, '角色查询', 'BUTTON', '', '', '', '', 1, 'system:role:list', 1, 1, 'system', NOW(), 'system', NOW(), 0, '角色列表权限'),
(222, 220, '角色新增', 'BUTTON', '', '', '', '', 2, 'system:role:add', 1, 1, 'system', NOW(), 'system', NOW(), 0, '角色新增权限'),
(223, 220, '角色编辑', 'BUTTON', '', '', '', '', 3, 'system:role:edit', 1, 1, 'system', NOW(), 'system', NOW(), 0, '角色编辑权限'),
(224, 220, '角色授权', 'BUTTON', '', '', '', '', 4, 'system:role:assign', 1, 1, 'system', NOW(), 'system', NOW(), 0, '角色授权权限'),

(230, 200, '菜单管理', 'MENU', 'menu', 'system/menu/index', 'SystemMenu', 'el-icon-menu', 3, 'system:menu:view', 1, 1, 'system', NOW(), 'system', NOW(), 0, '菜单管理页面'),
(231, 230, '菜单查询', 'BUTTON', '', '', '', '', 1, 'system:menu:list', 1, 1, 'system', NOW(), 'system', NOW(), 0, '菜单列表权限'),
(232, 230, '菜单新增', 'BUTTON', '', '', '', '', 2, 'system:menu:add', 1, 1, 'system', NOW(), 'system', NOW(), 0, '菜单新增权限'),
(233, 230, '菜单编辑', 'BUTTON', '', '', '', '', 3, 'system:menu:edit', 1, 1, 'system', NOW(), 'system', NOW(), 0, '菜单编辑权限'),
(234, 230, '菜单删除', 'BUTTON', '', '', '', '', 4, 'system:menu:delete', 1, 1, 'system', NOW(), 'system', NOW(), 0, '菜单删除权限'),

(240, 200, '部门管理', 'MENU', 'dept', 'system/dept/index', 'SystemDept', 'el-icon-office-building', 4, 'system:dept:view', 1, 1, 'system', NOW(), 'system', NOW(), 0, '部门管理页面'),
(241, 240, '部门查询', 'BUTTON', '', '', '', '', 1, 'system:dept:list', 1, 1, 'system', NOW(), 'system', NOW(), 0, '部门列表权限'),
(242, 240, '部门新增', 'BUTTON', '', '', '', '', 2, 'system:dept:add', 1, 1, 'system', NOW(), 'system', NOW(), 0, '部门新增权限'),
(243, 240, '部门编辑', 'BUTTON', '', '', '', '', 3, 'system:dept:edit', 1, 1, 'system', NOW(), 'system', NOW(), 0, '部门编辑权限'),
(244, 240, '部门删除', 'BUTTON', '', '', '', '', 4, 'system:dept:delete', 1, 1, 'system', NOW(), 'system', NOW(), 0, '部门删除权限'),

(300, 0, '日志管理', 'CATALOG', '/log', 'Layout', 'LogRoot', 'el-icon-document-copy', 3, NULL, 1, 1, 'system', NOW(), 'system', NOW(), 0, '日志目录'),
(310, 300, '登录日志', 'MENU', 'login', 'log/login/index', 'LoginLog', 'el-icon-notebook-2', 1, 'system:login-log:view', 1, 1, 'system', NOW(), 'system', NOW(), 0, '登录日志页面'),
(311, 310, '登录日志查询', 'BUTTON', '', '', '', '', 1, 'system:login-log:list', 1, 1, 'system', NOW(), 'system', NOW(), 0, '登录日志列表权限'),
(320, 300, '操作日志', 'MENU', 'operation', 'log/operation/index', 'OperationLog', 'el-icon-tickets', 2, 'system:operation-log:view', 1, 1, 'system', NOW(), 'system', NOW(), 0, '操作日志页面'),
(321, 320, '操作日志查询', 'BUTTON', '', '', '', '', 1, 'system:operation-log:list', 1, 1, 'system', NOW(), 'system', NOW(), 0, '操作日志列表权限'),

(400, 0, '个人中心', 'CATALOG', '/profile', 'Layout', 'ProfileRoot', 'el-icon-user-solid', 4, NULL, 1, 1, 'system', NOW(), 'system', NOW(), 0, '个人中心目录'),
(410, 400, '个人中心', 'MENU', 'index', 'profile/index', 'ProfileIndex', 'el-icon-user-solid', 1, 'profile:view', 1, 1, 'system', NOW(), 'system', NOW(), 0, '个人中心页面');

INSERT INTO `sys_user_role` (`id`, `user_id`, `role_id`) VALUES
(1, 1, 1),
(2, 2, 2),
(3, 3, 3),
(4, 4, 4),
(5, 5, 2);

INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES
-- 超级管理员：全部权限
(1, 1, 100),(2, 1, 110),(3, 1, 200),(4, 1, 210),(5, 1, 211),(6, 1, 212),(7, 1, 213),(8, 1, 214),(9, 1, 215),
(10, 1, 220),(11, 1, 221),(12, 1, 222),(13, 1, 223),(14, 1, 224),(15, 1, 230),(16, 1, 231),(17, 1, 232),(18, 1, 233),(19, 1, 234),
(20, 1, 240),(21, 1, 241),(22, 1, 242),(23, 1, 243),(24, 1, 244),(25, 1, 300),(26, 1, 310),(27, 1, 311),(28, 1, 320),(29, 1, 321),(30, 1, 400),(31, 1, 410),
-- 系统管理员：除超级管理员外的完整系统权限
(32, 2, 100),(33, 2, 110),(34, 2, 200),(35, 2, 210),(36, 2, 211),(37, 2, 212),(38, 2, 213),(39, 2, 214),(40, 2, 215),
(41, 2, 220),(42, 2, 221),(43, 2, 222),(44, 2, 223),(45, 2, 224),(46, 2, 230),(47, 2, 231),(48, 2, 232),(49, 2, 233),(50, 2, 234),
(51, 2, 240),(52, 2, 241),(53, 2, 242),(54, 2, 243),(55, 2, 244),(56, 2, 300),(57, 2, 310),(58, 2, 311),(59, 2, 320),(60, 2, 321),(61, 2, 400),(62, 2, 410),
-- 运营角色：工作台 + 用户管理 + 部门管理 + 个人中心
(63, 3, 100),(64, 3, 110),(65, 3, 200),(66, 3, 210),(67, 3, 211),(68, 3, 212),(69, 3, 213),(70, 3, 214),(71, 3, 240),(72, 3, 241),(73, 3, 242),(74, 3, 243),(75, 3, 400),(76, 3, 410),
-- 审计角色：工作台 + 用户只读 + 角色只读 + 日志查看 + 个人中心
(77, 4, 100),(78, 4, 110),(79, 4, 200),(80, 4, 210),(81, 4, 211),(82, 4, 220),(83, 4, 221),(84, 4, 300),(85, 4, 310),(86, 4, 311),(87, 4, 320),(88, 4, 321),(89, 4, 400),(90, 4, 410),
(91, 1, 216),(92, 2, 216),(93, 3, 216);

INSERT INTO `sys_login_log` (`id`, `username`, `nickname`, `login_ip`, `browser`, `os`, `login_status`, `message`, `login_time`) VALUES
(1, 'admin', '超级管理员', '127.0.0.1', 'Chrome', 'Windows', 1, '登录成功', NOW() - INTERVAL 1 DAY),
(2, 'sysadmin', '系统管理员', '127.0.0.1', 'Edge', 'Windows', 1, '登录成功', NOW() - INTERVAL 2 DAY),
(3, 'operator', '运营专员', '127.0.0.1', 'Chrome', 'Windows', 1, '登录成功', NOW() - INTERVAL 3 DAY),
(4, 'auditor', '审计专员', '127.0.0.1', 'Firefox', 'Windows', 1, '登录成功', NOW() - INTERVAL 4 DAY),
(5, 'ghost', NULL, '127.0.0.1', 'Chrome', 'Windows', 0, '用户名不存在', NOW() - INTERVAL 5 DAY);

INSERT INTO `sys_operation_log` (`id`, `module_name`, `operation_type`, `request_method`, `request_uri`, `operator_name`, `operator_username`, `request_params`, `operation_status`, `cost_time`, `error_message`, `operation_time`) VALUES
(1, '用户管理', '新增', 'POST', '/api/system/users', '超级管理员', 'admin', '{"username":"newuser"}', 1, 68, NULL, NOW() - INTERVAL 2 DAY),
(2, '角色管理', '分配权限', 'PUT', '/api/system/roles/2/menus', '系统管理员', 'sysadmin', '{"menuIds":[210,211,212]}', 1, 95, NULL, NOW() - INTERVAL 1 DAY),
(3, '菜单管理', '删除', 'DELETE', '/api/system/menus/999', '超级管理员', 'admin', NULL, 0, 23, '菜单不存在', NOW() - INTERVAL 6 HOUR),
(4, '部门管理', '编辑', 'PUT', '/api/system/depts/3', '运营专员', 'operator', '{"deptName":"运营中心"}', 1, 47, NULL, NOW() - INTERVAL 3 HOUR);
