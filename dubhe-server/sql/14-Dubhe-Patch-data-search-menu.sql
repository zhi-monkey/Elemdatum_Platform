-- 14-Dubhe-Patch-data-search-menu.sql
-- 新增「数据检索」菜单（挂在「数据中心」目录下），前端组件对应 src/views/mineai/data/data-search/index.vue
--
-- 说明：
-- 1) 菜单由后端 menu 表驱动，前端动态路由按 component 字符串匹配 src/views 下文件。
--    component='mineai/data/data-search/index' -> src/views/mineai/data/data-search/index.vue
-- 2) 父目录名已确认为「数据中心」（type=0 目录）。若实际库中该目录的 name/type 与下面不同，
--    请先执行：SELECT id, name, type, pid FROM menu WHERE pid = 0 AND type = 0;
--    找到「数据中心」的父菜单 id，替换下面子查询里的条件。
-- 3) 也可以用「系统管理 - 菜单管理」界面手动新增菜单（更直观、不易错）。

-- ============ 新增菜单 ============
INSERT INTO `menu`
(`pid`,`type`,`name`,`icon`,`path`,`component`,`component_name`,`layout`,`permission`,`back_to`,`ext_config`,`hidden`,`cache`,`sort`,`create_user_id`,`update_user_id`,`deleted`)
VALUES
(
  (SELECT id FROM (SELECT id FROM `menu` WHERE `name` = '数据中心' AND `type` = 0 LIMIT 1) t),
  1,
  '数据检索',
  NULL,
  'data/search',
  'mineai/data/data-search/index',
  'DataSearch',
  'BaseLayout',
  'data:search',
  NULL,
  NULL,
  b'0',
  b'0',
  999,
  NULL,
  NULL,
  b'0'
);

-- ============ 给角色授权（可选，也可在「系统管理 - 角色管理」勾选）============
-- 先查角色 id：
--   SELECT id, name FROM role;
-- 再把新菜单挂到目标角色（把 <角色id> 换成实际值）：
-- INSERT INTO `roles_menus` (`role_id`, `menu_id`)
-- SELECT <角色id>, id FROM `menu` WHERE `name` = '数据检索' AND `component` = 'mineai/data/data-search/index';
