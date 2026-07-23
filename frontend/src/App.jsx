import React, { useState, useMemo, useEffect } from 'react';
import Navbar from './components/Navbar';
import InteractiveMap from './components/InteractiveMap';
import IncidentSidePanel from './components/IncidentSidePanel';
import AdminPage from './components/AdminPage';
import { fetchIncidents } from './data/incidentsData';

export default function App() {
  const [incidents, setIncidents] = useState([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [activeFilter, setActiveFilter] = useState('all');
  const [selectedIncident, setSelectedIncident] = useState(null);
  const [flyToTarget, setFlyToTarget] = useState({ coords: [20, 0], zoom: 2 });
  const [currentZoom, setCurrentZoom] = useState(2);
  const [activeRegion, setActiveRegion] = useState('world');
  const [mobileTab, setMobileTab] = useState('split'); // 'split', 'map', 'stream'

  const currentPath = window.location.pathname;
  const currentHash = window.location.hash;

  const isAdminRoute =
    currentPath.endsWith('/admin') ||
    currentPath === '/admin' ||
    currentHash === '#/admin' ||
    currentHash === '#admin';

  const getPathId = () => {
    if (currentHash && currentHash.startsWith('#')) {
      return currentHash.replace(/^#\/?/, '');
    }
    const cleanPath = currentPath.replace(/\/$/, '');
    const parts = cleanPath.split('/').filter(Boolean);
    const lastPart = parts.length > 0 ? parts[parts.length - 1] : '';
    return lastPart === 'echo-fact' ? '' : lastPart;
  };

  // Load live incident records directly from SQLite Database on mount
  const refreshIncidentsFromDb = async () => {
    const data = await fetchIncidents();
    if (data) {
      setIncidents(data);

      const pathId = getPathId();
      if (pathId && pathId !== 'admin') {
        const found = data.find((inc) => inc.id === pathId);
        if (found) {
          setSelectedIncident(found);
          setFlyToTarget({ coords: [found.lat, found.lng], zoom: 5 });
        }
      }
    }
  };

  useEffect(() => {
    if (!isAdminRoute) {
      refreshIncidentsFromDb();
    }
  }, [isAdminRoute]);

  // Handle Browser Back / Forward Button Navigation (popstate) & Hash Change
  useEffect(() => {
    const handleNavigationChange = () => {
      const pathId = getPathId();
      if (!pathId || pathId === 'admin') {
        setSelectedIncident(null);
      } else if (incidents.length > 0) {
        const found = incidents.find((inc) => inc.id === pathId);
        if (found) {
          setSelectedIncident(found);
          setFlyToTarget({ coords: [found.lat, found.lng], zoom: 5 });
        } else {
          setSelectedIncident(null);
        }
      }
    };

    window.addEventListener('popstate', handleNavigationChange);
    window.addEventListener('hashchange', handleNavigationChange);
    return () => {
      window.removeEventListener('popstate', handleNavigationChange);
      window.removeEventListener('hashchange', handleNavigationChange);
    };
  }, [incidents]);

  const filteredIncidents = useMemo(() => {
    return incidents.filter((inc) => {
      if (activeFilter !== 'all' && inc.category !== activeFilter) return false;
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase();
        if (!inc.title.toLowerCase().includes(q) &&
            !inc.locationName.toLowerCase().includes(q) &&
            !inc.category.toLowerCase().includes(q)) return false;
      }
      return true;
    });
  }, [incidents, activeFilter, searchQuery]);

  const handleFlyToRegion = (coords, zoom, regionId) => {
    setActiveRegion(regionId);
    setFlyToTarget({ coords, zoom });
  };

  const handleSelectIncident = (incident) => {
    setSelectedIncident(incident);
    if (incident) {
      setFlyToTarget({ coords: [incident.lat, incident.lng], zoom: 5 });
      if (window.location.pathname !== `/${incident.id}`) {
        window.history.pushState({}, '', `/${incident.id}`);
      }
      if (mobileTab === 'stream') {
        setMobileTab('split');
      }
    } else {
      if (window.location.pathname !== '/') {
        window.history.pushState({}, '', '/');
      }
    }
  };

  // Dedicated Standalone Admin Page Route
  if (isAdminRoute) {
    return <AdminPage />;
  }

  return (
    <div className="h-screen w-screen bg-green-50 text-slate-800 flex flex-col font-sans overflow-hidden select-none">
      <Navbar
        searchQuery={searchQuery}
        setSearchQuery={setSearchQuery}
        activeFilter={activeFilter}
        setActiveFilter={setActiveFilter}
        onFlyToRegion={handleFlyToRegion}
        activeRegion={activeRegion}
      />

      {/* Mobile View Mode Switcher (< md screens) */}
      <div className="md:hidden flex items-center justify-center gap-1 px-2 py-1 mx-2 mt-1.5 bg-white/90 backdrop-blur-md rounded-xl border border-green-200/80 shadow-sm flex-shrink-0">
        <button
          onClick={() => setMobileTab('split')}
          className={`flex-1 py-1 text-xs font-semibold rounded-lg transition flex items-center justify-center gap-1 font-mono ${
            mobileTab === 'split'
              ? 'bg-green-600 text-white shadow-sm'
              : 'text-slate-600 hover:bg-green-50'
          }`}
        >
          <span>⚡ Split</span>
        </button>
        <button
          onClick={() => setMobileTab('map')}
          className={`flex-1 py-1 text-xs font-semibold rounded-lg transition flex items-center justify-center gap-1 font-mono ${
            mobileTab === 'map'
              ? 'bg-green-600 text-white shadow-sm'
              : 'text-slate-600 hover:bg-green-50'
          }`}
        >
          <span>🗺️ Map Only</span>
        </button>
        <button
          onClick={() => setMobileTab('stream')}
          className={`flex-1 py-1 text-xs font-semibold rounded-lg transition flex items-center justify-center gap-1 font-mono ${
            mobileTab === 'stream'
              ? 'bg-green-600 text-white shadow-sm'
              : 'text-slate-600 hover:bg-green-50'
          }`}
        >
          <span>📰 News ({filteredIncidents.length})</span>
        </button>
      </div>

      <main className="flex-1 p-2 sm:p-3 overflow-hidden flex flex-col md:flex-row items-stretch gap-2.5 sm:gap-3">
        {/* Interactive Map Container */}
        <div className={`w-full md:flex-1 md:h-full transition-all duration-300 ${
          mobileTab === 'map'
            ? 'h-full block'
            : mobileTab === 'stream'
            ? 'hidden md:block md:h-full'
            : 'h-[45%] min-h-[240px] md:h-full'
        }`}>
          <InteractiveMap
            incidents={filteredIncidents}
            selectedIncident={selectedIncident}
            onSelectIncident={handleSelectIncident}
            flyToTarget={flyToTarget}
            currentZoom={currentZoom}
            setCurrentZoom={setCurrentZoom}
          />
        </div>

        {/* Incident Side Panel Container */}
        <div className={`w-full md:w-80 lg:w-96 md:h-full flex flex-col transition-all duration-300 ${
          mobileTab === 'stream'
            ? 'h-full block'
            : mobileTab === 'map'
            ? 'hidden md:flex md:h-full'
            : 'h-[55%] min-h-0 md:h-full'
        }`}>
          <IncidentSidePanel
            incidents={filteredIncidents}
            selectedIncident={selectedIncident}
            onSelectIncident={handleSelectIncident}
          />
        </div>
      </main>
    </div>
  );
}
