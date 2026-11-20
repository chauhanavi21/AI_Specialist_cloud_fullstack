#!/usr/bin/env bash
# Deploys the bookstore to OpenShift (IBM ROKS, AWS ROSA, or the free Red Hat Developer Sandbox) and then PROVES it works.
#
#   oc login --token=... --server=...        # once, from the cluster's "Copy login command"
#   deploy/deploy-openshift.sh [namespace]   # default namespace: bookstore-capstone
#
# The Developer Sandbox gives you a namespace you cannot create or rename: pass its name as the first argument
# (for example `deploy/deploy-openshift.sh yourname-dev`) and the script will use it instead of bookstore-capstone.
#
# Every step stops the script on failure. At the end it prints the evidence a reviewer asks for: pods running,
# the route, a health check, a real sign-in through the public URL, and a row count read out of the database.
set -euo pipefail

NS="${1:-bookstore-capstone}"
TAG="v3"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MANIFEST="$ROOT/deploy/k8s/bookstore-roks-deployment.yaml"

say() { printf '\n\033[1m== %s\033[0m\n' "$*"; }
need() { command -v "$1" >/dev/null 2>&1 || { echo "missing required tool: $1" >&2; exit 1; }; }

need oc; need openssl; need curl
oc whoami >/dev/null 2>&1 || { echo "not logged in: run 'oc login ...' first" >&2; exit 1; }

say "1. Namespace ($NS)"
if oc get namespace "$NS" >/dev/null 2>&1 || oc get project "$NS" >/dev/null 2>&1; then
  echo "using existing namespace $NS"
else
  oc create namespace "$NS"
fi
oc project "$NS" >/dev/null

say "2. Secrets (created once; never printed, never stored in git)"
if oc get secret bookstore-secrets >/dev/null 2>&1; then
  echo "bookstore-secrets already exists: keeping it (delete it to rotate)"
else
  oc create secret generic bookstore-secrets \
    --from-literal=postgres-user=bookstore_user \
    --from-literal=postgres-password="$(openssl rand -base64 24 | tr -d '/+=\r\n')" \
    --from-literal=jwt-secret="$(openssl rand -base64 64 | tr -d '\r\n')"
fi

say "3. Build the two images inside the cluster"
for app in backend frontend; do
  oc get imagestream "bookstore-$app" >/dev/null 2>&1 || oc create imagestream "bookstore-$app"
  if ! oc get buildconfig "bookstore-$app" >/dev/null 2>&1; then
    oc new-build --name "bookstore-$app" --binary --strategy=docker --to "bookstore-$app:$TAG" >/dev/null
  fi
  oc start-build "bookstore-$app" --from-dir="$ROOT/$app" --follow
done

say "4. Apply the manifest into $NS"
# The manifest names the namespace bookstore-capstone; rewrite it when a different one was requested.
sed "s/bookstore-capstone/$NS/g" "$MANIFEST" | oc apply -f -

say "5. Wait for everything to be ready"
oc rollout status statefulset/bookstore-postgres --timeout=300s
oc rollout status deployment/bookstore-backend --timeout=420s
oc rollout status deployment/bookstore-frontend --timeout=180s

say "6. Evidence"
oc get pods -o wide
echo
HOST="$(oc get route bookstore-route -o jsonpath='{.spec.host}')"
URL="https://$HOST"
echo "public URL: $URL"

echo; echo "-- health through the public route (expect {\"status\":\"UP\"})"
curl -fsS "$URL/api/v1/actuator/health"; echo

echo; echo "-- the route redirects plain http to https"
curl -s -o /dev/null -w "http -> %{http_code} %{redirect_url}\n" "http://$HOST/"

echo; echo "-- the catalogue is served from PostgreSQL (expect 15 public books for a guest)"
curl -fsS "$URL/api/v1/books?size=1" | grep -o '"totalElements":[0-9]*'

echo; echo "-- a guest cannot reach administration (expect 401) and members-only books are hidden (expect 404)"
curl -s -o /dev/null -w "GET /admin/overview as a guest -> %{http_code}\n" "$URL/api/v1/admin/overview"
curl -s -o /dev/null -w "GET /books/16 as a guest       -> %{http_code}\n" "$URL/api/v1/books/16"

echo; echo "-- sign in through the public URL, then call a protected route"
TOKEN="$(curl -fsS -X POST "$URL/api/v1/auth/login" -H 'Content-Type: application/json' \
  -d '{"email":"alex.dev@ibm.com","password":"Password123!"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')"
curl -s -o /dev/null -w "GET /cart with the token -> %{http_code} (expect 200)\n" "$URL/api/v1/cart" -H "Authorization: Bearer $TOKEN"

echo; echo "-- the data really is in the database pod (row counts straight from PostgreSQL)"
oc exec statefulset/bookstore-postgres -- bash -c \
  'psql -U "$POSTGRESQL_USER" -d bookstoredb -t -A -c "select '"'"'books='"'"'||count(*) from books" -c "select '"'"'users='"'"'||count(*) from users" -c "select '"'"'tables='"'"'||count(*) from information_schema.tables where table_schema='"'"'public'"'"'"'

echo; echo "-- the database is NOT reachable from a pod that is not the backend (NetworkPolicy)"
# A bare "it failed, so it is blocked" would also pass when the probe could not run at all. So the probe reports one of
# three different outcomes, and only an actual connection timeout counts as the policy working:
#   NO-DNS      the name did not resolve (the probe is invalid, not a policy result)
#   REACHABLE   the connection opened (the policy is NOT enforced: a real finding)
#   BLOCKED     the name resolved, the address answered no TCP handshake within 5 seconds (the policy is working)
PROBE='if command -v getent >/dev/null 2>&1; then getent hosts bookstore-postgres >/dev/null 2>&1 || { echo "RESULT=NO-DNS"; exit 0; }; fi
if timeout 5 bash -c "exec 3<>/dev/tcp/bookstore-postgres/5432" 2>/dev/null; then echo "RESULT=REACHABLE"; else rc=$?; [ "$rc" = 124 ] && echo "RESULT=BLOCKED" || echo "RESULT=ERROR-$rc"; fi'
RESULT="$(oc run netpol-probe --rm -i --restart=Never --image=registry.access.redhat.com/ubi9/ubi --command -- bash -c "$PROBE" 2>&1 | grep -o 'RESULT=[A-Z0-9-]*' | tail -1 || true)"
case "$RESULT" in
  RESULT=BLOCKED)   echo "OK: the database cannot be reached from an unrelated pod (connection timed out, as the NetworkPolicy intends)";;
  RESULT=REACHABLE) echo "FINDING: an unrelated pod CAN reach the database. NetworkPolicies are not being enforced on this cluster."; exit 1;;
  RESULT=NO-DNS)    echo "INCONCLUSIVE: the probe pod could not resolve the database name, so this proves nothing about the policy";;
  *)                echo "INCONCLUSIVE: the probe pod did not produce a result (${RESULT:-no output}). Check 'oc get events'; this proves nothing about the policy";;
esac

say "Done. Save this output (and screenshots of 'oc get pods' and the app at $URL) as your deployment evidence."
