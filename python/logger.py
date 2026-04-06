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
    conn.commit()
    conn.close()

def log_query(domain, client, status):
    # This logic can be enhanced to respect privacy settings globally,
    # but currently dashboard.py handles masking upon reading for visibility.
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
