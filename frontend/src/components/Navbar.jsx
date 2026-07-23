import React from 'react';
import { Search, Leaf, Activity } from 'lucide-react';

export default function Navbar({
  searchQuery,
  setSearchQuery,
  activeFilter,
  setActiveFilter,
  onFlyToRegion,
  activeRegion
}) {
  const categories = [
    { id: 'all', label: 'All News' },
    { id: 'Science & Tech', label: 'Science & Tech' },
    { id: 'Disaster & Rescue', label: 'Disaster & Rescue' },
    { id: 'Geopolitical', label: 'Geopolitical' },
    { id: 'Environmental', label: 'Environmental' }
  ];

  const regions = [
    { id: 'world', label: 'World View', coords: [20, 0], zoom: 2 },
    { id: 'americas', label: 'Americas', coords: [15, -90], zoom: 3 },
    { id: 'europe', label: 'Europe', coords: [50, 15], zoom: 4 },
    { id: 'asia', label: 'Asia-Pacific', coords: [25, 105], zoom: 3 }
  ];

  return (
    <header className="z-[999] glass-panel border-b border-green-200/60 px-3 lg:px-6 py-2 space-y-1.5 flex-shrink-0">
      <div className="flex flex-col sm:flex-row items-center justify-between gap-2">
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-lg bg-gradient-to-tr from-green-500 via-emerald-500 to-teal-500 flex items-center justify-center shadow-md shadow-green-500/20">
            <Leaf className="w-4 h-4 text-white stroke-[2.5]" />
          </div>
          <div className="flex items-center gap-2">
            <h1 className="text-base font-bold text-slate-800 font-heading tracking-tight">
              EchoFact <span className="text-green-700 font-mono text-xs uppercase px-1.5 py-0.5 rounded bg-green-100 border border-green-300">World News Radar</span>
            </h1>
            <span className="hidden xl:inline-flex items-center gap-1 text-[10px] font-mono text-green-600 border border-green-300 px-2 py-0.5 rounded-full bg-green-50">
              <Activity className="w-3 h-3 text-green-500" /> LIVE RADAR
            </span>
          </div>
        </div>

        <div className="flex items-center gap-2 w-full sm:w-auto">
          {/* Search Box */}
          <div className="relative flex-1 sm:w-64">
            <Search className="w-3.5 h-3.5 absolute left-2.5 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              placeholder="Search news & sources..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-8 pr-3 py-1 bg-white border border-green-200 rounded-lg text-xs text-slate-700 placeholder-slate-400 focus:outline-none focus:border-green-500 focus:ring-1 focus:ring-green-500/30 transition"
            />
          </div>
        </div>
      </div>

      <div className="flex flex-wrap items-center justify-between gap-2 text-xs pt-1 border-t border-green-200/60">
        <div className="flex items-center gap-1.5 overflow-x-auto pb-0.5">
          {categories.map((cat) => (
            <button
              key={cat.id}
              onClick={() => setActiveFilter(cat.id)}
              className={`px-2.5 py-0.5 rounded-md border text-[11px] font-medium transition whitespace-nowrap ${
                activeFilter === cat.id
                  ? 'bg-green-100 border-green-500 text-green-700 font-bold'
                  : 'bg-white/60 border-green-200 text-slate-500 hover:text-slate-700 hover:border-green-300'
              }`}
            >
              {cat.label}
            </button>
          ))}
        </div>

        <div className="hidden lg:flex items-center gap-1 text-[11px]">
          <span className="text-slate-400 font-mono mr-1">ZOOM:</span>
          {regions.map((reg) => (
            <button
              key={reg.id}
              onClick={() => onFlyToRegion(reg.coords, reg.zoom, reg.id)}
              className={`px-2 py-0.5 rounded transition ${
                activeRegion === reg.id
                  ? 'bg-green-100 text-green-700 font-bold'
                  : 'text-slate-400 hover:text-slate-700'
              }`}
            >
              {reg.label}
            </button>
          ))}
        </div>
      </div>
    </header>
  );
}
