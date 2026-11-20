#!/usr/bin/env bash
# Deploy only into the project the user supplies. Build remotely: no local Docker or Java required.
# Usage: oc login ... (locally, never in a recording), then bash deploy/deploy-openshift.sh YOUR_PROJECT
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
NS="${1:?Pass the existing project/namespace from your OpenShift console}"
[[ "$NS" =~ ^[a-z0-9]([-a-z0-9]*[a-z0-9])?$ && ${#NS} -le 63 ]] || { echo 'Invalid namespace' >&2; exit 1; }
TAG="${IMAGE_TAG:-evidence-$(date -u +%Y%m%d%H%M%S)}"
[[ "$TAG" =~ ^[a-zA-Z0-9_][a-zA-Z0-9_.-]{0,127}$ ]] || { echo 'Invalid IMAGE_TAG' >&2; exit 1; }
PYTHON="${PYTHON:-python}"
for tool in oc openssl curl "$PYTHON"; do command -v "$tool" >/dev/null || { echo "Missing tool: $tool" >&2; exit 1; }; done
oc whoami >/dev/null 2>&1 || { echo 'Run oc login locally using Copy login command in the console.' >&2; exit 1; }
# Sandbox users can use their existing project; they cannot create or update Namespace objects.
oc project "$NS" >/dev/null
OUT="$ROOT/deploy/evidence/openshift-$(date -u +%Y%m%dT%H%M%SZ)"
mkdir -p "$OUT"
exec > >(tee "$OUT/deployment.txt") 2>&1
say() { printf '\n== %s\n' "$*"; }
ocn() { oc -n "$NS" "$@"; }
BACKEND_STOPPED=false
on_exit() {
  local rc=$?
  if "$BACKEND_STOPPED"; then ocn scale deployment/bookstore-backend --replicas=1 || true; fi
  if [ "$rc" -ne 0 ]; then
    echo "FAILED (exit $rc). Evidence: $OUT"
    ocn get pods || true
    ocn get events --sort-by=.lastTimestamp | tail -20 || true
  fi
  exit "$rc"
}
trap on_exit EXIT

say '1. Deployment identity and quota'
date -u +%FT%TZ
git -C "$ROOT" rev-parse HEAD
git -C "$ROOT" status --short
oc version
ocn get resourcequota
printf 'Project: %s\nBuild tag: %s\n' "$NS" "$TAG"

say '2. Secrets (random values, never printed or saved to the evidence)'
if ocn get secret bookstore-secrets >/dev/null 2>&1; then
  echo 'Keeping existing bookstore-secrets'
else
  ocn create secret generic bookstore-secrets \
    --from-literal=postgres-user=bookstore_user \
    --from-literal=postgres-password="$(openssl rand -hex 24)" \
    --from-literal=jwt-secret="$(openssl rand -base64 64 | tr -d '\r\n')"
fi

say '3. Build images inside OpenShift (or verify explicitly selected existing image tags)'
for app in backend frontend; do
  if [ "${REUSE_IMAGES:-0}" = 1 ]; then
    [ -n "${IMAGE_TAG:-}" ] || { echo 'REUSE_IMAGES=1 requires the exact existing IMAGE_TAG'; exit 1; }
    ocn get imagestreamtag "bookstore-$app:$TAG" -o jsonpath='{.image.dockerImageReference}{"\n"}'
    continue
  fi
  ocn get imagestream "bookstore-$app" >/dev/null 2>&1 || ocn create imagestream "bookstore-$app"
  if ! ocn get buildconfig "bookstore-$app" >/dev/null 2>&1; then
    ocn new-build --name "bookstore-$app" --binary --strategy=docker --to "bookstore-$app:$TAG" >/dev/null
  fi
  # Updating an existing BuildConfig is essential: otherwise reruns build into an old tag.
  ocn patch buildconfig "bookstore-$app" --type=merge \
    -p "{\"spec\":{\"output\":{\"to\":{\"kind\":\"ImageStreamTag\",\"name\":\"bookstore-$app:$TAG\"}}}}" >/dev/null
  # .dockerignore excludes target, node_modules, dist, logs and local environment files.
  ocn start-build "bookstore-$app" --from-dir="$ROOT/$app" \
    --exclude='(^|/)(\.git|node_modules|target|dist)(/|$)|(^|/)\.env[^/]*$|\.log$' --follow --wait
done

say '4. Scope the Sandbox baseline policy, then validate and apply the manifest'
# NetworkPolicies are additive. The Sandbox's managed allow-same-namespace rule would otherwise override isolation.
# Exclude only our three application labels from that broad rule, preserving its behavior for every other workload.
ocn get networkpolicy allow-same-namespace --ignore-not-found -o json > "$OUT/sandbox-policy-before.json"
if [ -s "$OUT/sandbox-policy-before.json" ]; then
  STATE="$("$PYTHON" - "$OUT/sandbox-policy-before.json" <<'PY'
import json,sys
p=json.load(open(sys.argv[1],encoding='utf-8-sig'))
assert p['metadata'].get('labels',{}).get('toolchain.dev.openshift.com/provider')=='codeready-toolchain', 'Refusing to modify an unfamiliar baseline policy'
selector={'matchExpressions':[{'key':'app','operator':'NotIn','values':['bookstore-frontend','bookstore-backend','bookstore-postgres']}]}
actual=p['spec'].get('podSelector',{})
assert actual in ({},selector), 'Baseline has a custom selector; review it manually'
assert p['spec']['ingress']==[{'from':[{'podSelector':{}}]}], 'Baseline ingress differs; review it manually'
print('patch' if actual=={} else 'already-excluded')
PY
)"
  if [ "$STATE" = patch ]; then
    ocn patch networkpolicy allow-same-namespace --type=json --patch-file "$ROOT/deploy/sandbox-policy-exclusion.json" --dry-run=server
    ocn patch networkpolicy allow-same-namespace --type=json --patch-file "$ROOT/deploy/sandbox-policy-exclusion.json"
  else
    echo 'Bookstore pods are already excluded from the broad Sandbox baseline.'
  fi
  ocn get networkpolicy allow-same-namespace -o json > "$OUT/sandbox-policy-after.json"
fi
sed -e "s/bookstore-capstone/$NS/g" -e "s/:v3$/:$TAG/" \
  "$ROOT/deploy/k8s/bookstore-roks-deployment.yaml" > "$OUT/applied-manifest.yaml"
ocn apply --dry-run=server -f "$OUT/applied-manifest.yaml"
ocn apply -f "$OUT/applied-manifest.yaml"
ocn rollout status statefulset/bookstore-postgres --timeout=420s
ocn rollout status deployment/bookstore-backend --timeout=600s
ocn rollout status deployment/bookstore-frontend --timeout=300s
ocn get pods -o wide
ocn get route bookstore-route
ocn get deployments,statefulsets,services,networkpolicies,pvc -o wide > "$OUT/resources.txt"
# Record the immutable image IDs actually running, not just the requested build tags.
ocn get pods -o jsonpath='{range .items[*]}{.metadata.name}{range .status.containerStatuses[*]}{"\t"}{.imageID}{end}{"\n"}{end}' > "$OUT/images.txt"
HOST="$(ocn get route bookstore-route -o jsonpath='{.spec.host}')"
URL="https://$HOST"
printf 'Public URL: %s\n' "$URL"
curl --fail --silent --show-error --retry 12 --retry-delay 5 --retry-all-errors --max-time 20 \
  "$URL/api/v1/actuator/health" > "$OUT/health.json"

say '5. API, TLS and security checks through the public route'
"$PYTHON" "$ROOT/deploy/verify-api.py" --base-url "$URL/api/v1" --require-https --frontend --output "$OUT/api-security.json"

say '6. Real PostgreSQL version, row fingerprints and persistence after restarting both database and backend'
# The application is stopped before the snapshot so no requests can change the database during the comparison.
ocn scale deployment/bookstore-backend --replicas=0
BACKEND_STOPPED=true
ocn wait --for=delete pod -l app=bookstore-backend --timeout=180s
dbquery() {
  ocn exec -i statefulset/bookstore-postgres -- sh -c \
    'PGPASSWORD="$POSTGRESQL_PASSWORD" psql -X -h 127.0.0.1 -U "$POSTGRESQL_USER" -d bookstoredb -v ON_ERROR_STOP=1 -t -A' \
    < "$1"
}
dbquery "$ROOT/deploy/database-evidence.sql" > "$OUT/database-before.txt"
cat "$OUT/database-before.txt"
grep -qx 'application_tables=22' "$OUT/database-before.txt"
grep -qx 'application_role_is_not_superuser=true' "$OUT/database-before.txt"
grep -qx 'stored_passwords_are_bcrypt=true' "$OUT/database-before.txt"
ocn rollout restart statefulset/bookstore-postgres
ocn rollout status statefulset/bookstore-postgres --timeout=420s
ocn scale deployment/bookstore-backend --replicas=1
ocn rollout status deployment/bookstore-backend --timeout=600s
BACKEND_STOPPED=false
dbquery "$ROOT/deploy/database-evidence.sql" > "$OUT/database-after.txt"
diff -u "$OUT/database-before.txt" "$OUT/database-after.txt"
curl -fsS --retry 12 --retry-delay 5 --retry-all-errors "$URL/api/v1/actuator/health" > "$OUT/health-after-restart.json"
echo 'PASS: all table row counts and data hashes survived database and application restart.'

say '7. NetworkPolicy enforcement (DNS, an allowed connection, then a denied connection)'
# Use the same Python image for positive and negative controls. This avoids confusing a missing tool or bad DNS with isolation.
PROBE='import socket,sys; socket.getaddrinfo("bookstore-postgres",5432); s=socket.socket(); s.settimeout(8)
try:
 s.connect(("bookstore-postgres",5432)); print("RESULT=REACHABLE",flush=True); sys.exit(0)
except TimeoutError:
 print("RESULT=BLOCKED",flush=True); sys.exit(0)
except OSError:
 print("RESULT=ERROR",flush=True); sys.exit(3)'
# The two pods have identical runtime settings and differ only in the app label selected by the NetworkPolicy.
probe() {
  local label="$1" name="bookstore-probe-$(date +%s)-$RANDOM"
  ocn run "$name" --restart=Never --image=registry.access.redhat.com/ubi9/python-311:latest \
    --labels="app=$label" --dry-run=client -o json --command -- python3 -c "$PROBE" | \
    "$PYTHON" -c 'import json,sys
p=json.load(sys.stdin)
p["spec"]["securityContext"]={"runAsNonRoot":True,"seccompProfile":{"type":"RuntimeDefault"}}
c=p["spec"]["containers"][0]
c["securityContext"]={"allowPrivilegeEscalation":False,"capabilities":{"drop":["ALL"]}}
c["resources"]={"requests":{"cpu":"10m","memory":"64Mi"},"limits":{"cpu":"100m","memory":"128Mi"}}
print(json.dumps(p))' | ocn create -f -
  if ! ocn wait --for=jsonpath='{.status.phase}'=Succeeded "pod/$name" --timeout=180s; then
    ocn describe "pod/$name"
    ocn logs "$name" || true
    ocn delete "pod/$name" --wait=false
    return 1
  fi
  ocn logs "$name"
  ocn delete "pod/$name" --wait=false
}
probe bookstore-backend | tee "$OUT/network-allowed.txt"
grep -q '^RESULT=REACHABLE' "$OUT/network-allowed.txt" || { echo 'Allowed network probe failed; isolation result is inconclusive.'; exit 1; }
probe unrelated | tee "$OUT/network-denied.txt"
grep -q '^RESULT=BLOCKED' "$OUT/network-denied.txt" || { echo 'Denied network probe failed or was inconclusive.'; exit 1; }

say 'PASS: deployment, API security, PostgreSQL persistence and network isolation verified'
printf 'Evidence: %s\nPublic URL: %s\n' "$OUT" "$URL"
echo 'Review the evidence and record the running application before resubmitting. This is a temporary demo with public demo accounts.'
