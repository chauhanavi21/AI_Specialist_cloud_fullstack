# Captured local validation — November 20, 2026

These files record an actual run on Windows with Java 21 and native **PostgreSQL 16.16**.
**This records the native database run. The separate [live OpenShift validation](../openshift-2026-11-20/README.md) also passed.**

| Check | Result | Evidence |
|---|---|---|
| Backend tests | 79 passed, zero failures/errors/skips | [backend-tests.txt](backend-tests.txt) |
| Production frontend | `npm run build` passed (Vite 5.4.21, 1,605 modules) | Command executed from `frontend/` during this revision |
| Fresh PostgreSQL initialization | Fresh isolated database; actual SQL scripts ran; Hibernate schema validation accepted all entities | [summary.json](summary.json) |
| API and security against PostgreSQL | 51 assertions passed, including roles, ownership, token forgery, logout, validation and rate limiting | [api-security.json](api-security.json) |
| Database and backend restart | All 22 tables retained identical row counts and data hashes, including a newly registered test customer and revoked token | [before](database-before.txt), [after](database-after.txt) |
| Database security | Application role is not a superuser; stored passwords have BCrypt hashes | Database snapshots |

No request/response bodies, signing keys, login tokens, passwords, or kubeconfig are included. The generated test customer
uses an `example.test` address; it is synthetic data. [source.json](source.json) identifies the base revision and the fact
that the run included uncommitted changes. Raw application logs remain local and are excluded from git.

Reproduce with the [native PostgreSQL verifier](../../../deploy/verify-postgres-native.py).
Follow the [Sandbox guide](../../RESUBMISSION.md) for the separate live cluster validation and recording.
