Revised submission: AI Specialist — Cloud FullStack capstone

Repository: https://github.com/chauhanavi21/AI_Specialist_cloud_fullstack

Live application: https://bookstore-route-avi-chauhan-dev.apps.rm1.0a51.p1.openshiftapps.com

I addressed the feedback with an actual deployment to Red Hat OpenShift 4.22.22 on November 20, 2026. Both container images
were built in the cluster. All 11 OpenShift manifest resources passed server-side validation and were applied successfully,
including the HTTPS Route and persistent PostgreSQL database.

The PostgreSQL 16.15 container executed the schema/seed SQL scripts, and Hibernate validated the entities against all 22 tables.
After creating a new test customer, I restarted both PostgreSQL and the backend: every table retained identical row counts
and data hashes. The 2 GiB persistent volume remained bound.

The repository includes 57 passing runtime API/security assertions through the public HTTPS route: JWT signature/forgery checks,
customer/admin roles, order ownership, server-side logout, login rate limiting, input validation, security headers, protected
operational endpoints and HTTP-to-HTTPS redirection. Actual container users/security contexts show non-root execution.
Positive and negative network probes also passed after fixing the Sandbox's broad default policy for the bookstore pods.

Evidence: docs/evidence/openshift-2026-11-20/README.md in the repository, including actual command output, build/image IDs,
JSON assertion results, database snapshots, policy before/after files and screenshots. Separately, all 79 backend tests and
51 API/security assertions against native PostgreSQL 16.16 passed.

This is a temporary Sandbox demonstration using synthetic data and simulated payments. The evidence is retained in the
repository if the Sandbox later expires.
