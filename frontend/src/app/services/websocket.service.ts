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
  aqiSeverity: number;
  floodSeverity: number;
  overallThreatLevel: number;
  threatCategory: string;
  riskProfile: string;
  activeNewsSignals: number;
  usAqi: number;
  pm25: number;
  pm10: number;
  riverDischargeM3s: number;
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
  referenceLink?: string;
  confidence: number;
  detectedAt: string;
  lastUpdated: string;
}

export interface AgentTraceEntry {
  role: string;
  agentName: string;
  phase: string;
  message: string;
  toolName: string | null;
  timestamp: string;
}

export interface DashboardStatsData {
  activeAlerts: number;
  signalsProcessed: number;
  avgResponseTimeMinutes: number;
  systemReadinessPercent: number;
  totalAffectedPopulation: number;
  activeAgents: number;
  dispatchesExecuted: number;
  totalCrisisEvents: number;
}

export interface DispatchManifestData {
  eventId: string;
  targetAgency: string;
  zone: string;
  latitude: number;
  longitude: number;
  squadsDeployed: number;
  status: string;
  manifestFile: string;
  authorization: string;
  createdAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class WebsocketService {

  public cityThreats = signal<CityThreat[]>([]);
  public crisisEvents = signal<CrisisEvent[]>([]);
  public agentTraces = signal<string[]>([]);
  public connectionStatus = signal<boolean>(false);
  public lastSyncTime = signal<Date | null>(null);

  // Structured agent trace drawer (global, visible from any tab)
  public agentTraceDrawer = signal<AgentTraceEntry[]>([]);
  public showTraceDrawer = signal<boolean>(false);

  // Dashboard stats from backend
  public dashboardStats = signal<DashboardStatsData>({
    activeAlerts: 0, signalsProcessed: 0, avgResponseTimeMinutes: 0,
    systemReadinessPercent: 100, totalAffectedPopulation: 0,
    activeAgents: 6, dispatchesExecuted: 0, totalCrisisEvents: 0
  });

  // Dispatch manifests
  public dispatchManifests = signal<DispatchManifestData[]>([]);

  // Selected event for detail view
  public selectedEvent = signal<CrisisEvent | null>(null);
  public showEventList = signal<boolean>(false);
  public selectedCriticality = signal<string>('');

  private stompClient: Client | null = null;

  constructor() {}
  
  public initConnections(crisisData: any) {
    this.connectWebSocket(crisisData);
  }

  private connectWebSocket(crisisData: any) {
    this.addTrace('> [System] Connecting to Nigehban AI backend...');

    this.stompClient = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws-aegisnet'),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,

      onConnect: () => {
        this.connectionStatus.set(true);
        this.lastSyncTime.set(new Date());
        this.addTrace('> [System] WebSocket CONNECTED. Crisis Intelligence Engine active.');

        this.stompClient!.subscribe('/topic/city-threats', (message) => {
          try {
            this.cityThreats.set(JSON.parse(message.body));
            this.lastSyncTime.set(new Date());
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

        this.stompClient!.subscribe('/topic/agent-logs', (message) => {
          try {
            const logEvent = JSON.parse(message.body);
            if (crisisData && crisisData.addAgentLog) {
               crisisData.addAgentLog(logEvent.role, logEvent.message, logEvent.phase, logEvent.toolName, logEvent.timestamp);
            }
            // Also push to global trace drawer
            this.agentTraceDrawer.update(logs => [{
              role: logEvent.role || logEvent.agentName,
              agentName: logEvent.agentName || logEvent.role,
              phase: logEvent.phase || 'REASONING',
              message: logEvent.message,
              toolName: logEvent.toolName || null,
              timestamp: logEvent.timestamp || new Date().toLocaleTimeString('en-GB')
            }, ...logs].slice(0, 100));
          } catch (e) { console.error('Agent log parse error:', e); }
        });

        this.stompClient!.subscribe('/topic/autonomous-actions', (message) => {
          try {
            const action = JSON.parse(message.body);
            if (crisisData && crisisData.handleAutonomousAction) {
               crisisData.handleAutonomousAction(action);
            }
          } catch (e) { console.error('Action parse error:', e); }
        });

        this.stompClient!.subscribe('/topic/dashboard-stats', (message) => {
          try {
            const stats = JSON.parse(message.body);
            this.dashboardStats.set(stats);
          } catch (e) { console.error('Dashboard stats parse error:', e); }
        });

        this.stompClient!.subscribe('/topic/dispatch-log', (message) => {
          try {
            const manifest = JSON.parse(message.body);
            this.dispatchManifests.update(list => [manifest, ...list].slice(0, 50));
          } catch (e) { console.error('Dispatch log parse error:', e); }
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
      // Geological
      case 'EARTHQUAKE': return '💥';
      case 'LANDSLIDE': return '⛰️';
      case 'AVALANCHE': return '🏔️';
      case 'VOLCANO': return '🌋';
      // Hydrological
      case 'FLOOD': return '🌊';
      case 'SNOWSTORM': return '❄️';
      case 'HEATWAVE': return '🔥';
      case 'CYCLONE': return '🌀';
      case 'DROUGHT': return '☀️';
      case 'WILDFIRE': return '🌲';
      // Public Safety
      case 'RIOT': return '🛡️';
      case 'CROWD_CRUSH': return '👥';
      case 'MCI': return '🚑';
      // National Security
      case 'TERRORISM': return '⚔️';
      case 'CBRN': return '☢️';
      case 'CYBERATTACK': return '💻';
      // Infrastructure
      case 'GRID_COLLAPSE': return '⚡';
      case 'DAM_FAILURE': return '🏗️';
      case 'HAZMAT': return '🛢️';
      // Biological
      case 'PANDEMIC': return '☣️';
      case 'EPIDEMIC': return '🦠';
      
      default: return '⚠️';
    }
  }

  getPhaseColor(phase: string): string {
    switch (phase) {
      case 'PERCEPTION': return '#38bdf8'; // sky
      case 'REASONING': return '#fbbf24';  // amber
      case 'TOOL_CALL': return '#a78bfa';  // purple
      case 'ACTION': return '#34d399';     // emerald
      case 'RESULT': return '#e2e8f0';     // slate-200
      case 'ERROR': return '#f87171';      // red
      case 'FALLBACK': return '#fb923c';   // orange
      default: return '#94a3b8';           // slate-400
    }
  }

  getPhaseIcon(phase: string): string {
    switch (phase) {
      case 'PERCEPTION': return '👁️';
      case 'REASONING': return '🧠';
      case 'TOOL_CALL': return '🔧';
      case 'ACTION': return '⚡';
      case 'RESULT': return '✅';
      case 'ERROR': return '❌';
      case 'FALLBACK': return '🔄';
      default: return '📋';
    }
  }
}
