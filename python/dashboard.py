from flask import Flask, render_template, jsonify
import sqlite3
import os

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

@app.route('/api/stats')
def api_stats():
    total, blocked, top_domains = get_stats()
    return jsonify({
        'total_queries': total,
        'blocked_queries': blocked,
        'top_domains': top_domains
    })

def main():
    app.run(host='127.0.0.1', port=8080)

if __name__ == '__main__':
    main()
