-- 修复 Docker MySQL 首次初始化时的中文双重编码问题
-- 现象：昵称/菜单名/角色名显示为 Ã§Â®Â¡... 乱码
-- 用法：cat db/fix-chinese-encoding.sql | docker exec -i admin-pro-mysql mysql -uroot -proot
USE admin_pro;
UPDATE sys_user SET
  nickname = CONVERT(CAST(CONVERT(nickname USING latin1) AS BINARY) USING utf8mb4);
UPDATE sys_role SET
  role_name = CONVERT(CAST(CONVERT(role_name USING latin1) AS BINARY) USING utf8mb4),
  remark = CONVERT(CAST(CONVERT(remark USING latin1) AS BINARY) USING utf8mb4);
UPDATE sys_menu SET
  menu_name = CONVERT(CAST(CONVERT(menu_name USING latin1) AS BINARY) USING utf8mb4);
