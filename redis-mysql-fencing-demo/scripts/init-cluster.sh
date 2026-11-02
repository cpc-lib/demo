#!/bin/sh
# Redis Cluster: 3 Master + 3 Replica formation using bridge IPs for node-to-node Gossip.
# Each node resolves its own bridge IP at startup (see command in docker-compose.yml).
# CLUSTER MEET must use real container bridge IPs so all 6 nodes can Gossip.
set -eu
export REDISCLI_AUTH="$REDIS_PASSWORD"

# Resolve each service name to its bridge IP inside Docker network.
A="$(getent hosts redis-7000 | awk '{print $1; exit}')"
B="$(getent hosts redis-7001 | awk '{print $1; exit}')"
C="$(getent hosts redis-7002 | awk '{print $1; exit}')"
D="$(getent hosts redis-7003 | awk '{print $1; exit}')"
E="$(getent hosts redis-7004 | awk '{print $1; exit}')"
F="$(getent hosts redis-7005 | awk '{print $1; exit}')"
echo "Bridge IPs: 7000=$A  7001=$B  7002=$C  7003=$D  7004=$E  7005=$F"

# init container's own 127.0.0.1 is ITSELF; use bridge IP to reach redis-7000.
node_info="$(redis-cli -h "$A" -p 7000 cluster info)"
known="$(printf '%s\n' "$node_info" | awk -F: '/^cluster_known_nodes:/ {gsub(/\r/, "", $2); print $2}')"

if [ "$known" = '6' ]; then
  echo 'Cluster metadata already exists (6 known nodes); skipping creation.'
elif [ "$known" = '1' ]; then
  echo "Creating new 3-Master + 3-Replica Redis Cluster via bridge IPs..."
  redis-cli --cluster create \
    "$A:7000" "$B:7001" "$C:7002" "$D:7003" "$E:7004" "$F:7005" \
    --cluster-replicas 1 --cluster-yes
else
  echo "Unexpected cluster_known_nodes=$known (want 6 or 1). Refusing partial cluster; delete all redis volumes and retry." >&2
  exit 1
fi

attempt=0
while [ "$attempt" -lt 30 ]; do
  info="$(redis-cli -h "$A" -p 7000 cluster info)"
  if printf '%s\n' "$info" | grep -q '^cluster_state:ok' && \
     printf '%s\n' "$info" | grep -q '^cluster_slots_assigned:16384'; then
    printf '%s\n' "$info" | grep -E '^(cluster_state|cluster_slots_assigned|cluster_known_nodes|cluster_size):'
    echo 'Cluster ready (3 Master + 3 Replica).'
    exit 0
  fi
  attempt=$((attempt + 1))
  sleep 1
done

echo 'Cluster creation/recovery timed out; examine redis-700* logs.' >&2
exit 1
