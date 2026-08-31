#!/bin/bash
# ============================================================
# admin-pro 一键部署脚本（Ubuntu 20.04+ / Alibaba Cloud Linux 3）
# 用法：sudo bash deploy.sh
# 前置：安全组需放行 22(SSH) 和 8080(应用)
# ============================================================
set -e

echo "==> [1/6] 安装 Docker（如未安装）"
if ! command -v docker >/dev/null 2>&1; then
  curl -fsSL https://get.docker.com | sh
  systemctl enable --now docker
fi

echo "==> [2/6] 安装 docker compose 插件（如缺失）"
if ! docker compose version >/dev/null 2>&1; then
  apt-get update -y && apt-get install -y docker-compose-plugin || true
fi

echo "==> [3/6] 克隆项目"
cd /opt
if [ ! -d admin-pro ]; then
  git clone https://github.com/javaConsumables/admin-pro.git
fi
cd admin-pro

echo "==> [4/6] 构建并启动（MySQL + Redis + App）"
docker compose up -d --build

echo "==> [5/6] 等待服务就绪"
for i in $(seq 1 20); do
  if curl -s --max-time 3 http://127.0.0.1:8080/api/health > /dev/null 2>&1; then
    echo "    应用已就绪（约 $((i*3))s）"
    break
  fi
  sleep 3
done

echo "==> [6/6] 验证"
echo "--- 本机健康检查 ---"
curl -s http://127.0.0.1:8080/api/health
echo ""
IP=$(curl -s --max-time 5 https://api.ipify.org || echo "你的公网IP")
echo ""
echo "============================================"
echo "  部署完成！"
echo "  健康检查: http://$IP:8080/api/health"
echo "  登录接口: http://$IP:8080/api/auth/login"
echo "  种子账号: admin / admin123（请尽快改密）"
echo "  日志查看: docker compose logs -f app"
echo "============================================"
