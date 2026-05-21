import { Component, inject, AfterViewInit, OnDestroy, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { WebsocketService, CrisisEvent } from './services/websocket.service';
import * as L from 'leaflet';

@Component({
  selector: 'app-root',
  imports: [CommonModule],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App implements AfterViewInit, OnDestroy {
  public wsService = inject(WebsocketService);

  private map!: L.Map;
  private crisisMarkers: Map<string, { marker: L.CircleMarker; circle: L.Circle }> = new Map();
  private intervals: any[] = [];

  public mouseCoords = { lat: 30.3753, lng: 69.3451 };
  public mapZoom = 5;
  public currentTime = '';

  constructor() {
    // Update clock
    setInterval(() => {
      this.currentTime = new Date().toLocaleTimeString('en-GB');
    }, 1000);

    // React to crisis events changes and update map markers
    effect(() => {
      const events = this.wsService.crisisEvents();
      if (this.map && events.length > 0) {
        this.syncCrisisMarkers(events);
      }
    });
  }

  ngAfterViewInit() {
    this.initMap();
  }

  ngOnDestroy() {
    this.intervals.forEach(i => clearInterval(i));
  }

  private initMap() {
    this.map = L.map('satellite-map', {
      center: [30.3753, 69.3451],
      zoom: 5.5,
      zoomControl: true,
      attributionControl: true,
      maxBounds: L.latLngBounds([18, 55], [40, 85]),
      minZoom: 4.5
    });

    // Dark satellite tiles
    L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', {
      attribution: '&copy; Esri',
      maxZoom: 18
    }).addTo(this.map);

    // Mouse tracking
    this.map.on('mousemove', (e: L.LeafletMouseEvent) => {
      this.mouseCoords.lat = Number(e.latlng.lat.toFixed(4));
      this.mouseCoords.lng = Number(e.latlng.lng.toFixed(4));
    });

    this.map.on('zoomend', () => {
      this.mapZoom = this.map.getZoom();
    });
  }

  private syncCrisisMarkers(events: CrisisEvent[]) {
    // Remove stale markers
    const activeIds = new Set(events.map(e => e.id));
    this.crisisMarkers.forEach((val, key) => {
      if (!activeIds.has(key)) {
        this.map.removeLayer(val.marker);
        this.map.removeLayer(val.circle);
        this.crisisMarkers.delete(key);
      }
    });

    // Add or update markers
    events.forEach(event => {
      const color = this.wsService.getCritColor(event.criticality);
      const radius = Math.max(event.criticalityScore * 300, 5000); // radius in meters

      if (this.crisisMarkers.has(event.id)) {
        // Update existing
        const existing = this.crisisMarkers.get(event.id)!;
        existing.circle.setStyle({ color, fillColor: color, fillOpacity: this.getOpacity(event.criticality) });
        existing.circle.setRadius(radius);
        existing.marker.setStyle({ color, fillColor: color });
      } else {
        // Create new
        const circle = L.circle([event.latitude, event.longitude], {
          radius: radius,
          color: color,
          weight: 1,
          fillColor: color,
          fillOpacity: this.getOpacity(event.criticality),
          className: 'crisis-pulse'
        }).addTo(this.map);

        const marker = L.circleMarker([event.latitude, event.longitude], {
          radius: this.getMarkerSize(event.criticality),
          color: color,
          weight: 2,
          fillColor: color,
          fillOpacity: 0.8
        }).addTo(this.map);

        // Popup
        marker.bindPopup(`
          <div style="font-family:monospace;font-size:11px;min-width:200px;background:#111;color:#fff;padding:10px;border:1px solid ${color};border-radius:4px;">
            <div style="font-weight:bold;font-size:13px;color:${color};margin-bottom:4px;">${this.wsService.getTypeIcon(event.type)} ${event.area}</div>
            <div style="color:#999;font-size:9px;margin-bottom:6px;">${event.region} | ${event.type}</div>
            <div style="color:#ddd;margin-bottom:6px;">${event.heading}</div>
            <div style="display:flex;gap:8px;font-size:9px;color:#888;">
              <span style="color:${color};">■ ${event.criticality} (${event.criticalityScore})</span>
              <span>Affected: ${this.formatNum(event.affectedPopulation)}</span>
            </div>
          </div>
        `, {
          className: 'dark-popup',
          closeButton: true
        });

        // Click to select in sidebar
        marker.on('click', () => {
          this.wsService.selectEvent(event);
        });

        this.crisisMarkers.set(event.id, { marker, circle });
      }
    });
  }

  private getOpacity(crit: string): number {
    switch (crit) {
      case 'CRITICAL': return 0.25;
      case 'SEVERE': return 0.20;
      case 'HIGH': return 0.15;
      case 'MODERATE': return 0.10;
      default: return 0.06;
    }
  }

  private getMarkerSize(crit: string): number {
    switch (crit) {
      case 'CRITICAL': return 10;
      case 'SEVERE': return 8;
      case 'HIGH': return 6;
      case 'MODERATE': return 5;
      default: return 4;
    }
  }

  formatNum(n: number): string {
    if (n >= 1000000) return (n / 1000000).toFixed(1) + 'M';
    if (n >= 1000) return (n / 1000).toFixed(1) + 'K';
    return n.toString();
  }
}
