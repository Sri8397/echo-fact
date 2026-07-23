import React, { useState, useEffect, useMemo } from 'react';
import { ChevronLeft, ChevronRight, X, Play, Pause, ExternalLink, MapPin, Eye, Calendar, Clock } from 'lucide-react';
export default function CaseReelOverlay({ isOpen, onClose, onPanToCase, onOpenDetail, incidents = [] }) {
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isPlaying, setIsPlaying] = useState(false);

  const casesList = incidents;
  const currentCase = casesList[currentIndex];

  // Group news sources chronologically by date (Oldest at top)
  const groupedNews = useMemo(() => {
    if (!currentCase?.sources) return [];
    const groups = {};
    currentCase.sources.forEach((src) => {
      const d = src.date || currentCase.date || 'Coverage Update';
      if (!groups[d]) {
        groups[d] = [];
      }
      groups[d].push(src);
    });
    return Object.keys(groups).map((d) => ({
      date: d,
      items: groups[d]
    }));
  }, [currentCase]);

  // Pan map camera whenever current case changes
  useEffect(() => {
    if (isOpen && currentCase) {
      onPanToCase([currentCase.lat, currentCase.lng], 5);
    }
  }, [currentIndex, isOpen]);

  // Keyboard Arrow Key Listener (Left/Right arrow)
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (!isOpen) return;
      if (e.key === 'ArrowRight') {
        handleNext();
      } else if (e.key === 'ArrowLeft') {
        handlePrev();
      } else if (e.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, currentIndex]);

  // Auto-play timer
  useEffect(() => {
    let timer;
    if (isPlaying && isOpen && casesList.length > 0) {
      timer = setInterval(() => {
        setCurrentIndex((prev) => (prev + 1) % casesList.length);
      }, 6000);
    }
    return () => clearInterval(timer);
  }, [isPlaying, isOpen, casesList]);

  if (!isOpen || !currentCase || casesList.length === 0) return null;

  const handleNext = () => {
    setCurrentIndex((prev) => (prev + 1) % casesList.length);
  };

  const handlePrev = () => {
    setCurrentIndex((prev) => (prev - 1 + casesList.length) % casesList.length);
  };

  return (
    <div className="fixed inset-0 z-[99999] flex items-end sm:items-center justify-center p-2 sm:p-4 bg-slate-950/85 backdrop-blur-md animate-fade-in">
      <div className="relative w-full max-w-2xl glass-panel rounded-3xl border border-amber-500/30 overflow-hidden shadow-2xl flex flex-col max-h-[85vh]">
        
        {/* Top Header & Progress Bar */}
        <div className="relative px-5 pt-4 pb-3 border-b border-slate-800 bg-gradient-to-r from-amber-950/40 via-slate-900 to-slate-900 flex-shrink-0">
          
          {/* Progress Bar Segments */}
          <div className="flex items-center gap-1.5 mb-2.5">
            {casesList.map((item, idx) => (
              <button
                key={item.id}
                onClick={() => setCurrentIndex(idx)}
                className={`h-1.5 flex-1 rounded-full transition-all duration-300 ${
                  idx === currentIndex
                    ? 'bg-amber-400 shadow-md shadow-amber-400/50'
                    : idx < currentIndex
                    ? 'bg-amber-600/60'
                    : 'bg-slate-800'
                }`}
                title={`Jump to Story ${idx + 1}: ${item.title}`}
              />
            ))}
          </div>

          <div className="flex items-center justify-between flex-wrap gap-2">
            <div className="flex items-center gap-2">
              <span className="px-2.5 py-0.5 rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/40 text-xs font-mono font-bold flex items-center gap-1">
                <Eye className="w-3.5 h-3.5" />
                STORY {currentIndex + 1} OF {casesList.length}
              </span>
              <span className="text-xs text-slate-400 flex items-center gap-1 font-mono">
                <Calendar className="w-3.5 h-3.5 text-slate-400" />
                {currentCase.date || currentCase.year}
              </span>
              <span className="text-xs text-slate-400 flex items-center gap-1 font-mono">
                <MapPin className="w-3.5 h-3.5 text-cyan-400" />
                {currentCase.locationName}
              </span>
            </div>

            <div className="flex items-center gap-2">
              <button
                onClick={() => setIsPlaying(!isPlaying)}
                className={`px-2.5 py-1 rounded-lg border text-xs font-mono flex items-center gap-1 transition ${
                  isPlaying
                    ? 'bg-amber-500 text-slate-950 border-amber-400 font-bold'
                    : 'bg-slate-800 text-slate-300 border-slate-700 hover:bg-slate-700'
                }`}
              >
                {isPlaying ? <Pause className="w-3.5 h-3.5" /> : <Play className="w-3.5 h-3.5" />}
                {isPlaying ? 'PAUSE' : 'AUTO-PLAY'}
              </button>

              <button
                onClick={onClose}
                className="p-1.5 rounded-full text-slate-400 hover:text-white hover:bg-slate-800 transition"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
          </div>

          <h2 className="text-lg sm:text-xl font-bold text-white font-heading mt-2 leading-snug">
            {currentCase.title}
          </h2>
          <p className="text-xs text-slate-300 line-clamp-2 mt-1 font-sans">
            {currentCase.summary}
          </p>
        </div>

        {/* Date Grouped News Coverage (Oldest at top) */}
        <div className="p-5 overflow-y-auto space-y-4 flex-1 bg-slate-950/40">
          
          <div className="space-y-4">
            <div className="flex items-center justify-between border-b border-slate-800 pb-2">
              <h3 className="text-xs font-mono font-bold text-amber-400 uppercase tracking-wider flex items-center gap-1.5">
                <Clock className="w-3.5 h-3.5 text-amber-400" /> DIRECT DATA GROUP NEWS (OLDEST AT TOP)
              </h3>
              <span className="text-[10px] font-mono text-slate-400 bg-slate-800 px-2.5 py-0.5 rounded-full border border-slate-700">
                {currentCase.sources?.length || 0} Reports Grouped
              </span>
            </div>
            
            {groupedNews.length > 0 ? (
              <div className="space-y-5 relative before:absolute before:left-3 before:top-3 before:bottom-3 before:w-0.5 before:bg-slate-800/80">
                {groupedNews.map((group, groupIdx) => (
                  <div key={groupIdx} className="relative pl-7 space-y-2">
                    {/* Timeline Node Dot */}
                    <div className="absolute left-1.5 top-1 -translate-x-1/2 w-3 h-3 rounded-full bg-amber-400 border-2 border-slate-950 shadow-md shadow-amber-400/50" />
                    
                    {/* Group Date Header */}
                    <div className="flex items-center gap-2">
                      <span className="px-2.5 py-0.5 rounded-md bg-amber-500/20 text-amber-300 border border-amber-500/40 text-[11px] font-mono font-bold">
                        📅 {group.date}
                      </span>
                      <span className="text-[10px] text-slate-400 font-mono">
                        ({group.items.length} {group.items.length === 1 ? 'report' : 'reports'})
                      </span>
                    </div>

                    {/* Group News items on same date - rendered one by one */}
                    <div className="space-y-2">
                      {group.items.map((src, i) => (
                        <a
                          key={i}
                          href={src.url}
                          target="_blank"
                          rel="noreferrer"
                          className="p-3 rounded-xl bg-slate-900/90 border border-slate-800 hover:border-amber-500/50 hover:bg-slate-850 text-xs transition block group shadow-sm"
                        >
                          <div className="flex items-center justify-between font-mono text-xs text-amber-400 font-bold">
                            <div className="flex items-center gap-1.5">
                              <span className="w-1.5 h-1.5 rounded-full bg-amber-400 flex-shrink-0" />
                              <span className="group-hover:underline">{src.publisher}</span>
                              {src.date && (
                                <span className="text-[10px] text-slate-400 font-normal">
                                  • {src.date}
                                </span>
                              )}
                            </div>
                            <ExternalLink className="w-3.5 h-3.5 text-amber-400 group-hover:translate-x-0.5 group-hover:-translate-y-0.5 transition-transform flex-shrink-0" />
                          </div>
                          <p className="text-slate-200 text-xs mt-1.5 leading-snug group-hover:text-white font-sans">
                            {src.title}
                          </p>
                        </a>
                      ))}
                    </div>

                  </div>
                ))}
              </div>
            ) : (
              <p className="text-xs text-slate-500 font-mono">No direct news links available.</p>
            )}
          </div>

        </div>

        {/* Bottom Navigation Toolbar */}
        <div className="px-5 py-3 border-t border-slate-800 bg-slate-900/95 flex items-center justify-between gap-3 flex-shrink-0">
          <button
            onClick={handlePrev}
            className="flex items-center gap-1.5 px-3 py-1.5 bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-200 text-xs font-semibold rounded-xl transition"
          >
            <ChevronLeft className="w-4 h-4" />
            <span>Previous</span>
          </button>

          <button
            onClick={() => {
              onClose();
              onOpenDetail(currentCase);
            }}
            className="text-xs text-cyan-400 hover:text-cyan-300 font-mono hover:underline"
          >
            Inspect Breakdown →
          </button>

          <button
            onClick={handleNext}
            className="flex items-center gap-1.5 px-4 py-1.5 bg-gradient-to-r from-amber-500 to-orange-600 hover:from-amber-400 hover:to-orange-500 text-slate-950 text-xs font-bold rounded-xl shadow-lg transition transform active:scale-95"
          >
            <span>Next</span>
            <ChevronRight className="w-4 h-4" />
          </button>
        </div>

      </div>
    </div>
  );
}
