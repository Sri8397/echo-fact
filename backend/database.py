import os
import json
import sqlite3
from typing import List, Dict, Any

DB_PATH = os.path.join(os.path.dirname(__file__), "data", "incidents.db")

def get_connection():
    os.makedirs(os.path.dirname(DB_PATH), exist_ok=True)
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn

def clean_existing_data(conn: sqlite3.Connection):
    cursor = conn.cursor()
    rows = cursor.execute("SELECT id, date, year, sources FROM incidents").fetchall()
    for row in rows:
        row_id = row["id"]
        d = row["date"] or ""
        y = row["year"] or ""
        sources_json = row["sources"]
        
        new_d = d
        new_y = y
        
        if d == "Recent Coverage" or not d:
            new_d = "23 Jul 2026"
        
        if " - " in y:
            new_y = y.split(" - ")[0].strip()
        elif y == "2026" and any(k in row_id for k in ["case-lk99", "case-fukushima", "case-thai-cave", "case-spy-balloon", "case-ever-given"]):
            if "lk99" in row_id or "fukushima" in row_id or "spy-balloon" in row_id:
                new_y = "2023"
            elif "thai-cave" in row_id:
                new_y = "2018"
            elif "ever-given" in row_id:
                new_y = "2021"
        elif not y:
            new_y = "2026"

        modified_sources = False
        try:
            sources = json.loads(sources_json or "[]")
            for s in sources:
                if isinstance(s, dict):
                    if s.get("date") == "Recent" or not s.get("date") or s.get("date") == "Recent Coverage":
                        s["date"] = new_d
                        modified_sources = True
            if modified_sources:
                sources_json = json.dumps(sources, ensure_ascii=False)
        except Exception:
            pass

        if new_d != d or new_y != y or modified_sources:
            cursor.execute(
                "UPDATE incidents SET date = ?, year = ?, sources = ? WHERE id = ?",
                (new_d, new_y, sources_json, row_id)
            )
    conn.commit()


def init_db():
    """Initializes the SQLite database table."""
    conn = get_connection()
    cursor = conn.cursor()
    
    cursor.execute("""
    CREATE TABLE IF NOT EXISTS incidents (
        id TEXT PRIMARY KEY,
        title TEXT NOT NULL,
        locationName TEXT,
        lat REAL,
        lng REAL,
        date TEXT,
        year TEXT,
        summary TEXT,
        category TEXT,
        currentReality TEXT,
        hypeTimeline TEXT,
        verifiedFacts TEXT,
        keyQuestions TEXT,
        sources TEXT,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );
    """)
    conn.commit()
    clean_existing_data(conn)
    conn.close()


def upsert_incident_row(cursor: sqlite3.Cursor, item: Dict[str, Any]):
    cursor.execute("""
    INSERT INTO incidents (
        id, title, locationName, lat, lng, date, year, summary,
        category, currentReality, hypeTimeline, verifiedFacts, keyQuestions, sources, updated_at
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
    ON CONFLICT(id) DO UPDATE SET
        title=excluded.title,
        locationName=excluded.locationName,
        lat=excluded.lat,
        lng=excluded.lng,
        date=excluded.date,
        year=excluded.year,
        summary=excluded.summary,
        category=excluded.category,
        currentReality=excluded.currentReality,
        hypeTimeline=excluded.hypeTimeline,
        verifiedFacts=excluded.verifiedFacts,
        keyQuestions=excluded.keyQuestions,
        sources=excluded.sources,
        updated_at=CURRENT_TIMESTAMP;
    """, (
        item.get("id"),
        item.get("title", ""),
        item.get("locationName", ""),
        item.get("lat", 0.0),
        item.get("lng", 0.0),
        item.get("date", ""),
        item.get("year", ""),
        item.get("summary", ""),
        item.get("category", "General News"),
        item.get("currentReality", ""),
        json.dumps(item.get("hypeTimeline", []), ensure_ascii=False),
        json.dumps(item.get("verifiedFacts", []), ensure_ascii=False),
        json.dumps(item.get("keyQuestions", []), ensure_ascii=False),
        json.dumps(item.get("sources", []), ensure_ascii=False)
    ))


def load_stored_incidents() -> List[Dict[str, Any]]:
    """Loads all incidents from the SQLite database, ordered by updated_at descending."""
    init_db()
    conn = get_connection()
    cursor = conn.cursor()
    
    cursor.execute("SELECT * FROM incidents ORDER BY rowid DESC")
    rows = cursor.fetchall()
    
    result = []
    for row in rows:
        result.append({
            "id": row["id"],
            "title": row["title"],
            "locationName": row["locationName"],
            "lat": row["lat"],
            "lng": row["lng"],
            "date": row["date"],
            "year": row["year"],
            "summary": row["summary"],
            "category": row["category"],
            "currentReality": row["currentReality"],
            "hypeTimeline": json.loads(row["hypeTimeline"] or "[]"),
            "verifiedFacts": json.loads(row["verifiedFacts"] or "[]"),
            "keyQuestions": json.loads(row["keyQuestions"] or "[]"),
            "sources": json.loads(row["sources"] or "[]")
        })
        
    conn.close()
    return result


def save_incidents(incidents: List[Dict[str, Any]]) -> None:
    """Upserts all incidents into the SQLite database."""
    init_db()
    conn = get_connection()
    cursor = conn.cursor()
    
    for item in incidents:
        upsert_incident_row(cursor, item)
        
    conn.commit()
    conn.close()
    print(f"🗄️ Saved {len(incidents)} incidents to SQLite database ({DB_PATH}).")

def delete_incident(incident_id: str) -> bool:
    """Deletes an incident from the SQLite database by ID."""
    init_db()
    conn = get_connection()
    cursor = conn.cursor()
    cursor.execute("DELETE FROM incidents WHERE id = ?", (incident_id,))
    deleted = cursor.rowcount > 0
    conn.commit()
    conn.close()
    return deleted

# Run init on import
init_db()
