use rusqlite::{params, Connection, Result};
use serde::{Serialize, Deserialize};

#[derive(Serialize, Deserialize)]
pub struct QueryLog {
    pub id: i64,
    pub domain: String,
    pub client_ip: String,
    pub blocked: bool,
    pub timestamp: String,
}

pub fn init_db() -> Result<()> {
    let conn = Connection::open("pyholex_logs.db")?;
    conn.execute(
        "CREATE TABLE IF NOT EXISTS queries (
            id INTEGER PRIMARY KEY,
            domain TEXT NOT NULL,
            client_ip TEXT,
            blocked INTEGER,
            timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
        )",
        [],
    )?;
    Ok(())
}

pub fn log_query(domain: &str, client_ip: &str, blocked: bool) -> Result<()> {
    let conn = Connection::open("pyholex_logs.db")?;
    conn.execute(
        "INSERT INTO queries (domain, client_ip, blocked) VALUES (?1, ?2, ?3)",
        params![domain, client_ip, if blocked { 1 } else { 0 }],
    )?;
    Ok(())
}

pub fn get_recent_logs(limit: i64) -> Result<Vec<QueryLog>> {
    let conn = Connection::open("pyholex_logs.db")?;
    let mut stmt = conn.prepare("SELECT id, domain, client_ip, blocked, timestamp FROM queries ORDER BY id DESC LIMIT ?1")?;
    let logs = stmt.query_map([limit], |row| {
        Ok(QueryLog {
            id: row.get(0)?,
            domain: row.get(1)?,
            client_ip: row.get(2)?,
            blocked: row.get::<_, i32>(3)? == 1,
            timestamp: row.get(4)?,
        })
    })?.filter_map(|r| r.ok()).collect();
    Ok(logs)
}

pub fn get_stats() -> Result<serde_json::Value> {
    let conn = Connection::open("pyholex_logs.db")?;
    let total: i64 = conn.query_row("SELECT COUNT(*) FROM queries", [], |r| r.get(0))?;
    let blocked: i64 = conn.query_row("SELECT COUNT(*) FROM queries WHERE blocked = 1", [], |r| r.get(0))?;
    Ok(serde_json::json!({
        "status": "running",
        "total_queries": total,
        "blocked_queries": blocked,
        "blocking_percentage": if total > 0 { (blocked as f64 / total as f64) * 100.0 } else { 0.0 }
    }))
}
