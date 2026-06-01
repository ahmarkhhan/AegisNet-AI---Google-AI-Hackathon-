// Domain models ported from mobile: com.aegisnet.mobile.domain.model.Models.kt

export interface CrisisAlert {
  id: number;
  type: string;
  title: string;
  description: string;
  casualtyRiskScore: number;
  escalationProbability: number;
  epicenterLat: number | null;
  epicenterLng: number | null;
  status: string;
  createdAt: string;
  affectedPopulation: number;
  responseTimeMinutes: number;
  resourcesDeployed: string[];
  casualtyEstimate?: number;
  recommendedActions?: string;
  zone: string;
  severity: string;
  timestamp: number;
  placeName?: string | null;
  nearbyHospitals?: string[];
  nearbyPolice?: string[];
  politicalParty?: string | null;
  politicalImplications?: string | null;
  seismicMagnitude?: number | null;
  seismicDepth?: number | null;
  seismicTremors?: boolean | null;
  photoPath?: string | null;
  videoPath?: string | null;
}

export interface EventSignal {
  id: number;
  source: string;
  type: string;
  rawPayload: string;
  confidence: number;
  credibility: number;
  location: string;
  timeAgo: string;
}

export interface DroneStatus {
  droneId: string;
  status: string;
  batteryPercent: number;
  scannedAreaKm2: number;
  rescuesAssisted: number;
}

export interface WeatherTelemetry {
  temperature: number;
  humidity: number;
  windSpeed: number;
  precipitation: number;
  condition: string;
}

export interface SocialTweet {
  username: string;
  handle: string;
  body: string;
  sentiment: string;
  timeAgo: string;
}

export type AgentRole = 'SOCIAL_SIGNAL' | 'CRISIS_ANALYZER' | 'ESCALATION_PREDICTOR' | 'RESOURCE_DISPATCHER' | 'NLP_TRANSLATOR';

export interface StructuredLog {
  message: string;
  phase: string;
  toolName?: string | null;
  timestamp: string;
}

export interface EocAgent {
  role: AgentRole;
  name: string;
  description: string;
  avatarEmoji: string;
  status: string;
  logs: StructuredLog[];
}

export interface ResourceAsset {
  name: string;
  total: number;
  baseInUse: number;
  icon: string;
}

export interface DepartmentRoster {
  departmentName: string;
  color: string;
  assets: ResourceAsset[];
}

export interface EocDepot {
  name: string;
  latitude: number;
  longitude: number;
  capacity: number;
  currentResources: number;
}

export interface ReportFormState {
  incidentType: string;
  description: string;
  addressQuery: string;
  photoFile: File | null;
  videoFile: File | null;
  voiceTranscript: string | null;
  politicalParty: string | null;
  politicalImplications: string | null;
  clashHazardScale: number;
  seismicMagnitude: number;
  seismicDepth: number;
  seismicTremors: boolean;
  isSubmitting: boolean;
  isSubmitted: boolean;
  errorMessage: string | null;
}

export interface DashboardUiState {
  alerts: CrisisAlert[];
  signals: EventSignal[];
  drones: DroneStatus[];
  weather: WeatherTelemetry;
  isLoading: boolean;
}
