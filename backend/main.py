from fastapi import FastAPI, BackgroundTasks, HTTPException, Header
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Dict, Any, Optional
import json
import os

from database import load_stored_incidents, delete_incident
from pipeline import run_agent_pipeline, process_url_to_incident
from auth import get_public_key_pem, verify_and_authenticate_admin, verify_admin_token

app = FastAPI(
    title="EchoFact News Engine API",
    description="Backend API powered by Google Antigravity SDK Multi-Agent Pipeline with RSA Asymmetric Authenticated Admin endpoints.",
    version="1.1.0"
)

# Enable CORS for frontend Vite dev server
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class URLRequest(BaseModel):
    url: str

class LoginRequest(BaseModel):
    encrypted_password: str

def require_admin_auth(x_admin_token: Optional[str]):
    if not verify_admin_token(x_admin_token):
        raise HTTPException(
            status_code=401,
            detail="Unauthorized: Admin session token is missing or invalid. Please log in via Admin Console."
        )

@app.get("/")
async def root():
    return {
        "status": "online",
        "service": "EchoFact Google ADK Multi-Agent Pipeline",
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
    return load_stored_incidents()

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
    require_admin_auth(x_admin_token)
    success = delete_incident(incident_id)
    if not success:
        raise HTTPException(status_code=404, detail=f"Incident with ID '{incident_id}' not found.")
    return {"status": "success", "message": f"Incident '{incident_id}' deleted."}

@app.post("/api/incidents/from-url")
async def add_incident_from_url(body: URLRequest, x_admin_token: Optional[str] = Header(None)) -> Dict[str, Any]:
    """Given a news URL, processes it via Agent pipeline into a full Incident JSON object (Admin Auth Required)."""
    require_admin_auth(x_admin_token)
    if not body.url or not body.url.startswith(("http://", "https://")):
        raise HTTPException(status_code=400, detail="Invalid URL provided. Must start with http:// or https://")
    
    try:
        new_incident = await process_url_to_incident(body.url)
        return new_incident
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to process URL into incident: {str(e)}")

@app.post("/api/pipeline/run")
async def trigger_pipeline(
    background_tasks: BackgroundTasks,
    prompt: str = "Discover and research recent major global news updates",
    x_admin_token: Optional[str] = Header(None)
):
    """Triggers the 4-agent Google ADK pipeline in background (Admin Auth Required)."""
    require_admin_auth(x_admin_token)
    try:
        background_tasks.add_task(run_agent_pipeline, prompt)
        return {
            "status": "pipeline_started",
            "message": f"ADK Multi-Agent Pipeline launched in background with prompt: '{prompt}'."
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
