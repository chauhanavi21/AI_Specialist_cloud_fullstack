#!/usr/bin/env python3
"""Validate real PostgreSQL without Docker; requires PostgreSQL binaries, Java 21 and a packaged backend.

Creates its own database directory and ports. Never uses an existing database or kills another application's process.
Example: python deploy/verify-postgres-native.py --pg-bin C:/PostgreSQL/16/bin --java-home C:/Java/jdk-21
"""
import argparse
import base64
import datetime as dt
import json
import os
from pathlib import Path
import secrets
import socket
import subprocess
import sys
import time
import urllib.request


ROOT = Path(__file__).resolve().parents[1]


def free_port():
    with socket.socket() as sock:
        sock.bind(('127.0.0.1', 0))
        return sock.getsockname()[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--pg-bin', type=Path, required=True)
    parser.add_argument('--java-home', type=Path, required=True)
    args = parser.parse_args()
    ext = '.exe' if os.name == 'nt' else ''
    java = args.java_home.resolve() / 'bin' / ('java' + ext)
    initdb = args.pg_bin.resolve() / ('initdb' + ext)
    pg_ctl = args.pg_bin.resolve() / ('pg_ctl' + ext)
    jars = list((ROOT / 'backend/target').glob('bookstore-api-*.jar'))
    driver_jars = sorted((Path.home() / '.m2/repository/org/postgresql/postgresql').glob('*/postgresql-*.jar'))
    if not all(p.is_file() for p in (java, initdb, pg_ctl)) or not jars or not driver_jars:
        parser.error('Need valid Java/PostgreSQL binaries and a Maven-packaged backend (including cached JDBC driver)')
    out = ROOT / 'deploy/evidence' / ('postgres-' + dt.datetime.now(dt.timezone.utc).strftime('%Y%m%dT%H%M%SZ'))
    out.mkdir(parents=True, exist_ok=False)
    runtime = ROOT / '.tools' / ('postgres-' + secrets.token_hex(5))
    runtime.mkdir(parents=True)
    data = runtime / 'data'
    pwfile = runtime / 'password.txt'
    password = secrets.token_hex(32)
    pwfile.write_text(password, encoding='ascii')
    pg_port, app_port = free_port(), free_port()
    while pg_port == app_port:
        app_port = free_port()
    pg_env = {**os.environ, 'EVIDENCE_DB_PASSWORD': password, 'EVIDENCE_DB_USER': 'postgres',
              'EVIDENCE_DB_URL': f'jdbc:postgresql://127.0.0.1:{pg_port}/postgres'}
    app_env = {**os.environ, 'SPRING_PROFILES_ACTIVE': 'postgres', 'SERVER_PORT': str(app_port),
               'SERVER_ADDRESS': '127.0.0.1', 'JWT_SECRET': base64.b64encode(secrets.token_bytes(64)).decode(),
               'SPRING_DATASOURCE_URL': f'jdbc:postgresql://127.0.0.1:{pg_port}/bookstoredb',
               'SPRING_DATASOURCE_USERNAME': 'bookstore_user', 'SPRING_DATASOURCE_PASSWORD': password}
    jdbc_command = [str(java), '-cp', str(driver_jars[-1]), str(ROOT / 'deploy/DatabaseEvidence.java')]
    app = None
    app_log = None
    pg_started = False
    report = {'started_at': dt.datetime.now(dt.timezone.utc).isoformat(), 'passed': False,
              'environment': 'Native PostgreSQL (not a container or cluster)', 'checks': []}

    def record(name):
        report['checks'].append({'name': name, 'passed': True})
        print('PASS ' + name, flush=True)

    def start_app(log_name):
        nonlocal app, app_log
        app_log = (out / log_name).open('w', encoding='utf-8')
        app = subprocess.Popen([str(java), '-jar', str(jars[0])], env=app_env, cwd=ROOT,
                               stdout=app_log, stderr=subprocess.STDOUT)
        for _ in range(120):
            if app.poll() is not None:
                raise RuntimeError('Backend exited; inspect its local log')
            try:
                with urllib.request.urlopen(f'http://127.0.0.1:{app_port}/api/v1/actuator/health', timeout=2) as r:
                    if json.load(r).get('status') == 'UP':
                        return
            except (OSError, ValueError):
                pass
            time.sleep(1)
        raise RuntimeError('Backend health timeout')

    def stop_app():
        nonlocal app, app_log
        if app is not None and app.poll() is None:
            app.terminate()
            try:
                app.wait(timeout=30)
            except subprocess.TimeoutExpired:
                app.kill()
                app.wait(timeout=10)
        if app_log:
            app_log.close()
        app = app_log = None

    try:
        with (out / 'postgres-init.txt').open('w', encoding='utf-8') as log:
            subprocess.run([str(initdb), '-D', str(data), '-U', 'postgres', '-A', 'scram-sha-256',
                            '--pwfile=' + str(pwfile), '--encoding=UTF8', '--no-locale'],
                           stdout=log, stderr=subprocess.STDOUT, check=True, timeout=120)
            pg_started = True
            subprocess.run([str(pg_ctl), '-D', str(data), '-l', str(runtime / 'postgres.log'),
                            '-o', f'-p {pg_port} -h 127.0.0.1', '-w', 'start'],
                           stdout=log, stderr=subprocess.STDOUT, check=True, timeout=60)
        subprocess.run(jdbc_command + ['init'], env=pg_env, check=True, timeout=60)
        pg_env.update(EVIDENCE_DB_USER='bookstore_user', EVIDENCE_DB_URL=app_env['SPRING_DATASOURCE_URL'])
        record('Fresh real PostgreSQL server and non-superuser application role')
        start_app('backend-first-start.log')
        record('PostgreSQL SQL scripts execute before Hibernate schema validation on a fresh database')
        subprocess.run([sys.executable, str(ROOT / 'deploy/verify-api.py'), '--base-url',
                        f'http://127.0.0.1:{app_port}/api/v1', '--output', str(out / 'api-security.json')], check=True)
        record('Live API and security assertions against PostgreSQL')
        stop_app()
        before = subprocess.check_output(jdbc_command + ['snapshot'], env=pg_env, timeout=60)
        (out / 'database-before.txt').write_bytes(before)
        subprocess.run([str(pg_ctl), '-D', str(data), '-m', 'fast', '-w', 'restart'],
                       check=True, timeout=60, stdout=subprocess.DEVNULL)
        record('Real PostgreSQL process restarted')
        start_app('backend-second-start.log')
        stop_app()
        after = subprocess.check_output(jdbc_command + ['snapshot'], env=pg_env, timeout=60)
        (out / 'database-after.txt').write_bytes(after)
        if before != after:
            raise RuntimeError('Database rows changed across restart; compare database-before.txt and database-after.txt')
        print(after.decode('utf-8'), flush=True)
        record('All 22 table row counts and data hashes survive database and application restart')
        record('Passwords stored as BCrypt; application role is not a PostgreSQL superuser')
        report['passed'] = True
    except Exception as exc:
        report['checks'].append({'name': 'Validation completed', 'passed': False, 'error_type': type(exc).__name__})
        print(f'FAIL {type(exc).__name__}: inspect local logs in {out}', file=sys.stderr)
    finally:
        stop_app()
        if pg_started:
            subprocess.run([str(pg_ctl), '-D', str(data), '-m', 'fast', '-w', 'stop'],
                           timeout=60, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        pwfile.unlink(missing_ok=True)
        report['finished_at'] = dt.datetime.now(dt.timezone.utc).isoformat()
        (out / 'summary.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
        print(f'Evidence: {out}', flush=True)
    return 0 if report['passed'] else 1


if __name__ == '__main__':
    sys.exit(main())
