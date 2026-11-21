
FROM ubuntu:24.04

RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends squid apache2-utils \
    && rm -rf /var/lib/apt/lists/*

COPY squid.conf /etc/squid/squid.conf
COPY docker-entrypoint.sh /usr/local/bin/cotai-squid-entrypoint

RUN chmod +x /usr/local/bin/cotai-squid-entrypoint \
    && mkdir -p /var/lib/squid /var/log/squid /var/spool/squid \
    && chown -R proxy:proxy /var/lib/squid /var/log/squid /var/spool/squid

EXPOSE 3128

ENTRYPOINT ["/usr/local/bin/cotai-squid-entrypoint"]

