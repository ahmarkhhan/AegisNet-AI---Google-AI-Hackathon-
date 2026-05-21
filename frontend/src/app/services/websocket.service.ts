import { Injectable, signal } from '@angular/core';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export interface CityThreat {
  cityName: string;
  latitude: number;
  longitude: number;
  temperatureC: number;
  precipitationMm: number;
  windSpeedKmh: number;
  humidityPct: number;
  weatherCode: number;
  weatherSummary: string;
  weatherSeverity: number;
  newsSeverity: number;
  overallThreatLevel: number;
  threatCategory: string;
  riskProfile: string;
  activeNewsSignals: number;
  lastUpdated: string;
}

export interface CrisisEvent {
  id: string;
  type: string;
  area: string;
  region: string;
  heading: string;
  description: string;
  criticality: string;
  criticalityScore: number;
  affectedPopulation: number;
  casualtyEstimate: number;
  displacedEstimate: number;
  infrastructureDamagePercent: number;
  latitude: number;
  longitude: number;
  radiusKm: number;
  responseStatus: string;
  resourcesDeployed: string;
  evacuationStatus: string;
  recommendedActions: string;
  source: string;
  confidence: number;
  detectedAt: string;
  lastUpdated: string;
}

@Injectable({
  providedIn: 'root'
})
export class WebsocketService {

  public cityThreats = signal<CityThreat[]>([]);
  public crisisEvents = signal<CrisisEvent[]>([]);
  public agentTraces = signal<string[]>([]);
  public connectionStatus = signal<boolean>(false);

  // Selected event for detail view
  public selectedEvent = signal<CrisisEvent | null>(null);
  public showEventList = signal<boolean>(false);
  public selectedCriticality = signal<string>('');

  private stompClient: Client | null = null;

  constructor() {
    this.connectWebSocket();
  }

  private connectWebSocket() {
    this.addTrace('> [System] Connecting to AegisNet backend...');

    this.stompClient = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws-aegisnet'),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,

      onConnect: () => {
        this.connectionStatus.set(true);
        this.addTrace('> [System] WebSocket CONNECTED. Crisis Intelligence Engine active.');

        this.stompClient!.subscribe('/topic/city-threats', (message) => {
          try {
            this.cityThreats.set(JSON.parse(message.body));
          } catch (e) { console.error('Parse error:', e); }
        });

        this.stompClient!.subscribe('/topic/crisis-events', (message) => {
          try {
            const events: CrisisEvent[] = JSON.parse(message.body);
            this.crisisEvents.set(events);
          } catch (e) { console.error('Parse error:', e); }
        });

        this.stompClient!.subscribe('/topic/traces', (message) => {
          this.addTrace('> ' + message.body);
        });
      },

      onDisconnect: () => {
        this.connectionStatus.set(false);
        this.addTrace('> [System] WebSocket DISCONNECTED. Reconnecting...');
      },

      onStompError: (frame) => {
        this.connectionStatus.set(false);
        this.addTrace('> [System] STOMP error: ' + frame.headers['message']);
      }
    });

    this.stompClient.activate();
  }

  private addTrace(msg: string) {
    this.agentTraces.update(traces => [msg, ...traces].slice(0, 150));
  }

  // --- Computed stats ---
  getEventsByCriticality(crit: string): CrisisEvent[] {
    return this.crisisEvents().filter(e => e.criticality === crit);
  }

  getTotalAffected(): number {
    return this.crisisEvents().reduce((sum, e) => sum + e.affectedPopulation, 0);
  }

  getTotalDisplaced(): number {
    return this.crisisEvents().reduce((sum, e) => sum + e.displacedEstimate, 0);
  }

  getActiveResponseCount(): number {
    return this.crisisEvents().filter(e => e.responseStatus === 'ACTIVE' || e.responseStatus === 'MOBILIZING').length;
  }

  // --- Click interactions ---
  selectCriticality(crit: string) {
    if (this.selectedCriticality() === crit) {
      this.showEventList.set(false);
      this.selectedCriticality.set('');
      this.selectedEvent.set(null);
    } else {
      this.selectedCriticality.set(crit);
      this.showEventList.set(true);
      this.selectedEvent.set(null);
    }
  }

  selectEvent(event: CrisisEvent) {
    if (this.selectedEvent()?.id === event.id) {
      this.selectedEvent.set(null);
    } else {
      this.selectedEvent.set(event);
    }
  }

  closeDetail() {
    this.selectedEvent.set(null);
    this.showEventList.set(false);
    this.selectedCriticality.set('');
  }

  // --- Color helpers ---
  getCritColor(crit: string): string {
    switch (crit) {
      case 'CRITICAL': return '#ef4444';
      case 'SEVERE': return '#f97316';
      case 'HIGH': return '#eab308';
      case 'MODERATE': return '#3b82f6';
      case 'LOW': return '#22c55e';
      default: return '#64748b';
    }
  }

  getCritClass(crit: string): string {
    switch (crit) {
      case 'CRITICAL': return 'text-red-500';
      case 'SEVERE': return 'text-orange-500';
      case 'HIGH': return 'text-yellow-500';
      case 'MODERATE': return 'text-blue-400';
      case 'LOW': return 'text-green-400';
      default: return 'text-slate-400';
    }
  }

  getCritBg(crit: string): string {
    switch (crit) {
      case 'CRITICAL': return 'bg-red-500/15 border-red-500/40';
      case 'SEVERE': return 'bg-orange-500/15 border-orange-500/40';
      case 'HIGH': return 'bg-yellow-500/15 border-yellow-500/40';
      case 'MODERATE': return 'bg-blue-400/15 border-blue-400/40';
      case 'LOW': return 'bg-green-400/15 border-green-400/40';
      default: return 'bg-slate-700/15 border-slate-700/40';
    }
  }

  getTypeIcon(type: string): string {
    switch (type) {
      case 'FLOOD': return '🌊';
      case 'EARTHQUAKE': return '🔴';
      case 'LANDSLIDE': return '⛰️';
      case 'SNOWSTORM': return '❄️';
      case 'HEATWAVE': return '🔥';
      case 'CYCLONE': return '🌀';
      case 'DROUGHT': return '☀️';
      default: return '⚠️';
    }
  }
}
