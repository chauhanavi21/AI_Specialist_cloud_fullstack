#!/usr/bin/env bash
# Proves the application works on a real PostgreSQL, from nothing, and keeps the evidence.
#
# Needs: Docker, Java 21 and the repo. Run from the repo root:
#   deploy/verify-postgres.sh
#
# It starts a throwaway PostgreSQL 16 in Docker, starts the backend against it (the idempotent scripts create and fill the
# tables, Hibernate validates every entity against them), replays the API collection, restarts the backend to prove the
# scripts can run again without duplicating or losing anything, and prints row counts read straight from PostgreSQL.
# The container is removed at the end, whatever happens.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PORT="${PGPORT:-5433}"
APP_PORT="${APP_PORT:-8092}"
NAME="bookstore-verify-pg"
PW="$(openssl rand -hex 12)"
SECRET="$(openssl rand -base64 64 | tr -d '\r\n')"
APP_PID=""

cleanup() { stop_app >/dev/null 2>&1 || true; docker rm -f "$NAME" >/dev/null 2>&1 || true; }
trap cleanup EXIT

say() { printf '\n\033[1m== %s\033[0m\n' "$*"; }
psqlc() { docker exec "$NAME" psql -U bookstore_user -d bookstoredb -t -A -c "$1"; }

start_app() {
  # A leftover application on this port would answer the health check and make this run "pass" against the wrong database.
  if curl -fs "http://127.0.0.1:$APP_PORT/api/v1/actuator/health" >/dev/null 2>&1; then
    echo "port $APP_PORT already has an application running. Stop it, or choose another port: APP_PORT=8099 deploy/verify-postgres.sh" >&2
    exit 1
  fi
  ( cd "$ROOT/backend" && SPRING_PROFILES_ACTIVE=postgres SERVER_PORT="$APP_PORT" JWT_SECRET="$SECRET" \
    SPRING_DATASOURCE_URL="jdbc:postgresql://127.0.0.1:$PORT/bookstoredb" \
    SPRING_DATASOURCE_USERNAME=bookstore_user SPRING_DATASOURCE_PASSWORD="$PW" \
    ./mvnw -q spring-boot:run > "$ROOT/deploy/verify-app.log" 2>&1 ) &
  APP_PID=$!
  for _ in $(seq 1 90); do
    curl -fs "http://127.0.0.1:$APP_PORT/api/v1/actuator/health" >/dev/null 2>&1 && return 0
    kill -0 "$APP_PID" 2>/dev/null || { echo "the application exited; see deploy/verify-app.log" >&2; exit 1; }
    sleep 2
  done
  echo "the application did not become healthy in 3 minutes; see deploy/verify-app.log" >&2; exit 1
}

# Process ids listening on a TCP port (works with lsof, ss, or Windows netstat under Git Bash).
pids_on_port() {
  if command -v lsof >/dev/null 2>&1; then lsof -t -iTCP:"$1" -sTCP:LISTEN 2>/dev/null
  elif command -v ss >/dev/null 2>&1; then ss -ltnp "sport = :$1" 2>/dev/null | grep -o 'pid=[0-9]*' | cut -d= -f2
  else netstat -ano 2>/dev/null | grep -E "[:.]$1 .*LISTEN" | awk '{print $NF}'
  fi | sort -u
}

# Stopping $APP_PID is not enough: it is the Maven wrapper, and the Spring Boot process it started keeps the port.
# So stop whatever is listening, then wait until the port is really free. Otherwise the "restart" would be a no-op and
# step 5 would compare the database with itself.
stop_app() {
  kill "$APP_PID" 2>/dev/null || true
  for pid in $(pids_on_port "$APP_PORT"); do
    kill "$pid" 2>/dev/null || taskkill //PID "$pid" //F >/dev/null 2>&1 || true
  done
  for _ in $(seq 1 30); do
    [ -z "$(pids_on_port "$APP_PORT")" ] && { APP_PID=""; return 0; }
    sleep 1
  done
  echo "could not stop the application on port $APP_PORT" >&2; exit 1
}

counts() { psqlc "select 'users='||(select count(*) from users)||' books='||(select count(*) from books)||' categories='||(select count(*) from categories)||' stores='||(select count(*) from stores)||' orders='||(select count(*) from orders)||' returns='||(select count(*) from order_returns)||' coupons='||(select count(*) from coupons)"; }

say "1. Start PostgreSQL 16"
docker run -d --name "$NAME" -e POSTGRES_USER=bookstore_user -e POSTGRES_PASSWORD="$PW" -e POSTGRES_DB=bookstoredb -p "$PORT:5432" postgres:16 >/dev/null
for _ in $(seq 1 30); do docker exec "$NAME" pg_isready -U bookstore_user -d bookstoredb >/dev/null 2>&1 && break; sleep 1; done
psqlc "select version()"

say "2. First start: the scripts create and fill the database; Hibernate validates all entities"
start_app
echo "tables: $(psqlc "select count(*) from information_schema.tables where table_schema='public'") (expect 22)"
counts

say "3. Replay the API collection against PostgreSQL (needs newman: npm i -g newman)"
if command -v newman >/dev/null 2>&1; then
  newman run "$ROOT/docs/bookstore-api-collection.json" --env-var baseUrl="http://127.0.0.1:$APP_PORT/api/v1" \
    --folder "8. Roles, Entitlement and Logout" --folder "9. Stores, Policies and Shipping Rates" \
    --folder "10. Catalogue Administration" --folder "11. Order Lifecycle: Modify, Ship, Track, Return, Refund" \
    --folder "13. Up-sell and Cross-sell" --reporters cli
else
  echo "newman not installed: skipping (install with: npm i -g newman)"
fi

say "4. State after the collection (a return was refunded, a store and a category were created)"
BEFORE="$(counts)"; echo "$BEFORE"
psqlc "select 'return '||status||' refund='||refund_amount from order_returns order by id"

say "5. Restart: the scripts run again and must change nothing"
stop_app; start_app
AFTER="$(counts)"; echo "$AFTER"
if [ "$BEFORE" = "$AFTER" ]; then echo "OK: identical, nothing duplicated or lost"; else echo "FINDING: the restart changed the data" >&2; exit 1; fi

say "6. Identity sequences stay ahead of the data (the next insert must not collide)"
psqlc "select 'next books id = '||nextval(pg_get_serial_sequence('books','id'))||', max id = '||(select max(id) from books)"

say "Done. The log is deploy/verify-app.log. Screenshot this terminal as evidence."
