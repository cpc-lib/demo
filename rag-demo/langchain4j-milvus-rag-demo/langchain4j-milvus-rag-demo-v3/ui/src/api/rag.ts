import http from './http';
import { buildMilvusCollectionDetailPath } from '../utils/milvusCollections';
import type {
  AdminImpersonationRequest,
  AgentPromptRollbackRequest,
  AgentPromptUpdateRequest,
  ChatAnswer,
  ChatRequest,
  ChunkRedisRebuildResponse,
  ChangePasswordRequest,
  CurrentUserResponse,
  CurrentVectorStoreResponse,
  ImageAssetReprocessRequest,
  ImageAssetReviewRequest,
  IngestionShardRetryResponse,
  IngestionTaskEventView,
  IngestionTaskProgressSnapshot,
  KnowledgeBaseMemberRequest,
  KnowledgeChunkCreateRequest,
  KnowledgeChunkRecord,
  KnowledgeChunkUpdateRequest,
  KeywordIndexHealthResponse,
  KeywordIndexTemplatesResponse,
  KeywordReindexJobRequest,
  LoginRequest,
  LoginResponse,
  MaterializedMetricBackfillResponse,
  ModelConfigApiKeyResponse,
  ModelConfigApiKeyUpdateRequest,
  ModelCacheStats,
  ModelConfigUpsertRequest,
  MilvusCollectionQueryRequest,
  MilvusCreateCollectionRequest,
  MultimodalCollectionStatus,
  PageResponse,
  RagApiResponse,
  RagAgentPrompt,
  RagDocument,
  RagDocumentVersion,
  RagFeedbackSummaryResponse,
  RagFeedbackAssignRequest,
  RagFeedbackCommentRequest,
  RagFeedbackDimensionItem,
  RagFeedbackQualitySummary,
  RagFeedbackQualityTrendPoint,
  RagFeedbackReviewRequest,
  RagFeedbackRevisionTask,
  RagFeedbackRevisionTaskActionRequest,
  RagFeedbackRevisionTaskRequest,
  RagFeedbackStatusRequest,
  RagImageAsset,
  RagImageSearchRequest,
  RagImageSearchResponse,
  RagIngestionSubmitResponse,
  RagIngestionTask,
  RagIngestionTaskRetryResponse,
  RagIngestionTaskShard,
  RagKnowledgeBase,
  RagKnowledgeBaseMember,
  RagKnowledgeBaseCreateRequest,
  RagKeywordReindexJob,
  RagModelType,
  RagModelPricing,
  RagModelPricingRequest,
  RagQueryCostAnomalyItem,
  RagQueryCostDimensionItem,
  RagQueryCostTrendPoint,
  RagQueryFeedback,
  RagQueryFeedbackEvent,
  RagQueryFeedbackRequest,
  RagQueryLog,
  RagQueryLogDeleteResponse,
  RagQueryLogDeleteAudit,
  RagQueryLogDetailResponse,
  RagQueryLogOperationRequest,
  RagQueryLogOperationResponse,
  RagQueryRequest,
  RagQueryResponse,
  RagQueryRetentionPolicy,
  RagQueryRetentionPolicyRequest,
  RagRerankCallLog,
  RagRerankObservationSummary,
  RagRerankObservationDimensionItem,
  RagRerankObservationTrendPoint,
  RagRetrievalEvalCase,
  RagRetrievalEvalRun,
  RagRetrievalEvaluationCaseUpsertRequest,
  RagRetrievalEvaluationReportItem,
  RagRetrievalEvaluationRequest,
  RagRetrievalEvaluationResponse,
  RagRetrievalEvaluationRunDetailResponse,
  RagRetrievalEvaluationSliceItem,
  RagRetrievalEvaluationTrendPoint,
  RagRetrievalFailureClusterItem,
  RagSearchRequest,
  RagSearchResponse,
  RagTenantQuota,
  RagTenantUsageDaily,
  RagTenantModelConfig,
  RagTextDocumentIngestRequest,
  SysOperationAuditLog,
  SysRole,
  SysTenant,
  SysUser,
  SysUserRole,
  SystemTenantRequest,
  SystemUserRequest,
  TenantDataDeletionTask,
  TenantDeletionTaskDetail,
  TenantDeletionTaskRequest,
  UserPasswordResetRequest,
  UserRolesUpdateRequest,
  VectorStoreConfig,
  VectorStoreSaveRequest
} from '../types';

type PageParams = {
  pageNo?: number;
  pageSize?: number;
  limit?: number;
  sortBy?: string;
  sortDirection?: string;
};

