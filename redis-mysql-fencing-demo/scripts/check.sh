#!/bin/sh
set -eu
printf '%s\n' '== Compose services =='
docker compose ps
printf '%s\n' '== Waiting for Cluster slots =='
attempt=0
while [ "$attempt" -lt 30 ]; do
  info="$(docker compose exec -T redis-7000 sh -ec 'export REDISCLI_AUTH="$REDIS_PASSWORD"; redis-cli -h 127.0.0.1 -p 7000 cluster info' 2>/dev/null || true)"
  if printf '%s\n' "$info" | grep -q '^cluster_state:ok' && \
     printf '%s\n' "$info" | grep -q '^cluster_slots_assigned:16384'; then
    break
  fi
  attempt=$((attempt + 1))
  sleep 1
done
if [ "$attempt" -eq 30 ]; then
  echo 'Redis Cluster not ready; check: docker compose logs redis-cluster-init' >&2
  exit 1
fi
printf '%s\n' '== Redis cluster =='
docker compose exec -T redis-7000 sh -ec 'export REDISCLI_AUTH="$REDIS_PASSWORD"; redis-cli -h 127.0.0.1 -p 7000 cluster info; redis-cli -h 127.0.0.1 -p 7000 cluster nodes'
printf '%s\n' '== MySQL health =='
docker compose exec -T mysql sh -ec 'mysqladmin ping -h 127.0.0.1 -uroot -p"$MYSQL_ROOT_PASSWORD" --silent'
