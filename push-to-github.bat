@echo off
chcp 65001 >nul
rem ============================================================
rem  push-to-github.bat - 推送 admin-pro 到 GitHub
rem  使用前先完成 GitHub 侧 3 步：
rem   1. 注册并登录 https://github.com
rem   2. Settings -> SSH and GPG keys -> New SSH key
rem      粘贴本机公钥（见同目录 SSH公钥.txt）
rem   3. 新建空仓库 https://github.com/new （名称 admin-pro，选 Private）
rem  然后双击本脚本，输入你的 GitHub 用户名即可。
rem ============================================================
set /p GH_USER=请输入你的 GitHub 用户名: 
cd /d "%~dp0"
git branch -M main
git remote remove origin 2>nul
git remote add origin git@github.com:%GH_USER%/admin-pro.git
git push -u origin main
if %errorlevel%==0 (
  echo.
  echo 推送成功！仓库地址：https://github.com/%GH_USER%/admin-pro
) else (
  echo.
  echo 推送失败：请检查①公钥是否已添加到 GitHub ②网络是否能访问 github.com
)
pause
