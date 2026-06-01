import { Injectable, signal, computed } from '@angular/core';
import {
  CrisisAlert, EventSignal, DroneStatus, WeatherTelemetry,
  SocialTweet, EocAgent, AgentRole, DashboardUiState, StructuredLog
} from '../models/crisis.models';

@Injectable({ providedIn: 'root' })
export class CrisisDataService {

  // ── Core State Signals ──
  public alerts = signal<CrisisAlert[]>([]);
  public signals = signal<EventSignal[]>([]);
  public drones = signal<DroneStatus[]>([]);
  public weather = signal<WeatherTelemetry>({ temperature: 18.5, humidity: 72, windSpeed: 14.5, precipitation: 0.2, condition: 'Cloudy' });

  // ── Agent State ──
  public agents = signal<Record<AgentRole, EocAgent>>(this.initAgents());
  public tweets = signal<SocialTweet[]>(this.initTweets());
  public isAiOnline = signal<boolean>(true);

  // ── Reported Incidents (localStorage-backed) ──
  public reportedIncidents = signal<CrisisAlert[]>(this.loadReportedIncidents());

  constructor() {
    this.loadMockData();
  }

  private initAgents(): Record<AgentRole, EocAgent> {
    const makeMockLogs = (messages: string[]) => messages.map((m, idx) => ({
      message: m,
      phase: m.includes('[SYSTEM]') ? 'INFO' : 'PERCEPTION',
      toolName: null,
      timestamp: new Date(Date.now() - (messages.length - idx) * 60 * 1000).toLocaleTimeString('en-GB')
    }));

    return {
      'SOCIAL_SIGNAL': { role: 'SOCIAL_SIGNAL', name: 'Social Signal Verifier', description: 'Scans X/Twitter for crisis-related panic signals and verifies credibility.', avatarEmoji: '🐦', status: 'ONLINE', logs: makeMockLogs(['[SYSTEM] Establishing connection to X/Twitter firehose...', '[SYSTEM] NLP sentiment models loaded.', '[IDLE] Polling for panic-related keyword clusters...']) },
      'CRISIS_ANALYZER': { role: 'CRISIS_ANALYZER', name: 'Crisis Analyzer', description: 'Deep analysis of incoming crisis data to determine severity and action plans.', avatarEmoji: '🔬', status: 'ONLINE', logs: makeMockLogs(['[SYSTEM] Core logic engine initialized.', '[SYSTEM] Multi-source verification protocols active.', '[IDLE] Awaiting anomalous event triggers...']) },
      'ESCALATION_PREDICTOR': { role: 'ESCALATION_PREDICTOR', name: 'Escalation Predictor', description: 'Multi-signal correlation engine predicting crisis escalation probability.', avatarEmoji: '📈', status: 'ONLINE', logs: makeMockLogs(['[SYSTEM] Predictive models calibrated to current geospatial data.', '[IDLE] Monitoring risk matrices...']) },
      'RESOURCE_DISPATCHER': { role: 'RESOURCE_DISPATCHER', name: 'Resource Dispatcher', description: 'Optimizes rescue resource allocation from nearest EOC depots.', avatarEmoji: '🚑', status: 'ONLINE', logs: makeMockLogs(['[SYSTEM] Connected to regional EOC dispatch APIs.', '[SYSTEM] Drone fleet telemetry synced.', '[IDLE] Ready for physical dispatch commands...']) },
      'NLP_TRANSLATOR': { role: 'NLP_TRANSLATOR', name: 'NLP Translator', description: 'Translates multilingual crisis reports (Urdu/Pashto/English) and classifies type.', avatarEmoji: '🌐', status: 'ONLINE', logs: makeMockLogs(['[SYSTEM] Regional dialects loaded (Urdu, Pashto, Punjabi).', '[IDLE] Awaiting incoming citizen reports...']) },
    };
  }

  private initTweets(): SocialTweet[] {
    return [
      { username: 'Ahmad Bilal', handle: 'AhmadBilal', body: 'Nullah Lai water level rising rapidly near Liaquat Bagh! Stay safe everyone. #RawalpindiFloods', sentiment: 'PANIC', timeAgo: '2 mins ago' },
      { username: 'Dr. Ayesha Khan', handle: 'AyeshaMD', body: 'Emergency ambulances dispatched from closest sector depot. Drone sweeps scanning perimeter structures.', sentiment: 'SAFE', timeAgo: '5 mins ago' },
      { username: 'Kamran Shah', handle: 'Kamran_Shah', body: 'Mall Road rally is massive. EOC Security Sector Posts are active directing traffic to grid annexes.', sentiment: 'CONCERNED', timeAgo: '8 mins ago' },
      { username: 'Zara Khan', handle: 'ZaraK_EOC', body: 'Nearby trauma units fully pre-allocated. Smart dispatch sorting backup assets. #NigehbanAI', sentiment: 'SAFE', timeAgo: '12 mins ago' },
      { username: 'Hassan Jamil', handle: 'HassanJ', body: 'Roads reported collapsed near underpass, but Nigehban safety detour vectors are fully flashing on maps!', sentiment: 'CONCERNED', timeAgo: '15 mins ago' },
    ];
  }

