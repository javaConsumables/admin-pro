@echo off
rem 一键启动开发环境：MySQL + Redis + 应用（启动前请确保系统 MySQL80 服务处于停止状态）
start "admin-pro-mysql" "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe" --datadir="C:\Users\yang\Documents\Deepseek\mysql8-data" --basedir="C:\Program Files\MySQL\MySQL Server 8.0" --port=3306 --bind-address=127.0.0.1 --mysqlx=OFF --console
start "admin-pro-redis" /d "C:\Users\yang\Documents\Deepseek\redis-win\Redis-8.10.1-Windows-x64-msys2" redis-server.exe --port 6379
timeout /t 6 > nul
cd /d "%~dp0"
call mvn spring-boot:run
