import React, { useEffect } from 'react';
import { MapContainer, TileLayer, Marker, useMap, useMapEvents } from 'react-leaflet';
import L from 'leaflet';

function MapController({ flyToTarget, onZoomChange }) {
  const map = useMap();

  useEffect(() => {
    // Invalidate map size on mount, resize, and layout orientation changes
    const timer = setTimeout(() => {
      map.invalidateSize();
    }, 150);

    const handleResize = () => {
      map.invalidateSize();
    };

    window.addEventListener('resize', handleResize);
    return () => {
      clearTimeout(timer);
      window.removeEventListener('resize', handleResize);
    };
  }, [map]);

  useEffect(() => {
    if (flyToTarget && flyToTarget.coords) {
      map.invalidateSize();
      map.flyTo(flyToTarget.coords, flyToTarget.zoom || 5, {
        duration: 2.2,
        easeLinearity: 0.25
      });
    }
  }, [flyToTarget, map]);

  useMapEvents({
    zoomend() {
      if (onZoomChange) {
        onZoomChange(map.getZoom());
      }
    }
  });

  return null;
}

const createCustomMarker = (sentiment, isSelected) => {
  const selectedRing = isSelected ? 'ring-4 ring-green-300 shadow-2xl scale-125' : '';

  return L.divIcon({
    className: 'custom-leaflet-marker',
    html: `
      <div class="relative group cursor-pointer">
        <div class="w-6 h-6 rounded-full bg-green-600 pulse-green border-green-300 ${selectedRing} border-2 flex items-center justify-center text-white font-bold text-xs transition transform hover:scale-125 shadow-lg">
          <div class="w-2 h-2 rounded-full bg-white"></div>
        </div>
      </div>
    `,
    iconSize: [24, 24],
    iconAnchor: [12, 12]
  });
};

export default function InteractiveMap({
  incidents,
  selectedIncident,
  onSelectIncident,
  flyToTarget,
  currentZoom,
  setCurrentZoom
}) {
  return (
    <div className="relative w-full h-full rounded-2xl overflow-hidden border border-green-200 shadow-lg">
      
      {/* Zoom Level Indicator */}
      <div className="absolute top-3 right-3 z-[400] glass-panel px-3 py-1.5 rounded-xl text-xs flex flex-col gap-1 border border-green-200 shadow-md pointer-events-none">
        <div className="flex items-center justify-between text-[10px] font-mono text-green-700 border-b border-green-200 pb-1 gap-2 font-bold">
          <span>ZOOM LEVEL {currentZoom}</span>
          <span className="text-slate-400">
            {currentZoom < 4 ? 'WORLD VIEW' : currentZoom <= 6 ? 'REGIONAL VIEW' : 'LOCAL VIEW'}
          </span>
        </div>
        <div className="flex items-center gap-2.5 text-[10px] text-slate-500 font-mono">
          <span className="flex items-center gap-1">
            <span className="w-2 h-2 rounded-full bg-green-500 pulse-green"></span> Faded Story
          </span>
        </div>
      </div>

      {/* Main Map — Light CartoDB Voyager tiles */}
      <MapContainer
        center={[20, 0]}
        zoom={2}
        minZoom={2}
        maxZoom={12}
        scrollWheelZoom={true}
        className="w-full h-full"
      >
        <MapController flyToTarget={flyToTarget} onZoomChange={setCurrentZoom} />

        <TileLayer
          url="https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png"
          attribution='&copy; CARTO &copy; OpenStreetMap'
          maxZoom={18}
        />

        {incidents.map((incident) => {
          const isSelected = selectedIncident?.id === incident.id;
          return (
            <Marker
              key={incident.id}
              position={[incident.lat, incident.lng]}
              icon={createCustomMarker(incident.sentiment, isSelected)}
              eventHandlers={{
                click: () => onSelectIncident(incident)
              }}
            />
          );
        })}
      </MapContainer>
    </div>
  );
}
