from flask import Flask, render_template, jsonify, request
import sqlite3
import os
import json
from .blocklist import BlocklistManager, CONFIG_PATH

app = Flask(__name__, template_folder='../gui/templates', static_folder='../gui/static')
DB_PATH = os.path.join(os.path.dirname(__file__), '../storage/logs/query_log.db')

def get_stats():
    if not os.path.exists(DB_PATH):
        return 0, 0, []
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    c.execute("SELECT count(*) FROM queries")
    total_queries = c.fetchone()[0]
    c.execute("SELECT count(*) FROM queries WHERE status='BLOCKED'")
    blocked_queries = c.fetchone()[0]
    c.execute("SELECT domain, count(*) FROM queries GROUP BY domain ORDER BY count(*) DESC LIMIT 5")
    top_domains = c.fetchall()
    conn.close()
    return total_queries, blocked_queries, top_domains

@app.route('/')
def dashboard():
    total, blocked, top_domains = get_stats()
    return render_template('dashboard.html', total=total, blocked=blocked, top_domains=top_domains)

@app.route('/settings')
def settings():
    if os.path.exists(CONFIG_PATH):
        with open(CONFIG_PATH, 'r') as f:
            config = json.load(f)
    else:
        config = {'blocklists': [], 'custom_blocked': [], 'custom_allowed': []}
    return render_template('settings.html', config=config)

@app.route('/clients')
def clients():
    if not os.path.exists(DB_PATH):
        clients_data = []
    else:
        conn = sqlite3.connect(DB_PATH)
        c = conn.cursor()
        c.execute("SELECT client, count(*) FROM queries GROUP BY client ORDER BY count(*) DESC")
        clients_data = c.fetchall()
        conn.close()
    return render_template('clients.html', clients=clients_data)

@app.route('/api/stats')
def api_stats():
    total, blocked, top_domains = get_stats()
    return jsonify({
        'total_queries': total,
        'blocked_queries': blocked,
        'top_domains': top_domains
    })

@app.route('/api/settings', methods=['POST'])
def api_save_settings():
    new_config = request.json
    with open(CONFIG_PATH, 'w') as f:
        json.dump(new_config, f)
    return jsonify({"status": "success"})

def main():
    app.run(host='127.0.0.1', port=8080)

if __name__ == '__main__':
    main()
