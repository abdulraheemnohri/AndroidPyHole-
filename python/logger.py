import sqlite3
import datetime
import os

DB_PATH = os.path.join(os.path.dirname(__file__), '../storage/logs/query_log.db')

def init_db():
    os.makedirs(os.path.dirname(DB_PATH), exist_ok=True)
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    c.execute('''CREATE TABLE IF NOT EXISTS queries
                 (timestamp TEXT, domain TEXT, client TEXT, status TEXT)''')
    c.execute('''CREATE TABLE IF NOT EXISTS client_groups
                 (client TEXT PRIMARY KEY, group_name TEXT)''')
    c.execute('''CREATE TABLE IF NOT EXISTS alerts
                 (timestamp TEXT, type TEXT, message TEXT, severity TEXT)''')
    conn.commit()
    conn.close()

def log_query(domain, client, status):
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    timestamp = datetime.datetime.now().isoformat()
    c.execute("INSERT INTO queries VALUES (?, ?, ?, ?)", (timestamp, domain, client, status))
    conn.commit()
    conn.close()

def get_recent_logs(limit=100):
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    c.execute("SELECT * FROM queries ORDER BY timestamp DESC LIMIT ?", (limit,))
    logs = c.fetchall()
    conn.close()
    return logs

def add_alert(alert_type, message, severity="INFO"):
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    timestamp = datetime.datetime.now().isoformat()
    c.execute("INSERT INTO alerts VALUES (?, ?, ?, ?)", (timestamp, alert_type, message, severity))
    conn.commit()
    conn.close()

def get_alerts(limit=50):
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    c = conn.cursor()
    c.execute("SELECT * FROM alerts ORDER BY timestamp DESC LIMIT ?", (limit,))
    alerts = [dict(row) for row in c.fetchall()]
    conn.close()
    return alerts

if __name__ == "__main__":
    init_db()
