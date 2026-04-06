use rusqlite::{params, Connection, Result};

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