  private loadMockData(): void {
    // Mock alerts from AlertRepository.kt
    const mockAlerts: CrisisAlert[] = [
      {
        id: 1, type: 'MASS_ENTRAPMENT_RISK', title: 'Murree Snowstorm Entrapment',
        description: 'Critical: mass vehicle entrapment within 45 min. Freezing risk imminent.',
        casualtyRiskScore: 95, escalationProbability: 98,
        epicenterLat: 33.9070, epicenterLng: 73.3943,
        status: 'RESOURCE_DISPATCHED', createdAt: '15 mins ago',
        affectedPopulation: 2300, responseTimeMinutes: 15,
        resourcesDeployed: ['High-Altitude Snowplow (4)', 'Rescue Ranger Squad (3)', 'Medical Mobile Clinic (1)'],
        zone: 'Murree Expressway, Punjab', severity: 'CRITICAL',
        timestamp: Date.now() - 15 * 60 * 1000
      },
      {
        id: 2, type: 'POLITICAL_RALLY', title: 'Jalsa Crowd Surge - Charing Cross',
        description: 'Massive gathering at Charing Cross. Potential for stampede. Crowd crush risk escalating.',
        casualtyRiskScore: 78, escalationProbability: 65,
        epicenterLat: 31.5584, epicenterLng: 74.3268,
        status: 'RESOURCE_DISPATCHED', createdAt: '8 mins ago',
        affectedPopulation: 45000, responseTimeMinutes: 8,
        resourcesDeployed: ['Police Crowd Containment (5)', 'Medical Ambulance Unit (2)'],
        zone: 'FOB Charing Cross, Lahore', severity: 'HIGH',
        politicalParty: 'PTI', politicalImplications: 'Severe roadblocks around Mall Road. Heavy clash risk with security containment.',
        timestamp: Date.now() - 8 * 60 * 1000
      },
      {
        id: 3, type: 'ARMED_VIOLENCE', title: 'Active Shooting - Defence Mall',
        description: 'Reports of gunfire near Defence Shopping Mall. Civilians trapped. Armed suspect on loose.',
        casualtyRiskScore: 92, escalationProbability: 88,
        epicenterLat: 31.4802, epicenterLng: 74.3725,
        status: 'RESOURCE_DISPATCHED', createdAt: '5 mins ago',
        affectedPopulation: 850, responseTimeMinutes: 5,
        resourcesDeployed: ['Police Tactical SWAT (4)', 'Paramedic Ambulance (3)'],
        zone: 'Defence, Lahore', severity: 'CRITICAL',
        timestamp: Date.now() - 5 * 60 * 1000
      },
      {
        id: 4, type: 'STAMPEDE_RISK', title: 'Concert Venue Overcrowding',
        description: 'Venue capacity exceeded at Gaddafi Stadium concert. Crowd control failing. Risk of crush injury.',
        casualtyRiskScore: 85, escalationProbability: 72,
        epicenterLat: 31.5126, epicenterLng: 74.3315,
        status: 'PREDICTED', createdAt: '10 mins ago',
        affectedPopulation: 12000, responseTimeMinutes: 10,
        resourcesDeployed: [],
        zone: 'Gaddafi Stadium, Lahore', severity: 'HIGH',
        timestamp: Date.now() - 10 * 60 * 1000
      }
    ];

    const mockSignals: EventSignal[] = [
      { id: 1, source: 'SUPARCO_WEATHER', type: 'WEATHER_ANOMALY', rawPayload: '{"precip_mm_hr": 55, "temp_c": -6, "wind_kmh": 65}', confidence: 88, credibility: 1.0, location: 'Murree Expressway', timeAgo: '1 min ago' },
      { id: 2, source: 'SOCIAL_X', type: 'SOCIAL_PANIC', rawPayload: '"Cars completely stuck near Guldana. People are freezing!"', confidence: 92, credibility: 0.94, location: 'Murree, Punjab', timeAgo: '3 mins ago' },
      { id: 3, source: 'HOTLINE_1122', type: 'EMERGENCY_CALL', rawPayload: 'Multiple calls about trapped vehicles near Barian.', confidence: 85, credibility: 0.99, location: 'Barian, Murree', timeAgo: '5 mins ago' },
    ];

    const mockDrones: DroneStatus[] = [
      { droneId: 'DRONE_MRE_01', status: 'SCANNING', batteryPercent: 78, scannedAreaKm2: 34, rescuesAssisted: 127 },
      { droneId: 'DRONE_MRE_02', status: 'IN_TRANSIT', batteryPercent: 91, scannedAreaKm2: 0, rescuesAssisted: 0 },
    ];

    this.alerts.set([...this.loadReportedIncidents(), ...mockAlerts]);
    this.signals.set(mockSignals);
    this.drones.set(mockDrones);
  }

