import React, { useState, useEffect } from 'react';
import {
  Shield, Lock, Key, ArrowLeft, Database, Plus, Trash2, Link, Play,
  RefreshCw, Code, CheckCircle, AlertCircle, LogOut, Eye, EyeOff, Globe, MapPin, Cpu, Sun, Moon
} from 'lucide-react';
import {
  loginAdmin, getAdminToken, logoutAdmin, isAdminAuthenticated,
  fetchIncidents, addIncidentFromUrl, deleteIncidentFromDb, triggerAgentPipeline
} from '../data/incidentsData';

export default function AdminPage() {
  const [isAuthenticated, setIsAuthenticated] = useState(isAdminAuthenticated());
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loginError, setLoginError] = useState(null);
  const [isAuthenticating, setIsAuthenticating] = useState(false);

  // Theme State: default to light mode (matching site aesthetic), persistent in localStorage
  const [theme, setTheme] = useState(() => localStorage.getItem('echofact_admin_theme') || 'light');

  // Admin Dashboard State
  const [incidents, setIncidents] = useState([]);
  const [isLoadingIncidents, setIsLoadingIncidents] = useState(false);
  const [activeTab, setActiveTab] = useState('add_url'); // 'add_url', 'manage_db', 'pipeline'
  const [newsUrl, setNewsUrl] = useState('');
  const [pipelinePrompt, setPipelinePrompt] = useState('Discover and research recent major global news updates');
  const [isProcessing, setIsProcessing] = useState(false);
  const [statusMessage, setStatusMessage] = useState(null);
  const [selectedJsonView, setSelectedJsonView] = useState(null);
  const [deleteConfirmIncident, setDeleteConfirmIncident] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);
  const [searchFilter, setSearchFilter] = useState('');

  const toggleTheme = () => {
    const nextTheme = theme === 'light' ? 'dark' : 'light';
    setTheme(nextTheme);
    localStorage.setItem('echofact_admin_theme', nextTheme);
  };

  const loadIncidents = async () => {
    setIsLoadingIncidents(true);
    const data = await fetchIncidents();
    if (data) setIncidents(data);
    setIsLoadingIncidents(false);
  };

  useEffect(() => {
    if (isAuthenticated) {
      loadIncidents();
    }
  }, [isAuthenticated]);

  const handleLogin = async (e) => {
    e.preventDefault();
    if (!password.trim()) return;

    setIsAuthenticating(true);
    setLoginError(null);

    try {
      await loginAdmin(password.trim());
      setIsAuthenticated(true);
      setPassword('');
    } catch (err) {
      setLoginError(err.message || 'Invalid administrator credentials.');
    } finally {
      setIsAuthenticating(false);
    }
  };

  const handleLogout = () => {
    logoutAdmin();
    setIsAuthenticated(false);
  };

  const handleUrlSubmit = async (e) => {
    e.preventDefault();
    if (!newsUrl.trim()) return;

    setIsProcessing(true);
    setStatusMessage(null);

    try {
      const newIncident = await addIncidentFromUrl(newsUrl.trim());
      setStatusMessage({ type: 'success', text: `Successfully processed URL into incident: "${newIncident.title}"` });
      setNewsUrl('');
      await loadIncidents();
    } catch (err) {
      if (err.message?.includes('Unauthorized') || !isAdminAuthenticated()) {
        setIsAuthenticated(false);
        setLoginError('Admin session expired or invalid. Please log in again.');
      } else {
        setStatusMessage({ type: 'error', text: err.message || 'Failed to process URL into incident.' });
      }
    } finally {
      setIsProcessing(false);
    }
  };

  const handleRunPipeline = async (e) => {
    e.preventDefault();
    setIsProcessing(true);
    setStatusMessage(null);

    try {
      const res = await triggerAgentPipeline(pipelinePrompt);
      setStatusMessage({ type: 'success', text: res.message || 'Multi-Agent Pipeline launched in background!' });
    } catch (err) {
      if (err.message?.includes('Unauthorized') || !isAdminAuthenticated()) {
        setIsAuthenticated(false);
        setLoginError('Admin session expired or invalid. Please log in again.');
      } else {
        setStatusMessage({ type: 'error', text: err.message || 'Failed to trigger agent pipeline.' });
      }
    } finally {
      setIsProcessing(false);
    }
  };

  const triggerDeleteIncident = (id, title) => {
    setDeleteConfirmIncident({ id, title });
  };

  const confirmDeleteIncident = async () => {
    if (!deleteConfirmIncident) return;
    const { id, title } = deleteConfirmIncident;
    setIsDeleting(true);

    try {
      await deleteIncidentFromDb(id);
      setIncidents(prev => prev.filter(inc => inc.id !== id));
      setStatusMessage({ type: 'success', text: `Deleted incident "${title}" from database.` });
      setDeleteConfirmIncident(null);
      await loadIncidents();
    } catch (err) {
      setDeleteConfirmIncident(null);
      if (err.message?.includes('Unauthorized') || !isAdminAuthenticated()) {
        setIsAuthenticated(false);
        setLoginError('Admin session expired or invalid. Please log in again.');
      } else {
        setStatusMessage({ type: 'error', text: err.message || 'Failed to delete incident.' });
      }
    } finally {
      setIsDeleting(false);
    }
  };

  const filteredIncidents = incidents.filter(inc => {
    if (!searchFilter.trim()) return true;
    const q = searchFilter.toLowerCase();
    return (
      inc.title?.toLowerCase().includes(q) ||
      inc.locationName?.toLowerCase().includes(q) ||
      inc.category?.toLowerCase().includes(q) ||
      inc.id?.toLowerCase().includes(q)
    );
  });

  const isLight = theme === 'light';

  // --- UNAUTHENTICATED LOGIN SCREEN ---
  if (!isAuthenticated) {
    return (
      <div className={`min-h-screen w-full flex flex-col items-center justify-center p-4 relative overflow-hidden font-sans select-text transition-colors duration-300 ${
        isLight ? 'bg-gradient-to-br from-green-50 via-emerald-50/60 to-teal-50 text-slate-800' : 'bg-slate-950 text-slate-100'
      }`}>
        
        {/* Theme Toggle Button (Top Right) */}
        <div className="absolute top-5 right-5 z-20">
          <button
            onClick={toggleTheme}
            className={`p-2.5 rounded-full border shadow-md transition flex items-center gap-2 text-xs font-semibold ${
              isLight
                ? 'bg-white text-slate-700 border-green-200 hover:bg-green-50 shadow-green-950/5'
                : 'bg-slate-900 text-emerald-400 border-slate-800 hover:bg-slate-800'
            }`}
            title={`Switch to ${isLight ? 'Dark' : 'Light'} Mode`}
          >
            {isLight ? <Moon className="w-4 h-4 text-slate-600" /> : <Sun className="w-4 h-4 text-amber-400" />}
            <span>{isLight ? 'Dark Mode' : 'Light Mode'}</span>
          </button>
        </div>

        {/* Background Orbs */}
        <div className={`absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-96 h-96 rounded-full blur-3xl pointer-events-none ${
          isLight ? 'bg-emerald-300/30' : 'bg-emerald-500/10'
        }`} />
        <div className={`absolute bottom-1/4 right-1/4 w-80 h-80 rounded-full blur-3xl pointer-events-none ${
          isLight ? 'bg-teal-300/20' : 'bg-teal-500/10'
        }`} />

        <div className={`w-full max-w-md border rounded-3xl p-6 sm:p-8 shadow-2xl backdrop-blur-xl relative z-10 space-y-6 transition-all duration-300 ${
          isLight
            ? 'bg-white/90 border-green-200/80 shadow-emerald-950/10'
            : 'bg-slate-900/90 border-slate-800 shadow-slate-950/50'
        }`}>
          
          {/* Header & Logo */}
          <div className="text-center space-y-2">
            <div className="w-14 h-14 mx-auto rounded-2xl bg-gradient-to-tr from-emerald-500 via-teal-500 to-cyan-500 p-0.5 shadow-lg shadow-emerald-500/20">
              <div className={`w-full h-full rounded-[14px] flex items-center justify-center ${
                isLight ? 'bg-white' : 'bg-slate-950'
              }`}>
                <Shield className="w-7 h-7 text-emerald-600" />
              </div>
            </div>
            <h1 className={`text-xl sm:text-2xl font-bold font-heading tracking-tight ${
              isLight ? 'text-slate-900' : 'text-white'
            }`}>
              EchoFact Admin Console
            </h1>
            <p className={`text-xs ${isLight ? 'text-slate-500' : 'text-slate-400'}`}>
              Database management, URL ingest & Multi-Agent orchestration
            </p>
          </div>

          {/* RSA Encryption Security Status Banner */}
          <div className={`p-3 rounded-2xl border flex items-center gap-3 ${
            isLight
              ? 'bg-emerald-50/80 border-emerald-200 text-slate-800'
              : 'bg-slate-950 border-slate-800 text-slate-100'
          }`}>
            <Cpu className="w-5 h-5 text-emerald-600 flex-shrink-0 animate-pulse" />
            <div className="text-[11px] leading-tight">
              <span className={`font-bold block font-mono ${isLight ? 'text-emerald-900' : 'text-slate-200'}`}>
                RSA 2048-BIT ENCRYPTION
              </span>
              <span className={isLight ? 'text-slate-600' : 'text-slate-400'}>
                Password encrypted client-side with server Public Key before payload transmission.
              </span>
            </div>
          </div>

          {/* Login Form */}
          <form onSubmit={handleLogin} className="space-y-4">
            <div className="space-y-1.5">
              <label className={`block text-xs font-mono font-semibold ${
                isLight ? 'text-slate-700' : 'text-slate-300'
              }`}>
                MASTER ADMIN PASSWORD:
              </label>
              <div className="relative">
                <div className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-400">
                  <Lock className="w-4 h-4 text-emerald-600" />
                </div>
                <input
                  type={showPassword ? 'text' : 'password'}
                  required
                  autoFocus
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Enter administrator password..."
                  className={`w-full pl-10 pr-10 py-3 rounded-xl text-xs font-mono transition ${
                    isLight
                      ? 'bg-slate-50 border border-slate-300 text-slate-900 placeholder-slate-400 focus:bg-white focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20'
                      : 'bg-slate-950 border border-slate-700/80 text-white placeholder-slate-500 focus:border-emerald-400 focus:ring-1 focus:ring-emerald-400/40'
                  }`}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 transition p-1"
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            {loginError && (
              <div className={`p-3 rounded-xl border text-xs font-mono flex items-center gap-2 ${
                isLight
                  ? 'bg-rose-50 border-rose-200 text-rose-700'
                  : 'bg-rose-950/80 border-rose-500/40 text-rose-300'
              }`}>
                <AlertCircle className="w-4 h-4 text-rose-500 flex-shrink-0" />
                <span>{loginError}</span>
              </div>
            )}

            <button
              type="submit"
              disabled={isAuthenticating || !password.trim()}
              className="w-full py-3 bg-gradient-to-r from-emerald-600 via-teal-600 to-cyan-600 hover:from-emerald-500 hover:to-cyan-500 disabled:opacity-50 text-white font-bold text-xs rounded-xl shadow-lg shadow-emerald-600/20 transition flex items-center justify-center gap-2 font-mono uppercase tracking-wider"
            >
              {isAuthenticating ? (
                <>
                  <RefreshCw className="w-4 h-4 animate-spin" />
                  <span>Encrypting Payload & Authenticating...</span>
                </>
              ) : (
                <>
                  <Key className="w-4 h-4" />
                  <span>Authenticate & Open Console</span>
                </>
              )}
            </button>
          </form>

          {/* Quick Credential Hint */}
          <div className={`text-center text-[11px] font-mono flex items-center justify-center gap-1.5 pt-2 border-t ${
            isLight ? 'border-slate-200 text-slate-500' : 'border-slate-800/80 text-slate-500'
          }`}>
            <span>Default Credential:</span>
            <span className={`px-2 py-0.5 rounded font-bold border ${
              isLight ? 'bg-emerald-100 text-emerald-800 border-emerald-300' : 'bg-slate-800 text-emerald-400 border-slate-700'
            }`}>admin123</span>
          </div>

          {/* Return to Radar */}
          <div className="text-center pt-1">
            <a
              href="/"
              className={`inline-flex items-center gap-1.5 text-xs transition font-medium ${
                isLight ? 'text-slate-600 hover:text-emerald-700' : 'text-slate-400 hover:text-emerald-400'
              }`}
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              <span>Return to Main EchoFact News Radar</span>
            </a>
          </div>
        </div>
      </div>
    );
  }

  // --- AUTHENTICATED DASHBOARD VIEW ---
  return (
    <div className={`min-h-screen w-full flex flex-col font-sans select-text transition-colors duration-300 ${
      isLight ? 'bg-slate-50 text-slate-800' : 'bg-slate-950 text-slate-100'
    }`}>
      
      {/* Navbar Header */}
      <header className={`px-4 lg:px-8 py-3.5 border-b flex items-center justify-between gap-4 flex-shrink-0 sticky top-0 z-50 transition-colors ${
        isLight ? 'bg-white/90 border-slate-200/80 backdrop-blur-md shadow-sm' : 'bg-slate-900 border-slate-800'
      }`}>
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-emerald-500 to-teal-500 flex items-center justify-center shadow-md shadow-emerald-500/20">
            <Shield className="w-5 h-5 text-white" />
          </div>
          <div>
            <h1 className={`text-base sm:text-lg font-bold font-heading flex items-center gap-2 ${
              isLight ? 'text-slate-900' : 'text-white'
            }`}>
              EchoFact Admin Console
              <span className={`text-[10px] font-mono px-2 py-0.5 rounded border font-semibold ${
                isLight
                  ? 'bg-emerald-100 text-emerald-800 border-emerald-300'
                  : 'bg-emerald-500/20 text-emerald-400 border-emerald-500/40'
              }`}>
                SQLITE DB & RSA AUTH
              </span>
            </h1>
            <p className={`text-xs ${isLight ? 'text-slate-500' : 'text-slate-400'}`}>
              Authenticated Administrator Session Active
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2.5">
          {/* Theme Toggle Button */}
          <button
            onClick={toggleTheme}
            className={`p-2 rounded-xl border transition flex items-center gap-1.5 text-xs font-semibold ${
              isLight
                ? 'bg-slate-100 hover:bg-slate-200 text-slate-700 border-slate-300'
                : 'bg-slate-800 hover:bg-slate-700 text-emerald-400 border-slate-700'
            }`}
            title={`Switch to ${isLight ? 'Dark' : 'Light'} Mode`}
          >
            {isLight ? <Moon className="w-3.5 h-3.5 text-slate-600" /> : <Sun className="w-3.5 h-3.5 text-amber-400" />}
            <span className="hidden sm:inline">{isLight ? 'Dark Mode' : 'Light Mode'}</span>
          </button>

          <a
            href="/"
            className={`px-3 py-1.5 border text-xs font-semibold rounded-xl transition flex items-center gap-1.5 ${
              isLight
                ? 'bg-white hover:bg-slate-50 text-slate-700 border-slate-300'
                : 'bg-slate-800 hover:bg-slate-700 text-slate-300 border-slate-700'
            }`}
          >
            <Globe className="w-3.5 h-3.5 text-emerald-600" />
            <span className="hidden sm:inline">Open Radar Site</span>
          </a>

          <button
            onClick={handleLogout}
            className={`px-3 py-1.5 border text-xs font-semibold rounded-xl transition flex items-center gap-1.5 font-mono ${
              isLight
                ? 'bg-rose-50 hover:bg-rose-100 text-rose-700 border-rose-200'
                : 'bg-rose-950/80 hover:bg-rose-900 border-rose-800 text-rose-300'
            }`}
            title="Log out of Admin Session"
          >
            <LogOut className="w-3.5 h-3.5" />
            <span>Logout</span>
          </button>
        </div>
      </header>

      {/* Navigation Sub-Header */}
      <div className={`px-4 lg:px-8 py-3 border-b flex items-center justify-between flex-wrap gap-3 ${
        isLight ? 'bg-slate-100/70 border-slate-200' : 'bg-slate-900/50 border-slate-800'
      }`}>
        <div className="flex items-center gap-2 overflow-x-auto">
          <button
            onClick={() => setActiveTab('add_url')}
            className={`px-4 py-2 rounded-xl text-xs font-semibold transition flex items-center gap-2 ${
              activeTab === 'add_url'
                ? 'bg-emerald-600 text-white shadow-md border border-emerald-500'
                : isLight
                ? 'bg-white text-slate-600 hover:text-slate-900 border border-slate-200'
                : 'bg-slate-900 text-slate-400 hover:text-white border border-slate-800'
            }`}
          >
            <Link className="w-4 h-4" />
            <span>Import URL</span>
          </button>

          <button
            onClick={() => setActiveTab('manage_db')}
            className={`px-4 py-2 rounded-xl text-xs font-semibold transition flex items-center gap-2 ${
              activeTab === 'manage_db'
                ? 'bg-emerald-600 text-white shadow-md border border-emerald-500'
                : isLight
                ? 'bg-white text-slate-600 hover:text-slate-900 border border-slate-200'
                : 'bg-slate-900 text-slate-400 hover:text-white border border-slate-800'
            }`}
          >
            <Database className="w-4 h-4" />
            <span>Database Records ({incidents.length})</span>
          </button>

          <button
            onClick={() => setActiveTab('pipeline')}
            className={`px-4 py-2 rounded-xl text-xs font-semibold transition flex items-center gap-2 ${
              activeTab === 'pipeline'
                ? 'bg-emerald-600 text-white shadow-md border border-emerald-500'
                : isLight
                ? 'bg-white text-slate-600 hover:text-slate-900 border border-slate-200'
                : 'bg-slate-900 text-slate-400 hover:text-white border border-slate-800'
            }`}
          >
            <Play className="w-4 h-4" />
            <span>Agent Pipeline</span>
          </button>
        </div>

        <button
          onClick={loadIncidents}
          disabled={isLoadingIncidents}
          className={`px-3.5 py-1.5 text-xs border rounded-xl transition flex items-center gap-1.5 font-mono ${
            isLight
              ? 'bg-white hover:bg-slate-50 text-slate-700 border-slate-300'
              : 'bg-slate-800 hover:bg-slate-700 text-slate-300 border-slate-700'
          }`}
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isLoadingIncidents ? 'animate-spin' : ''}`} />
          <span>Reload DB</span>
        </button>
      </div>

      {/* Alert Status Notification */}
      {statusMessage && (
        <div className={`px-4 lg:px-8 py-3 text-xs font-mono border-b flex items-center justify-between gap-3 ${
          statusMessage.type === 'success'
            ? isLight
              ? 'bg-emerald-50 text-emerald-900 border-emerald-200'
              : 'bg-emerald-950/90 text-emerald-300 border-emerald-500/40'
            : isLight
              ? 'bg-rose-50 text-rose-900 border-rose-200'
              : 'bg-rose-950/90 text-rose-300 border-rose-500/40'
        }`}>
          <div className="flex items-center gap-2">
            {statusMessage.type === 'success' ? <CheckCircle className="w-4 h-4 text-emerald-600" /> : <AlertCircle className="w-4 h-4 text-rose-600" />}
            <span>{statusMessage.text}</span>
          </div>
          <button onClick={() => setStatusMessage(null)} className="text-slate-500 hover:text-slate-900 font-bold">✕</button>
        </div>
      )}

      {/* Main Body Content */}
      <main className="flex-1 p-4 lg:p-8 max-w-7xl w-full mx-auto space-y-6">
        
        {/* TAB 1: IMPORT NEWS FROM URL */}
        {activeTab === 'add_url' && (
          <div className="max-w-3xl mx-auto space-y-5 py-4">
            <div className="space-y-1">
              <h2 className={`text-lg font-bold flex items-center gap-2 ${
                isLight ? 'text-slate-900' : 'text-white'
              }`}>
                <Plus className="w-5 h-5 text-emerald-600" /> Ingest Article from News URL
              </h2>
              <p className={`text-xs ${isLight ? 'text-slate-500' : 'text-slate-400'}`}>
                Paste any live news URL. The backend scrapes HTML metadata, geocodes coordinates, runs deduplication, and persists clean records into SQLite database.
              </p>
            </div>

            <form onSubmit={handleUrlSubmit} className={`space-y-4 p-6 rounded-3xl border shadow-xl transition-colors ${
              isLight ? 'bg-white border-slate-200 shadow-slate-900/5' : 'bg-slate-900 border-slate-800'
            }`}>
              <div className="space-y-2">
                <label className={`block text-xs font-mono font-bold ${
                  isLight ? 'text-slate-700' : 'text-slate-300'
                }`}>
                  NEWS ARTICLE URL:
                </label>
                <input
                  type="url"
                  required
                  placeholder="https://timesofindia.indiatimes.com/city/delhi/..."
                  value={newsUrl}
                  onChange={(e) => setNewsUrl(e.target.value)}
                  className={`w-full p-3.5 rounded-2xl text-xs transition ${
                    isLight
                      ? 'bg-slate-50 border border-slate-300 text-slate-900 placeholder-slate-400 focus:bg-white focus:border-emerald-500'
                      : 'bg-slate-950 border border-slate-700 text-white placeholder-slate-500 focus:border-emerald-400'
                  }`}
                />
              </div>

              <button
                type="submit"
                disabled={isProcessing || !newsUrl.trim()}
                className="w-full py-3.5 bg-gradient-to-r from-emerald-600 to-teal-600 hover:from-emerald-500 hover:to-teal-500 disabled:opacity-50 text-white font-bold text-xs rounded-2xl shadow-lg shadow-emerald-600/20 transition flex items-center justify-center gap-2 font-mono uppercase tracking-wider"
              >
                {isProcessing ? (
                  <>
                    <RefreshCw className="w-4 h-4 animate-spin" />
                    <span>Extracting HTML & Ingesting to SQLite...</span>
                  </>
                ) : (
                  <>
                    <Plus className="w-4 h-4" />
                    <span>Ingest Article to Database</span>
                  </>
                )}
              </button>
            </form>
          </div>
        )}

        {/* TAB 2: MANAGE DATABASE RECORDS */}
        {activeTab === 'manage_db' && (
          <div className="space-y-4">
            <div className={`flex flex-col sm:flex-row sm:items-center justify-between gap-3 p-4 rounded-2xl border ${
              isLight ? 'bg-white border-slate-200 shadow-sm' : 'bg-slate-900 border-slate-800'
            }`}>
              <div className={`text-xs font-mono ${isLight ? 'text-slate-600' : 'text-slate-400'}`}>
                Total Incidents in SQLite (`incidents.db`): <span className="text-emerald-600 font-bold text-sm">{incidents.length}</span>
              </div>
              <input
                type="text"
                placeholder="Filter by title, category, location..."
                value={searchFilter}
                onChange={(e) => setSearchFilter(e.target.value)}
                className={`px-3.5 py-1.5 border rounded-xl text-xs transition w-full sm:w-64 ${
                  isLight
                    ? 'bg-slate-50 border-slate-300 text-slate-900 placeholder-slate-400 focus:bg-white focus:border-emerald-500'
                    : 'bg-slate-950 border-slate-700 text-white placeholder-slate-500 focus:border-emerald-400'
                }`}
              />
            </div>

            <div className={`overflow-hidden border rounded-2xl shadow-xl ${
              isLight ? 'bg-white border-slate-200 shadow-slate-900/5' : 'bg-slate-900 border-slate-800'
            }`}>
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className={`border-b text-[11px] font-mono uppercase ${
                    isLight ? 'bg-slate-100 border-slate-200 text-slate-600' : 'bg-slate-950 border-slate-800 text-slate-400'
                  }`}>
                    <tr>
                      <th className="p-3.5">Incident Title & ID</th>
                      <th className="p-3.5">Category</th>
                      <th className="p-3.5">Location</th>
                      <th className="p-3.5">Sources</th>
                      <th className="p-3.5 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className={`divide-y ${isLight ? 'divide-slate-200' : 'divide-slate-800/60'}`}>
                    {filteredIncidents.map((inc) => (
                      <tr key={inc.id} className={`transition ${
                        isLight ? 'hover:bg-emerald-50/40' : 'hover:bg-slate-800/40'
                      }`}>
                        <td className={`p-3.5 font-semibold max-w-sm ${
                          isLight ? 'text-slate-800' : 'text-slate-200'
                        }`}>
                          <div className="line-clamp-2">{inc.title}</div>
                          <a
                            href={`/${inc.id}`}
                            target="_blank"
                            rel="noreferrer"
                            className={`inline-block text-[10px] font-mono px-1.5 py-0.5 rounded border hover:underline mt-1 ${
                              isLight
                                ? 'bg-emerald-50 text-emerald-700 border-emerald-300'
                                : 'bg-emerald-950/60 text-emerald-400 border-emerald-500/30'
                            }`}
                            title={`Open route http://localhost:5173/${inc.id}`}
                          >
                            {inc.id} ↗
                          </a>
                        </td>
                        <td className="p-3.5 font-mono text-[11px]">
                          <span className={`px-2.5 py-1 rounded-full border ${
                            isLight
                              ? 'bg-slate-100 text-slate-700 border-slate-200'
                              : 'bg-slate-950 text-slate-300 border-slate-800'
                          }`}>
                            {inc.category}
                          </span>
                        </td>
                        <td className={`p-3.5 font-mono text-[11px] ${
                          isLight ? 'text-slate-600' : 'text-slate-400'
                        }`}>
                          <div className="flex items-center gap-1.5">
                            <MapPin className="w-3.5 h-3.5 text-emerald-600 flex-shrink-0" />
                            <span>{inc.locationName}</span>
                          </div>
                          <span className={`text-[10px] ${isLight ? 'text-slate-400' : 'text-slate-500'}`}>
                            ({inc.lat}, {inc.lng})
                          </span>
                        </td>
                        <td className="p-3.5 font-mono text-emerald-600 text-[11px] font-bold">
                          {inc.sources?.length || 0} link(s)
                        </td>
                        <td className="p-3.5 text-right space-x-2 whitespace-nowrap">
                          <button
                            onClick={() => setSelectedJsonView(inc)}
                            className={`px-3 py-1 text-[11px] font-mono rounded-xl transition ${
                              isLight
                                ? 'bg-slate-100 hover:bg-slate-200 text-cyan-700 border border-slate-200'
                                : 'bg-slate-800 hover:bg-slate-700 text-cyan-400'
                            }`}
                          >
                            <Code className="w-3.5 h-3.5 inline mr-1" />
                            JSON
                          </button>
                          <button
                            onClick={() => triggerDeleteIncident(inc.id, inc.title)}
                            className={`px-3 py-1 border text-[11px] font-mono rounded-xl transition ${
                              isLight
                                ? 'bg-rose-50 hover:bg-rose-100 text-rose-700 border-rose-200'
                                : 'bg-rose-950/60 hover:bg-rose-900 border-rose-800 text-rose-300'
                            }`}
                          >
                            <Trash2 className="w-3.5 h-3.5 inline mr-1" />
                            Delete
                          </button>
                        </td>
                      </tr>
                    ))}
                    {filteredIncidents.length === 0 && (
                      <tr>
                        <td colSpan="5" className="p-8 text-center text-slate-500 font-mono text-xs">
                          No matching incidents found in database.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}

        {/* TAB 3: AGENT PIPELINE */}
        {activeTab === 'pipeline' && (
          <div className="max-w-3xl mx-auto space-y-5 py-4">
            <div className="space-y-1">
              <h2 className={`text-lg font-bold flex items-center gap-2 ${
                isLight ? 'text-slate-900' : 'text-white'
              }`}>
                <Play className="w-5 h-5 text-cyan-600" /> Run Multi-Agent Discovery Pipeline
              </h2>
              <p className={`text-xs ${isLight ? 'text-slate-500' : 'text-slate-400'}`}>
                Triggers the 4-stage Google ADK Multi-Agent pipeline (Scout → Researcher → Fact Checker → Writer) in background to discover news incidents across Google News.
              </p>
            </div>

            <form onSubmit={handleRunPipeline} className={`space-y-4 p-6 rounded-3xl border shadow-xl transition-colors ${
              isLight ? 'bg-white border-slate-200 shadow-slate-900/5' : 'bg-slate-900 border-slate-800'
            }`}>
              <div className="space-y-2">
                <label className={`block text-xs font-mono font-bold ${
                  isLight ? 'text-slate-700' : 'text-slate-300'
                }`}>
                  DISCOVERY PROMPT:
                </label>
                <input
                  type="text"
                  required
                  value={pipelinePrompt}
                  onChange={(e) => setPipelinePrompt(e.target.value)}
                  className={`w-full p-3.5 rounded-2xl text-xs font-mono transition ${
                    isLight
                      ? 'bg-slate-50 border border-slate-300 text-slate-900 focus:bg-white focus:border-cyan-500'
                      : 'bg-slate-950 border border-slate-700 text-white focus:border-cyan-400'
                  }`}
                />
              </div>

              <button
                type="submit"
                disabled={isProcessing}
                className="w-full py-3.5 bg-gradient-to-r from-cyan-600 to-blue-600 hover:from-cyan-500 hover:to-blue-500 disabled:opacity-50 text-white font-bold text-xs rounded-2xl shadow-lg shadow-cyan-600/20 transition flex items-center justify-center gap-2 font-mono uppercase tracking-wider"
              >
                {isProcessing ? (
                  <>
                    <RefreshCw className="w-4 h-4 animate-spin" />
                    <span>Launching Background Pipeline...</span>
                  </>
                ) : (
                  <>
                    <Play className="w-4 h-4" />
                    <span>Launch Discovery Pipeline</span>
                  </>
                )}
              </button>
            </form>
          </div>
        )}

      </main>

      {/* CONFIRM DELETE MODAL */}
      {deleteConfirmIncident && (
        <div className={`fixed inset-0 z-[999999] flex items-center justify-center p-4 backdrop-blur-md ${
          isLight ? 'bg-slate-900/40' : 'bg-slate-950/90'
        }`}>
          <div className={`relative w-full max-w-md border rounded-3xl p-6 space-y-4 shadow-2xl ${
            isLight ? 'bg-white border-rose-200 text-slate-800' : 'bg-slate-900 border-rose-900/50 text-slate-100'
          }`}>
            <div className="flex items-center gap-3">
              <div className="p-3 rounded-2xl bg-rose-100 text-rose-600 dark:bg-rose-950 dark:text-rose-400 border border-rose-300 dark:border-rose-800">
                <Trash2 className="w-6 h-6" />
              </div>
              <div>
                <h3 className="font-bold text-base">Delete Incident Record?</h3>
                <p className={`text-xs ${isLight ? 'text-slate-500' : 'text-slate-400'}`}>
                  This action will permanently delete this record from the SQLite database.
                </p>
              </div>
            </div>

            <div className={`p-3 rounded-xl border text-xs font-mono ${
              isLight ? 'bg-slate-50 border-slate-200 text-slate-700' : 'bg-slate-950 border-slate-800 text-slate-300'
            }`}>
              <div className="font-bold text-rose-600 dark:text-rose-400">{deleteConfirmIncident.title}</div>
              <div className="text-[10px] text-slate-400 mt-1">ID: {deleteConfirmIncident.id}</div>
            </div>

            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                type="button"
                disabled={isDeleting}
                onClick={() => setDeleteConfirmIncident(null)}
                className={`px-4 py-2 text-xs font-semibold rounded-xl border transition ${
                  isLight
                    ? 'bg-slate-100 hover:bg-slate-200 text-slate-700 border-slate-300'
                    : 'bg-slate-800 hover:bg-slate-700 text-slate-300 border-slate-700'
                }`}
              >
                Cancel
              </button>

              <button
                type="button"
                disabled={isDeleting}
                onClick={confirmDeleteIncident}
                className="px-5 py-2 text-xs font-bold font-mono text-white bg-rose-600 hover:bg-rose-700 disabled:opacity-50 rounded-xl shadow-lg shadow-rose-600/20 transition flex items-center gap-2"
              >
                {isDeleting ? (
                  <>
                    <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                    <span>Deleting...</span>
                  </>
                ) : (
                  <>
                    <Trash2 className="w-3.5 h-3.5" />
                    <span>Confirm Delete</span>
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* RAW JSON INSPECTOR MODAL */}
      {selectedJsonView && (
        <div className={`fixed inset-0 z-[999999] flex items-center justify-center p-4 backdrop-blur-md ${
          isLight ? 'bg-slate-900/40' : 'bg-slate-950/90'
        }`}>
          <div className={`relative w-full max-w-2xl border rounded-3xl p-5 space-y-4 flex flex-col max-h-[85vh] shadow-2xl ${
            isLight ? 'bg-white border-slate-200' : 'bg-slate-900 border-cyan-500/40'
          }`}>
            <div className="flex items-center justify-between border-b border-slate-200 pb-3">
              <span className={`font-mono text-xs font-bold ${
                isLight ? 'text-cyan-700' : 'text-cyan-400'
              }`}>
                JSON Record: {selectedJsonView.id}
              </span>
              <button onClick={() => setSelectedJsonView(null)} className="text-slate-400 hover:text-slate-700 font-bold">✕</button>
            </div>
            <pre className={`p-4 rounded-2xl text-[11px] font-mono overflow-auto flex-1 border ${
              isLight
                ? 'bg-slate-900 text-emerald-400 border-slate-800'
                : 'bg-slate-950 text-emerald-400 border-slate-800'
            }`}>
              {JSON.stringify(selectedJsonView, null, 2)}
            </pre>
            <div className="flex justify-end">
              <button
                onClick={() => setSelectedJsonView(null)}
                className={`px-5 py-2 text-xs font-semibold rounded-xl transition ${
                  isLight
                    ? 'bg-slate-100 hover:bg-slate-200 text-slate-700 border border-slate-200'
                    : 'bg-slate-800 hover:bg-slate-700 text-slate-200'
                }`}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
