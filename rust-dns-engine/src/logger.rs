use rusqlite::{params, Connection, Result};
use serde::{Serialize, Deserialize};
use std::path::PathBuf;
use std::sync::RwLock;
use once_cell::sync::Lazy;

#[derive(Serialize, Deserialize)]
pub struct QueryLog {
    pub id: i64,
    pub domain: String,
    pub client_ip: String,
    pub blocked: bool,
    pub reason: Option<String>,
    pub category: String,
    pub app_package: String,
    pub timestamp: String,
}

#[derive(Serialize, Deserialize)]
pub struct TopDomain {
    pub domain: String,
    pub count: i64,
}

#[derive(Serialize, Deserialize)]
pub struct AppStats {
    pub package: String,
    pub total: i64,
    pub blocked: i64,
}

static DB_PATH: Lazy<RwLock<Option<PathBuf>>> = Lazy::new(|| RwLock::new(None));

pub fn init_db_with_path(path: PathBuf) -> Result<()> {
    {
        let mut p = DB_PATH.write().unwrap();
        *p = Some(path.clone());
    }
    let conn = Connection::open(path)?;
    conn.execute(
        "CREATE TABLE IF NOT EXISTS queries (
            id INTEGER PRIMARY KEY,
            domain TEXT NOT NULL,
            client_ip TEXT,
            blocked INTEGER,
            reason TEXT,
            category TEXT DEFAULT 'uncategorized',
            app_package TEXT DEFAULT 'unknown',
            timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
        )",
        [],
    )?;
    conn.execute("CREATE INDEX IF NOT EXISTS idx_domain ON queries(domain)", [])?;
    conn.execute("CREATE INDEX IF NOT EXISTS idx_app ON queries(app_package)", [])?;
    Ok(())
}

fn get_conn() -> Result<Connection> {
    let p = DB_PATH.read().unwrap();
    let path = p.as_ref().expect("DB Path not initialized");
    Connection::open(path)
}

pub fn log_query_with_metadata(domain: &str, client_ip: &str, blocked: bool, reason: Option<String>, category: &str, app: &str) -> Result<()> {
    let conn = get_conn()?;
    conn.execute(
        "INSERT INTO queries (domain, client_ip, blocked, reason, category, app_package) VALUES (?1, ?2, ?3, ?4, ?5, ?6)",
        params![domain, client_ip, if blocked { 1 } else { 0 }, reason, category, app],
    )?;
    Ok(())
}

pub fn search_logs(query: &str, blocked_only: Option<bool>, category: Option<&str>, limit: i64) -> Result<Vec<QueryLog>> {
    let conn = get_conn()?;
    let mut sql = "SELECT id, domain, client_ip, blocked, reason, category, app_package, timestamp FROM queries WHERE 1=1".to_string();
    let mut params_vec: Vec<rusqlite::types::Value> = Vec::new();

    if !query.is_empty() {
        sql.push_str(" AND (domain LIKE ? OR app_package LIKE ?)");
        params_vec.push(format!("%{}%", query).into());
        params_vec.push(format!("%{}%", query).into());
    }
    if let Some(b) = blocked_only {
        sql.push_str(" AND blocked = ?");
        params_vec.push((if b { 1 } else { 0 }).into());
    }
    if let Some(cat) = category {
        sql.push_str(" AND category = ?");
        params_vec.push(cat.into());
    }

    sql.push_str(" ORDER BY id DESC LIMIT ?");
    params_vec.push(limit.into());

    let mut stmt = conn.prepare(&sql)?;
    let logs = stmt.query_map(rusqlite::params_from_iter(params_vec), |row| {
        Ok(QueryLog {
            id: row.get(0)?,
            domain: row.get(1)?,
            client_ip: row.get(2)?,
            blocked: row.get::<_, i32>(3)? == 1,
            reason: row.get(4)?,
            category: row.get(5)?,
            app_package: row.get(6)?,
            timestamp: row.get(7)?,
        })
    })?.filter_map(|r| r.ok()).collect();
    Ok(logs)
}

pub fn get_app_stats(limit: i64) -> Result<Vec<AppStats>> {
    let conn = get_conn()?;
    let mut stmt = conn.prepare("SELECT app_package, COUNT(*) as t, SUM(CASE WHEN blocked=1 THEN 1 ELSE 0 END) as b FROM queries GROUP BY app_package ORDER BY t DESC LIMIT ?1")?;
    let stats = stmt.query_map([limit], |row| {
        Ok(AppStats {
            package: row.get(0)?,
            total: row.get(1)?,
            blocked: row.get::<_, i64>(2).unwrap_or(0),
        })
    })?.filter_map(|r| r.ok()).collect();
    Ok(stats)
}

pub fn flush_logs() -> Result<()> {
    let conn = get_conn()?;
    conn.execute("DELETE FROM queries", [])?;
    Ok(())
}

pub fn apply_retention(days: i64) -> Result<()> {
    let conn = get_conn()?;
    conn.execute(
        "DELETE FROM queries WHERE timestamp < datetime('now', '-' || ?1 || ' days')",
        params![days],
    )?;
    Ok(())
}

pub fn get_top_blocked(limit: i64) -> Result<Vec<TopDomain>> {
    let conn = get_conn()?;
    let mut stmt = conn.prepare("SELECT domain, COUNT(*) as c FROM queries WHERE blocked = 1 GROUP BY domain ORDER BY c DESC LIMIT ?1")?;
    let top = stmt.query_map([limit], |row| {
        Ok(TopDomain {
            domain: row.get(0)?,
            count: row.get(1)?,
        })
    })?.filter_map(|r| r.ok()).collect();
    Ok(top)
}

pub fn get_stats() -> Result<serde_json::Value> {
    let conn = get_conn()?;
    let total: i64 = conn.query_row("SELECT COUNT(*) FROM queries", [], |r| r.get(0))?;
    let blocked: i64 = conn.query_row("SELECT COUNT(*) FROM queries WHERE blocked = 1", [], |r| r.get(0))?;
    let saved_data_kb = blocked * 200;
    let saved_power_mwh = (saved_data_kb as f64 / 1024.0) * 0.5;

    Ok(serde_json::json!({
        "status": "running",
        "total_queries": total,
        "blocked_queries": blocked,
        "blocking_percentage": if total > 0 { (blocked as f64 / total as f64) * 100.0 } else { 0.0 },
        "saved_data_kb": saved_data_kb,
        "saved_power_mwh": saved_power_mwh
    }))
}
