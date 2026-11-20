# Addressing the returned submission

The reviewer requested evidence of a running cluster deployment, execution of the PostgreSQL scripts and OpenShift manifest,
and security implementation **tested at runtime**. A successful local build or a valid YAML file does not establish a live deployment.

**Completed November 20, 2026:** the [live OpenShift run passed](evidence/openshift-2026-11-20/README.md), including 57 runtime
assertions, 22-table persistence and allowed/blocked network probes. [Open the running app](https://bookstore-route-avi-chauhan-dev.apps.rm1.0a51.p1.openshiftapps.com).
The steps below reproduce that run. Ready-to-paste submission text is in [RESUBMISSION_TEXT.md](RESUBMISSION_TEXT.md).

For the revised attempt, keep the original project/IBM Bob workflow demonstration and attach the new
[MP4 proof clip](evidence/openshift-2026-11-20/Cloud_Deployment_Proof.mp4) and
[evidence PDF](evidence/openshift-2026-11-20/Cloud_Deployment_Evidence.pdf), or append the clip to the original video.
The new clip shows the live app/endpoints plus captured evidence, with computer-generated narration; it supplements the original explanation.
On the author's machine, a combined upload-ready video is provided at `resubmission/AI_Specialist_Cloud_FullStack_REVISED.mp4`.
It retains the full original demonstration/audio and appends the narrated proof. The local `resubmission/` folder is excluded from
git so the author's original recording is not published to the public repository. The original source video is kept unchanged.

## 1. Activate the free OpenShift Sandbox

1. Open <https://developers.redhat.com/developer-sandbox> and sign in to Red Hat.
2. In the catalog, choose **Try it** on **OpenShift**. Complete any verification or activation prompts.
3. Open the OpenShift console and note the existing project name, usually `<username>-dev`.
4. In the console's user menu, open **Copy login command**. The API server address is safe to share; the login token is a credential.

The project is already configured for this temporary demonstration. It contains public demo users and simulated payments;
use only synthetic data. The Sandbox expires, so record the evidence while it is available.

## 2. Connect from Windows

Run PowerShell from the repository root:

```powershell
.\deploy\setup-openshift.ps1
# Replace the example address with the API server address shown by your own cluster.
.\deploy\connect-openshift.ps1 -Server 'https://api.YOUR_CLUSTER:6443'
```

The second command opens a browser sign-in. If the cluster does not support that flow, run the console's login command
**locally**, substituting `.\.tools\openshift\oc.exe` for `oc`. Keep the token out of chat, git, screenshots and your recording.
No local Docker installation is needed: OpenShift builds both container images.

## 3. Deploy and collect actual evidence

```powershell
.\deploy\deploy-openshift.ps1 -Namespace 'YOUR_PROJECT-dev'
```

On Linux or Git Bash with `oc`, `openssl`, `curl` and Python 3 available:

```bash
bash deploy/deploy-openshift.sh YOUR_PROJECT-dev
```

The command uses your existing project. It creates a random database password and JWT signing key, builds fresh images,
validates the manifest against the actual API server and applies it, waits for readiness, then checks the public HTTPS route.
It stops on a failed or inconclusive check. A failed run is evidence of a failure, not proof of a deployment.

On the Developer Sandbox it also checks the managed `allow-same-namespace` baseline and excludes only the bookstore's three
application labels from that broad rule. This preserves its behavior for other workloads and lets the bookstore's restrictive
policies take effect. The before/after policy files are captured. An unfamiliar baseline causes the script to stop for review.
The platform may reconcile its managed defaults later; rerun validation after platform changes.

The generated directory `deploy/evidence/openshift-<UTC timestamp>/` contains:

| File | What it establishes |
|---|---|
| `deployment.txt` | Actual command output, source revision and working-tree status, build results, server validation, rollout status and checks |
| `applied-manifest.yaml` | Namespaced resources actually submitted to the cluster; contains Secret references, never Secret values |
| `resources.txt`, `images.txt` | Runtime resources and immutable image IDs reported by the cluster |
| `health.json`, `health-after-restart.json` | Application/database health through the public route before and after restarts |
| `api-security.json` | Every HTTP/security assertion and its observed result; no credentials or response bodies |
| `database-before.txt`, `database-after.txt` | Real PostgreSQL version and deterministic row counts/data hashes for every application table |
| `network-allowed.txt`, `network-denied.txt` | An allowed database connection and a blocked connection from otherwise identical probe pods |

The script registers one uniquely named test customer, then restarts both the backend and PostgreSQL and compares every table.
Stop using the UI during the persistence check, since concurrent changes could invalidate the comparison.
Network isolation only passes if DNS works, a pod labelled as the backend connects, and an unrelated pod times out.

## 4. Security implementation and runtime checks

| Control | Runtime evidence |
|---|---|
| BCrypt password hashing | Database evidence confirms stored passwords have BCrypt hashes without exposing them |
| JWT authentication and signature verification | Valid token works; modified signatures, unsigned `alg=none` and malformed tokens return 401 |
| Role authorization | Anonymous admin request returns 401; customer returns 403; administrator succeeds |
| Object ownership | A second customer cannot read the seeded first customer's order (404) |
| Members-only catalog | A guest cannot retrieve the restricted book (404); a member succeeds |
| Server-side logout | Revoked token immediately returns 401 |
| Login rate limiting | Five failed attempts against a unique test email are followed by 429; existing customer token remains usable |
| SQL parameter binding and request validation | Injection-shaped search returns no matches; invalid request returns 400; errors omit Java traces |
| API headers and internal endpoints | Checks CSP, framing protection, MIME sniffing protection and referrer policy; H2 console and actuator environment are denied |
| TLS | Requests verify the route certificate; HTTP redirects to the same host over HTTPS |
| Container privileges and secrets | Server validates restricted container settings; application pods require non-root, drop capabilities and forbid privilege escalation; secrets are generated outside git |
| Database network isolation | Allowed and denied connection probes test actual enforcement, not just the presence of a NetworkPolicy |

The native PostgreSQL verifier additionally checks that the application role is not a superuser. Limits still apply:
the login limiter is per instance, browser tokens use localStorage, demo credentials are public, payments are simulated,
and these checks are not a dependency/image vulnerability audit. See the submission report's security section.

## 5. Reproduce PostgreSQL validation without a cluster

For Docker, use `bash deploy/verify-postgres.sh`. For native PostgreSQL on Windows, package the backend using Java 21,
then point this command at a PostgreSQL 16 binary directory and your JDK:

```powershell
.\backend\mvnw.cmd -f backend/pom.xml package
python deploy/verify-postgres-native.py --pg-bin 'C:/PostgreSQL/16/bin' --java-home 'C:/Java/jdk-21'
```

This uses an isolated data directory, random ports/passwords and a non-superuser application role. It runs the SQL scripts
through the real application, exercises the API, restarts PostgreSQL and the app, and compares all 22 tables.
It stops only the processes/data directory it created. It does not require an existing PostgreSQL service.
Its summary clearly identifies this as a native database run, separate from cluster evidence.

## 6. Update the submission and recording

Only after the cluster checks pass:

1. Capture the running application's HTTPS URL and the OpenShift workloads showing ready pods and bound storage.
2. Show the real deployment output and `api-security.json` in a short addition to the demonstration video.
3. Show PostgreSQL version, the 22 table counts/hashes and the successful restart comparison.
4. Explain JWT signature checks, roles/ownership, BCrypt, input validation, secret generation, TLS and the actual network probe results.
5. Review the generated evidence for private information, then include it with the revised report or copy the reviewed files into `docs/evidence/` for git.
6. Update the GitHub repository and resubmit the revised video/report through **Start New Attempt**.

Suggested submission text, **after** a successful live run (fill in the real URL and evidence paths):

> Revised to address the deployment and security feedback. The React/Spring Boot application was built and deployed
> to OpenShift at [actual HTTPS URL]. The attached evidence includes server-side validation and application of the
> OpenShift manifest, ready workloads, PostgreSQL initialization and schema validation, persistence after database and
> application restarts, and passing runtime API/security/network isolation checks. The revised recording shows these results.

Until that run succeeds, state that live cluster validation is pending; do not submit the template as a completed claim.