  // ── Agent Log Actions ──
  addAgentLog(role: AgentRole, message: string, phase?: string, toolName?: string | null, timestamp?: string): void {
    this.agents.update(agents => {
      const agent = { ...agents[role] };
      const newLog: StructuredLog = {
        message,
        phase: phase || 'INFO',
        toolName: toolName || null,
        timestamp: timestamp || new Date().toLocaleTimeString('en-GB')
      };
      agent.logs = [...agent.logs, newLog];
      return { ...agents, [role]: agent };
    });
  }

  // ── Lethal Action Dispatch ──
  handleAutonomousAction(action: any): void {
    const droneId = 'DRONE_AUTO_' + Math.floor(Math.random() * 900 + 100);
    const newDrone: DroneStatus = {
      droneId,
      status: 'DISPATCHED_TO_' + (action.zone || 'UNKNOWN').substring(0, 8).toUpperCase(),
      batteryPercent: 100,
      scannedAreaKm2: 0,
      rescuesAssisted: 0
    };
    
    // Push the new drone to the top of the drones list so it's visibly deployed in the UI dashboard!
    this.drones.update(d => [newDrone, ...d]);
    
    // Also log an overarching system message about the autonomous action
    this.addAgentLog('RESOURCE_DISPATCHER', `[SYSTEM NOTIFICATION] Physical Drone ${droneId} mobilized to ${action.zone} successfully.`);
  }

  // ── Report Submission ──
  submitReport(alert: CrisisAlert): void {
    this.reportedIncidents.update(list => [alert, ...list]);
    this.alerts.update(list => [alert, ...list]);
    localStorage.setItem('aegis_reports', JSON.stringify(this.reportedIncidents()));

    // Trigger backend agentic reasoning
    fetch('http://localhost:8080/api/simulation/report', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        type: alert.type,
        title: alert.title,
        description: alert.description,
        severity: alert.severity,
        affectedCount: alert.affectedPopulation,
        latitude: alert.epicenterLat,
        longitude: alert.epicenterLng
      })
    }).catch(err => console.error('Failed to trigger backend agent loop:', err));
  }

  private loadReportedIncidents(): CrisisAlert[] {
    try {
      const stored = localStorage.getItem('aegis_reports');
      return stored ? JSON.parse(stored) : [];
    } catch { return []; }
  }

  // ── Helpers ──
  getAlertsByRegion(region: string): CrisisAlert[] {
    return this.alerts().filter(a =>
      a.zone.toLowerCase().includes(region.toLowerCase()) ||
      a.title.toLowerCase().includes(region.toLowerCase())
    );
  }

  getSeverityColor(severity: string): string {
    switch (severity) {
      case 'CRITICAL': return '#FF2A6D';
      case 'HIGH': return '#FF9F0A';
      default: return '#05FF80';
    }
  }

  getCrisisTypeLabel(type: string): string {
    const map: Record<string, string> = {
      'POLITICAL_RALLY': '📢 Political Rally', 'ARMED_VIOLENCE': '🔫 Armed Violence',
      'STAMPEDE_RISK': '🏃 Stampede Risk', 'DISEASE_OUTBREAK': '🦠 Disease',
      'CYBERATTACK': '💻 Cyber Attack', 'INFRASTRUCTURE_COLLAPSE': '🏗️ Infrastructure',
      'MASS_ENTRAPMENT_RISK': '⛓️ Entrapment', 'FLOOD_ESCALATION': '🌊 Flooding',
      'FLOOD_WATER': '🌊 Flood Water', 'SEISMIC_ACTIVITY': '🌍 Seismic',
      'TRAFFIC_BLOCKAGE': '🚗 Traffic Blockage', 'ROAD_COLLAPSE': '🛣️ Road Collapse',
    };
    return map[type] || '⚠️ Crisis';
  }
}
