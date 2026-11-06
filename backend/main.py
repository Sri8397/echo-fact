from pathlib import Path

from dotenv import load_dotenv

# Load backend/.env (GEMINI_API_KEY, ADMIN_PASSWORD) before any module reads the environment.
load_dotenv(Path(__file__).resolve().parent / ".env")

from fastapi import FastAPI, BackgroundTasks, HTTPException, Header
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Dict, Any, Optional

from auth import get_public_key_pem, verify_and_authenticate_admin
from rules.bindings import build_engine
from rules.engine import RuleResult

app = FastAPI(
    title="Suvidha News Engine API",
    description="Backend API powered by Google Antigravity SDK Multi-Agent Pipeline with RSA Asymmetric Authenticated Admin endpoints.",
    version="1.1.0"
)

# Enable CORS for the frontend dev server
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Loads and validates backend/rules/*.yaml at startup. Unknown names stop the server from starting.
engine = build_engine()

class URLRequest(BaseModel):
    url: str

class LoginRequest(BaseModel):
    encrypted_password: str

def respond(result: RuleResult) -> Any:
    """Turns a rule outcome into the body, or into the HTTP error the rule asked for."""
    if not result.ok:
        raise HTTPException(status_code=result.status_code, detail=result.detail)
    return result.body

@app.get("/")
async def root():
    return {
        "status": "online",
        "service": "Suvidha Google ADK Multi-Agent Pipeline",
        "security": "RSA 2048-bit Asymmetric Encrypted Admin Console",
        "endpoints": {
            "get_incidents": "GET /api/incidents",
            "get_public_key": "GET /api/admin/public-key",
            "admin_login": "POST /api/admin/login",
            "add_from_url": "POST /api/incidents/from-url [Protected]",
            "delete_incident": "DELETE /api/incidents/{incident_id} [Protected]",
            "trigger_pipeline": "POST /api/pipeline/run [Protected]"
        }
    }

@app.get("/api/incidents")
async def get_incidents() -> List[Dict[str, Any]]:
    """Returns stored neutral news incidents directly from SQLite database."""
    return respond(await engine.run("incident_list", {}))

@app.get("/api/admin/public-key")
async def get_admin_public_key():
    """Returns RSA 2048-bit Public Key in PEM format for encrypting credentials."""
    return {
        "public_key": get_public_key_pem(),
        "algorithm": "RSA-OAEP",
        "hash": "SHA-256"
    }

@app.post("/api/admin/login")
async def admin_login(body: LoginRequest):
    """Decrypts RSA encrypted password with backend Private Key, verifies admin credentials, and returns session token."""
    if not body.encrypted_password:
        raise HTTPException(status_code=400, detail="Missing encrypted_password in request payload.")

    try:
        auth_result = verify_and_authenticate_admin(body.encrypted_password)
        return auth_result
    except ValueError as err:
        raise HTTPException(status_code=401, detail=str(err))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Authentication processing error: {str(e)}")

@app.delete("/api/incidents/{incident_id}")
async def remove_incident(incident_id: str, x_admin_token: Optional[str] = Header(None)):
    """Deletes an incident by ID from the SQLite database (Admin Auth Required)."""
    return respond(await engine.run("incident_delete", {"admin_token": x_admin_token, "incident_id": incident_id}))

@app.post("/api/incidents/from-url")
async def add_incident_from_url(body: URLRequest, x_admin_token: Optional[str] = Header(None)) -> Dict[str, Any]:
    """Given a news URL, processes it via Agent pipeline into a full Incident JSON object (Admin Auth Required)."""
    return respond(await engine.run("incident_from_url", {"admin_token": x_admin_token, "url": body.url}))

@app.post("/api/pipeline/run")
async def trigger_pipeline(
    background_tasks: BackgroundTasks,
    prompt: str = "Discover and research recent major global news updates",
    x_admin_token: Optional[str] = Header(None)
):
    """Triggers the 4-agent Google ADK pipeline in background (Admin Auth Required)."""
    inputs = {"admin_token": x_admin_token, "background_tasks": background_tasks, "prompt": prompt}
    return respond(await engine.run("pipeline_run", inputs))

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
