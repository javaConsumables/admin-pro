@echo off
chcp 65001 >nul
rem ============================================================
rem  push-to-gitee.bat - 推送 admin-pro 到 Gitee（国内推荐）
rem  使用前先完成 Gitee 侧 3 步：
rem   1. 注册并登录 https://gitee.com
rem   2. 设置 -> SSH公钥 -> 添加公钥（见同目录 SSH公钥.txt）
rem   3. 新建仓库（名称 admin-pro，选私有）
rem  然后双击本脚本，输入你的 Gitee 用户名即可。
rem ============================================================
set /p GITEE_USER=请输入你的 Gitee 用户名: 
cd /d "%~dp0"
git branch -M main
git remote remove origin 2>nul
git remote add origin git@gitee.com:%GITEE_USER%/admin-pro.git
git push -u origin main
if %errorlevel%==0 (
  echo.
  echo 推送成功！仓库地址：https://gitee.com/%GITEE_USER%/admin-pro
) else (
  echo.
  echo 推送失败：请检查①公钥是否已添加到 Gitee ②网络是否能访问 gitee.com
)
pause
