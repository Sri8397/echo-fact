// EchoFact Incident Data API Client
// All news incident data is dynamically fetched and persisted in the SQLite Database backend.

const API_BASE_URL = 'http://localhost:8000';

// --- AUTHENTICATION STATE & RSA ENCRYPTION UTILITIES ---

export function getAdminToken() {
  return sessionStorage.getItem('echofact_admin_token') || localStorage.getItem('echofact_admin_token') || null;
}

export function setAdminToken(token) {
  sessionStorage.setItem('echofact_admin_token', token);
  localStorage.setItem('echofact_admin_token', token);
}

export function logoutAdmin() {
  sessionStorage.removeItem('echofact_admin_token');
  localStorage.removeItem('echofact_admin_token');
}

export function isAdminAuthenticated() {
  return !!getAdminToken();
}

/**
 * Encrypts a plaintext password using the backend's RSA 2048 Public Key via Web Crypto API.
 */
async function encryptPasswordWithPublicKey(password, pemPublicKey) {
  // Strip PEM headers and whitespace
  const pemHeader = "-----BEGIN PUBLIC KEY-----";
  const pemFooter = "-----END PUBLIC KEY-----";
  const pemContents = pemPublicKey
    .replace(pemHeader, "")
    .replace(pemFooter, "")
    .replace(/\s/g, "");

  // Base64 decode to binary ArrayBuffer
  const binaryDerString = window.atob(pemContents);
  const binaryDer = new Uint8Array(binaryDerString.length);
  for (let i = 0; i < binaryDerString.length; i++) {
    binaryDer[i] = binaryDerString.charCodeAt(i);
  }

  // Import public key into WebCrypto API
  const cryptoKey = await window.crypto.subtle.importKey(
    "spki",
    binaryDer.buffer,
    {
      name: "RSA-OAEP",
      hash: "SHA-256",
    },
    false,
    ["encrypt"]
  );

  // Encrypt plaintext password UTF-8 bytes
  const encoder = new TextEncoder();
  const dataBytes = encoder.encode(password);
  const encryptedBuffer = await window.crypto.subtle.encrypt(
    { name: "RSA-OAEP" },
    cryptoKey,
    dataBytes
  );

  // Convert ArrayBuffer to Base64 string
  const bytes = new Uint8Array(encryptedBuffer);
  let binary = '';
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return window.btoa(binary);
}

export async function loginAdmin(password) {
  try {
    // 1. Fetch RSA Public Key from backend
    const keyRes = await fetch(`${API_BASE_URL}/api/admin/public-key`);
    if (!keyRes.ok) throw new Error("Failed to fetch RSA security credentials from server.");
    const { public_key } = await keyRes.json();

    // 2. Encrypt password with Public Key on client side
    const encryptedPassword = await encryptPasswordWithPublicKey(password, public_key);

    // 3. Send encrypted payload to backend for decryption & validation
    const loginRes = await fetch(`${API_BASE_URL}/api/admin/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ encrypted_password: encryptedPassword })
    });

    if (loginRes.ok) {
      const data = await loginRes.json();
      setAdminToken(data.token);
      return data;
    } else {
      const err = await loginRes.json();
      throw new Error(err.detail || 'Authentication failed.');
    }
  } catch (e) {
    console.error("Admin Login Error:", e);
    throw e;
  }
}

// --- INCIDENT & DATABASE MANAGEMENT ENDPOINTS ---

export async function fetchIncidents() {
  try {
    const res = await fetch(`${API_BASE_URL}/api/incidents`);
    if (res.ok) {
      const data = await res.json();
      if (Array.isArray(data)) return data;
    }
  } catch (e) {
    console.error("Backend database offline or unreachable:", e);
  }
  return [];
}

export async function addIncidentFromUrl(url) {
  const token = getAdminToken();
  try {
    const res = await fetch(`${API_BASE_URL}/api/incidents/from-url`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Admin-Token': token || ''
      },
      body: JSON.stringify({ url })
    });
    if (res.ok) {
      return await res.json();
    } else {
      if (res.status === 401) {
        logoutAdmin();
      }
      const err = await res.json();
      throw new Error(err.detail || 'Failed to parse incident from URL');
    }
  } catch (e) {
    console.error("Error adding incident from URL:", e);
    throw e;
  }
}

export async function deleteIncidentFromDb(incidentId) {
  const token = getAdminToken();
  try {
    const res = await fetch(`${API_BASE_URL}/api/incidents/${encodeURIComponent(incidentId)}`, {
      method: 'DELETE',
      headers: {
        'X-Admin-Token': token || ''
      }
    });
    if (res.ok) {
      return await res.json();
    } else {
      if (res.status === 401) {
        logoutAdmin();
      }
      const err = await res.json();
      throw new Error(err.detail || 'Failed to delete incident');
    }
  } catch (e) {
    console.error("Error deleting incident:", e);
    throw e;
  }
}

export async function triggerAgentPipeline(prompt = "Discover and research recent major global news updates") {
  const token = getAdminToken();
  try {
    const res = await fetch(`${API_BASE_URL}/api/pipeline/run?prompt=${encodeURIComponent(prompt)}`, {
      method: 'POST',
      headers: {
        'X-Admin-Token': token || ''
      }
    });
    if (res.ok) {
      return await res.json();
    } else {
      if (res.status === 401) {
        logoutAdmin();
      }
      const err = await res.json();
      throw new Error(err.detail || 'Failed to trigger agent pipeline');
    }
  } catch (e) {
    console.error("Error triggering pipeline:", e);
    throw e;
  }
}
