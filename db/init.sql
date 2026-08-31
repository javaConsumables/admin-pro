-- admin-pro 全量初始化脚本（Day 1-10 完整版，幂等可重复执行）
-- 用法：mysql -h127.0.0.1 -P3306 -uroot -proot < db/init.sql

CREATE DATABASE IF NOT EXISTS admin_pro DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE admin_pro;

-- ===== 用户表 =====
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username    VARCHAR(50)  NOT NULL COMMENT '用户名',
    password    VARCHAR(128) NOT NULL COMMENT '密码（加盐哈希）',
    salt        VARCHAR(32)  NOT NULL COMMENT '密码盐',
    nickname    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    email       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删 1已删',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统用户表';

-- ===== 角色表 =====
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    role_name   VARCHAR(50)  NOT NULL COMMENT '角色名',
    role_code   VARCHAR(50)  NOT NULL COMMENT '角色编码',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
    remark      VARCHAR(200) DEFAULT NULL,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色表';

-- ===== 菜单/权限表 =====
DROP TABLE IF EXISTS sys_menu;
CREATE TABLE sys_menu (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    parent_id   BIGINT       NOT NULL DEFAULT 0 COMMENT '父菜单ID，0为根',
    menu_name   VARCHAR(50)  NOT NULL COMMENT '菜单/按钮名',
    perms       VARCHAR(100) DEFAULT NULL COMMENT '权限标识 system:user:list',
    menu_type   TINYINT      NOT NULL DEFAULT 1 COMMENT '1菜单 2按钮',
    path        VARCHAR(100) DEFAULT NULL,
    sort        INT          NOT NULL DEFAULT 0,
    status      TINYINT      NOT NULL DEFAULT 1,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '菜单/权限表';

-- ===== 用户-角色关联 =====
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
    id      BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户角色关联';

-- ===== 角色-权限关联 =====
DROP TABLE IF EXISTS sys_role_menu;
CREATE TABLE sys_role_menu (
    id      BIGINT NOT NULL AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_menu (role_id, menu_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '角色权限关联';

-- ===== 操作日志表 =====
DROP TABLE IF EXISTS sys_operation_log;
CREATE TABLE sys_operation_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       DEFAULT NULL,
    username    VARCHAR(50)  DEFAULT NULL,
    operation   VARCHAR(100) DEFAULT NULL COMMENT '操作描述',
    method      VARCHAR(200) DEFAULT NULL COMMENT '方法名',
    params      TEXT         COMMENT '请求参数',
    ip          VARCHAR(50)  DEFAULT NULL,
    cost_time   BIGINT       DEFAULT NULL COMMENT '耗时ms',
    status      TINYINT      DEFAULT 1 COMMENT '1成功 0失败',
    error_msg   VARCHAR(500) DEFAULT NULL,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '操作日志表';

-- ===== 文件表 =====
DROP TABLE IF EXISTS sys_file;
CREATE TABLE sys_file (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    file_name     VARCHAR(200) NOT NULL COMMENT '存储文件名',
    original_name VARCHAR(200) DEFAULT NULL COMMENT '原始文件名',
    file_path     VARCHAR(300) DEFAULT NULL,
    file_size     BIGINT       DEFAULT NULL,
    file_type     VARCHAR(50)  DEFAULT NULL,
    uploader_id   BIGINT       DEFAULT NULL,
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '文件表';

-- ===== 种子数据 =====
-- 用户：admin（超管，密码不公开，部署后另行设置）、zhangsan/123456（演示用）
INSERT INTO sys_user (username, password, salt, nickname, status) VALUES
('admin', '81f14b09809b98210248e62f5375d0e3af104199bede1bfde2a85a5012745cc5', '65b3968c63c3b9f0ac8f6450cd82443b', '管理员', 1),
('zhangsan', '5d584381ea650259adeb6611bdbfc2d46e9d6b64849fe83addb77c233770b3d1', 'aecd6943509939e54ec1257a322b5a07', '张三', 1);

-- 角色
INSERT INTO sys_role (role_name, role_code, status, remark) VALUES
('超级管理员', 'admin', 1, '拥有所有权限'),
('普通用户', 'user', 1, '基础业务权限');

-- 菜单/权限
INSERT INTO sys_menu (parent_id, menu_name, perms, menu_type, path, sort) VALUES
(0, '用户管理', 'system:user:list', 1, '/user', 1),
(1, '用户新增', 'system:user:add', 2, NULL, 1),
(1, '用户修改', 'system:user:edit', 2, NULL, 2),
(1, '用户删除', 'system:user:delete', 2, NULL, 3),
(0, '角色管理', 'system:role:list', 1, '/role', 2),
(5, '角色新增', 'system:role:add', 2, NULL, 1),
(5, '角色修改', 'system:role:edit', 2, NULL, 2),
(5, '角色删除', 'system:role:delete', 2, NULL, 3),
(0, '权限管理', 'system:menu:list', 1, '/menu', 3),
(9, '权限新增', 'system:menu:add', 2, NULL, 1),
(9, '权限修改', 'system:menu:edit', 2, NULL, 2),
(9, '权限删除', 'system:menu:delete', 2, NULL, 3),
(0, '日志查看', 'system:log:list', 1, '/log', 4),
(0, '文件管理', 'system:file:list', 1, '/file', 5),
(14, '文件上传', 'system:file:upload', 2, NULL, 1);

-- 用户角色：admin->超级管理员, zhangsan->普通用户
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1), (2, 2);

-- 普通用户角色的权限：用户查看 + 日志查看 + 文件管理（含上传）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 2, id FROM sys_menu
WHERE perms IN ('system:user:list', 'system:log:list', 'system:file:list', 'system:file:upload');
