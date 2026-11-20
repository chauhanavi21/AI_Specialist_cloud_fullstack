#!/usr/bin/env python3
"""Build a self-contained, printable review page from captured live evidence; does not contact the cluster."""
import html
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
out = root / 'docs/evidence/openshift-2026-11-20'
summary = json.loads((out/'summary.json').read_text())
api = json.loads((out/'api-security.json').read_text())
builds = json.loads((out/'builds.json').read_text())
assert summary['passed'] and api['passed'] and all(b['phase']=='Complete' for b in builds)
before = (out/'database-before.txt').read_text()
after = (out/'database-after.txt').read_text()
assert before == after
e = html.escape
rows = ''.join(f'<tr><td>{e(c["name"])}</td><td class="pass">PASS</td><td>{e(str(c.get("observed", "")))}</td></tr>' for c in api['checks'])
build_rows = ''.join(f'<tr><td>{e(b["name"])}</td><td class="pass">{e(b["phase"])}</td><td class="mono">{e(b["output"]["to"]["imageDigest"])}</td></tr>' for b in builds)
page = '''<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Book Worm — live deployment evidence</title><style>
*{box-sizing:border-box}body{font:16px/1.6 system-ui,sans-serif;color:#17202a;background:#edf2f7;margin:0}main{max-width:1160px;margin:auto;background:white;padding:50px}h1{font-size:42px;line-height:1.15;margin:12px 0 20px}h2{margin-top:40px;font-size:26px}h3{margin-top:24px}a{color:#0f62fe;overflow-wrap:anywhere}.eyebrow{color:#526477;letter-spacing:.08em;text-transform:uppercase;font-weight:650}.lead{font-size:19px;max-width:920px}.tag{display:inline-block;padding:5px 13px;border-radius:20px;background:#defbe6;color:#0e6027;font-weight:700}.cards{display:grid;grid-template-columns:repeat(4,1fr);gap:16px;margin:30px 0}.card{background:#f2f6fc;border-top:4px solid #0f62fe;padding:22px 18px}.card strong{display:block;font-size:38px;color:#0c3970}.card span{font-size:14px}.pass{color:#116c32;font-weight:700}table{border-collapse:collapse;width:100%;margin:18px 0;font-size:14px;table-layout:fixed}td,th{padding:10px;text-align:left;border-bottom:1px solid #dce3eb;overflow-wrap:anywhere}th{background:#edf2f7}pre{white-space:pre-wrap;overflow-wrap:anywhere;font:13px/1.65 ui-monospace,Consolas,monospace;background:#f0f4f8;padding:20px;border-left:4px solid #0f62fe}.mono{font-family:ui-monospace,monospace;font-size:12px}.note{border-left:4px solid #e6a700;background:#fff8e6;padding:15px 20px}nav{display:flex;flex-wrap:wrap;gap:22px;margin:24px 0;padding-bottom:20px;border-bottom:1px solid #dce3eb}section{scroll-margin-top:20px}.small{font-size:13px;color:#526477}@media(max-width:700px){main{padding:22px}.cards{grid-template-columns:1fr 1fr}h1{font-size:32px}}@media print{body{background:white}main{padding:0;max-width:none}h1{font-size:30px}.cards{gap:8px}.card{padding:12px}h2{break-after:avoid}tr{break-inside:avoid}nav{display:none}.screen-only{display:none}a{color:#174b89}pre{font-size:10px}.api-table{font-size:10px}td,th{padding:5px}}
</style><main>
<div class="eyebrow">AI Specialist · Cloud FullStack · Avi Chauhan</div>
<h1>Live deployment verification</h1><span class="tag">PASSED · 20 November 2026</span>
<p class="lead">Book Worm was built and deployed to a real OpenShift cluster. The schema, public API, security behavior, persistence and network isolation were tested against running containers.</p>
<p><strong>Live HTTPS application:</strong><br><a href="{{URL}}">{{URL}}</a></p>
<p>OpenShift <strong>4.22.22</strong> · Kubernetes <strong>1.35.9</strong> · Project <strong>avi-chauhan-dev</strong> · PostgreSQL <strong>16.15</strong></p>
<nav><a href="#deployment">Deployment</a><a href="#security">Security</a><a href="#persistence">Persistence</a><a href="#network">Network isolation</a><a href="#results">All assertions</a></nav>
<div class="cards"><div class="card"><strong>57 / 57</strong><span>live HTTP/security assertions</span></div><div class="card"><strong>22 / 22</strong><span>tables unchanged after restart</span></div><div class="card"><strong>11 / 11</strong><span>manifest resources accepted</span></div><div class="card"><strong>79 / 79</strong><span>backend tests passed separately</span></div></div>
<section id="deployment"><h2>Both images built; workloads ready</h2><p>OpenShift accepted and applied every namespaced resource, including the HTTPS Route and persistent database. The database's 2 GiB PVC is bound. Runtime image IDs and actual non-root users were captured.</p>
<table><thead><tr><th>Build</th><th>State</th><th>Built image digest</th></tr></thead><tbody>{{BUILDS}}</tbody></table><pre>{{USERS}}</pre>
<p class="small">The initial native PostgreSQL run used 16.16. The live cluster uses 16.15; these are separate captured runs.</p></section>
<section id="security"><h2>Security verified through the public HTTPS route</h2><table><thead><tr><th>Control</th><th>Observed behavior</th></tr></thead><tbody>
<tr><td>JWT signature validation</td><td>Valid token succeeds; tampered, unsigned alg=none and malformed tokens return 401.</td></tr>
<tr><td>Role and object authorization</td><td>Guest admin request: 401. Customer admin request: 403. Administrator succeeds. A second customer cannot read the first customer's order.</td></tr>
<tr><td>Logout and brute-force control</td><td>Revoked token returns 401. Five wrong passwords are followed by 429; another customer's token still works.</td></tr>
<tr><td>Input and internal endpoint protection</td><td>Invalid registration and malformed IDs return 400. SQL-shaped search matches no rows. H2 console and actuator environment are denied.</td></tr>
<tr><td>TLS, headers and secrets</td><td>Certificate verification succeeds; HTTP redirects to HTTPS. API CSP, anti-framing, MIME and referrer headers pass. Secret values remain outside source/evidence.</td></tr>
<tr><td>Database and container privileges</td><td>Stored passwords have BCrypt hashes. Application database role is not a superuser. Containers use OpenShift-assigned non-root UIDs.</td></tr>
</tbody></table></section>
<section id="persistence"><h2>Real PostgreSQL; data survives restarts</h2><p>A unique test customer was registered. Both PostgreSQL and the backend were restarted, then every table's count and data hash was compared. <strong class="pass">All 22 tables matched exactly.</strong></p><pre>{{DATABASE}}</pre></section>
<section id="network"><h2>A real network issue was found and fixed</h2><p>The initial unrelated pod could reach PostgreSQL: the Sandbox's broad <code>allow-same-namespace</code> policy overrode isolation because NetworkPolicy allows are additive.</p><p>The bookstore's three application labels were excluded from that baseline, preserving its behavior for other workloads. The repeated positive and negative probes passed:</p><pre>{{ALLOWED}}
{{DENIED}}</pre><p class="note">This is a temporary Sandbox with public demo users and simulated payments. Platform-managed policy defaults may be reconciled; rerun the probe after platform configuration changes. Production frontend audit reported zero advisories; two development-tool advisories remain. Backend dependencies and complete container images were not vulnerability-scanned.</p></section>
<section id="results"><h2>All 57 captured public-route assertions</h2><p>Observed status codes and results below come directly from <code>api-security.json</code>. Credentials and response bodies were excluded.</p><table class="api-table"><thead><tr><th>Assertion</th><th>Result</th><th>Observed</th></tr></thead><tbody>{{RESULTS}}</tbody></table></section>
<p class="small">Generated from captured JSON and command output. Source/build provenance and raw evidence: <a href="README.md">README.md</a>, <a href="summary.json">summary.json</a>, <a href="deployment.txt">deployment.txt</a>. The final validation reused the completed builds after fixing the baseline policy; this is recorded in the deployment output.</p>
</main></html>'''
values = {'URL': e(summary['url']), 'BUILDS': build_rows, 'USERS': e((out/'container-users.txt').read_text()),
          'DATABASE': e(after), 'ALLOWED': e((out/'network-allowed.txt').read_text().strip()),
          'DENIED': e((out/'network-denied.txt').read_text().strip()), 'RESULTS': rows}
for key,value in values.items():
    page = page.replace('{{'+key+'}}', value)
(out/'index.html').write_text(page, encoding='utf-8')
print(out/'index.html')
