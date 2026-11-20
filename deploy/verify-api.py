#!/usr/bin/env python3
"""Exercise the deployed demo over HTTP; save assertions without tokens, passwords or response bodies.

Uses only Python 3's standard library. Creates one uniquely named test customer per run.
The default credentials are the repository's PUBLIC demo accounts, for a temporary capstone environment.
"""
import argparse
import base64
import datetime as dt
import json
import os
from pathlib import Path
import sys
import urllib.error
import urllib.parse
import urllib.request
import uuid


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base-url', required=True, help='Include /api/v1')
    parser.add_argument('--output', required=True)
    parser.add_argument('--require-https', action='store_true')
    parser.add_argument('--frontend', action='store_true', help='Also check the UI and its hashed assets')
    args = parser.parse_args()
    base = args.base_url.rstrip('/')
    parsed = urllib.parse.urlsplit(base)
    if parsed.scheme not in ('http', 'https') or not parsed.netloc or parsed.username or parsed.password:
        parser.error('Use an http(s) URL without embedded credentials')
    if parsed.query or parsed.fragment or parsed.path != '/api/v1':
        parser.error('The URL must end in /api/v1, without a query or fragment')
    if args.require_https and parsed.scheme != 'https':
        parser.error('This run requires a trusted HTTPS route')
    results = []
    report = {'started_at': dt.datetime.now(dt.timezone.utc).isoformat(), 'base_url': base,
              'checks': results, 'passed': False}

    def check(name, condition, observed=None):
        results.append({'name': name, 'passed': bool(condition), 'observed': observed})
        print(f"{'PASS' if condition else 'FAIL'} {name}" + (f' ({observed})' if observed is not None else ''))
        return condition

    def request(path, expected, method='GET', data=None, token=None, root=False, no_redirect=False):
        headers = {'Accept': 'application/json'}
        if token:
            headers['Authorization'] = 'Bearer ' + token
        payload = None
        if data is not None:
            payload = json.dumps(data).encode()
            headers['Content-Type'] = 'application/json'
        url = path if root else base + path
        req = urllib.request.Request(url, payload, headers, method=method)
        opener = urllib.request.build_opener(NoRedirect()) if no_redirect else urllib.request.build_opener()
        try:
            response = opener.open(req, timeout=30)
        except urllib.error.HTTPError as exc:
            response = exc
        raw = response.read().decode('utf-8', errors='replace')
        check(f'{method} {urllib.parse.urlsplit(url).path} expects {expected}', response.code == expected, response.code)
        try:
            body = json.loads(raw)
        except ValueError:
            body = None
        return body, response.headers, raw

    def login(email):
        body, _, _ = request('/auth/login', 200, 'POST', {
            'email': email, 'password': os.environ.get('BOOKSTORE_DEMO_PASSWORD', 'Password123!')})
        token = body.get('token') if isinstance(body, dict) else None
        if not check('Sign-in returns a bearer token', isinstance(token, str) and token.count('.') == 2):
            raise RuntimeError('Sign-in failed; dependent checks could not run')
        return token

    try:
        health, _, _ = request('/actuator/health', 200)
        check('Database-backed application health is UP', isinstance(health, dict) and health.get('status') == 'UP')
        books, headers, _ = request('/books?size=50', 200)
        check('Public catalogue contains books', isinstance(books, dict) and books.get('totalElements', 0) > 0)
        for header, value in [('X-Content-Type-Options', 'nosniff'), ('Referrer-Policy', 'no-referrer'),
                              ('X-Frame-Options', 'DENY')]:
            check(f'API header {header}', headers.get(header) == value, headers.get(header))
        check('API content security policy', "default-src 'none'" in headers.get('Content-Security-Policy', ''))
        request('/cart', 401)
        request('/admin/overview', 401)
        request('/books/16', 404)
        request('/h2-console/', 401)
        request('/actuator/env', 401)
        member = login('alex.dev@ibm.com')
        request('/cart', 200, token=member)
        request('/books/16', 200, token=member)
        request('/admin/overview', 403, token=member)
        request('/actuator/env', 403, token=member)
        request('/h2-console/', 403, token=member)
        profile, _, _ = request('/auth/me', 200, token=member)
        check('Profile omits password fields', isinstance(profile, dict)
              and not any('password' in key.lower() for key in profile))
        admin = login('admin@bookworm.example')
        request('/admin/overview', 200, token=admin)
        other = login('sarah.connor@example.com')
        request('/orders/1', 404, token=other)
        parts = member.split('.')
        signature = parts[2]
        tampered = '.'.join(parts[:2] + [signature[:10] + ('A' if signature[10] != 'A' else 'B') + signature[11:]])
        encode = lambda text: base64.urlsafe_b64encode(text.encode()).decode().rstrip('=')
        unsigned = encode('{"alg":"none","typ":"JWT"}') + '.' + encode('{"sub":"admin@bookworm.example","exp":9999999999}') + '.'
        for forged in (tampered, unsigned, 'not-a-token'):
            request('/cart', 401, token=forged)
            request('/admin/overview', 401, token=forged)
        injected, _, _ = request('/books?' + urllib.parse.urlencode({'query': "' OR 1=1--"}), 200)
        check('SQL-shaped search cannot bypass filtering', isinstance(injected, dict) and injected.get('totalElements') == 0)
        request('/auth/register', 400, 'POST', {'email': 'invalid', 'password': 'x', 'fullName': ''})
        _, _, error = request('/books/not-a-number', 400)
        check('Validation errors omit Java traces', 'Exception' not in error and 'at com.ibm' not in error)

        # A unique marker proves writes work and gives the persistence check a real new row to preserve.
        marker_email = 'evidence-' + uuid.uuid4().hex + '@example.test'
        registered, _, _ = request('/auth/register', 201, 'POST', {
            'email': marker_email, 'password': uuid.uuid4().hex, 'fullName': 'Deployment Evidence'})
        marker_token = registered.get('token') if isinstance(registered, dict) else None
        if not check('New customer registration returns a token', bool(marker_token)):
            raise RuntimeError('Cannot verify persistence without a test customer')
        report['persistence_marker'] = {'email': marker_email, 'user_id': registered['user']['id']}
        request('/auth/me', 200, token=marker_token)
        request('/auth/logout', 200, 'POST', token=marker_token)
        request('/cart', 401, token=marker_token)
        rate_email = 'ratelimit-' + uuid.uuid4().hex + '@example.test'
        for _ in range(5):
            request('/auth/login', 401, 'POST', {'email': rate_email, 'password': 'wrong-password'})
        request('/auth/login', 429, 'POST', {'email': rate_email, 'password': 'wrong-password'})
        request('/cart', 200, token=member)

        origin = urllib.parse.urlunsplit((parsed.scheme, parsed.netloc, '', '', ''))
        if args.frontend:
            import re
            _, _, html = request(origin + '/', 200, root=True)
            asset = re.search(r'src="(/assets/[^\"]+\.js)"', html)
            check('Frontend serves the built React application', '<div id="root">' in html and asset is not None)
            if asset:
                request(origin + asset.group(1), 200, root=True)
            request(origin + '/orders', 200, root=True)
        if args.require_https:
            _, redirect_headers, _ = request('http://' + parsed.netloc + '/', 302, root=True, no_redirect=True)
            check('HTTP redirects to the same HTTPS host', redirect_headers.get('Location', '').startswith(origin + '/'))
    except Exception as exc:
        # Do not persist raw request/response objects or exception messages, which can contain credentials.
        check('All dependent checks completed', False, type(exc).__name__)
    finally:
        report['finished_at'] = dt.datetime.now(dt.timezone.utc).isoformat()
        report['passed'] = bool(results) and all(item['passed'] for item in results)
        output = Path(args.output)
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
        print(f"{sum(item['passed'] for item in results)}/{len(results)} checks passed; evidence: {output}")
    return 0 if report['passed'] else 1


if __name__ == '__main__':
    sys.exit(main())
