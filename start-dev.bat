@echo off
rem 一键启动开发环境：先启 Redis，再启应用
start "admin-pro-redis" /d "C:\Users\yang\Documents\Deepseek\redis-win\Redis-8.10.1-Windows-x64-msys2" redis-server.exe --port 6379
timeout /t 3 > nul
cd /d "%~dp0"
call mvn spring-boot:run
