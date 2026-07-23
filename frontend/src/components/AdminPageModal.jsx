import React, { useState } from 'react';
import { X, Database, Plus, Trash2, Link, Play, RefreshCw, Code, CheckCircle, AlertCircle, Shield, Globe, MapPin, Calendar, HelpCircle, Sparkles, CheckCircle2, ExternalLink } from 'lucide-react';
import { addIncidentFromUrl, deleteIncidentFromDb, triggerAgentPipeline, fetchIncidents } from '../data/incidentsData';

export default function AdminPageModal({ isOpen, onClose, incidents, onRefreshIncidents, onSelectIncident }) {
  const [activeTab, setActiveTab] = useState('add_url'); // 'add_url', 'manage_db', 'pipeline'
  const [newsUrl, setNewsUrl] = useState('');
  const [pipelinePrompt, setPipelinePrompt] = useState('Discover and research recent major global news updates');
  const [isProcessing, setIsProcessing] = useState(false);
  const [statusMessage, setStatusMessage] = useState(null);
  const [selectedJsonView, setSelectedJsonView] = useState(null);
  const [deleteConfirmIncident, setDeleteConfirmIncident] = useState(null);
  const [isDeleting, setIsDeleting] = useState(false);

  if (!isOpen) return null;

  const handleUrlSubmit = async (e) => {
    e.preventDefault();
    if (!newsUrl.trim()) return;

    setIsProcessing(true);
    setStatusMessage(null);

    try {
      const newIncident = await addIncidentFromUrl(newsUrl.trim());
      setStatusMessage({ type: 'success', text: `Successfully processed URL into incident: "${newIncident.title}"` });
      setNewsUrl('');
      if (onRefreshIncidents) await onRefreshIncidents();
    } catch (err) {
      setStatusMessage({ type: 'error', text: err.message || 'Failed to process URL into incident.' });
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
      setStatusMessage({ type: 'error', text: err.message || 'Failed to trigger agent pipeline.' });
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
      setStatusMessage({ type: 'success', text: `Deleted incident "${title}" from database.` });
      setDeleteConfirmIncident(null);
      if (onRefreshIncidents) await onRefreshIncidents();
    } catch (err) {
      setDeleteConfirmIncident(null);
      setStatusMessage({ type: 'error', text: err.message || 'Failed to delete incident.' });
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-[999999] flex items-center justify-center p-3 sm:p-5 bg-slate-950/90 backdrop-blur-xl animate-fade-in select-text">
      <div className="relative w-full max-w-4xl bg-slate-900 border border-slate-700/80 rounded-3xl overflow-hidden shadow-2xl flex flex-col max-h-[90vh]">
        
        {/* Header */}
        <div className="p-4 sm:p-5 border-b border-slate-800 bg-slate-950 flex items-center justify-between gap-4 flex-shrink-0">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-2xl bg-emerald-500/20 text-emerald-400 border border-emerald-500/40">
              <Shield className="w-6 h-6" />
            </div>
            <div>
              <h2 className="text-lg sm:text-xl font-bold text-white font-heading flex items-center gap-2">
                EchoFact Admin Console <span className="text-[10px] font-mono bg-emerald-500/20 text-emerald-400 px-2 py-0.5 rounded border border-emerald-500/40">SQLITE DB</span>
              </h2>
              <p className="text-xs text-slate-400">Database management, URL ingest & Multi-Agent orchestration</p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-2 rounded-full text-slate-400 hover:text-white hover:bg-slate-800 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Navigation Tabs */}
        <div className="px-5 py-2.5 bg-slate-950/60 border-b border-slate-800 flex items-center justify-between flex-wrap gap-2 flex-shrink-0">
          <div className="flex items-center gap-2">
            <button
              onClick={() => setActiveTab('add_url')}
              className={`px-3.5 py-1.5 rounded-xl text-xs font-semibold transition flex items-center gap-1.5 ${
                activeTab === 'add_url'
                  ? 'bg-emerald-600 text-white shadow border border-emerald-500'
                  : 'bg-slate-800/80 text-slate-400 hover:text-white'
              }`}
            >
              <Link className="w-3.5 h-3.5" />
              <span>Import URL</span>
            </button>

            <button
              onClick={() => setActiveTab('manage_db')}
              className={`px-3.5 py-1.5 rounded-xl text-xs font-semibold transition flex items-center gap-1.5 ${
                activeTab === 'manage_db'
                  ? 'bg-emerald-600 text-white shadow border border-emerald-500'
                  : 'bg-slate-800/80 text-slate-400 hover:text-white'
              }`}
            >
              <Database className="w-3.5 h-3.5" />
              <span>Database Records ({incidents.length})</span>
            </button>

            <button
              onClick={() => setActiveTab('pipeline')}
              className={`px-3.5 py-1.5 rounded-xl text-xs font-semibold transition flex items-center gap-1.5 ${
                activeTab === 'pipeline'
                  ? 'bg-emerald-600 text-white shadow border border-emerald-500'
                  : 'bg-slate-800/80 text-slate-400 hover:text-white'
              }`}
            >
              <Play className="w-3.5 h-3.5" />
              <span>Agent Pipeline</span>
            </button>
          </div>

          <button
            onClick={onRefreshIncidents}
            className="px-3 py-1.5 text-xs text-slate-400 hover:text-white bg-slate-800 hover:bg-slate-700 rounded-xl transition flex items-center gap-1 font-mono"
            title="Reload from SQLite Database"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            <span>Reload DB</span>
          </button>
        </div>

        {/* Feedback Alert Message */}
        {statusMessage && (
          <div className={`px-5 py-2.5 text-xs font-mono border-b flex items-center gap-2 flex-shrink-0 ${
            statusMessage.type === 'success' ? 'bg-emerald-950/80 text-emerald-300 border-emerald-500/40' : 'bg-rose-950/80 text-rose-300 border-rose-500/40'
          }`}>
            {statusMessage.type === 'success' ? <CheckCircle className="w-4 h-4 text-emerald-400" /> : <AlertCircle className="w-4 h-4 text-rose-400" />}
            <span className="flex-1">{statusMessage.text}</span>
            <button onClick={() => setStatusMessage(null)} className="text-slate-400 hover:text-white">✕</button>
          </div>
        )}

        {/* Content Body */}
        <div className="p-5 overflow-y-auto flex-1 space-y-4">
          
          {/* TAB 1: ADD NEWS FROM URL */}
          {activeTab === 'add_url' && (
            <div className="space-y-4 max-w-2xl mx-auto py-3">
              <div className="space-y-1">
                <h3 className="text-base font-bold text-white flex items-center gap-2">
                  <Plus className="w-4 h-4 text-emerald-400" /> Ingest News Article from URL
                </h3>
                <p className="text-xs text-slate-400">
                  Paste any news URL. The backend scrapes HTML metadata, geocodes the location, runs deduplication, and inserts/merges records into SQLite.
                </p>
              </div>

              <form onSubmit={handleUrlSubmit} className="space-y-4 p-5 rounded-2xl bg-slate-950 border border-slate-800">
                <div className="space-y-1.5">
                  <label className="block text-xs font-mono text-slate-300 font-bold">
                    NEWS ARTICLE URL:
                  </label>
                  <input
                    type="url"
                    required
                    placeholder="https://timesofindia.indiatimes.com/city/delhi/..."
                    value={newsUrl}
                    onChange={(e) => setNewsUrl(e.target.value)}
                    className="w-full p-3 bg-slate-900 border border-slate-700 rounded-xl text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-emerald-400 transition"
                  />
                </div>

                <button
                  type="submit"
                  disabled={isProcessing || !newsUrl.trim()}
                  className="w-full py-3 bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-400 hover:to-teal-500 disabled:opacity-50 text-slate-950 font-bold text-xs rounded-xl shadow-lg transition flex items-center justify-center gap-2"
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

          {/* TAB 2: DATABASE RECORDS MANAGEMENT */}
          {activeTab === 'manage_db' && (
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-mono text-slate-400">
                  Total Records in SQLite (`incidents.db`): <span className="text-emerald-400 font-bold">{incidents.length}</span>
                </span>
              </div>

              <div className="overflow-x-auto border border-slate-800 rounded-2xl bg-slate-950 shadow-inner">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-900 border-b border-slate-800 text-[11px] font-mono text-slate-400">
                    <tr>
                      <th className="p-3.5 min-w-[200px]">INCIDENT TITLE & ID</th>
                      <th className="p-3.5 min-w-[110px]">DATE / YEAR</th>
                      <th className="p-3.5 min-w-[130px]">LOCATION & CAT</th>
                      <th className="p-3.5 min-w-[220px]">CURRENT REALITY</th>
                      <th className="p-3.5 min-w-[220px]">KEY QUESTIONS</th>
                      <th className="p-3.5 min-w-[180px]">VERIFIED FACTS</th>
                      <th className="p-3.5 min-w-[160px]">SOURCES</th>
                      <th className="p-3.5 text-right min-w-[120px]">ACTIONS</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800/70">
                    {incidents.map((inc) => (
                      <tr key={inc.id} className="hover:bg-slate-900/60 transition align-top">
                        {/* 1. Title & ID */}
                        <td className="p-3.5 font-semibold text-slate-200">
                          <div className="font-bold text-xs sm:text-sm text-white leading-snug line-clamp-2">{inc.title}</div>
                          <span className="inline-block mt-1 text-[10px] font-mono text-emerald-400 bg-emerald-950/60 px-1.5 py-0.5 rounded border border-emerald-500/30">
                            {inc.id}
                          </span>
                          {inc.summary && (
                            <p className="text-[11px] text-slate-400 font-sans mt-1.5 line-clamp-2 leading-relaxed">
                              {inc.summary}
                            </p>
                          )}
                        </td>

                        {/* 2. Date & Year */}
                        <td className="p-3.5 font-mono text-[11px] whitespace-nowrap">
                          <div className="flex items-center gap-1 text-emerald-400 font-bold">
                            <Calendar className="w-3.5 h-3.5 text-emerald-500 flex-shrink-0" />
                            <span>{inc.date}</span>
                          </div>
                          <div className="text-[10px] text-slate-400 font-mono mt-0.5">Year: {inc.year}</div>
                        </td>

                        {/* 3. Location & Category */}
                        <td className="p-3.5 font-mono text-[11px]">
                          <div className="flex items-center gap-1 text-slate-300 font-semibold">
                            <MapPin className="w-3.5 h-3.5 text-emerald-400 flex-shrink-0" />
                            <span className="truncate max-w-[120px]" title={inc.locationName}>{inc.locationName}</span>
                          </div>
                          <span className="inline-block mt-1 px-2 py-0.5 rounded-full bg-slate-800 text-slate-300 border border-slate-700 text-[10px]">
                            {inc.category}
                          </span>
                        </td>

                        {/* 4. Current Reality Column */}
                        <td className="p-3.5 font-sans text-xs">
                          {inc.currentReality ? (
                            <div className="p-2.5 rounded-xl bg-slate-900 border border-slate-800 text-slate-300 leading-relaxed text-[11px]">
                              <span className="text-[10px] font-mono text-emerald-400 font-bold block mb-1">
                                CURRENT REALITY:
                              </span>
                              <p className="line-clamp-3">{inc.currentReality}</p>
                            </div>
                          ) : (
                            <span className="text-slate-500 font-mono text-[11px]">No data recorded</span>
                          )}
                        </td>

                        {/* 5. Key Questions Column */}
                        <td className="p-3.5 font-sans text-xs">
                          {inc.keyQuestions && inc.keyQuestions.length > 0 ? (
                            <div className="space-y-1">
                              <span className="text-[10px] font-mono text-amber-400 font-bold block mb-1">
                                {inc.keyQuestions.length} QUESTION(S):
                              </span>
                              <ul className="space-y-1">
                                {inc.keyQuestions.map((q, qIdx) => (
                                  <li key={qIdx} className="text-[11px] text-amber-200/90 bg-amber-950/40 p-1.5 rounded border border-amber-500/20 leading-tight line-clamp-2">
                                    • {q}
                                  </li>
                                ))}
                              </ul>
                            </div>
                          ) : (
                            <span className="text-slate-500 font-mono text-[11px]">None listed</span>
                          )}
                        </td>

                        {/* 6. Verified Facts Column */}
                        <td className="p-3.5 font-sans text-xs">
                          {inc.verifiedFacts && inc.verifiedFacts.length > 0 ? (
                            <div className="space-y-1">
                              <span className="text-[10px] font-mono text-cyan-400 font-bold block mb-1">
                                {inc.verifiedFacts.length} FACT(S):
                              </span>
                              <ul className="space-y-1">
                                {inc.verifiedFacts.slice(0, 2).map((f, fIdx) => (
                                  <li key={fIdx} className="text-[11px] text-cyan-200/90 bg-cyan-950/30 p-1.5 rounded border border-cyan-500/20 leading-tight line-clamp-2">
                                    ✓ {f}
                                  </li>
                                ))}
                                {inc.verifiedFacts.length > 2 && (
                                  <span className="text-[10px] text-slate-400 font-mono block">
                                    +{inc.verifiedFacts.length - 2} more...
                                  </span>
                                )}
                              </ul>
                            </div>
                          ) : (
                            <span className="text-slate-500 font-mono text-[11px]">None listed</span>
                          )}
                        </td>

                        {/* 7. Sources Column */}
                        <td className="p-3.5 font-mono text-[11px]">
                          {inc.sources && inc.sources.length > 0 ? (
                            <div className="space-y-1">
                              {inc.sources.map((src, sIdx) => (
                                <a
                                  key={sIdx}
                                  href={src.url}
                                  target="_blank"
                                  rel="noreferrer"
                                  className="block p-1.5 rounded bg-slate-900 border border-slate-800 hover:border-emerald-500/50 text-[10px] text-emerald-400 hover:underline truncate max-w-[150px]"
                                  title={`${src.publisher} (${src.date}): ${src.title}`}
                                >
                                  <span className="font-bold">{src.publisher}</span>
                                  {src.date && <span className="text-slate-500 font-normal"> · {src.date}</span>}
                                </a>
                              ))}
                            </div>
                          ) : (
                            <span className="text-slate-500 text-[10px]">No links</span>
                          )}
                        </td>

                        {/* 8. Actions */}
                        <td className="p-3.5 text-right space-y-1.5 whitespace-nowrap">
                          <button
                            onClick={() => setSelectedJsonView(inc)}
                            className="w-full px-2.5 py-1 bg-slate-800 hover:bg-slate-700 text-cyan-400 text-[11px] font-mono rounded-lg transition border border-slate-700 flex items-center justify-center gap-1"
                            title="Inspect raw JSON record"
                          >
                            <Code className="w-3.5 h-3.5" />
                            <span>JSON</span>
                          </button>

                          <button
                            onClick={() => triggerDeleteIncident(inc.id, inc.title)}
                            className="w-full px-2.5 py-1 bg-rose-950/60 hover:bg-rose-900 border border-rose-800 text-rose-300 text-[11px] font-mono rounded-lg transition flex items-center justify-center gap-1"
                            title="Delete from SQLite database"
                          >
                            <Trash2 className="w-3.5 h-3.5" />
                            <span>Delete</span>
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {/* TAB 3: AGENT PIPELINE */}
          {activeTab === 'pipeline' && (
            <div className="space-y-4 max-w-2xl mx-auto py-3">
              <div className="space-y-1">
                <h3 className="text-base font-bold text-white flex items-center gap-2">
                  <Play className="w-4 h-4 text-cyan-400" /> Run Multi-Agent Discovery Pipeline
                </h3>
                <p className="text-xs text-slate-400">
                  Triggers the 4-stage Google ADK Multi-Agent pipeline (Scout → Researcher → Fact Checker → Writer) in background to discover news incidents across Google News.
                </p>
              </div>

              <form onSubmit={handleRunPipeline} className="space-y-4 p-5 rounded-2xl bg-slate-950 border border-slate-800">
                <div className="space-y-1.5">
                  <label className="block text-xs font-mono text-slate-300 font-bold">
                    DISCOVERY PROMPT:
                  </label>
                  <input
                    type="text"
                    required
                    value={pipelinePrompt}
                    onChange={(e) => setPipelinePrompt(e.target.value)}
                    className="w-full p-3 bg-slate-900 border border-slate-700 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-cyan-400 transition"
                  />
                </div>

                <button
                  type="submit"
                  disabled={isProcessing}
                  className="w-full py-3 bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 disabled:opacity-50 text-slate-950 font-bold text-xs rounded-xl shadow-lg transition flex items-center justify-center gap-2"
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

        </div>

        {/* CONFIRM DELETE MODAL */}
        {deleteConfirmIncident && (
          <div className="fixed inset-0 z-[9999999] flex items-center justify-center p-4 bg-slate-950/90 backdrop-blur-md">
            <div className="relative w-full max-w-md bg-slate-900 border border-rose-900/50 rounded-2xl p-5 space-y-4 text-slate-100 shadow-2xl">
              <div className="flex items-center gap-3">
                <div className="p-3 rounded-2xl bg-rose-950 text-rose-400 border border-rose-800">
                  <Trash2 className="w-6 h-6" />
                </div>
                <div>
                  <h3 className="font-bold text-base text-white">Delete Incident Record?</h3>
                  <p className="text-xs text-slate-400">
                    This action will permanently delete this record from the SQLite database.
                  </p>
                </div>
              </div>

              <div className="p-3 rounded-xl border border-slate-800 bg-slate-950 text-xs font-mono">
                <div className="font-bold text-rose-400">{deleteConfirmIncident.title}</div>
                <div className="text-[10px] text-slate-400 mt-1">ID: {deleteConfirmIncident.id}</div>
              </div>

              <div className="flex items-center justify-end gap-3 pt-2">
                <button
                  type="button"
                  disabled={isDeleting}
                  onClick={() => setDeleteConfirmIncident(null)}
                  className="px-4 py-2 text-xs font-semibold rounded-xl border border-slate-700 bg-slate-800 hover:bg-slate-700 text-slate-300 transition"
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
          <div className="fixed inset-0 z-[9999999] flex items-center justify-center p-4 bg-slate-950/90 backdrop-blur-md">
            <div className="relative w-full max-w-2xl bg-slate-900 border border-cyan-500/40 rounded-2xl p-4 space-y-3 flex flex-col max-h-[80vh]">
              <div className="flex items-center justify-between border-b border-slate-800 pb-2">
                <span className="font-mono text-xs text-cyan-400 font-bold">
                  JSON Record: {selectedJsonView.id}
                </span>
                <button onClick={() => setSelectedJsonView(null)} className="text-slate-400 hover:text-white">✕</button>
              </div>
              <pre className="p-3 bg-slate-950 border border-slate-800 rounded-xl text-[11px] font-mono text-emerald-400 overflow-auto flex-1">
                {JSON.stringify(selectedJsonView, null, 2)}
              </pre>
              <div className="flex justify-end">
                <button
                  onClick={() => setSelectedJsonView(null)}
                  className="px-4 py-1.5 bg-slate-800 text-slate-200 text-xs font-semibold rounded-lg hover:bg-slate-700"
                >
                  Close
                </button>
              </div>
            </div>
          </div>
        )}

      </div>
    </div>
  );
}
