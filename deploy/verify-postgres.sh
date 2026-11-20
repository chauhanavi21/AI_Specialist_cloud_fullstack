#!/usr/bin/env bash
# Docker alternative to verify-postgres-native.py. Needs Docker, Java 21, Python 3, curl and OpenSSL.
# Uses a unique disposable database container and only stops the Java process it launches itself.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PYTHON="${PYTHON:-python}"
for tool in docker java openssl curl "$PYTHON"; do command -v "$tool" >/dev/null || { echo "Missing tool: $tool" >&2; exit 1; }; done
docker info >/dev/null
PORT="${PGPORT:-5433}"
APP_PORT="${APP_PORT:-8092}"
[[ "$PORT" =~ ^[0-9]+$ && "$APP_PORT" =~ ^[0-9]+$ ]] || { echo 'Ports must be numbers'; exit 1; }
"$PYTHON" -c 'import socket,sys
for port in sys.argv[1:]:
 s=socket.socket(); s.bind(("127.0.0.1",int(port))); s.close()' "$PORT" "$APP_PORT"
NAME="bookstore-verify-$(date -u +%Y%m%d%H%M%S)-$RANDOM"
PW="$(openssl rand -hex 24)"
SECRET="$(openssl rand -base64 64 | tr -d '\r\n')"
APP_PID=''
CREATED=false
OUT="$ROOT/deploy/evidence/docker-$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$OUT"
exec > >(tee "$OUT/verification.txt") 2>&1
stop_app() {
  if [ -n "$APP_PID" ]; then
    kill "$APP_PID" 2>/dev/null || true
    wait "$APP_PID" 2>/dev/null || true
    APP_PID=''
  fi
}
cleanup() {
  local rc=$?
  stop_app
  if "$CREATED"; then docker rm -f "$NAME" >/dev/null || true; fi
  exit "$rc"
}
trap cleanup EXIT

echo '1. Package the backend (backend tests are run separately with mvnw test)'
(cd "$ROOT/backend" && bash ./mvnw -q package -DskipTests)
JAR="$(find "$ROOT/backend/target" -maxdepth 1 -name 'bookstore-api-*.jar' -print -quit)"
[ -n "$JAR" ] || { echo 'Packaged jar was not found'; exit 1; }

echo '2. Create an isolated PostgreSQL 16 container and a non-superuser app role'
docker run -d --name "$NAME" -e POSTGRES_USER=pg_admin -e POSTGRES_PASSWORD="$PW" \
  -e POSTGRES_DB=bookstoredb -p "127.0.0.1:$PORT:5432" postgres:16 >/dev/null
CREATED=true
for _ in $(seq 1 60); do
  docker exec "$NAME" pg_isready -U pg_admin -d bookstoredb >/dev/null 2>&1 && break
  sleep 1
done
docker exec "$NAME" psql -X -v ON_ERROR_STOP=1 -U pg_admin -d bookstoredb \
  -c "CREATE ROLE bookstore_user LOGIN PASSWORD '$PW'" -c 'ALTER DATABASE bookstoredb OWNER TO bookstore_user'

start_app() {
  (exec env SPRING_PROFILES_ACTIVE=postgres SERVER_ADDRESS=127.0.0.1 SERVER_PORT="$APP_PORT" JWT_SECRET="$SECRET" \
    SPRING_DATASOURCE_URL="jdbc:postgresql://127.0.0.1:$PORT/bookstoredb" \
    SPRING_DATASOURCE_USERNAME=bookstore_user SPRING_DATASOURCE_PASSWORD="$PW" \
    java -jar "$JAR") >> "$OUT/backend.log" 2>&1 &
  APP_PID=$!
  for _ in $(seq 1 90); do
    kill -0 "$APP_PID" 2>/dev/null || { echo 'Backend exited; see backend.log'; exit 1; }
    curl -fsS --max-time 2 "http://127.0.0.1:$APP_PORT/api/v1/actuator/health" >/dev/null 2>&1 && return 0
    sleep 2
  done
  echo 'Backend did not become healthy'; exit 1
}
snapshot() {
  docker exec -i "$NAME" psql -X -v ON_ERROR_STOP=1 -U bookstore_user -d bookstoredb -t -A \
    < "$ROOT/deploy/database-evidence.sql"
}
echo '3. Execute SQL initialization and validate the real HTTP API/security behavior'
start_app
"$PYTHON" "$ROOT/deploy/verify-api.py" --base-url "http://127.0.0.1:$APP_PORT/api/v1" --output "$OUT/api-security.json"
stop_app
snapshot > "$OUT/database-before.txt"
grep -qx 'application_tables=22' "$OUT/database-before.txt"
grep -qx 'application_role_is_not_superuser=true' "$OUT/database-before.txt"
grep -qx 'stored_passwords_are_bcrypt=true' "$OUT/database-before.txt"

echo '4. Restart both PostgreSQL and the app; assert every table is unchanged'
docker restart "$NAME" >/dev/null
for _ in $(seq 1 60); do
  docker exec "$NAME" pg_isready -U bookstore_user -d bookstoredb >/dev/null 2>&1 && break
  sleep 1
done
start_app
stop_app
snapshot > "$OUT/database-after.txt"
diff -u "$OUT/database-before.txt" "$OUT/database-after.txt"
cat "$OUT/database-after.txt"
printf 'PASS: PostgreSQL SQL scripts, API/security assertions and persistence. Evidence: %s\n' "$OUT"
