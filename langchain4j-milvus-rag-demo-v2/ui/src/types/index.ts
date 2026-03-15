export interface ApiError {
  ok: boolean;
  code: number;
  message: string;
  path: string;
  timestamp: string;
  details?: Record<string, unknown>;
}

export interface SourceItem {
  type: string;
  title?: string;
  url?: string;
  content?: string;
  fileName?: string;
  chunkId?: number;
  score?: number;
}

export interface ToolTrace {
  toolName: string;
  summary: string;
}

export interface ChatAnswer {
  conversationId?: string;
  question: string;
  answer: string;
  knowledgeHit: boolean;
  webSearchUsed: boolean;
  weatherUsed: boolean;
  sources: SourceItem[];
  toolTraces: ToolTrace[];
}

export interface VectorStoreConfig {
  alias: string;
  host: string;
  port: number;
  collection: string;
  topK: number;
  minScore: number;
}

export interface CurrentVectorStoreResponse {
  ok: boolean;
  activeAlias: string;
  config: VectorStoreConfig;
}

export interface VectorStoreSaveRequest extends VectorStoreConfig {}

export interface IngestTextRequest {
  text: string;
}

export interface MilvusCollectionQueryRequest {
  databaseName?: string;
  collectionName: string;
  filter?: string;
  outputFields?: string[];
  partitionNames?: string[];
  offset: number;
  limit: number;
  loadBeforeQuery: boolean;
}

export interface MilvusCreateCollectionRequest {
  databaseName?: string;
  collectionName: string;
  description?: string;
  dimension: number;
  primaryFieldName?: string;
  idType?: string;
  maxLength?: number;
  vectorFieldName?: string;
  metricType?: string;
  autoId?: boolean;
  enableDynamicField?: boolean;
  numShards?: number;
}
