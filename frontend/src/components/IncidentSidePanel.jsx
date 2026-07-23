import React from 'react';
import { MapPin, ExternalLink, Calendar, ArrowLeft, X } from 'lucide-react';

export default function IncidentSidePanel({
  incidents,
  selectedIncident,
  onSelectIncident
}) {
  // DETAIL VIEW
  if (selectedIncident) {
    return (
      <div className="w-full h-full flex flex-col glass-panel rounded-2xl border border-green-200 p-3.5 space-y-3 overflow-hidden animate-fade-in">
        <div className="flex items-center justify-between border-b border-green-200 pb-2.5 flex-shrink-0">
          <button
            onClick={() => onSelectIncident(null)}
            className="flex items-center gap-1.5 px-2.5 py-1 rounded-lg bg-green-50 hover:bg-green-100 text-green-700 text-xs font-semibold transition border border-green-200"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            <span>Back to Stream</span>
          </button>
          <button
            onClick={() => onSelectIncident(null)}
            className="p-1 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-green-50 transition"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <div className="overflow-y-auto space-y-3 flex-1 pr-1">
          <div className="space-y-1.5">
            <div className="flex flex-wrap items-center gap-2 text-[11px] font-mono text-slate-500">
              <span className="px-2 py-0.5 rounded-full bg-green-100 text-green-700 border border-green-300 font-bold uppercase text-[10px]">
                {selectedIncident.category}
              </span>
              <span className="flex items-center gap-1">
                <Calendar className="w-3 h-3 text-green-600" />
                {selectedIncident.date}
              </span>
              <span className="flex items-center gap-1">
                <MapPin className="w-3 h-3 text-emerald-500" />
                {selectedIncident.locationName}
              </span>
            </div>
            <h2 className="text-base font-bold text-slate-800 font-heading leading-snug">
              {selectedIncident.title}
            </h2>
          </div>

          {selectedIncident.summary && (
            <p className="text-xs text-slate-600 leading-relaxed border-l-2 border-green-400 pl-3">
              {selectedIncident.summary}
            </p>
          )}

          <div className="space-y-2 pt-2 border-t border-green-200/80">
            <span className="text-[11px] font-mono text-green-700 font-bold uppercase tracking-wider block">
              News Sources
            </span>
            {selectedIncident.sources && selectedIncident.sources.length > 0 ? (
              <div className="space-y-2">
                {selectedIncident.sources.map((src, i) => (
                  <a
                    key={i}
                    href={src.url}
                    target="_blank"
                    rel="noreferrer"
                    className="p-2.5 rounded-xl bg-white border border-green-200 hover:border-green-400 text-xs transition block group shadow-sm hover:shadow-md"
                  >
                    <div className="flex items-center justify-between font-mono text-[11px]">
                      <div className="flex items-center gap-1.5 text-green-700 font-bold">
                        <span className="group-hover:underline">{src.publisher}</span>
                        {src.date && (
                          <span className="text-[10px] text-slate-400 font-normal">
                            • {src.date}
                          </span>
                        )}
                      </div>
                      <ExternalLink className="w-3 h-3 text-green-600 flex-shrink-0" />
                    </div>
                    <p className="text-slate-600 text-xs mt-1 leading-snug group-hover:text-slate-800 font-sans">
                      {src.title}
                    </p>
                  </a>
                ))}
              </div>
            ) : (
              <p className="text-xs text-slate-400 font-mono">No direct link available.</p>
            )}
          </div>
        </div>
      </div>
    );
  }

  // STREAM LIST VIEW
  return (
    <div className="w-full h-full flex flex-col glass-panel rounded-2xl border border-green-200/80 p-3 space-y-2.5 overflow-hidden">
      <div className="flex items-center justify-between border-b border-green-200 pb-2 flex-shrink-0">
        <div>
          <h2 className="font-bold text-sm text-slate-800 font-heading flex items-center gap-1.5">
            News Stream <span className="text-[10px] font-mono text-green-700 bg-green-100 px-1.5 py-0.5 rounded border border-green-300">{incidents.length} STORIES</span>
          </h2>
          <p className="text-[11px] text-slate-400">Click story for details & source links</p>
        </div>
      </div>

      <div className="overflow-y-auto space-y-2 flex-1 pr-1">
        {incidents.length === 0 ? (
          <div className="py-12 text-center text-slate-400 text-xs">
            <p>No news items match active filter.</p>
          </div>
        ) : (
          incidents.map((incident) => {
            const primarySource = incident.sources?.[0];
            return (
              <div
                key={incident.id}
                onClick={() => onSelectIncident(incident)}
                className="p-3 rounded-xl transition cursor-pointer glass-card border border-green-200/80 hover:border-green-400 space-y-1.5"
              >
                <div className="flex items-center justify-between gap-2 text-[10px] font-mono">
                  <span className="text-green-700 font-bold uppercase tracking-wider">
                    {incident.category}
                  </span>
                  <div className="flex items-center gap-2 text-slate-400">
                    <span className="flex items-center gap-1">
                      <Calendar className="w-3 h-3 text-green-600" />
                      {incident.date}
                    </span>
                  </div>
                </div>
                <h3 className="font-bold text-xs sm:text-sm text-slate-700 font-heading leading-snug">
                  {incident.title}
                </h3>
                <div className="pt-1 border-t border-green-200/80 flex items-center justify-between text-[11px]">
                  {primarySource ? (
                    <a
                      href={primarySource.url}
                      target="_blank"
                      rel="noreferrer"
                      onClick={(e) => e.stopPropagation()}
                      className="text-green-600 hover:text-green-700 font-mono flex items-center gap-1 font-semibold hover:underline truncate max-w-[220px]"
                      title={primarySource.title}
                    >
                      <ExternalLink className="w-3 h-3 flex-shrink-0" />
                      <span>{primarySource.publisher}</span>
                    </a>
                  ) : (
                    <span className="text-slate-400 font-mono">View Details</span>
                  )}
                  <span className="text-slate-400 font-mono text-[10px] hover:text-slate-600">
                    View Sources →
                  </span>
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}
