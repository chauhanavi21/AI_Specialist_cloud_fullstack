# Live OpenShift deployment evidence — November 20, 2026

**Result: passed.** The application was built and deployed to Red Hat Developer Sandbox on OpenShift **4.22.22**
(Kubernetes **1.35.9**) in project **`avi-chauhan-dev`**.

**Live application:** <https://bookstore-route-avi-chauhan-dev.apps.rm1.0a51.p1.openshiftapps.com>

**Health:** <https://bookstore-route-avi-chauhan-dev.apps.rm1.0a51.p1.openshiftapps.com/api/v1/actuator/health>

**Submission files:** [MP4 proof clip](Cloud_Deployment_Proof.mp4), [printable evidence PDF](Cloud_Deployment_Evidence.pdf),
and [interactive/local review page](index.html). The MP4 is a silent browser capture of the actual live application/endpoints
and this separately captured evidence report; it supplements the original project/workflow demonstration.

| Reviewer feedback | Actual validation | Evidence |
|---|---|---|
| No live cluster execution | Both images built in OpenShift; all 11 manifest resources passed server-side validation and were applied; deployments and database ready; 2 GiB PVC bound | [builds](builds.json), [deployment output](deployment.txt), [workloads](workloads.txt), [applied manifest](applied-manifest.yaml), [running image IDs](images.txt) |
| PostgreSQL scripts unvalidated | Fresh PostgreSQL **16.15** container initialized the real schema/seed scripts; Hibernate validated all entities; 22 application tables | [database snapshot](database-before.txt), [health](health.json) |
| No proof of persistence | Registered a unique test customer; restarted PostgreSQL and the backend; all 22 table counts and data hashes matched exactly | [before](database-before.txt), [after](database-after.txt), [health after restart](health-after-restart.json) |
| Security explained only by review | **57 runtime assertions passed** through the public HTTPS route: valid/forged/unsigned tokens, roles, ownership, logout, rate limiting, input validation, hidden tooling and headers; certificate verified and HTTP redirects to HTTPS | [API/security results](api-security.json) |
| Container security unproven | Backend, frontend and PostgreSQL all actually ran with OpenShift-assigned **non-root UID 1003930000** under restricted SCC settings | [actual users](container-users.txt), [live security contexts](security-contexts.json) |
| Network isolation unproven | An allowed pod connected; an unrelated pod timed out after DNS resolution succeeded | [allowed](network-allowed.txt), [blocked](network-denied.txt) |
| Running UI evidence | Real Chromium loaded the deployed app and its catalog; desktop/mobile screenshots; no page errors or mobile horizontal overflow | [browser checks](browser-checks.json), [desktop screenshot](live-desktop.png), [mobile screenshot](live-mobile.png) |

## Security issue found and fixed by the live test

The first negative network probe **connected successfully**, revealing a real configuration gap. Kubernetes NetworkPolicies are
additive: the Sandbox's platform-provided `allow-same-namespace` policy selected every pod and allowed every same-project source.
Adding a default-deny policy cannot override that allow.

The deployment now excludes only the bookstore's three `app` labels from that broad baseline. Other workloads retain its previous behavior;
the bookstore uses its explicit router-to-frontend, frontend-to-backend and backend-to-database policies. The script checks that the existing
baseline is the expected Sandbox-managed rule before changing it and stops for unfamiliar configurations.

The repeated live probe passed: `RESULT=REACHABLE` for the allowed source and `RESULT=BLOCKED` for the unrelated source.
The script treats DNS errors, missing tools, missing probe output or an unexpected connection as failures.

Evidence: [original baseline](sandbox-policy-before.json), [scoped baseline](sandbox-policy-after.json),
[original failed security probe](network-before-fix.txt), [passing probe](network-denied.txt).
The platform manages that baseline and may reconcile it during platform changes; rerun the deployment/probe after such changes.

## Reproducibility and limits

The [summary](summary.json) identifies the source base revision and records that the run included uncommitted changes.
The [build records](builds.json) and [running image IDs](images.txt) identify the exact deployed images. The final validation reused the
two already completed builds after fixing the Sandbox policy; `deployment.txt` records this explicitly. It did not fabricate a second build.

No passwords, signing keys, bearer tokens or kubeconfig are included. Test accounts use synthetic `example.test` addresses.
The app retains the repository's public demo users and simulated payments; it is a temporary demonstration.
The Sandbox URL is temporary; the captured evidence remains usable after the Sandbox expires.
`npm audit --omit=dev` reported zero production-package advisories; the image build reported two development-tool advisories.
Backend dependencies and full container images were not vulnerability-scanned.

The separate [native PostgreSQL evidence](../local-2026-11-20/README.md) records 79 passing backend tests and
51 API/security assertions on PostgreSQL 16.16. Those local results are distinct from this live cluster run.
