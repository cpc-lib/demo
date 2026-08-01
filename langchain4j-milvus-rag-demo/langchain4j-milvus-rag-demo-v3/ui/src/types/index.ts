export interface ApiError {
  ok: boolean;
  traceId?: string;
  data?: null;
  error?: RagApiErrorDetail;
  code?: number | string;
  message?: string;
  path?: string;
  timestamp?: string;
  details?: Record<string, unknown>;
}

export interface SourceItem {
  type: string;
  title?: string;
  url?: string;
  content?: string;
  fileName?: string;
  chunkId?: string;
  version?: number;
  chunkStatus?: string;
  contentType?: string;
  imageUrl?: string;
  pageNo?: number;
  sectionTitle?: string;
  imageCaption?: string;
  imageNumber?: string;
  score?: number;
}

export interface ToolTrace {
  toolName: string;
  summary: string;
}

export interface ChatRequest {
  question: string;
  conversationId?: string;
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

export interface RagApiErrorDetail {
  code: string;
  message: string;
  details?: Record<string, unknown>;
}

export interface RagApiResponse<T> {
  ok: boolean;
  traceId?: string;
  data: T;
  error?: RagApiErrorDetail;
}

export interface RagAgentPrompt {
  id?: number;
  tenantId?: number;
  promptName?: string;
  promptContent: string;
  version?: number;
  status?: number;
  createdBy?: string;
  updatedBy?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface AgentPromptUpdateRequest {
  promptName?: string;
  promptContent: string;
  enabled?: boolean;
}

export interface AgentPromptRollbackRequest {
  version: number;
}

export interface PageResponse<T> {
  pageNo: number;
  pageSize: number;
  total: number;
  pages: number;
  maxPageSize?: number;
  sortBy?: string;
  sortDirection?: string;
  records: T[];
}

export interface RagQueryRequest {
  tenantId?: number;
  knowledgeBaseIds: number[];
  question?: string;
  imageUrl?: string;
  imageAssetId?: number;
  imageBase64?: string;
  modalities?: string[];
  conversationId?: string;
  retrievalMode?: string;
  topK?: number;
  minScore?: number;
  textVectorWeight?: number;
  imageVectorWeight?: number;
  keywordWeight?: number;
  includeReviewPending?: boolean;
  contentTypes?: string[];
  permissionTags?: string[];
  enableRewrite?: boolean;
  enableRerank?: boolean;
  includeSources?: boolean;
}

export interface RagSearchRequest {
  tenantId?: number;
  knowledgeBaseIds: number[];
  query?: string;
  imageUrl?: string;
  imageAssetId?: number;
  imageBase64?: string;
  modalities?: string[];
  retrievalMode?: string;
  topK?: number;
  minScore?: number;
  textVectorWeight?: number;
  imageVectorWeight?: number;
  keywordWeight?: number;
  includeReviewPending?: boolean;
  contentTypes?: string[];
  permissionTags?: string[];
}

export interface RagSearchItem {
  rank: number;
  score?: number;
  knowledgeBaseId?: number;
  documentId?: string;
  documentName?: string;
  chunkId?: string;
  version?: number;
  contentType?: string;
  pageNo?: number;
  sectionTitle?: string;
  imageCaption?: string;
  imageNumber?: string;
  imageUrl?: string;
  content?: string;
  metadata?: Record<string, unknown>;
  modality?: string;
  retrievalSource?: string;
  imageAssetId?: number;
  fusionScore?: number;
}

export interface RagUsage {
  promptTokens?: number;
  completionTokens?: number;
  totalTokens?: number;
}

export interface RagQueryResponse {
  queryLogId?: number;
  conversationId?: string;
  answer: string;
  knowledgeHit: boolean;
  sources: RagSearchItem[];
  usage?: RagUsage;
}

export interface RagSearchResponse {
  queryLogId?: number;
  items: RagSearchItem[];
}

export interface RagImageSearchRequest {
  knowledgeBaseIds: number[];
  question?: string;
  imageUrl?: string;
  imageAssetId?: number;
  imageBase64?: string;
  retrievalMode?: string;
  topK?: number;
  minScore?: number;
  includeReviewPending?: boolean;
  contentTypes?: string[];
  permissionTags?: string[];
}

export interface RagImageSearchResponse {
  queryLogId?: number;
  similarImages: RagSearchItem[];
  relatedKnowledge: RagSearchItem[];
  items: RagSearchItem[];
}

export interface RagRetrievalEvaluationCase {
  caseId?: string;
  query: string;
  tenantId?: number;
  knowledgeBaseIds?: number[];
  retrievalMode?: string;
  topK?: number;
  minScore?: number;
  contentTypes?: string[];
  permissionTags?: string[];
  expectedChunkIds: string[];
  caseDbId?: number;
  knowledgeBaseId?: number;
  versionTag?: string;
  queryCategory?: string;
  difficultyLevel?: string;
  language?: string;
  expectedAnswerType?: string;
}

export interface RagRetrievalEvaluationRequest {
  tenantId?: number;
  knowledgeBaseId?: number;
  versionTag?: string;
  retrievalMode?: string;
  caseIds?: number[];
  cases?: RagRetrievalEvaluationCase[];
}

export interface RagRetrievalEvaluationCaseResult {
  caseId?: string;
  query: string;
  topK: number;
  expectedChunkIds: string[];
  retrievedChunkIds: string[];
  hit: boolean;
  reciprocalRank: number;
  recall: number;
  failureType?: string;
  failureReason?: string;
  retrievalTraceJson?: string;
  clusterKey?: string;
}

export interface RagRetrievalEvaluationResponse {
  runId?: number;
  runNo?: string;
  knowledgeBaseId?: number;
  versionTag?: string;
  retrievalMode?: string;
  totalCases: number;
  hitRate: number;
  meanReciprocalRank: number;
  meanRecall: number;
  results: RagRetrievalEvaluationCaseResult[];
}

export interface RagRetrievalEvalCase {
  id: number;
  tenantId: number;
  knowledgeBaseId?: number;
  versionTag: string;
  caseId: string;
  queryText: string;
  retrievalMode: string;
  queryCategory?: string;
  difficultyLevel?: string;
  language?: string;
  expectedAnswerType?: string;
  topK?: number;
  minScore?: number;
  contentTypesJson?: string;
  permissionTagsJson?: string;
  expectedChunkIdsJson: string;
  enabled: boolean;
  metadataJson?: string;
  createdAt?: string;
  updatedAt?: string;
  isDeleted?: number;
}

export interface RagRetrievalEvaluationCaseUpsertRequest {
  id?: number;
  tenantId?: number;
  knowledgeBaseId?: number;
  versionTag?: string;
  caseId?: string;
  query: string;
  retrievalMode?: string;
  queryCategory?: string;
  difficultyLevel?: string;
  language?: string;
  expectedAnswerType?: string;
  topK?: number;
  minScore?: number;
  contentTypes?: string[];
  permissionTags?: string[];
  expectedChunkIds: string[];
  enabled?: boolean;
  metadataJson?: string;
}

export interface RagRetrievalEvalRun {
  id: number;
  runNo: string;
  tenantId: number;
  knowledgeBaseId?: number;
  versionTag: string;
  retrievalMode: string;
  totalCases: number;
  hitRate: number;
  meanReciprocalRank: number;
  meanRecall: number;
  source: string;
  metadataJson?: string;
  createdAt?: string;
}

export interface RagRetrievalEvalCaseResult {
  id: number;
  runId: number;
  caseDbId?: number;
  caseId?: string;
  queryText: string;
  topK: number;
  expectedChunkIdsJson: string;
  retrievedChunkIdsJson: string;
  hit: boolean;
  reciprocalRank: number;
  recall: number;
  failureType?: string;
  failureReason?: string;
  retrievalTraceJson?: string;
  clusterKey?: string;
  createdAt?: string;
}

export interface RagRetrievalEvaluationRunDetailResponse {
  run: RagRetrievalEvalRun;
  results: RagRetrievalEvalCaseResult[];
}

export interface RagRetrievalEvaluationReportItem {
  knowledgeBaseId?: number;
  versionTag: string;
  retrievalMode: string;
  runCount: number;
  totalCases: number;
  avgHitRate: number;
  avgMeanReciprocalRank: number;
  avgMeanRecall: number;
  latestRunId?: number;
  latestRunNo?: string;
  latestCreatedAt?: string;
}

export interface RagRerankCallLog {
  id: number;
  tenantId: number;
  provider: string;
  model: string;
  queryHash: string;
  apiKeyHash?: string;
  tenantExternalId?: string;
  requestWindow?: string;
  candidateCount: number;
  topK: number;
  inputTokens: number;
  outputTokens: number;
  totalTokens: number;
  latencyMs: number;
  success: boolean;
  fallback: boolean;
  estimatedCost: number;
  errorCode?: string;
  httpStatus?: number;
  errorCodeNormalized?: string;
  degradedReason?: string;
  retryCount?: number;
  cacheHit?: boolean;
  errorMessage?: string;
  createdAt?: string;
}

export interface RagRerankObservationSummary {
  totalRequests: number;
  successCount: number;
  failedCount: number;
  degradedCount: number;
  failureRate: number;
  averageLatencyMs: number;
  totalTokens: number;
  estimatedCost: number;
}

export interface KeywordIndexHealthResponse {
  enabled: boolean;
  provider: string;
  engineCompatible: string;
  baseUrl: string;
  indexName: string;
  indexAlias?: string;
  indexVersion?: string;
  analyzerProfile?: string;
  templateManaged: boolean;
  autoCreateIndex: boolean;
  activeIndexTarget: string;
  status: string;
}

export interface KeywordIndexTemplateDescriptor {
  engine: string;
  profile: string;
  templateName: string;
  resourcePath: string;
  renderedJson: string;
}

export interface KeywordIndexTemplatesResponse {
  profiles: string[];
  current: KeywordIndexTemplateDescriptor;
}

export interface KeywordReindexJobRequest {
  tenantId?: number;
  sourceIndex?: string;
  targetIndex?: string;
  aliasName?: string;
  templateVersion?: string;
}

export interface RagKeywordReindexJob {
  id: number;
  tenantId: number;
  jobNo: string;
  sourceIndex: string;
  targetIndex: string;
  aliasName: string;
  templateVersion?: string;
  jobStatus: string;
  progress: number;
  totalCount?: number;
  successCount?: number;
  failedCount?: number;
  sampleValidationJson?: string;
  rollbackTarget?: string;
  errorMessage?: string;
  startedAt?: string;
  finishedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface MaterializedMetricBackfillResponse {
  from: string;
  to: string;
  queryHourly: number;
  queryDaily: number;
  feedbackHourly: number;
  feedbackDaily: number;
  rerankHourly: number;
  rerankDaily: number;
}

export interface RagRetrievalEvaluationTrendPoint {
  bucket: string;
  window: string;
  knowledgeBaseId?: number;
  versionTag?: string;
  retrievalMode?: string;
  queryCategory?: string;
  language?: string;
  difficultyLevel?: string;
  topK?: number;
  totalCases: number;
  hitRate: number;
  meanReciprocalRank: number;
  meanRecall: number;
  failureRate: number;
  rerankDropRate: number;
  keywordOnlyHitRate: number;
  vectorOnlyHitRate: number;
}

export interface RagRetrievalEvaluationSliceItem {
  dimension: string;
  value: string;
  totalCases: number;
  hitRate: number;
  meanReciprocalRank: number;
  meanRecall: number;
  failureRate: number;
}

export interface RagRetrievalFailureClusterItem {
  clusterKey: string;
  clusterLabel: string;
  failureType: string;
  queryCategory?: string;
  retrievalMode?: string;
  caseCount: number;
  sampleCaseIds: string[];
  suggestion?: string;
}

export interface RagRerankObservationTrendPoint {
  bucket: string;
  window: string;
  provider?: string;
  model?: string;
  tenantId?: number;
  apiKeyHash?: string;
  errorCode?: string;
  degradedReason?: string;
  requestCount: number;
  successCount: number;
  failureCount: number;
  failureRate: number;
  fallbackCount: number;
  fallbackRate: number;
  timeoutCount: number;
  rateLimitCount: number;
  p50LatencyMs: number;
  p90LatencyMs: number;
  p99LatencyMs: number;
  avgCandidateCount: number;
  avgInputTokens: number;
  totalTokens: number;
  estimatedCost: number;
}

export interface RagRerankObservationDimensionItem {
  dimension: string;
  value: string;
  requestCount: number;
  successCount: number;
  failureCount: number;
  failureRate: number;
  fallbackCount: number;
  fallbackRate: number;
  timeoutCount: number;
  rateLimitCount: number;
  p50LatencyMs: number;
  p90LatencyMs: number;
  p99LatencyMs: number;
  avgCandidateCount: number;
  avgInputTokens: number;
  totalTokens: number;
  estimatedCost: number;
}

export interface RagQueryLog {
  id: number;
  tenantId: number;
  traceId?: string;
  conversationId?: string;
  queryType: 'QUERY' | 'SEARCH' | string;
  queryText: string;
  retrievalMode?: string;
  knowledgeBaseIdsJson?: string;
  topK?: number;
  minScore?: number;
  contentTypesJson?: string;
  permissionTagsJson?: string;
  multimodalTraceJson?: string;
  promptText?: string;
  answerText?: string;
  knowledgeHit: boolean;
  hitCount: number;
  promptTokens?: number;
  completionTokens?: number;
  totalTokens?: number;
  llmProvider?: string;
  llmModel?: string;
  embeddingProvider?: string;
  embeddingModel?: string;
  estimatedInputCost?: number;
  estimatedOutputCost?: number;
  estimatedEmbeddingCost?: number;
  estimatedTotalCost?: number;
  costCurrency?: string;
  latencyMs: number;
  status: 'SUCCESS' | 'FAILED' | string;
  archiveStatus?: string;
  retentionUntil?: string;
  deletedAt?: string;
  deletedBy?: string;
  deleteReason?: string;
  deleted?: boolean;
  errorCode?: string;
  errorMessage?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface RagQueryHit {
  id: number;
  queryLogId: number;
  rankNo: number;
  score?: number;
  knowledgeBaseId?: number;
  documentId?: string;
  documentName?: string;
  chunkId?: string;
  chunkVersion?: number;
  contentType?: string;
  modality?: string;
  retrievalSource?: string;
  imageAssetId?: number;
  fusionScore?: number;
  pageNo?: number;
  sectionTitle?: string;
  imageUrl?: string;
  contentSnippet?: string;
  metadataJson?: string;
  createdAt?: string;
}

export interface RagQueryFeedback {
  id: number;
  queryLogId: number;
  rating: 'HELPFUL' | 'NOT_HELPFUL' | 'CORRECTION' | string;
  createdBy?: string;
  comment?: string;
  correctedAnswer?: string;
  feedbackStatus?: string;
  priority?: string;
  assignee?: string;
  reviewResult?: string;
  reviewComment?: string;
  resolvedAt?: string;
  closedAt?: string;
  reopenedCount?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface RagQueryFeedbackRequest {
  rating: 'HELPFUL' | 'NOT_HELPFUL' | 'CORRECTION' | string;
  createdBy?: string;
  comment?: string;
  correctedAnswer?: string;
}

export interface RagQueryLogDetailResponse {
  log: RagQueryLog;
  hits: RagQueryHit[];
  feedbacks: RagQueryFeedback[];
}

export interface RagFeedbackRatingCount {
  rating: 'HELPFUL' | 'NOT_HELPFUL' | 'CORRECTION' | string;
  count: number;
}

export interface RagFeedbackSummaryItem {
  feedbackId: number;
  queryLogId: number;
  rating: 'HELPFUL' | 'NOT_HELPFUL' | 'CORRECTION' | string;
  createdBy?: string;
  comment?: string;
  correctedAnswer?: string;
  feedbackStatus?: string;
  priority?: string;
  assignee?: string;
  reviewResult?: string;
  feedbackCreatedAt?: string;
  tenantId?: number;
  queryType?: string;
  status?: string;
  traceId?: string;
  conversationId?: string;
  queryText?: string;
  retrievalMode?: string;
  hitCount?: number;
  totalTokens?: number;
  latencyMs?: number;
  queryCreatedAt?: string;
}

export interface RagFeedbackSummaryResponse {
  totalFeedbacks: number;
  helpfulCount: number;
  notHelpfulCount: number;
  correctionCount: number;
  ratingCounts: RagFeedbackRatingCount[];
  recentFeedbacks: RagFeedbackSummaryItem[];
}

export interface RagQueryLogDeleteResponse {
  requested: number;
  deletedLogs: number;
  deletedHits: number;
  deletedFeedbacks: number;
}

export interface RagQueryLogOperationRequest {
  ids?: number[];
  tenantId?: number;
  queryType?: string;
  status?: string;
  conversationId?: string;
  traceId?: string;
  queryText?: string;
  operator?: string;
  reason?: string;
  retentionUntil?: string;
}

export interface RagQueryLogOperationResponse {
  deleteNo: string;
  mode: string;
  matchedCount: number;
  successCount: number;
  failedCount: number;
  queryLogIds: number[];
}

export interface RagQueryLogDeleteAudit {
  id: number;
  deleteNo: string;
  operator?: string;
  deleteMode: string;
  reason?: string;
  queryLogIdsJson?: string;
  matchedCount: number;
  successCount: number;
  failedCount: number;
  filterJson?: string;
  resultJson?: string;
  createdAt?: string;
}

export interface RagQueryRetentionPolicy {
  id: number;
  tenantId: number;
  policyName: string;
  queryType: string;
  statusFilter: string;
  retentionDays: number;
  archiveBeforeDelete: boolean;
  enabled: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface RagQueryRetentionPolicyRequest {
  tenantId?: number;
  policyName: string;
  queryType?: string;
  statusFilter?: string;
  retentionDays?: number;
  archiveBeforeDelete?: boolean;
  enabled?: boolean;
}

export interface RagModelPricing {
  id: number;
  provider: string;
  model: string;
  inputCostPer1kTokens: number;
  outputCostPer1kTokens: number;
  currency: string;
  effectiveFrom?: string;
  effectiveTo?: string;
  enabled: boolean;
}

export interface RagModelPricingRequest {
  provider: string;
  model: string;
  inputCostPer1kTokens?: number;
  outputCostPer1kTokens?: number;
  currency?: string;
  effectiveFrom?: string;
  effectiveTo?: string;
  enabled?: boolean;
}

export type RagModelType = 'LLM' | 'EMBEDDING' | 'IMAGE';

export interface RagTenantModelConfig {
  id?: number;
  provider: string;
  modelType: RagModelType | string;
  modelName: string;
  baseUrl?: string;
  temperature?: number;
  dimension?: number;
  imageSize?: string;
  imageQuality?: string;
  pollIntervalMillis?: number;
  rateLimitQps?: number;
  monthlyBudgetCents?: number;
  apiKeyConfigured?: boolean;
  timeoutSeconds?: number;
  maxRetries?: number;
  maxTokens?: number;
  frequencyPenalty?: number;
  presencePenalty?: number;
  topP?: number;
  enabled?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface ModelConfigUpsertRequest {
  modelType: RagModelType;
  provider?: string;
  modelName: string;
  baseUrl?: string;
  apiKeySecretRef?: string;
  temperature?: number;
  dimension?: number;
  imageSize?: string;
  imageQuality?: string;
  pollIntervalMillis?: number;
  rateLimitQps?: number;
  monthlyBudgetCents?: number;
  enabled?: boolean;
  timeoutSeconds?: number;
  maxRetries?: number;
  maxTokens?: number;
  frequencyPenalty?: number;
  presencePenalty?: number;
  topP?: number;
}

export interface ModelConfigApiKeyResponse {
  id?: number;
  modelType: RagModelType | string;
  enabled?: boolean;
  apiKeyConfigured: boolean;
  apiKeySecretRef?: string;
}

export interface ModelConfigApiKeyUpdateRequest {
  apiKeySecretRef: string;
}

export interface ModelCacheStats {
  llmCacheSize: number;
  streamingLlmCacheSize: number;
  embeddingCacheSize: number;
  llmHits: number;
  llmMisses: number;
  llmCreations: number;
  llmExpirations: number;
  streamingLlmHits: number;
  streamingLlmMisses: number;
  streamingLlmCreations: number;
  streamingLlmExpirations: number;
  embeddingHits: number;
  embeddingMisses: number;
  embeddingCreations: number;
  embeddingExpirations: number;
  llmHitRate?: number;
  streamingLlmHitRate?: number;
  embeddingHitRate?: number;
  totalCacheSize?: number;
}

export interface RagQueryCostTrendPoint {
  bucket: string;
  window: string;
  tenantId?: number;
  queryType?: string;
  retrievalMode?: string;
  llmModel?: string;
  embeddingModel?: string;
  status?: string;
  knowledgeBaseId?: number;
  queryCount: number;
  successCount: number;
  failedCount: number;
  totalPromptTokens: number;
  totalCompletionTokens: number;
  totalTokens: number;
  avgTokensPerQuery: number;
  p50LatencyMs: number;
  p90LatencyMs: number;
  estimatedTotalCost: number;
  avgCostPerQuery: number;
  costPerHelpfulFeedback: number;
  costPerKnowledgeHit: number;
}

export interface RagQueryCostDimensionItem {
  dimension: string;
  value: string;
  queryCount: number;
  successCount: number;
  failedCount: number;
  totalPromptTokens: number;
  totalCompletionTokens: number;
  totalTokens: number;
  avgTokensPerQuery: number;
  p50LatencyMs: number;
  p90LatencyMs: number;
  estimatedTotalCost: number;
  avgCostPerQuery: number;
}

export interface RagQueryCostAnomalyItem {
  anomalyType: string;
  severity: string;
  metricName: string;
  metricValue?: number;
  baselineValue?: number;
  windowStart?: string;
  windowEnd?: string;
  metadata?: string;
}

export interface RagFeedbackAssignRequest {
  assignee: string;
  operator?: string;
  comment?: string;
}

export interface RagFeedbackStatusRequest {
  status: string;
  operator?: string;
  comment?: string;
  linkedRevision?: boolean;
}

export interface RagFeedbackReviewRequest {
  reviewResult: string;
  reviewComment: string;
  operator?: string;
}

export interface RagFeedbackCommentRequest {
  operator?: string;
  comment: string;
}

export interface RagQueryFeedbackEvent {
  id: number;
  feedbackId: number;
  eventType: string;
  fromStatus?: string;
  toStatus?: string;
  operator?: string;
  comment?: string;
  payloadJson?: string;
  createdAt?: string;
}

export interface RagFeedbackRevisionTaskRequest {
  knowledgeBaseId?: number;
  documentId?: number;
  chunkUid?: string;
  revisionType?: string;
  beforeSnapshotJson?: string;
  expectedFix?: string;
  verificationQuery?: string;
  createdBy?: string;
  assignee?: string;
}

export interface RagFeedbackRevisionTaskActionRequest {
  operator?: string;
  comment?: string;
  afterSnapshotJson?: string;
  verificationResultJson?: string;
  verified?: boolean;
}

export interface RagFeedbackRevisionTask {
  id: number;
  revisionNo: string;
  feedbackId: number;
  queryLogId?: number;
  tenantId?: number;
  knowledgeBaseId?: number;
  documentId?: number;
  chunkUid?: string;
  revisionType: string;
  revisionStatus: string;
  beforeSnapshotJson?: string;
  afterSnapshotJson?: string;
  expectedFix?: string;
  verificationQuery?: string;
  verificationResultJson?: string;
  createdBy?: string;
  assignee?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface RagFeedbackQualityTrendPoint {
  bucket: string;
  window: string;
  tenantId?: number;
  knowledgeBaseId?: number;
  retrievalMode?: string;
  queryType?: string;
  feedbackRating?: string;
  feedbackStatus?: string;
  assignee?: string;
  feedbackCount: number;
  feedbackRate: number;
  helpfulRate: number;
  notHelpfulRate: number;
  correctionRate: number;
  correctionAcceptedRate: number;
  avgTimeToFirstReviewHours: number;
  avgTimeToResolveHours: number;
  reopenedCount: number;
  linkedRevisionCount: number;
  verifiedFixRate: number;
}

export interface RagFeedbackQualitySummary {
  queryCount: number;
  feedbackCount: number;
  feedbackRate: number;
  helpfulRate: number;
  notHelpfulRate: number;
  correctionRate: number;
  correctionAcceptedRate: number;
  avgTimeToFirstReviewHours: number;
  avgTimeToResolveHours: number;
  reopenedCount: number;
  linkedRevisionCount: number;
  verifiedFixRate: number;
}

export interface RagFeedbackDimensionItem {
  dimension: string;
  value: string;
  feedbackCount: number;
  helpfulRate: number;
  notHelpfulRate: number;
  correctionRate: number;
  avgTimeToResolveHours: number;
  linkedRevisionCount: number;
  verifiedFixRate: number;
}

export interface RagImageAsset {
  id: number;
  tenantId: number;
  knowledgeBaseId: number;
  documentId?: number;
  documentVersionId?: number;
  sourceDocumentId?: string;
  imageId: string;
  chunkUid?: string;
  contentType: string;
  assetPath?: string;
  imageUrl?: string;
  pageNo?: number;
  coordinateJson?: string;
  sectionTitle?: string;
  imageCaption?: string;
  imageNumber?: string;
  ocrText?: string;
  ocrStatus?: string;
  ocrConfidence?: number;
  ocrProvider?: string;
  ocrModel?: string;
  ocrErrorMessage?: string;
  visualStatus: 'SUCCESS' | 'INVALID' | 'FAILED' | 'EMPTY' | string;
  visualSchemaValid: boolean;
  visualConfidence?: number;
  visualJson?: string;
  visualSchemaErrors?: string;
  textVectorIds?: string;
  imageVectorIds?: string;
  imageEmbeddingStatus?: string;
  imageEmbeddingModel?: string;
  imageEmbeddingDimension?: number;
  imageEmbeddingErrorMessage?: string;
  imageEmbeddingUpdatedAt?: string;
  reviewStatus?: string;
  reviewComment?: string;
  reviewedBy?: string;
  reviewedAt?: string;
  reviewUpdatedVisualJson?: string;
  reviewUpdatedOcrText?: string;
  createdAt?: string;
  updatedAt?: string;
  isDeleted?: number;
}

export interface ImageAssetReviewRequest {
  operator?: string;
  comment?: string;
  updatedVisualJson?: string;
  updatedOcrText?: string;
}

export interface ImageAssetReprocessRequest {
  ocr?: boolean;
  visionAnalysis?: boolean;
  imageEmbedding?: boolean;
  operator?: string;
}

export interface MultimodalCollectionStatus {
  enabled: boolean;
  collection: string;
  textVectorField: string;
  imageVectorField: string;
  textDimension: number;
  imageDimension: number;
  fields: string[];
  status: string;
  message?: string;
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
  activeAlias: string;
  config: VectorStoreConfig;
}

export interface VectorStoreSaveRequest extends VectorStoreConfig {}

export interface RagTextDocumentIngestRequest {
  text: string;
  fileName?: string;
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

export interface RagKnowledgeBase {
  id: number;
  tenantId: number;
  kbCode: string;
  name: string;
  description?: string;
  vectorStoreType: string;
  vectorCollection: string;
  embeddingModel: string;
  embeddingDimension: number;
  chunkStrategy: string;
  chunkSize: number;
  chunkOverlap: number;
  retrievalTopK: number;
  minScore: number;
  configJson?: string;
  status: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface RagKnowledgeBaseCreateRequest {
  tenantId?: number;
  kbCode: string;
  name: string;
  description?: string;
}

export interface CurrentUserResponse {
  tenantId?: number | null;
  operatorTenantId?: number | null;
  tenantExternalId?: string;
  userId: string;
  displayName?: string;
  roles: string[];
  authorizedKnowledgeBaseIds: number[];
  permissionTags: string[];
  platformAdmin: boolean;
  impersonating: boolean;
  requestId?: string;
  sourceIp?: string;
}

export interface SysTenant {
  id: number;
  tenantCode: string;
  tenantName: string;
  externalId?: string;
  tenantExternalId?: string;
  status?: number;
  createdAt?: string;
  updatedAt?: string;
  isDeleted?: number;
}

export interface SysUser {
  id: number;
  tenantId: number;
  externalUserId: string;
  username?: string;
  displayName?: string;
  email?: string;
  passwordUpdatedAt?: string;
  mustChangePassword?: number;
  status?: number;
  createdAt?: string;
  updatedAt?: string;
  isDeleted?: number;
}

export interface SysRole {
  id?: number;
  tenantId: number;
  roleCode: string;
  roleName?: string;
  roleScope?: string;
  createdAt?: string;
  updatedAt?: string;
  isDeleted?: number;
}

export interface SysUserRole {
  id?: number;
  tenantId: number;
  userId: string;
  roleId?: number;
  roleCode: string;
  createdAt?: string;
  isDeleted?: number;
}

export interface LoginRequest {
  tenantId?: number;
  tenantCode?: string;
  tenant?: string;
  account: string;
  password: string;
  loginType?: 'TENANT' | 'SYSTEM';
}

export interface LoginResponse {
  accessToken: string;
  issuedAt: string;
  expiresAt: string;
  currentUser: CurrentUserResponse;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface SystemTenantRequest {
  tenantCode?: string;
  tenantName?: string;
  externalId?: string;
  status?: number;
}

export interface SystemUserRequest {
  tenantId?: number;
  externalUserId?: string;
  username?: string;
  displayName?: string;
  email?: string;
  password?: string;
  status?: number;
  mustChangePassword?: boolean;
}

export interface UserPasswordResetRequest {
  password: string;
  mustChangePassword?: boolean;
}

export interface UserRolesUpdateRequest {
  roleCodes: string[];
}

export interface RagKnowledgeBaseMember {
  id: number;
  tenantId: number;
  knowledgeBaseId: number;
  userId: string;
  memberRole: string;
  permissionTags?: string;
  createdAt?: string;
  updatedAt?: string;
  isDeleted?: number;
}

export interface KnowledgeBaseMemberRequest {
  userId: string;
  role: string;
  permissionTags?: string[];
}

export interface RagTenantQuota {
  id?: number;
  tenantId: number;
  maxDocuments?: number;
  maxStorageBytes?: number;
  maxFileBytes?: number;
  dailyOcrLimit?: number;
  dailyEmbeddingTokens?: number;
  maxConcurrentIngestionTasks?: number;
  dailyQueryLimit?: number;
  monthlyBudgetCents?: number;
  enabled?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface RagTenantUsageDaily {
  id?: number;
  tenantId: number;
  usageDate?: string;
  documentCount?: number;
  storageBytes?: number;
  ocrCount?: number;
  embeddingTokens?: number;
  vectorCount?: number;
  queryCount?: number;
  llmTokens?: number;
  estimatedCostCents?: number;
}

export interface SysOperationAuditLog {
  id: number;
  operatorUserId: string;
  operatorTenantId?: number;
  targetTenantId?: number;
  impersonationReason?: string;
  requestId?: string;
  sourceIp?: string;
  operation: string;
  resourceType: string;
  resourceId?: string;
  result?: string;
  detailJson?: string;
  createdAt?: string;
}

export interface AdminImpersonationRequest {
  targetTenantId: number;
  reason: string;
  ttlMinutes?: number;
}

export interface TenantDataDeletionTask {
  id: number;
  taskNo: string;
  tenantId: number;
  requestedBy: string;
  reason?: string;
  taskStatus: string;
  startedAt?: string;
  finishedAt?: string;
  errorMessage?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface TenantDeletionTaskRequest {
  reason?: string;
  executionMode?: 'DRY_RUN' | 'EXECUTE';
}

export interface TenantDataDeletionStage {
  id?: number;
  taskId: number;
  stageCode: string;
  stageStatus: string;
  deletedCount?: number;
  errorCode?: string;
  errorMessage?: string;
  dryRunResultJson?: string;
  verifyStatus?: string;
  verifyResultJson?: string;
  startedAt?: string;
  finishedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface TenantDeletionTaskDetail {
  task: TenantDataDeletionTask;
  stages: TenantDataDeletionStage[];
}

export interface RagDocument {
  id: number;
  tenantId: number;
  knowledgeBaseId: number;
  documentUid: string;
  documentName: string;
  sourceType: number;
  sourceUri?: string;
  objectKey?: string;
  originalFilename?: string;
  fileExtension?: string;
  mimeType?: string;
  fileSize?: number;
  fileHash?: string;
  currentVersionNo: number;
  pageCount?: number;
  chunkCount: number;
  characterCount: number;
  tokenCount: number;
  parseStatus: number;
  chunkStatus: number;
  embeddingStatus: number;
  documentStatus: number;
  errorCode?: string;
  errorMessage?: string;
  metadataJson?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface RagDocumentVersion {
  id: number;
  tenantId: number;
  knowledgeBaseId: number;
  documentId: number;
  versionNo: number;
  versionUid: string;
  documentName: string;
  sourceType: number;
  sourceUri?: string;
  objectKey?: string;
  originalFilename?: string;
  fileExtension?: string;
  mimeType?: string;
  fileSize?: number;
  fileHash?: string;
  pageCount?: number;
  chunkCount: number;
  characterCount: number;
  tokenCount: number;
  parseStatus: number;
  chunkStatus: number;
  embeddingStatus: number;
  versionStatus: number;
  currentFlag: boolean;
  errorCode?: string;
  errorMessage?: string;
  metadataJson?: string;
  createdAt?: string;
  updatedAt?: string;
  isDeleted?: number;
}

export interface RagIngestionTask {
  id: number;
  tenantId: number;
  knowledgeBaseId: number;
  documentId: number;
  documentVersionId?: number;
  taskNo: string;
  taskType: string;
  taskStatus: number;
  progress: number;
  currentStage?: string;
  stageProgress?: number;
  totalCount: number;
  successCount: number;
  failedCount: number;
  retryCount: number;
  maxRetryCount: number;
  nextRetryAt?: string;
  cancelRequested?: boolean;
  cancelRequestedAt?: string;
  cancelRequestedBy?: string;
  partialSuccess?: boolean;
  lastEventId?: number;
  heartbeatAt?: string;
  errorCode?: string;
  errorMessage?: string;
  traceId?: string;
  idempotencyKey?: string;
  startedAt?: string;
  finishedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface RagIngestionTaskRetryResponse {
  task: RagIngestionTask;
  published: boolean;
}

export interface RagIngestionTaskStage {
  id: number;
  taskId: number;
  stageCode: string;
  stageName: string;
  stageOrder: number;
  stageWeight: number;
  stageStatus: string;
  progress: number;
  totalCount: number;
  successCount: number;
  failedCount: number;
  startedAt?: string;
  finishedAt?: string;
  errorCode?: string;
  errorMessage?: string;
  metadataJson?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface RagIngestionTaskShard {
  id: number;
  taskId: number;
  stageCode?: string;
  documentId?: number;
  documentVersionId?: number;
  shardKey: string;
  shardType: string;
  shardIndex?: number;
  shardStatus: string;
  retryCount: number;
  maxRetryCount: number;
  nextRetryAt?: string;
  errorCode?: string;
  errorMessage?: string;
  inputHash?: string;
  outputRef?: string;
  metadataJson?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface IngestionShardSummary {
  total: number;
  pending: number;
  running: number;
  success: number;
  failedRetryable: number;
  failedFinal: number;
  cancelled: number;
}

export interface IngestionTaskProgressSnapshot {
  taskId: number;
  taskStatus: number;
  progress: number;
  currentStage?: string;
  stageProgress?: number;
  cancelRequested?: boolean;
  lastEventId?: number;
  task: RagIngestionTask;
  stages: RagIngestionTaskStage[];
  shardSummary: IngestionShardSummary;
}

export interface IngestionTaskEventView {
  id: number;
  taskId: number;
  eventType: string;
  stageCode?: string;
  shardKey?: string;
  progress?: number;
  stageProgress?: number;
  message?: string;
  payloadJson?: string;
  createdAt?: string;
}

export interface IngestionShardRetryResponse {
  taskId: number;
  requested: number;
  resetCount: number;
  published: boolean;
}

export interface RagIngestionSubmitResponse {
  knowledgeBaseId: number;
  documentId: number;
  documentVersionId?: number;
  documentVersionNo?: number;
  taskId: number;
  taskNo: string;
  documentUid: string;
  objectKey: string;
  fileHash: string;
  taskStatus: string;
}

export interface KnowledgeChunkRecord {
  chunkId: string;
  documentId?: string;
  source?: string;
  fileName?: string;
  contentType?: string;
  textContent: string;
  textVectorIds?: string[];
  imageVectorIds?: string[];
  imageUrl?: string;
  pageNo?: number;
  sectionTitle?: string;
  imageCaption?: string;
  imageNumber?: string;
  parentChunkId?: string;
  permissionTags?: string[];
  tenantId?: string;
  version: number;
  status: 'ACTIVE' | 'SUPERSEDED' | 'DISABLED' | 'DELETED';
  current: boolean;
  milvusAlias?: string;
  milvusCollection?: string;
  metadataJson?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface ChunkRedisRebuildResponse {
  mysqlRows: number;
  chunkCount: number;
  activeCount: number;
  deletedRedisKeys: number;
}

export interface KnowledgeChunkCreateRequest {
  documentId?: string;
  contentType?: string;
  textContent: string;
  imageUrl?: string;
  pageNo?: number;
  sectionTitle?: string;
  imageCaption?: string;
  imageNumber?: string;
  parentChunkId?: string;
  permissionTags?: string[];
  tenantId?: string;
  metadataJson?: string;
}

export interface KnowledgeChunkUpdateRequest {
  textContent?: string;
  contentType?: string;
  imageUrl?: string;
  pageNo?: number;
  sectionTitle?: string;
  imageCaption?: string;
  imageNumber?: string;
  parentChunkId?: string;
  permissionTags?: string[];
  tenantId?: string;
  metadataJson?: string;
}
