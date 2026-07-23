import React, { useMemo } from 'react';
import { X, MapPin, ExternalLink, CheckCircle2, Sparkles, Clock, HelpCircle, FileText } from 'lucide-react';

export default function IncidentDetailModal({ incident, onClose }) {
  if (!incident) return null;

  // Group news sources chronologically by date (Oldest at top)
  const groupedNews = useMemo(() => {
    if (!incident?.sources) return [];
    const groups = {};
    incident.sources.forEach((src) => {
      const d = src.date || incident.date || 'Coverage Update';
      if (!groups[d]) {
        groups[d] = [];
      }
      groups[d].push(src);
    });
    return Object.keys(groups).map((d) => ({
      date: d,
      items: groups[d]
    }));
  }, [incident]);

  // Category Color Map
  const categoryStyles = {
    'Disaster & Rescue': 'from-amber-500/20 to-emerald-500/20 text-emerald-300 border-emerald-500/40',
    'Science & Tech': 'from-cyan-500/20 to-blue-500/20 text-cyan-300 border-cyan-500/40',
    'Geopolitical': 'from-rose-500/20 to-purple-500/20 text-rose-300 border-rose-500/40',
    'Environmental': 'from-teal-500/20 to-emerald-500/20 text-teal-300 border-teal-500/40',
  };

  const catStyle = categoryStyles[incident.category] || 'from-cyan-500/20 to-blue-500/20 text-cyan-300 border-cyan-500/40';

  return (
    <div className="fixed inset-0 z-[99999] flex items-center justify-center p-3 sm:p-5 bg-slate-950/85 backdrop-blur-xl animate-fade-in">
      <div className="relative w-full max-w-3xl bg-slate-900/95 border border-slate-700/60 rounded-3xl overflow-hidden shadow-2xl flex flex-col max-h-[92vh] transition-all">
        
        {/* Top Decorative Glow Line */}
        <div className="h-1.5 w-full bg-gradient-to-r from-amber-500 via-cyan-400 to-emerald-400" />

        {/* Modal Header */}
        <div className="p-5 border-b border-slate-800 bg-slate-900/60 flex items-start justify-between gap-4 flex-shrink-0">
          <div className="space-y-2">
            <div className="flex flex-wrap items-center gap-2 text-xs font-mono">
              <span className={`px-2.5 py-0.5 rounded-full bg-gradient-to-r ${catStyle} border font-bold uppercase tracking-wide`}>
                {incident.category}
              </span>
              <span className="text-slate-400 flex items-center gap-1">
                <MapPin className="w-3.5 h-3.5 text-cyan-400" />
                {incident.locationName}
              </span>
              <span className="text-slate-500 font-mono">({incident.year})</span>
            </div>

            <h2 className="text-xl sm:text-2xl font-extrabold text-white font-heading leading-tight tracking-tight">
              {incident.title}
            </h2>
          </div>

          <button
            onClick={onClose}
            className="p-2 rounded-full text-slate-400 hover:text-white hover:bg-slate-800 transition border border-transparent hover:border-slate-700"
            title="Close modal"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Scroll Body */}
        <div className="p-5 overflow-y-auto space-y-6 flex-1 bg-slate-900/30">

          {/* Summary + Current Reality — Side by Side */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* Then Card */}
            <div className="p-4 rounded-2xl bg-gradient-to-b from-slate-800/50 via-slate-900/90 to-slate-900 border border-slate-700/60 space-y-3 shadow-lg">
              <div className="flex items-center gap-2">
                <span className="px-2.5 py-1 rounded-full bg-slate-700/50 text-slate-300 border border-slate-600/50 text-[11px] font-mono font-bold flex items-center gap-1">
                  <FileText className="w-3.5 h-3.5" /> When It Happened
                </span>
              </div>
              <p className="text-xs sm:text-sm text-slate-200 leading-relaxed font-sans">
                {incident.summary}
              </p>
            </div>

            {/* Now Card */}
            <div className="p-4 rounded-2xl bg-gradient-to-b from-emerald-950/30 via-slate-900/90 to-slate-900 border border-emerald-500/40 space-y-3 shadow-lg">
              <div className="flex items-center gap-2">
                <span className="px-2.5 py-1 rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 text-[11px] font-mono font-bold flex items-center gap-1">
                  <Sparkles className="w-3.5 h-3.5" /> Where It Stands Today
                </span>
              </div>
              <p className="text-xs sm:text-sm text-slate-100 leading-relaxed font-sans">
                {incident.currentReality}
              </p>
            </div>
          </div>

          {/* Timeline */}
          {incident.hypeTimeline && incident.hypeTimeline.length > 0 && (
            <div className="space-y-3">
              <h3 className="text-xs font-mono font-bold text-cyan-400 uppercase tracking-wider flex items-center gap-1.5">
                <Clock className="w-4 h-4 text-cyan-400" /> Event Timeline
              </h3>
              <div className="space-y-2 relative before:absolute before:left-3 before:top-3 before:bottom-3 before:w-0.5 before:bg-slate-800">
                {incident.hypeTimeline.map((step, idx) => (
                  <div key={idx} className="relative pl-8 flex items-start gap-3">
                    <div className="absolute left-1.5 top-2 -translate-x-1/2 w-3 h-3 rounded-full bg-cyan-400 border-2 border-slate-950 shadow-md shadow-cyan-400/30" />
                    <div className="p-3 rounded-xl bg-slate-950/70 border border-slate-800 flex-1">
                      <div className="flex items-center gap-2 mb-1">
                        <span className="px-2 py-0.5 rounded-md bg-cyan-950 text-cyan-300 border border-cyan-500/40 text-[10px] font-mono font-bold">
                          {step.year}
                        </span>
                        <span className="text-[10px] font-mono text-slate-500 uppercase">{step.phase}</span>
                      </div>
                      <p className="text-xs text-slate-300 leading-relaxed">{step.event}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Key Data Points */}
          {incident.verifiedFacts && (
            <div className="space-y-2.5">
              <h3 className="text-xs font-mono font-bold text-emerald-400 uppercase tracking-wider flex items-center gap-1.5">
                <CheckCircle2 className="w-4 h-4 text-emerald-400" /> Key Data Points
              </h3>
              <div className="grid grid-cols-1 gap-2">
                {incident.verifiedFacts.map((fact, idx) => (
                  <div key={idx} className="p-3 rounded-xl bg-slate-950/70 border border-slate-800 text-xs text-slate-200 flex items-start gap-2.5">
                    <span className="p-1 rounded-md bg-emerald-500/20 text-emerald-400 flex-shrink-0 mt-0.5">
                      <CheckCircle2 className="w-3.5 h-3.5" />
                    </span>
                    <span className="leading-relaxed">{fact}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Questions to Explore */}
          {incident.keyQuestions && incident.keyQuestions.length > 0 && (
            <div className="space-y-2.5">
              <h3 className="text-xs font-mono font-bold text-amber-400 uppercase tracking-wider flex items-center gap-1.5">
                <HelpCircle className="w-4 h-4 text-amber-400" /> Questions to Explore
              </h3>
              <div className="grid grid-cols-1 gap-2">
                {incident.keyQuestions.map((q, idx) => (
                  <div key={idx} className="p-3 rounded-xl bg-amber-950/30 border border-amber-500/20 text-xs text-slate-200 flex items-start gap-2.5">
                    <span className="p-1 rounded-md bg-amber-500/20 text-amber-400 flex-shrink-0 mt-0.5">
                      <HelpCircle className="w-3.5 h-3.5" />
                    </span>
                    <span className="leading-relaxed">{q}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Dated Group News Timeline */}
          <div className="space-y-3 pt-2">
            <div className="flex items-center justify-between border-b border-slate-800 pb-2">
              <h3 className="text-xs font-mono font-bold text-cyan-400 uppercase tracking-wider flex items-center gap-1.5">
                <Clock className="w-4 h-4 text-cyan-400" /> Sources by Date
              </h3>
              <span className="text-[10px] font-mono text-slate-400 bg-slate-800 px-2 py-0.5 rounded-full border border-slate-700">
                {incident.sources?.length || 0} Articles
              </span>
            </div>

            {groupedNews.length > 0 ? (
              <div className="space-y-5 relative before:absolute before:left-3.5 before:top-3.5 before:bottom-3.5 before:w-0.5 before:bg-slate-800">
                {groupedNews.map((group, gIdx) => (
                  <div key={gIdx} className="relative pl-8 space-y-2">
                    <div className="absolute left-1.5 top-1 -translate-x-1/2 w-4 h-4 rounded-full bg-slate-900 border-2 border-cyan-400 flex items-center justify-center">
                      <div className="w-1.5 h-1.5 rounded-full bg-cyan-400" />
                    </div>
                    <div className="flex items-center gap-2">
                      <span className="px-2.5 py-0.5 rounded-md bg-cyan-950 text-cyan-300 border border-cyan-500/40 text-xs font-mono font-bold">
                        📅 {group.date}
                      </span>
                      <span className="text-[11px] text-slate-400 font-mono">
                        ({group.items.length} {group.items.length === 1 ? 'article' : 'articles'})
                      </span>
                    </div>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                      {group.items.map((src, i) => (
                        <a
                          key={i}
                          href={src.url}
                          target="_blank"
                          rel="noreferrer"
                          className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-800 hover:border-cyan-500/50 hover:bg-slate-900 text-xs transition group flex flex-col justify-between"
                        >
                          <div className="space-y-1.5">
                            <div className="flex items-center justify-between">
                              <div className="font-mono text-[11px] text-cyan-400 font-bold flex items-center gap-1.5">
                                <span className="group-hover:underline">{src.publisher}</span>
                                {src.date && (
                                  <span className="text-[10px] text-slate-400 font-normal">
                                    • {src.date}
                                  </span>
                                )}
                              </div>
                              <ExternalLink className="w-3.5 h-3.5 text-slate-500 group-hover:text-cyan-400 transition flex-shrink-0" />
                            </div>
                            <p className="text-slate-200 text-xs font-medium leading-snug group-hover:text-white line-clamp-2">
                              {src.title}
                            </p>
                          </div>
                          <span className="text-[10px] text-slate-500 font-mono mt-2 block">Read source →</span>
                        </a>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-xs text-slate-500 font-mono">No news sources recorded.</p>
            )}
          </div>

        </div>

        {/* Modal Footer */}
        <div className="p-4 border-t border-slate-800 bg-slate-900/90 flex items-center justify-between flex-shrink-0">
          <div className="text-xs text-slate-400 flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
            <span className="font-mono text-[11px]">EchoFact — Read the sources. Form your own view.</span>
          </div>

          <button
            onClick={onClose}
            className="px-5 py-2 bg-gradient-to-r from-slate-800 to-slate-700 hover:from-slate-700 hover:to-slate-600 text-white text-xs font-bold rounded-xl transition shadow border border-slate-600"
          >
            Close
          </button>
        </div>

      </div>
    </div>
  );
}
