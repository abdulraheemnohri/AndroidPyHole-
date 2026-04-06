import sqlite3
import os

DB_PATH = os.path.join(os.path.dirname(__file__), '../storage/logs/query_log.db')

def get_clients():
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    c.execute("SELECT client, count(*) FROM queries GROUP BY client ORDER BY count(*) DESC")
    clients = c.fetchall()
    conn.close()
    return clients

def get_blocked_per_client():
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    c.execute("SELECT client, count(*) FROM queries WHERE status='BLOCKED' GROUP BY client")
    blocked_per_client = c.fetchall()
    conn.close()
    return blocked_per_client
