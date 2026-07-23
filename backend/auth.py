import os
import base64
import secrets
import time
from typing import Dict, Any, Optional
from cryptography.hazmat.primitives.asymmetric import rsa, padding
from cryptography.hazmat.primitives import hashes, serialization

# System Admin Password (Defaults to admin123 if not set in environment)
ADMIN_PASSWORD = os.getenv("ADMIN_PASSWORD", "admin123")

# Active Admin Session Tokens: token_string -> expiry_timestamp
ACTIVE_TOKENS: Dict[str, float] = {}
TOKEN_EXPIRY_SECONDS = 86400  # 24 hours

# Generate RSA 2048-bit Key Pair on backend initialization
private_key = rsa.generate_private_key(
    public_exponent=65537,
    key_size=2048
)
public_key = private_key.public_key()

def get_public_key_pem() -> str:
    """Returns the RSA Public Key in SubjectPublicKeyInfo PEM format."""
    pem = public_key.public_bytes(
        encoding=serialization.Encoding.PEM,
        format=serialization.PublicFormat.SubjectPublicKeyInfo
    )
    return pem.decode('utf-8')

def decrypt_rsa_payload(encrypted_b64: str) -> str:
    """Decrypts a base64 encoded payload encrypted with backend RSA Public Key (RSA-OAEP SHA256)."""
    try:
        ciphertext = base64.b64decode(encrypted_b64)
        decrypted_bytes = private_key.decrypt(
            ciphertext,
            padding.OAEP(
                mgf=padding.MGF1(algorithm=hashes.SHA256()),
                algorithm=hashes.SHA256(),
                label=None
            )
        )
        return decrypted_bytes.decode('utf-8')
    except Exception as e:
        raise ValueError(f"Failed to decrypt RSA payload: {str(e)}")

def verify_and_authenticate_admin(encrypted_password_b64: str) -> Dict[str, Any]:
    """Decrypts RSA encrypted password, verifies against ADMIN_PASSWORD, and returns a session token if valid."""
    decrypted_password = decrypt_rsa_payload(encrypted_password_b64)
    
    if decrypted_password != ADMIN_PASSWORD:
        raise ValueError("Invalid administrator password.")
    
    # Generate secure 256-bit token
    token = secrets.token_hex(32)
    ACTIVE_TOKENS[token] = time.time() + TOKEN_EXPIRY_SECONDS
    return {
        "success": True,
        "token": token,
        "expires_in": TOKEN_EXPIRY_SECONDS,
        "message": "Admin authentication successful."
    }

def verify_admin_token(token: Optional[str]) -> bool:
    """Checks if provided admin session token is valid and unexpired."""
    if not token:
        return False
    
    expiry = ACTIVE_TOKENS.get(token)
    if not expiry:
        return False
    
    if time.time() > expiry:
        del ACTIVE_TOKENS[token]
        return False
    
    return True
