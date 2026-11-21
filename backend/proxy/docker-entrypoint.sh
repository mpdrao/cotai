
#!/bin/sh
set -eu

: "${PROXY_USERNAME:=cotai_proxy}"
: "${PROXY_PASSWORD:?Defina PROXY_PASSWORD no ambiente antes de iniciar o COTAI}"

if [ "${#PROXY_PASSWORD}" -lt 12 ]; then
  echo "PROXY_PASSWORD precisa ter pelo menos 12 caracteres." >&2
  exit 1
fi

# Configura a autenticação do proxy
htpasswd -bcB /etc/squid/passwd "$PROXY_USERNAME" "$PROXY_PASSWORD" >/dev/null

chown proxy:proxy /etc/squid/passwd
chmod 640 /etc/squid/passwd

# Remove um possível PID antigo após reiniciar o contêiner
rm -f /run/squid.pid

# Inicia o Squid como processo principal
exec /usr/sbin/squid -N -f /etc/squid/squid.conf
