import React, { useState } from 'react';
import { X, Search, ExternalLink, RefreshCw, Globe, FileText } from 'lucide-react';
export default function FactAnalyzerModal({ isOpen, onClose, incidents = [] }) {
  const [inputText, setInputText] = useState('');
  const [searchResults, setSearchResults] = useState(null);
  const [isSearching, setIsSearching] = useState(false);

  if (!isOpen) return null;

  const handleSearch = (e) => {
    e.preventDefault();
    if (!inputText.trim()) return;

    setIsSearching(true);
    setTimeout(() => {
      const query = inputText.toLowerCase();
      
      // Search across all incidents for matching sources and facts
      const matchedSources = [];
      const matchedFacts = [];
      const matchedIncidents = [];

      incidents.forEach((incident) => {
        const titleMatch = incident.title.toLowerCase().includes(query);
        const summaryMatch = incident.summary.toLowerCase().includes(query);
        const categoryMatch = incident.category.toLowerCase().includes(query);
        const locationMatch = incident.locationName.toLowerCase().includes(query);
        
        // Check facts
        const factMatches = (incident.verifiedFacts || []).filter(f => 
          f.toLowerCase().includes(query)
        );

        // Check sources
        const sourceMatches = (incident.sources || []).filter(s =>
          s.title.toLowerCase().includes(query) || 
          s.publisher.toLowerCase().includes(query)
        );

        if (titleMatch || summaryMatch || categoryMatch || locationMatch || factMatches.length > 0 || sourceMatches.length > 0) {
          matchedIncidents.push(incident);
          matchedFacts.push(...factMatches.map(f => ({ fact: f, incident: incident.title })));
          matchedSources.push(...(sourceMatches.length > 0 
            ? sourceMatches.map(s => ({ ...s, incident: incident.title }))
            : (incident.sources || []).slice(0, 2).map(s => ({ ...s, incident: incident.title }))
          ));
        }
      });

      setSearchResults({
        query: inputText,
        incidents: matchedIncidents,
        facts: matchedFacts.slice(0, 6),
        sources: matchedSources.slice(0, 8),
        total: matchedIncidents.length
      });
      setIsSearching(false);
    }, 500);
  };

  const sampleQueries = [
    "superconductor",
    "Fukushima tritium",
    "Suez Canal",
    "ocean plastic cleanup"
  ];

  return (
    <div className="fixed inset-0 z-[99999] flex items-center justify-center p-4 bg-slate-950/85 backdrop-blur-md animate-fade-in">
      <div className="relative w-full max-w-2xl glass-panel rounded-3xl border border-cyan-500/30 overflow-hidden shadow-2xl flex flex-col max-h-[85vh]">
        
        {/* Header */}
        <div className="p-5 border-b border-slate-800 bg-gradient-to-r from-cyan-950/50 to-slate-900 flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-xl bg-cyan-500/20 text-cyan-400 border border-cyan-500/40">
              <Search className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-white font-heading">
                Source Finder
              </h2>
              <p className="text-xs text-slate-400">Search verified sources & data points across all incidents</p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-1.5 rounded-full text-slate-400 hover:text-white hover:bg-slate-800 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Body */}
        <div className="p-6 space-y-5 overflow-y-auto flex-1">
          
          <form onSubmit={handleSearch} className="space-y-3">
            <label className="block text-xs font-mono text-slate-300">
              SEARCH TOPIC, KEYWORD, OR LOCATION:
            </label>
            
            <div className="flex gap-2">
              <input
                type="text"
                value={inputText}
                onChange={(e) => setInputText(e.target.value)}
                placeholder="e.g. 'tritium levels', 'cave rescue', 'carbon fiber hull'..."
                className="flex-1 p-3 bg-slate-900 border border-slate-700/80 rounded-xl text-sm text-slate-100 placeholder-slate-500 focus:outline-none focus:border-cyan-400 transition"
              />
              <button
                type="submit"
                disabled={isSearching || !inputText.trim()}
                className="px-5 py-2.5 bg-gradient-to-r from-cyan-500 to-blue-600 hover:from-cyan-400 hover:to-blue-500 disabled:opacity-50 text-slate-950 text-sm font-bold rounded-xl shadow-lg transition flex items-center gap-2"
              >
                {isSearching ? (
                  <RefreshCw className="w-4 h-4 animate-spin" />
                ) : (
                  <Search className="w-4 h-4" />
                )}
              </button>
            </div>

            <div className="flex flex-wrap items-center gap-1.5 text-[11px] text-slate-400">
              <span className="font-mono text-slate-500">TRY:</span>
              {sampleQueries.map((sample, i) => (
                <button
                  key={i}
                  type="button"
                  onClick={() => setInputText(sample)}
                  className="px-2.5 py-0.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 transition"
                >
                  {sample}
                </button>
              ))}
            </div>
          </form>

          {/* Search Results */}
          {searchResults && (
            <div className="space-y-4 animate-fade-in">
              <div className="flex items-center justify-between text-xs font-mono">
                <span className="text-slate-300">
                  Found <span className="text-cyan-400 font-bold">{searchResults.total}</span> related {searchResults.total === 1 ? 'incident' : 'incidents'} for "{searchResults.query}"
                </span>
              </div>

              {/* Matched Data Points */}
              {searchResults.facts.length > 0 && (
                <div className="space-y-2">
                  <h3 className="text-xs font-mono text-emerald-400 font-bold flex items-center gap-1.5">
                    <FileText className="w-3.5 h-3.5" /> RELATED DATA POINTS
                  </h3>
                  {searchResults.facts.map((item, idx) => (
                    <div key={idx} className="p-3 rounded-xl bg-slate-950/60 border border-emerald-500/20 text-xs text-slate-200 space-y-1">
                      <p className="leading-relaxed">"{item.fact}"</p>
                      <span className="text-[10px] text-emerald-400 font-mono">— {item.incident}</span>
                    </div>
                  ))}
                </div>
              )}

              {/* Matched Sources */}
              {searchResults.sources.length > 0 && (
                <div className="space-y-2">
                  <h3 className="text-xs font-mono text-cyan-400 font-bold flex items-center gap-1.5">
                    <Globe className="w-3.5 h-3.5" /> NEWS SOURCES
                  </h3>
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
                    {searchResults.sources.map((src, idx) => (
                      <a
                        key={idx}
                        href={src.url}
                        target="_blank"
                        rel="noreferrer"
                        className="p-3 rounded-xl bg-slate-950/80 border border-slate-800 hover:border-cyan-500/50 text-xs transition group"
                      >
                        <div className="flex items-center justify-between mb-1">
                          <div className="flex items-center gap-1.5 font-mono text-[11px] text-cyan-400 font-bold">
                            <span className="group-hover:underline">{src.publisher}</span>
                            {src.date && (
                              <span className="text-[10px] text-slate-400 font-normal">
                                • {src.date}
                              </span>
                            )}
                          </div>
                          <ExternalLink className="w-3 h-3 text-slate-500 group-hover:text-cyan-400 transition flex-shrink-0" />
                        </div>
                        <p className="text-slate-300 text-[11px] leading-snug line-clamp-2 group-hover:text-white">
                          {src.title}
                        </p>
                        <span className="text-[10px] text-slate-500 font-mono mt-1 block">{src.incident}</span>
                      </a>
                    ))}
                  </div>
                </div>
              )}

              {searchResults.total === 0 && (
                <div className="py-8 text-center text-slate-400 text-xs space-y-2">
                  <Search className="w-8 h-8 mx-auto text-slate-600" />
                  <p>No matching incidents found. Try a different keyword.</p>
                </div>
              )}
            </div>
          )}

        </div>

      </div>
    </div>
  );
}
