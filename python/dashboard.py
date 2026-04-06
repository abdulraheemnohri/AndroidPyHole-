from flask import Flask, render_template, jsonify, request, session, redirect, url_for, send_file
import sqlite3
import os
import json
import datetime
import threading
import secrets
from .blocklist import BlocklistManager, CONFIG_PATH

app = Flask(__name__, template_folder='../gui/templates', static_folder='../gui/static')
app.secret_key = secrets.token_hex(16)
DB_PATH = os.path.join(os.path.dirname(__file__), '../storage/logs/query_log.db')

# Global manager instance
manager = BlocklistManager()

def get_db_conn():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn

def check_auth():
    if not manager.dashboard_password:
        return True
    return session.get('authenticated', False)

@app.before_request
def require_login():
    if request.endpoint in ['login', 'static']:
        return
    if not check_auth():
        return redirect(url_for('login'))

@app.route('/login', methods=['GET', 'POST'])
def login():
    if request.method == 'POST':
        password = request.form.get('password')
        if password == manager.dashboard_password:
            session['authenticated'] = True
            return redirect(url_for('dashboard'))
        return render_template('login.html', error="Invalid Password")
    return render_template('login.html')

@app.route('/logout')
def logout():
    session.pop('authenticated', None)
    return redirect(url_for('login'))

@app.route('/export-config')
def export_config():
    if not os.path.exists(CONFIG_PATH):
        manager.save_config()
    return send_file(CONFIG_PATH, as_attachment=True, download_name='pyhole_config.json')

@app.route('/import-config', methods=['POST'])
def import_config():
    file = request.files.get('config_file')
    if file and file.filename.endswith('.json'):
        try:
            content = file.read()
            new_config = json.loads(content)
            if isinstance(new_config, dict):
                with open(CONFIG_PATH, 'wb') as f:
                    f.write(content)
                manager.load_config()
                return jsonify({"status": "success"})
        except Exception as e:
            return jsonify({"status": "error", "message": str(e)})
    return jsonify({"status": "error", "message": "Invalid file"})

def get_stats():
    if not os.path.exists(DB_PATH):
        return 0, 0, [], [], []
    conn = get_db_conn()
    c = conn.cursor()
    c.execute("SELECT count(*) as total FROM queries")
    total_queries = c.fetchone()['total']
    c.execute("SELECT count(*) as blocked FROM queries WHERE status='BLOCKED'")
    blocked_queries = c.fetchone()['blocked']

    if manager.privacy_level == "HIDE_ALL":
        return total_queries, blocked_queries, [], [], []

    c.execute("SELECT domain, count(*) as count FROM queries WHERE status='BLOCKED' GROUP BY domain ORDER BY count DESC LIMIT 10")
    top_blocked = [dict(row) for row in c.fetchall()]

    c.execute("SELECT domain, count(*) as count FROM queries WHERE status='ALLOWED' GROUP BY domain ORDER BY count DESC LIMIT 10")
    top_allowed = [dict(row) for row in c.fetchall()]

    c.execute("SELECT client, count(*) as count FROM queries GROUP BY client ORDER BY count DESC LIMIT 5")
    top_clients = [dict(row) for row in c.fetchall()]

    if manager.privacy_level == "ANONYMOUS":
        top_blocked = [{"domain": "MASKED", "count": row["count"]} for row in top_blocked]
        top_allowed = [{"domain": "MASKED", "count": row["count"]} for row in top_allowed]
        top_clients = [{"client": "MASKED", "count": row["count"]} for row in top_clients]

    conn.close()
    return total_queries, blocked_queries, top_blocked, top_allowed, top_clients

@app.route('/')
def dashboard():
    total, blocked, top_blocked, top_allowed, top_clients = get_stats()
    return render_template('dashboard.html', total=total, blocked=blocked, top_blocked=top_blocked, top_allowed=top_allowed)

@app.route('/statistics')
def statistics():
    return render_template('statistics.html')

@app.route('/settings')
def settings():
    return render_template('settings.html', config=manager.__dict__)

@app.route('/logs')
def logs():
    if not os.path.exists(DB_PATH):
        log_data = []
    else:
        conn = get_db_conn()
        c = conn.cursor()
        c.execute("SELECT timestamp, domain, client, status FROM queries ORDER BY timestamp DESC LIMIT 100")
        log_data = []
        for row in c.fetchall():
            d = dict(row)
            if manager.privacy_level == "ANONYMOUS":
                d["domain"] = "MASKED"
                d["client"] = "MASKED"
            elif manager.privacy_level == "HIDE_ALL":
                continue
            log_data.append(d)
        conn.close()
    return render_template('logs.html', logs=log_data)

@app.route('/api/stats')
def api_stats():
    total, blocked, top_blocked, top_allowed, top_clients = get_stats()
    return jsonify({
        'total_queries': total,
        'blocked_queries': blocked,
        'top_blocked': top_blocked,
        'top_allowed': top_allowed,
        'top_clients': top_clients,
        'domains_on_blocklist': len(manager.blocked_domains)
    })

@app.route('/api/settings', methods=['POST'])
def api_save_settings():
    new_config = request.json
    for key, value in new_config.items():
        if key in manager.__dict__:
            if isinstance(manager.__dict__[key], set):
                setattr(manager, key, set(value))
            else:
                setattr(manager, key, value)
    manager.save_config()
    return jsonify({"status": "success"})

@app.route('/api/update-gravity', methods=['POST'])
def api_update_gravity():
    threading.Thread(target=manager.update_blocklists).start()
    return jsonify({"status": "updating"})

def main():
    app.run(host='127.0.0.1', port=8080)

if __name__ == '__main__':
    main()