export const ragApi = {
  login(data: LoginRequest) {
    return http.post<RagApiResponse<LoginResponse>>('/api/auth/login', data);
  },
  changePassword(data: ChangePasswordRequest) {
    return http.post<RagApiResponse<{ changed: boolean }>>('/api/auth/change-password', data);
  },
  logout() {
    return http.post<RagApiResponse<{ loggedOut: boolean }>>('/api/auth/logout');
  },
  getCurrentUser() {
    return http.get<RagApiResponse<CurrentUserResponse>>('/api/me');
  },
  listCurrentTenants() {
    return http.get<RagApiResponse<SysTenant[]>>('/api/me/tenants');
  },
  listCurrentKnowledgeBases() {
    return http.get<RagApiResponse<RagKnowledgeBase[]>>('/api/me/knowledge-bases');
  },
  listSystemTenants(params?: { keyword?: string; status?: number; limit?: number }) {
    return http.get<RagApiResponse<SysTenant[]>>('/api/admin/system/tenants', { params });
  },
  createSystemTenant(data: SystemTenantRequest) {
    return http.post<RagApiResponse<SysTenant>>('/api/admin/system/tenants', data);
  },
  updateSystemTenant(tenantId: number, data: SystemTenantRequest) {
    return http.put<RagApiResponse<SysTenant>>(`/api/admin/system/tenants/${tenantId}`, data);
  },
  enableSystemTenant(tenantId: number) {
    return http.put<RagApiResponse<SysTenant>>(`/api/admin/system/tenants/${tenantId}/enable`);
  },
  disableSystemTenant(tenantId: number) {
    return http.put<RagApiResponse<SysTenant>>(`/api/admin/system/tenants/${tenantId}/disable`);
  },
  listSystemUsers(params?: { tenantId?: number; keyword?: string; status?: number; limit?: number }) {
    return http.get<RagApiResponse<SysUser[]>>('/api/admin/system/users', { params });
  },
  createSystemUser(data: SystemUserRequest) {
    return http.post<RagApiResponse<SysUser>>('/api/admin/system/users', data);
  },
  updateSystemUser(id: number, data: SystemUserRequest) {
    return http.put<RagApiResponse<SysUser>>(`/api/admin/system/users/${id}`, data);
  },
  enableSystemUser(id: number) {
    return http.put<RagApiResponse<SysUser>>(`/api/admin/system/users/${id}/enable`);
  },
  disableSystemUser(id: number) {
    return http.put<RagApiResponse<SysUser>>(`/api/admin/system/users/${id}/disable`);
  },
  resetSystemUserPassword(id: number, data: UserPasswordResetRequest) {
    return http.put<RagApiResponse<SysUser>>(`/api/admin/system/users/${id}/reset-password`, data);
  },
  listSystemRoles(params?: { tenantId?: number }) {
    return http.get<RagApiResponse<SysRole[]>>('/api/admin/system/roles', { params });
  },
  listSystemUserRoles(id: number) {
    return http.get<RagApiResponse<SysUserRole[]>>(`/api/admin/system/users/${id}/roles`);
  },
  updateSystemUserRoles(id: number, data: UserRolesUpdateRequest) {
    return http.put<RagApiResponse<SysUserRole[]>>(`/api/admin/system/users/${id}/roles`, data);
  },
  listTenantUsers(params?: { keyword?: string; status?: number; limit?: number }) {
    return http.get<RagApiResponse<SysUser[]>>('/api/admin/tenant/users', { params });
  },
  createTenantUser(data: SystemUserRequest) {
    return http.post<RagApiResponse<SysUser>>('/api/admin/tenant/users', data);
  },
  updateTenantUser(id: number, data: SystemUserRequest) {
    return http.put<RagApiResponse<SysUser>>(`/api/admin/tenant/users/${id}`, data);
  },
  enableTenantUser(id: number) {
    return http.put<RagApiResponse<SysUser>>(`/api/admin/tenant/users/${id}/enable`);
  },
  disableTenantUser(id: number) {
    return http.put<RagApiResponse<SysUser>>(`/api/admin/tenant/users/${id}/disable`);
  },
  resetTenantUserPassword(id: number, data: UserPasswordResetRequest) {
    return http.put<RagApiResponse<SysUser>>(`/api/admin/tenant/users/${id}/reset-password`, data);
  },
  listTenantRoles() {
    return http.get<RagApiResponse<SysRole[]>>('/api/admin/tenant/roles');
  },
  listTenantUserRoles(id: number) {
    return http.get<RagApiResponse<SysUserRole[]>>(`/api/admin/tenant/users/${id}/roles`);
  },
  updateTenantUserRoles(id: number, data: UserRolesUpdateRequest) {
    return http.put<RagApiResponse<SysUserRole[]>>(`/api/admin/tenant/users/${id}/roles`, data);
  },
  chat(data: ChatRequest) {
    return http.post<RagApiResponse<string>>('/api/chat', data);
  },
  chatDetail(data: ChatRequest) {
    return http.post<RagApiResponse<ChatAnswer>>('/api/chat/detail', data);
  },
  ragQuery(data: RagQueryRequest) {
    return http.post<RagApiResponse<RagQueryResponse>>('/api/rag/query', data);
  },
  ragSearch(data: RagSearchRequest) {
    return http.post<RagApiResponse<RagSearchResponse>>('/api/rag/search', data);
  },
  listModelConfigs() {
    return http.get<RagApiResponse<RagTenantModelConfig[]>>('/api/admin/model-configs');
  },
  getModelConfig(id: number) {
    return http.get<RagApiResponse<RagTenantModelConfig>>(`/api/admin/model-configs/${id}`);
  },
  getActiveLlmConfig() {
    return http.get<RagApiResponse<RagTenantModelConfig>>('/api/admin/model-configs/llm');
  },
  getActiveEmbeddingConfig() {
    return http.get<RagApiResponse<RagTenantModelConfig>>('/api/admin/model-configs/embedding');
  },
  getActiveImageConfig() {
    return http.get<RagApiResponse<RagTenantModelConfig>>('/api/admin/model-configs/image');
  },
  upsertModelConfig(data: ModelConfigUpsertRequest) {
    return http.post<RagApiResponse<RagTenantModelConfig>>('/api/admin/model-configs', data);
  },
  updateModelConfig(id: number, data: ModelConfigUpsertRequest) {
    return http.put<RagApiResponse<RagTenantModelConfig>>(`/api/admin/model-configs/${id}`, data);
  },
  getModelConfigApiKey(id: number) {
    return http.get<RagApiResponse<ModelConfigApiKeyResponse>>(`/api/admin/model-configs/${id}/api-key`);
  },
  updateModelConfigApiKey(id: number, data: ModelConfigApiKeyUpdateRequest) {
    return http.put<RagApiResponse<RagTenantModelConfig>>(`/api/admin/model-configs/${id}/api-key`, data);
  },
  getActiveModelConfigApiKey(modelType: RagModelType) {
    return http.get<RagApiResponse<ModelConfigApiKeyResponse>>(`/api/admin/model-configs/active/${modelType}/api-key`);
  },
  updateActiveModelConfigApiKey(modelType: RagModelType, data: ModelConfigApiKeyUpdateRequest) {
    return http.put<RagApiResponse<RagTenantModelConfig>>(`/api/admin/model-configs/active/${modelType}/api-key`, data);
  },
  getActiveAgentPrompt() {
    return http.get<RagApiResponse<RagAgentPrompt | null>>('/api/admin/agent-prompts');
  },
  listAgentPrompts() {
    return http.get<RagApiResponse<RagAgentPrompt[]>>('/api/admin/agent-prompts/all');
  },
  createAgentPrompt(data: AgentPromptUpdateRequest) {
    return http.post<RagApiResponse<RagAgentPrompt>>('/api/admin/agent-prompts', data);
  },
  updateAgentPrompt(data: AgentPromptUpdateRequest) {
    return http.put<RagApiResponse<RagAgentPrompt>>('/api/admin/agent-prompts', data);
  },
  updateAgentPromptById(id: number, data: AgentPromptUpdateRequest) {
    return http.put<RagApiResponse<RagAgentPrompt>>(`/api/admin/agent-prompts/${id}`, data);
  },
  enableAgentPrompt(id: number) {
    return http.put<RagApiResponse<RagAgentPrompt>>(`/api/admin/agent-prompts/${id}/enable`);
  },
  disableAgentPrompt(id: number) {
    return http.put<RagApiResponse<RagAgentPrompt>>(`/api/admin/agent-prompts/${id}/disable`);
  },
  listAgentPromptVersions() {
    return http.get<RagApiResponse<RagAgentPrompt[]>>('/api/admin/agent-prompts/versions');
  },
  rollbackAgentPrompt(data: AgentPromptRollbackRequest) {
    return http.post<RagApiResponse<RagAgentPrompt>>('/api/admin/agent-prompts/rollback', data);
  },
  enableModelConfig(id: number) {
    return http.put<RagApiResponse<RagTenantModelConfig>>(`/api/admin/model-configs/${id}/enable`);
  },
  disableModelConfig(id: number) {
    return http.put<RagApiResponse<RagTenantModelConfig>>(`/api/admin/model-configs/${id}/disable`);
  },
  deleteModelConfig(id: number) {
    return http.delete<RagApiResponse<null>>(`/api/admin/model-configs/${id}`);
  },
  reloadModelConfigs() {
    return http.post<RagApiResponse<null>>('/api/admin/model-configs/reload');
  },
  invalidateTenantModels() {
    return http.post<RagApiResponse<null>>('/api/admin/model-configs/invalidate-models');
  },
  invalidateAllModels() {
    return http.post<RagApiResponse<null>>('/api/admin/model-configs/invalidate-models/all');
  },
  getModelCacheStats() {
    return http.get<RagApiResponse<ModelCacheStats>>('/api/admin/model-configs/cache-stats');
  },
  resetModelCacheStats() {
    return http.post<RagApiResponse<ModelCacheStats>>('/api/admin/model-configs/cache-stats/reset');
  },
  imageSearchByReference(data: RagImageSearchRequest) {
    return http.post<RagApiResponse<RagImageSearchResponse>>('/api/rag/image-search', data);
  },
  imageSearchByFile(
    file: File,
    data: Omit<RagImageSearchRequest, 'imageBase64' | 'imageUrl' | 'imageAssetId'>
  ) {
    const formData = new FormData();
    formData.append('file', file);
    data.knowledgeBaseIds.forEach((id) => formData.append('knowledgeBaseIds', String(id)));
    appendOptional(formData, 'question', data.question);
    appendOptional(formData, 'retrievalMode', data.retrievalMode);
    appendOptional(formData, 'topK', data.topK);
    appendOptional(formData, 'minScore', data.minScore);
    appendOptional(formData, 'includeReviewPending', data.includeReviewPending);
    data.contentTypes?.forEach((value) => formData.append('contentTypes', value));
    data.permissionTags?.forEach((value) => formData.append('permissionTags', value));
    return http.post<RagApiResponse<RagImageSearchResponse>>('/api/rag/image-search', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  },
  runRetrievalEvaluation(data?: RagRetrievalEvaluationRequest) {
    return http.post<RagApiResponse<RagRetrievalEvaluationResponse>>('/api/rag/retrieval-evaluations/run', data || {});
  },
  listRetrievalEvaluationCases(params?: PageParams & {
    tenantId?: number;
    knowledgeBaseId?: number;
    versionTag?: string;
    enabled?: boolean;
  }) {
    return http.get<RagApiResponse<PageResponse<RagRetrievalEvalCase>>>('/api/rag/retrieval-evaluations/cases', {
      params
    });
  },
  saveRetrievalEvaluationCase(data: RagRetrievalEvaluationCaseUpsertRequest) {
    return http.post<RagApiResponse<RagRetrievalEvalCase>>('/api/rag/retrieval-evaluations/cases', data);
  },
  deleteRetrievalEvaluationCase(caseId: number) {
    return http.delete<RagApiResponse<{ deleted: boolean }>>(`/api/rag/retrieval-evaluations/cases/${caseId}`);
  },
  listRetrievalEvaluationRuns(params?: PageParams & {
    tenantId?: number;
    knowledgeBaseId?: number;
    versionTag?: string;
    retrievalMode?: string;
  }) {
    return http.get<RagApiResponse<PageResponse<RagRetrievalEvalRun>>>('/api/rag/retrieval-evaluations/runs', {
      params
    });
  },
  getRetrievalEvaluationRun(runId: number) {
    return http.get<RagApiResponse<RagRetrievalEvaluationRunDetailResponse>>(
      `/api/rag/retrieval-evaluations/runs/${runId}`
    );
  },
  getRetrievalEvaluationReport(params?: {
    tenantId?: number;
    knowledgeBaseId?: number;
    retrievalMode?: string;
  }) {
    return http.get<RagApiResponse<RagRetrievalEvaluationReportItem[]>>('/api/rag/retrieval-evaluations/report', {
      params
    });
  },
  getKeywordIndexHealth() {
    return http.get<RagApiResponse<KeywordIndexHealthResponse>>('/api/rag/keyword-index/health');
  },
  getKeywordIndexTemplates() {
    return http.get<RagApiResponse<KeywordIndexTemplatesResponse>>('/api/rag/keyword-index/templates');
  },
  createKeywordReindexJob(data?: KeywordReindexJobRequest) {
    return http.post<RagApiResponse<RagKeywordReindexJob>>('/api/rag/keyword-index/reindex-jobs', data || {});
  },
  listKeywordReindexJobs(params?: { tenantId?: number; limit?: number }) {
    return http.get<RagApiResponse<RagKeywordReindexJob[]>>('/api/rag/keyword-index/reindex-jobs', { params });
  },
  getKeywordReindexJob(jobId: number) {
    return http.get<RagApiResponse<RagKeywordReindexJob>>(`/api/rag/keyword-index/reindex-jobs/${jobId}`);
  },
  previewKeywordReindexJob(jobId: number) {
    return http.get<RagApiResponse<unknown>>(`/api/rag/keyword-index/reindex-jobs/${jobId}/preview`);
  },
  runKeywordReindexJob(jobId: number) {
    return http.post<RagApiResponse<unknown>>(`/api/rag/keyword-index/reindex-jobs/${jobId}/run`);
  },
  switchKeywordReindexAlias(jobId: number) {
    return http.post<RagApiResponse<unknown>>(`/api/rag/keyword-index/reindex-jobs/${jobId}/switch-alias`);
  },
  rollbackKeywordReindexJob(jobId: number) {
    return http.post<RagApiResponse<unknown>>(`/api/rag/keyword-index/reindex-jobs/${jobId}/rollback`);
  },
  cancelKeywordReindexJob(jobId: number) {
    return http.post<RagApiResponse<unknown>>(`/api/rag/keyword-index/reindex-jobs/${jobId}/cancel`);
  },
  backfillMaterializedMetrics(params?: { from?: string; to?: string }) {
    return http.post<RagApiResponse<MaterializedMetricBackfillResponse>>('/api/rag/metrics/materialized/backfill', null, {
      params
    });
  },
  getMaterializedMetricWatermarks() {
    return http.get<RagApiResponse<unknown[]>>('/api/rag/metrics/materialized/watermarks');
  },
  getRetrievalEvaluationTrends(params?: {
    tenantId?: number;
    knowledgeBaseId?: number;
    versionTag?: string;
    retrievalMode?: string;
    queryCategory?: string;
    language?: string;
    difficultyLevel?: string;
    window?: string;
    from?: string;
    to?: string;
  }) {
    return http.get<RagApiResponse<RagRetrievalEvaluationTrendPoint[]>>('/api/rag/retrieval-evaluations/trends', {
      params
    });
  },
  getRetrievalEvaluationSlices(params?: {
    tenantId?: number;
    knowledgeBaseId?: number;
    versionTag?: string;
    retrievalMode?: string;
    dimension?: string;
    from?: string;
    to?: string;
  }) {
    return http.get<RagApiResponse<RagRetrievalEvaluationSliceItem[]>>('/api/rag/retrieval-evaluations/slices', {
      params
    });
  },
  getRetrievalFailureClusters(params?: {
    runId?: number;
    tenantId?: number;
    knowledgeBaseId?: number;
    retrievalMode?: string;
  }) {
    return http.get<RagApiResponse<RagRetrievalFailureClusterItem[]>>(
      '/api/rag/retrieval-evaluations/failure-clusters',
      { params }
    );
  },
  getRerankObservationSummary(params?: {
    tenantId?: number;
    provider?: string;
    model?: string;
  }) {
    return http.get<RagApiResponse<RagRerankObservationSummary>>('/api/rag/rerank-observability/summary', { params });
  },
  getRerankObservationTrends(params?: {
    tenantId?: number;
    provider?: string;
    model?: string;
    apiKeyHash?: string;
    errorCode?: string;
    degradedReason?: string;
    window?: string;
    from?: string;
    to?: string;
  }) {
    return http.get<RagApiResponse<RagRerankObservationTrendPoint[]>>('/api/rag/rerank-observability/trends', {
      params
    });
  },
  getRerankByApiKey(params?: {
    tenantId?: number;
    provider?: string;
    model?: string;
    from?: string;
    to?: string;
  }) {
    return http.get<RagApiResponse<RagRerankObservationDimensionItem[]>>('/api/rag/rerank-observability/by-api-key', {
      params
    });
  },
  getRerankByErrorCode(params?: {
    tenantId?: number;
    provider?: string;
    model?: string;
    from?: string;
    to?: string;
  }) {
    return http.get<RagApiResponse<RagRerankObservationDimensionItem[]>>('/api/rag/rerank-observability/by-error-code', {
      params
    });
  },
  listRerankObservationLogs(params?: PageParams & {
    tenantId?: number;
    provider?: string;
    model?: string;
    success?: boolean;
    fallback?: boolean;
  }) {
    return http.get<RagApiResponse<PageResponse<RagRerankCallLog>>>('/api/rag/rerank-observability/logs', {
      params
    });
  },
  listQueryLogs(params?: PageParams & {
    tenantId?: number;
    queryType?: string;
    status?: string;
    conversationId?: string;
    traceId?: string;
    queryText?: string;
    visibility?: string;
  }) {
    return http.get<RagApiResponse<PageResponse<RagQueryLog>>>('/api/rag/query-logs', { params });
  },
  getQueryLog(queryLogId: number) {
    return http.get<RagApiResponse<RagQueryLogDetailResponse>>(`/api/rag/query-logs/${queryLogId}`);
  },
  submitQueryFeedback(queryLogId: number, data: RagQueryFeedbackRequest) {
    return http.post<RagApiResponse<RagQueryFeedback>>(`/api/rag/query-logs/${queryLogId}/feedback`, data);
  },
  getQueryLogFeedbackSummary(params?: {
    tenantId?: number;
    queryType?: string;
    status?: string;
    conversationId?: string;
    traceId?: string;
    rating?: string;
    createdBy?: string;
    limit?: number;
  }) {
    return http.get<RagApiResponse<RagFeedbackSummaryResponse>>('/api/rag/query-logs/feedback-summary', { params });
  },
  deleteQueryLog(queryLogId: number) {
    return http.delete<RagApiResponse<RagQueryLogDeleteResponse>>(`/api/rag/query-logs/${queryLogId}`);
  },
  deleteQueryLogs(ids: number[]) {
    return http.delete<RagApiResponse<RagQueryLogDeleteResponse>>('/api/rag/query-logs', {
      params: { ids: ids.join(',') }
    });
  },
  softDeleteQueryLogs(data: RagQueryLogOperationRequest) {
    return http.post<RagApiResponse<RagQueryLogOperationResponse>>('/api/rag/query-logs/delete', data);
  },
  archiveQueryLogs(data: RagQueryLogOperationRequest) {
    return http.post<RagApiResponse<RagQueryLogOperationResponse>>('/api/rag/query-logs/archive', data);
  },
  restoreQueryLogs(data: RagQueryLogOperationRequest) {
    return http.post<RagApiResponse<RagQueryLogOperationResponse>>('/api/rag/query-logs/restore', data);
  },
  purgeQueryLogs(data: RagQueryLogOperationRequest) {
    return http.post<RagApiResponse<RagQueryLogOperationResponse>>('/api/rag/query-logs/purge', data);
  },
  listQueryLogDeleteAudits(params?: PageParams & {
    deleteNo?: string;
    mode?: string;
    operator?: string;
  }) {
    return http.get<RagApiResponse<PageResponse<RagQueryLogDeleteAudit>>>('/api/rag/query-logs/delete-audits', {
      params
    });
  },
  getQueryLogDeleteAudit(deleteNo: string) {
    return http.get<RagApiResponse<RagQueryLogDeleteAudit>>(`/api/rag/query-logs/delete-audits/${deleteNo}`);
  },
  listQueryRetentionPolicies(params?: PageParams & {
    tenantId?: number;
    enabled?: boolean;
  }) {
    return http.get<RagApiResponse<PageResponse<RagQueryRetentionPolicy>>>('/api/rag/query-retention-policies', {
      params
    });
  },
  createQueryRetentionPolicy(data: RagQueryRetentionPolicyRequest) {
    return http.post<RagApiResponse<RagQueryRetentionPolicy>>('/api/rag/query-retention-policies', data);
  },
  updateQueryRetentionPolicy(id: number, data: RagQueryRetentionPolicyRequest) {
    return http.put<RagApiResponse<RagQueryRetentionPolicy>>(`/api/rag/query-retention-policies/${id}`, data);
  },
  getQueryCostTrends(params?: {
    tenantId?: number;
    queryType?: string;
    retrievalMode?: string;
    status?: string;
    llmModel?: string;
    embeddingModel?: string;
    knowledgeBaseId?: number;
    window?: string;
    from?: string;
    to?: string;
  }) {
    return http.get<RagApiResponse<RagQueryCostTrendPoint[]>>('/api/rag/query-costs/trends', { params });
  },
  getQueryCostsByModel(params?: {
    tenantId?: number;
    queryType?: string;
    retrievalMode?: string;
    status?: string;
    from?: string;
    to?: string;
  }) {
    return http.get<RagApiResponse<RagQueryCostDimensionItem[]>>('/api/rag/query-costs/by-model', { params });
  },
  getQueryCostsByTenant(params?: {
    queryType?: string;
    retrievalMode?: string;
    status?: string;
    from?: string;
    to?: string;
  }) {
    return http.get<RagApiResponse<RagQueryCostDimensionItem[]>>('/api/rag/query-costs/by-tenant', { params });
  },
  getQueryCostAnomalies(params?: {
    tenantId?: number;
    from?: string;
    to?: string;
    tokenThreshold?: number;
  }) {
    return http.get<RagApiResponse<RagQueryCostAnomalyItem[]>>('/api/rag/query-costs/anomalies', { params });
  },
  getQueryCostExportUrl(params?: {
    tenantId?: number;
    queryType?: string;
    retrievalMode?: string;
    status?: string;
    window?: string;
    from?: string;
    to?: string;
  }) {
    return exportUrl('/api/rag/query-costs/export', params);
  },
  listModelPricing(params?: PageParams & {
    provider?: string;
    model?: string;
    enabled?: boolean;
  }) {
    return http.get<RagApiResponse<PageResponse<RagModelPricing>>>('/api/rag/model-pricing', { params });
  },
  createModelPricing(data: RagModelPricingRequest) {
    return http.post<RagApiResponse<RagModelPricing>>('/api/rag/model-pricing', data);
  },
  updateModelPricing(id: number, data: RagModelPricingRequest) {
    return http.put<RagApiResponse<RagModelPricing>>(`/api/rag/model-pricing/${id}`, data);
  },
  listQueryFeedback(params?: PageParams & {
    queryLogId?: number;
    tenantId?: number;
    rating?: string;
    feedbackStatus?: string;
    priority?: string;
    assignee?: string;
    sortBy?: string;
    sortDirection?: string;
  }) {
    return http.get<RagApiResponse<PageResponse<RagQueryFeedback>>>('/api/rag/query-feedback', { params });
  },
  getQueryFeedback(id: number) {
    return http.get<RagApiResponse<RagQueryFeedback>>(`/api/rag/query-feedback/${id}`);
  },
  assignQueryFeedback(id: number, data: RagFeedbackAssignRequest) {
    return http.post<RagApiResponse<RagQueryFeedback>>(`/api/rag/query-feedback/${id}/assign`, data);
  },
  changeQueryFeedbackStatus(id: number, data: RagFeedbackStatusRequest) {
    return http.post<RagApiResponse<RagQueryFeedback>>(`/api/rag/query-feedback/${id}/status`, data);
  },
  reviewQueryFeedback(id: number, data: RagFeedbackReviewRequest) {
    return http.post<RagApiResponse<RagQueryFeedback>>(`/api/rag/query-feedback/${id}/review`, data);
  },
  commentQueryFeedback(id: number, data: RagFeedbackCommentRequest) {
    return http.post<RagApiResponse<RagQueryFeedback>>(`/api/rag/query-feedback/${id}/comment`, data);
  },
  closeQueryFeedback(id: number, data?: RagFeedbackCommentRequest) {
    return http.post<RagApiResponse<RagQueryFeedback>>(`/api/rag/query-feedback/${id}/close`, data || {});
  },
  reopenQueryFeedback(id: number, data?: RagFeedbackCommentRequest) {
    return http.post<RagApiResponse<RagQueryFeedback>>(`/api/rag/query-feedback/${id}/reopen`, data || {});
  },
  listQueryFeedbackEvents(id: number) {
    return http.get<RagApiResponse<RagQueryFeedbackEvent[]>>(`/api/rag/query-feedback/${id}/events`);
  },
  getFeedbackTrends(params?: {
    tenantId?: number;
    knowledgeBaseId?: number;
    retrievalMode?: string;
    queryType?: string;
    feedbackRating?: string;
    feedbackStatus?: string;
    assignee?: string;
    window?: string;
    from?: string;
    to?: string;
  }) {
    return http.get<RagApiResponse<RagFeedbackQualityTrendPoint[]>>('/api/rag/query-feedback/trends', { params });
  },
  getFeedbackQualitySummary(params?: {
    tenantId?: number;
    knowledgeBaseId?: number;
    retrievalMode?: string;
    queryType?: string;
    feedbackRating?: string;
    feedbackStatus?: string;
    assignee?: string;
    from?: string;
    to?: string;
  }) {
    return http.get<RagApiResponse<RagFeedbackQualitySummary>>('/api/rag/query-feedback/quality-summary', { params });
  },
  getFeedbackByKnowledgeBase(params?: { tenantId?: number; from?: string; to?: string }) {
    return http.get<RagApiResponse<RagFeedbackDimensionItem[]>>('/api/rag/query-feedback/by-knowledge-base', { params });
  },
  getFeedbackByAssignee(params?: { tenantId?: number; from?: string; to?: string }) {
    return http.get<RagApiResponse<RagFeedbackDimensionItem[]>>('/api/rag/query-feedback/by-assignee', { params });
  },
  getFeedbackExportUrl(params?: {
    tenantId?: number;
    knowledgeBaseId?: number;
    retrievalMode?: string;
    queryType?: string;
    window?: string;
    from?: string;
    to?: string;
  }) {
    return exportUrl('/api/rag/query-feedback/export', params);
  },
  createFeedbackRevisionTask(feedbackId: number, data?: RagFeedbackRevisionTaskRequest) {
    return http.post<RagApiResponse<RagFeedbackRevisionTask>>(
      `/api/rag/query-feedback/${feedbackId}/revision-tasks`,
      data || {}
    );
  },
  listFeedbackRevisionTasks(params?: PageParams & {
    feedbackId?: number;
    tenantId?: number;
    knowledgeBaseId?: number;
    revisionStatus?: string;
    assignee?: string;
  }) {
    return http.get<RagApiResponse<PageResponse<RagFeedbackRevisionTask>>>('/api/rag/feedback-revision-tasks', {
      params
    });
  },
  getFeedbackRevisionTask(id: number) {
    return http.get<RagApiResponse<RagFeedbackRevisionTask>>(`/api/rag/feedback-revision-tasks/${id}`);
  },
  applyFeedbackRevisionTask(id: number, data?: RagFeedbackRevisionTaskActionRequest) {
    return http.post<RagApiResponse<RagFeedbackRevisionTask>>(
      `/api/rag/feedback-revision-tasks/${id}/apply`,
      data || {}
    );
  },
  verifyFeedbackRevisionTask(id: number, data?: RagFeedbackRevisionTaskActionRequest) {
    return http.post<RagApiResponse<RagFeedbackRevisionTask>>(
      `/api/rag/feedback-revision-tasks/${id}/verify`,
      data || {}
    );
  },
  rejectFeedbackRevisionTask(id: number, data?: RagFeedbackRevisionTaskActionRequest) {
    return http.post<RagApiResponse<RagFeedbackRevisionTask>>(
      `/api/rag/feedback-revision-tasks/${id}/reject`,
      data || {}
    );
  },
  cancelFeedbackRevisionTask(id: number, data?: RagFeedbackRevisionTaskActionRequest) {
    return http.post<RagApiResponse<RagFeedbackRevisionTask>>(
      `/api/rag/feedback-revision-tasks/${id}/cancel`,
      data || {}
    );
  },
  getQueryLogExportUrl(params?: {
    tenantId?: number;
    queryType?: string;
    status?: string;
    conversationId?: string;
    traceId?: string;
    queryText?: string;
    visibility?: string;
    limit?: number;
  }) {
    return exportUrl('/api/rag/query-logs/export', params);
  },
  listImageAssets(params?: PageParams & {
    tenantId?: number;
    knowledgeBaseId?: number;
    sourceDocumentId?: string;
    contentType?: string;
    visualStatus?: string;
    reviewStatus?: string;
    ocrStatus?: string;
    imageEmbeddingStatus?: string;
    minConfidence?: number;
  }) {
    return http.get<RagApiResponse<PageResponse<RagImageAsset>>>('/api/rag/image-assets', { params });
  },
  listReviewPendingImageAssets(params?: PageParams & {
    tenantId?: number;
    knowledgeBaseId?: number;
  }) {
    return http.get<RagApiResponse<PageResponse<RagImageAsset>>>('/api/rag/image-assets/review-pending', { params });
  },
  getImageAsset(id: number) {
    return http.get<RagApiResponse<RagImageAsset>>(`/api/rag/image-assets/${id}`);
  },
  approveImageAsset(id: number, data?: ImageAssetReviewRequest) {
    return http.post<RagApiResponse<RagImageAsset>>(`/api/rag/image-assets/${id}/review/approve`, data || {});
  },
  rejectImageAsset(id: number, data?: ImageAssetReviewRequest) {
    return http.post<RagApiResponse<RagImageAsset>>(`/api/rag/image-assets/${id}/review/reject`, data || {});
  },
  updateImageAssetReview(id: number, data?: ImageAssetReviewRequest) {
    return http.post<RagApiResponse<RagImageAsset>>(`/api/rag/image-assets/${id}/review/update`, data || {});
  },
  reprocessImageAsset(id: number, data?: ImageAssetReprocessRequest) {
    return http.post<RagApiResponse<RagImageAsset>>(`/api/rag/image-assets/${id}/reprocess`, data || {});
  },
  ensureMultimodalCollection() {
    return http.post<RagApiResponse<MultimodalCollectionStatus>>('/api/rag/multimodal/collections/ensure');
  },
  getCurrentVectorStore() {
    return http.get<RagApiResponse<CurrentVectorStoreResponse>>('/api/vector-stores/current');
  },
  listVectorStores(params?: PageParams) {
    return http.get<RagApiResponse<PageResponse<VectorStoreConfig>>>('/api/vector-stores', { params });
  },
  saveVectorStore(data: VectorStoreSaveRequest) {
    return http.post<RagApiResponse<{ alias: string }>>('/api/vector-stores', data);
  },
  switchVectorStore(alias: string) {
    return http.post<RagApiResponse<{ activeAlias: string }>>('/api/vector-stores/switch', null, { params: { alias } });
  },
  listCollections(databaseName?: string, params?: PageParams) {
    return http.get<RagApiResponse<PageResponse<unknown>>>('/api/milvus/collections', {
      params: { ...params, databaseName: databaseName || undefined }
    });
  },
  describeCollection(collectionName: string, databaseName?: string) {
    return http.get<RagApiResponse<unknown>>(buildMilvusCollectionDetailPath(collectionName), {
      params: { databaseName: databaseName || undefined }
    });
  },
  queryCollection(data: MilvusCollectionQueryRequest) {
    return http.post<RagApiResponse<unknown[]>>('/api/milvus/collections/query', data);
  },
  createCollection(data: MilvusCreateCollectionRequest) {
    return http.post<RagApiResponse<unknown>>('/api/milvus/collections', data);
  },
  listKnowledgeBases(tenantId?: number, params?: PageParams) {
    return http.get<RagApiResponse<PageResponse<RagKnowledgeBase>>>('/api/rag/knowledge-bases', {
      params: { ...params, tenantId }
    });
  },
  getKnowledgeBase(id: number) {
    return http.get<RagApiResponse<RagKnowledgeBase>>(`/api/rag/knowledge-bases/${id}`);
  },
  createKnowledgeBase(data: RagKnowledgeBaseCreateRequest) {
    return http.post<RagApiResponse<RagKnowledgeBase>>('/api/rag/knowledge-bases', data);
  },
  listKnowledgeBaseMembers(knowledgeBaseId: number) {
    return http.get<RagApiResponse<RagKnowledgeBaseMember[]>>(`/api/rag/knowledge-bases/${knowledgeBaseId}/members`);
  },
  upsertKnowledgeBaseMember(knowledgeBaseId: number, data: KnowledgeBaseMemberRequest) {
    return http.post<RagApiResponse<RagKnowledgeBaseMember>>(`/api/rag/knowledge-bases/${knowledgeBaseId}/members`, data);
  },
  removeKnowledgeBaseMember(knowledgeBaseId: number, userId: string) {
    return http.delete<RagApiResponse<{ removed: boolean }>>(
      `/api/rag/knowledge-bases/${knowledgeBaseId}/members/${encodeURIComponent(userId)}`
    );
  },
  startImpersonation(data: AdminImpersonationRequest) {
    return http.post<RagApiResponse<unknown>>('/api/admin/impersonations', data);
  },
  revokeCurrentImpersonation() {
    return http.delete<RagApiResponse<{ revoked: boolean }>>('/api/admin/impersonations/current');
  },
  listAuditLogs(params?: { operation?: string; resourceType?: string; targetTenantId?: number; limit?: number }) {
    return http.get<RagApiResponse<SysOperationAuditLog[]>>('/api/admin/audit-logs', { params });
  },
  getTenantQuota(tenantId: number) {
    return http.get<RagApiResponse<RagTenantQuota>>(`/api/admin/tenants/${tenantId}/quota`);
  },
  updateTenantQuota(tenantId: number, data: RagTenantQuota) {
    return http.put<RagApiResponse<RagTenantQuota>>(`/api/admin/tenants/${tenantId}/quota`, data);
  },
  getTenantUsage(tenantId: number, date?: string) {
    return http.get<RagApiResponse<RagTenantUsageDaily>>(`/api/admin/tenants/${tenantId}/usage`, {
      params: { date }
    });
  },
  createTenantDeletionTask(tenantId: number, data?: TenantDeletionTaskRequest) {
    return http.post<RagApiResponse<TenantDataDeletionTask>>(`/api/admin/tenants/${tenantId}/deletion-tasks`, data || {});
  },
  getTenantDeletionTask(taskId: number) {
    return http.get<RagApiResponse<TenantDeletionTaskDetail>>(`/api/admin/tenant-deletion-tasks/${taskId}`);
  },
  runTenantDeletionTask(taskId: number, executionMode?: 'DRY_RUN' | 'EXECUTE') {
    return http.post<RagApiResponse<TenantDeletionTaskDetail>>(`/api/admin/tenant-deletion-tasks/${taskId}/run`, null, {
      params: { executionMode }
    });
  },
  cancelTenantDeletionTask(taskId: number) {
    return http.post<RagApiResponse<TenantDeletionTaskDetail>>(`/api/admin/tenant-deletion-tasks/${taskId}/cancel`);
  },
  retryTenantDeletionStage(taskId: number, stageCode: string, executionMode?: 'DRY_RUN' | 'EXECUTE') {
    return http.post<RagApiResponse<TenantDeletionTaskDetail>>(
      `/api/admin/tenant-deletion-tasks/${taskId}/stages/${encodeURIComponent(stageCode)}/retry`,
      null,
      { params: { executionMode } }
    );
  },
  ingestRagDocument(file: File, knowledgeBaseId?: number) {
    const formData = new FormData();
    formData.append('file', file);
    const url = knowledgeBaseId
      ? `/api/rag/knowledge-bases/${knowledgeBaseId}/documents/ingest`
      : '/api/rag/documents/ingest';
    return http.post<RagApiResponse<RagIngestionSubmitResponse>>(url, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  },
  ingestRagTextDocument(data: RagTextDocumentIngestRequest, knowledgeBaseId?: number) {
    const url = knowledgeBaseId
      ? `/api/rag/knowledge-bases/${knowledgeBaseId}/documents/ingest`
      : '/api/rag/documents/ingest';
    return http.post<RagApiResponse<RagIngestionSubmitResponse>>(url, data);
  },
  listRagDocuments(knowledgeBaseId: number, status?: number, params?: PageParams) {
    return http.get<RagApiResponse<PageResponse<RagDocument>>>(
      `/api/rag/knowledge-bases/${knowledgeBaseId}/documents`,
      { params: { ...params, status } }
    );
  },
  getRagDocument(documentId: number) {
    return http.get<RagApiResponse<RagDocument>>(`/api/rag/documents/${documentId}`);
  },
  getRagDocumentDownloadUrl(documentId: number, versionNo?: number) {
    const params = new URLSearchParams();
    if (versionNo !== undefined) params.set('versionNo', String(versionNo));
    const query = params.toString();
    return `/api/rag/documents/${documentId}/download${query ? `?${query}` : ''}`;
  },
  listRagDocumentVersions(documentId: number, params?: PageParams) {
    return http.get<RagApiResponse<PageResponse<RagDocumentVersion>>>(`/api/rag/documents/${documentId}/versions`, {
      params
    });
  },
  getRagDocumentVersion(documentId: number, versionNo: number) {
    return http.get<RagApiResponse<RagDocumentVersion>>(
      `/api/rag/documents/${documentId}/versions/${versionNo}`
    );
  },
  replaceRagDocument(documentId: number, file: File) {
    const formData = new FormData();
    formData.append('file', file);
    return http.post<RagApiResponse<RagIngestionSubmitResponse>>(
      `/api/rag/documents/${documentId}/replace`,
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    );
  },
  reparseRagDocument(documentId: number) {
    return http.post<RagApiResponse<RagIngestionSubmitResponse>>(`/api/rag/documents/${documentId}/reparse`);
  },
  rollbackRagDocument(documentId: number, versionNo: number) {
    return http.post<RagApiResponse<RagIngestionSubmitResponse>>(
      `/api/rag/documents/${documentId}/versions/${versionNo}/rollback`
    );
  },
  disableRagDocument(documentId: number) {
    return http.post<RagApiResponse<RagDocument>>(`/api/rag/documents/${documentId}/disable`);
  },
  enableRagDocument(documentId: number) {
    return http.post<RagApiResponse<RagDocument>>(`/api/rag/documents/${documentId}/enable`);
  },
  deleteRagDocument(documentId: number) {
    return http.delete<RagApiResponse<RagDocument>>(`/api/rag/documents/${documentId}`);
  },
  getIngestionTask(taskId: number) {
    return http.get<RagApiResponse<RagIngestionTask>>(`/api/rag/ingestion-tasks/${taskId}`);
  },
  getIngestionTaskProgress(taskId: number) {
    return http.get<RagApiResponse<IngestionTaskProgressSnapshot>>(`/api/rag/ingestion-tasks/${taskId}/progress`);
  },
  listIngestionTaskEvents(taskId: number, params?: PageParams & { afterEventId?: number }) {
    return http.get<RagApiResponse<PageResponse<IngestionTaskEventView>>>(
      `/api/rag/ingestion-tasks/${taskId}/events/history`,
      { params }
    );
  },
  listIngestionTaskShards(taskId: number, params?: PageParams & { stageCode?: string; status?: string }) {
    return http.get<RagApiResponse<PageResponse<RagIngestionTaskShard>>>(
      `/api/rag/ingestion-tasks/${taskId}/shards`,
      { params }
    );
  },
  getIngestionTaskEventsUrl(taskId: number) {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || '';
    return `${baseUrl}/api/rag/ingestion-tasks/${taskId}/events`;
  },
  listIngestionTasks(params?: PageParams & {
    tenantId?: number;
    knowledgeBaseId?: number;
    documentId?: number;
    status?: number;
    taskType?: string;
  }) {
    return http.get<RagApiResponse<PageResponse<RagIngestionTask>>>('/api/rag/ingestion-tasks', { params });
  },
  cancelIngestionTask(taskId: number) {
    return http.post<RagApiResponse<RagIngestionTask>>(`/api/rag/ingestion-tasks/${taskId}/cancel`);
  },
  retryIngestionTask(taskId: number) {
    return http.post<RagApiResponse<RagIngestionTaskRetryResponse>>(`/api/rag/ingestion-tasks/${taskId}/retry`);
  },
  retryIngestionTaskShards(taskId: number, data?: { shardIds?: number[]; stageCode?: string }) {
    return http.post<RagApiResponse<IngestionShardRetryResponse>>(
      `/api/rag/ingestion-tasks/${taskId}/shards/retry`,
      data || {}
    );
  },
  listDocumentTasks(documentId: number, params?: PageParams) {
    return http.get<RagApiResponse<PageResponse<RagIngestionTask>>>(
      `/api/rag/documents/${documentId}/ingestion-tasks`,
      { params }
    );
  },
  listKnowledgeChunks(params?: PageParams & {
    documentId?: string;
    contentType?: string;
    status?: string;
    includeHistory?: boolean;
  }) {
    return http.get<RagApiResponse<PageResponse<KnowledgeChunkRecord>>>('/api/rag/chunks', { params });
  },
  getKnowledgeChunk(chunkId: string, version?: number) {
    return http.get<RagApiResponse<KnowledgeChunkRecord>>(
      `/api/rag/chunks/${encodeURIComponent(chunkId)}`,
      { params: { version } }
    );
  },
  listKnowledgeChunkVersions(chunkId: string, params?: PageParams) {
    return http.get<RagApiResponse<PageResponse<KnowledgeChunkRecord>>>(
      `/api/rag/chunks/${encodeURIComponent(chunkId)}/versions`,
      { params }
    );
  },
  createKnowledgeChunk(data: KnowledgeChunkCreateRequest) {
    return http.post<RagApiResponse<KnowledgeChunkRecord>>('/api/rag/chunks', data);
  },
  updateKnowledgeChunk(chunkId: string, data: KnowledgeChunkUpdateRequest) {
    return http.put<RagApiResponse<KnowledgeChunkRecord>>(
      `/api/rag/chunks/${encodeURIComponent(chunkId)}`,
      data
    );
  },
  rollbackKnowledgeChunk(chunkId: string, version: number) {
    return http.post<RagApiResponse<KnowledgeChunkRecord>>(
      `/api/rag/chunks/${encodeURIComponent(chunkId)}/rollback`,
      null,
      { params: { version } }
    );
  },
  disableKnowledgeChunk(chunkId: string) {
    return http.post<RagApiResponse<KnowledgeChunkRecord>>(`/api/rag/chunks/${encodeURIComponent(chunkId)}/disable`);
  },
  deleteKnowledgeChunk(chunkId: string) {
    return http.delete<RagApiResponse<KnowledgeChunkRecord>>(`/api/rag/chunks/${encodeURIComponent(chunkId)}`);
  },
  rebuildChunkRedisIndex(clearExisting = true) {
    return http.post<RagApiResponse<ChunkRedisRebuildResponse>>('/api/rag/chunks/rebuild-redis-index', null, {
      params: { clearExisting }
    });
  }
};

function exportUrl(path: string, params?: Record<string, unknown>) {
  const search = new URLSearchParams();
  Object.entries(params || {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim() !== '') {
      search.set(key, String(value));
    }
  });
  const query = search.toString();
  const baseUrl = import.meta.env.VITE_API_BASE_URL || '';
  return `${baseUrl}${path}${query ? `?${query}` : ''}`;
}

function appendOptional(formData: FormData, key: string, value: unknown) {
  if (value === undefined || value === null || String(value).trim() === '') {
    return;
  }
  formData.append(key, String(value));
}
