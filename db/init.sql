-- admin-pro 初始化脚本（Day 3-4）
-- 用法：mysql -h127.0.0.1 -P3306 -uroot -proot < db/init.sql

CREATE DATABASE IF NOT EXISTS admin_pro DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE admin_pro;

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
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统用户表';

-- 种子账号：admin / admin123
INSERT INTO sys_user (username, password, salt, nickname, status)
VALUES ('admin', 'bfb9d215350aef3d09848b6254d5451bfe4e77893c15d1853a4ef9afb032bab9', 'a0910dc36ad3bf5421aa1df9bcc668a0', '管理员', 1);
