#!/bin/bash

# 全局路径变量参考：
# DATALIGHT_DIR="/opt/datalight"
# SERVICE_DIR="/srv/datalight"
# LOG_DIR="/data/datalight/logs"
# PID_DIR="/data/datalight/pids"
# DATA_DIR="/data/datalight/data"

# 检查是否以 root 身份运行脚本
if [ "$EUID" -ne 0 ]; then
  echo "Please run the script with root privileges."
  exit 1
fi

SERVICE_NAME="LDAP"

ldapadd -Q -Y EXTERNAL -H ldapi:/// -f "${SERVICE_DIR}/${SERVICE_NAME}/add-memberof.ldif"
ldapmodify -Q -Y EXTERNAL -H ldapi:/// -f "${SERVICE_DIR}/${SERVICE_NAME}/modify-refint-1.ldif"
ldapadd -Q -Y EXTERNAL -H ldapi:/// -f "${SERVICE_DIR}/${SERVICE_NAME}/add-refint-2.ldif"

exit 0