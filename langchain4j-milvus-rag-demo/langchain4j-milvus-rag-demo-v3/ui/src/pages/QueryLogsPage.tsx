import {
  AuditOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  DeleteOutlined,
  DislikeOutlined,
  DollarOutlined,
  DownloadOutlined,
  EditOutlined,
  EyeOutlined,
  InboxOutlined,
  LikeOutlined,
  ReloadOutlined,
  SaveOutlined,
  SearchOutlined,
  UndoOutlined
} from '@ant-design/icons';
import {
  App,
  Button,
  Card,
  Col,
  DatePicker,
  Descriptions,
  Divider,
  Drawer,
  Empty,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Progress,
  Row,
  Segmented,
  Select,
  Space,
  Statistic,
  Switch,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Typography
} from 'antd';
import type { Dayjs } from 'dayjs';
import type { Key } from 'react';
import { useEffect, useMemo, useState } from 'react';
import { ragApi } from '../api/rag';
import PageHeaderCard from '../components/PageHeaderCard';
import type {
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
  MaterializedMetricBackfillResponse,
  RagFeedbackSummaryItem,
  RagFeedbackSummaryResponse,
  RagModelPricing,
  RagModelPricingRequest,
  RagQueryCostAnomalyItem,
  RagQueryCostDimensionItem,
  RagQueryCostTrendPoint,
  RagQueryFeedback,
  RagQueryFeedbackEvent,
  RagQueryFeedbackRequest,
  RagQueryHit,
  RagQueryLog,
  RagQueryLogDeleteAudit,
  RagQueryLogDetailResponse,
  RagQueryLogOperationRequest,
  RagQueryRetentionPolicy,
  RagQueryRetentionPolicyRequest
} from '../types';
import { getErrorMessage } from '../utils/message';

const { Paragraph, Text } = Typography;
const { RangePicker } = DatePicker;

type OperationMode = 'SOFT_DELETE' | 'ARCHIVE' | 'RESTORE' | 'PURGE';

interface FilterValues {
  tenantId?: number;
  queryType?: string;
  status?: string;
  conversationId?: string;
  traceId?: string;
  queryText?: string;
  visibility?: string;
}

interface SummaryFilterValues {
  rating?: string;
  createdBy?: string;
  limit?: number;
}

interface OperationFormValues {
  operator?: string;
  reason?: string;
  retentionUntil?: Dayjs;
}

interface CostFilterValues {
  tenantId?: number;
  knowledgeBaseId?: number;
  queryType?: string;
  retrievalMode?: string;
  status?: string;
  llmModel?: string;
  embeddingModel?: string;
  window?: string;
  range?: [Dayjs, Dayjs];
}

interface MaterializedBackfillFormValues {
  range?: [Dayjs, Dayjs];
}

interface FeedbackAnalyticsFilterValues {
  tenantId?: number;
  knowledgeBaseId?: number;
  retrievalMode?: string;
  queryType?: string;
  feedbackRating?: string;
  feedbackStatus?: string;
  assignee?: string;
  window?: string;
  range?: [Dayjs, Dayjs];
}

interface FeedbackQueueFilterValues {
  tenantId?: number;
  rating?: string;
  feedbackStatus?: string;
  priority?: string;
  assignee?: string;
}

interface RevisionFilterValues {
  feedbackId?: number;
  tenantId?: number;
  knowledgeBaseId?: number;
  revisionStatus?: string;
  assignee?: string;
}

const statusColor: Record<string, string> = {
  SUCCESS: 'success',
  FAILED: 'error'
};

const queryTypeColor: Record<string, string> = {
  QUERY: 'blue',
  SEARCH: 'purple'
};

const feedbackColor: Record<string, string> = {
  HELPFUL: 'success',
  NOT_HELPFUL: 'error',
  CORRECTION: 'warning'
};

const feedbackLabel: Record<string, string> = {
  HELPFUL: '有帮助',
  NOT_HELPFUL: '无帮助',
  CORRECTION: '纠错'
};

const workflowStatusColor: Record<string, string> = {
  OPEN: 'default',
  TRIAGED: 'processing',
  IN_REVIEW: 'blue',
  REVISION_PLANNED: 'purple',
  RESOLVED: 'success',
  REJECTED: 'error',
  CLOSED: 'default'
};

const priorityColor: Record<string, string> = {
  LOW: 'default',
  MEDIUM: 'blue',
  HIGH: 'orange',
  URGENT: 'red'
};

const revisionStatusColor: Record<string, string> = {
  PLANNED: 'default',
  IN_PROGRESS: 'processing',
  APPLIED: 'blue',
  VERIFIED: 'success',
  REJECTED: 'error',
  CANCELLED: 'default'
};

const operationText: Record<OperationMode, string> = {
  SOFT_DELETE: '软删除',
  ARCHIVE: '归档',
  RESTORE: '恢复',
  PURGE: '物理清理'
};

const visibilityOptions = [
  { label: '活跃', value: 'ACTIVE' },
  { label: '回收站', value: 'DELETED' },
  { label: '归档', value: 'ARCHIVED' },
  { label: '全部', value: 'ALL' }
];

const ratingOptions = [
  { label: '有帮助', value: 'HELPFUL' },
  { label: '无帮助', value: 'NOT_HELPFUL' },
  { label: '纠错', value: 'CORRECTION' }
];

const statusOptions = [
  { label: 'SUCCESS', value: 'SUCCESS' },
  { label: 'FAILED', value: 'FAILED' }
];

const feedbackStatusOptions = [
  'OPEN',
  'TRIAGED',
  'IN_REVIEW',
  'REVISION_PLANNED',
  'RESOLVED',
  'REJECTED',
  'CLOSED'
].map((value) => ({ label: value, value }));

const priorityOptions = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'].map((value) => ({ label: value, value }));

const revisionStatusOptions = ['PLANNED', 'IN_PROGRESS', 'APPLIED', 'VERIFIED', 'REJECTED', 'CANCELLED'].map(
  (value) => ({ label: value, value })
);

const revisionTypeOptions = ['UPDATE_CHUNK', 'ADD_DOCUMENT', 'DISABLE_CHUNK', 'REPARSE_DOCUMENT', 'OTHER'].map(
  (value) => ({ label: value, value })
);

export default function QueryLogsPage() {
  const { message } = App.useApp();
  const [form] = Form.useForm<FilterValues>();
  const [summaryForm] = Form.useForm<SummaryFilterValues>();
  const [feedbackForm] = Form.useForm<RagQueryFeedbackRequest>();
  const [operationForm] = Form.useForm<OperationFormValues>();
  const [costForm] = Form.useForm<CostFilterValues>();
  const [materializedBackfillForm] = Form.useForm<MaterializedBackfillFormValues>();
  const [feedbackAnalyticsForm] = Form.useForm<FeedbackAnalyticsFilterValues>();
  const [feedbackQueueForm] = Form.useForm<FeedbackQueueFilterValues>();
  const [revisionFilterForm] = Form.useForm<RevisionFilterValues>();

  const [logs, setLogs] = useState<RagQueryLog[]>([]);
  const [summary, setSummary] = useState<RagFeedbackSummaryResponse>();
  const [detail, setDetail] = useState<RagQueryLogDetailResponse>();
  const [selectedRowKeys, setSelectedRowKeys] = useState<Key[]>([]);
  const [pagination, setPagination] = useState({ pageNo: 1, pageSize: 10, total: 0 });
  const [loading, setLoading] = useState(false);
  const [summaryLoading, setSummaryLoading] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [feedbackSaving, setFeedbackSaving] = useState(false);
  const [operating, setOperating] = useState(false);
  const [operation, setOperation] = useState<{ mode: OperationMode; ids: number[] }>();

  const [deleteAudits, setDeleteAudits] = useState<RagQueryLogDeleteAudit[]>([]);
  const [deleteAuditDetail, setDeleteAuditDetail] = useState<RagQueryLogDeleteAudit>();
  const [auditPagination, setAuditPagination] = useState({ pageNo: 1, pageSize: 10, total: 0 });
  const [auditLoading, setAuditLoading] = useState(false);

  const [retentionPolicies, setRetentionPolicies] = useState<RagQueryRetentionPolicy[]>([]);
  const [policyLoading, setPolicyLoading] = useState(false);

  const [costTrends, setCostTrends] = useState<RagQueryCostTrendPoint[]>([]);
  const [costByModel, setCostByModel] = useState<RagQueryCostDimensionItem[]>([]);
  const [costByTenant, setCostByTenant] = useState<RagQueryCostDimensionItem[]>([]);
  const [costAnomalies, setCostAnomalies] = useState<RagQueryCostAnomalyItem[]>([]);
  const [costLoading, setCostLoading] = useState(false);
  const [materializedBackfill, setMaterializedBackfill] = useState<MaterializedMetricBackfillResponse>();
  const [materializedWatermarks, setMaterializedWatermarks] = useState<unknown[]>([]);
  const [materializedLoading, setMaterializedLoading] = useState(false);
  const [pricing, setPricing] = useState<RagModelPricing[]>([]);
  const [pricingLoading, setPricingLoading] = useState(false);

  const [feedbackQuality, setFeedbackQuality] = useState<RagFeedbackQualitySummary>();
  const [feedbackTrends, setFeedbackTrends] = useState<RagFeedbackQualityTrendPoint[]>([]);
  const [feedbackByKnowledgeBase, setFeedbackByKnowledgeBase] = useState<RagFeedbackDimensionItem[]>([]);
  const [feedbackByAssignee, setFeedbackByAssignee] = useState<RagFeedbackDimensionItem[]>([]);
  const [feedbackAnalyticsLoading, setFeedbackAnalyticsLoading] = useState(false);
  const [feedbackQueue, setFeedbackQueue] = useState<RagQueryFeedback[]>([]);
  const [feedbackPagination, setFeedbackPagination] = useState({ pageNo: 1, pageSize: 10, total: 0 });
  const [feedbackQueueLoading, setFeedbackQueueLoading] = useState(false);
  const [feedbackDetail, setFeedbackDetail] = useState<RagQueryFeedback>();
  const [feedbackEvents, setFeedbackEvents] = useState<RagQueryFeedbackEvent[]>([]);
  const [feedbackDetailLoading, setFeedbackDetailLoading] = useState(false);

  const [revisionTasks, setRevisionTasks] = useState<RagFeedbackRevisionTask[]>([]);
  const [revisionPagination, setRevisionPagination] = useState({ pageNo: 1, pageSize: 10, total: 0 });
  const [revisionLoading, setRevisionLoading] = useState(false);

  const buildLogParams = () => {
    const values = form.getFieldsValue();
    return {
      tenantId: values.tenantId,
      queryType: values.queryType,
      status: values.status,
      conversationId: trimToUndefined(values.conversationId),
      traceId: trimToUndefined(values.traceId),
      queryText: trimToUndefined(values.queryText),
      visibility: values.visibility || 'ACTIVE'
    };
  };

  const loadLogs = async (pageNo = pagination.pageNo, pageSize = pagination.pageSize) => {
    try {
      setLoading(true);
      const { data } = await ragApi.listQueryLogs({
        ...buildLogParams(),
        pageNo,
        pageSize
      });
      const page = data.data;
      setLogs(page?.records || []);
      setPagination({
        pageNo: page?.pageNo || pageNo,
        pageSize: page?.pageSize || pageSize,
        total: page?.total || 0
      });
      setSelectedRowKeys([]);
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  const loadSummary = async () => {
    try {
      setSummaryLoading(true);
      const values = summaryForm.getFieldsValue();
      const logParams = buildLogParams();
      const { data } = await ragApi.getQueryLogFeedbackSummary({
        tenantId: logParams.tenantId,
        queryType: logParams.queryType,
        status: logParams.status,
        conversationId: logParams.conversationId,
        traceId: logParams.traceId,
        rating: values.rating,
        createdBy: trimToUndefined(values.createdBy),
        limit: values.limit
      });
      setSummary(data.data);
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setSummaryLoading(false);
    }
  };

  const loadDeleteAudits = async (pageNo = auditPagination.pageNo, pageSize = auditPagination.pageSize) => {
    try {
      setAuditLoading(true);
      const { data } = await ragApi.listQueryLogDeleteAudits({ pageNo, pageSize });
      const page = data.data;
      setDeleteAudits(page.records || []);
      setAuditPagination({ pageNo: page.pageNo, pageSize: page.pageSize, total: page.total });
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setAuditLoading(false);
    }
  };

  const loadRetentionPolicies = async () => {
    try {
      setPolicyLoading(true);
      const { data } = await ragApi.listQueryRetentionPolicies({ pageNo: 1, pageSize: 50 });
      setRetentionPolicies(data.data.records || []);
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setPolicyLoading(false);
    }
  };

  const loadPricing = async () => {
    try {
      setPricingLoading(true);
      const { data } = await ragApi.listModelPricing({ pageNo: 1, pageSize: 100 });
      setPricing(data.data.records || []);
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setPricingLoading(false);
    }
  };

  const costParams = () => {
    const values = costForm.getFieldsValue();
    const range = dateRange(values.range);
    return {
      tenantId: values.tenantId,
      knowledgeBaseId: values.knowledgeBaseId,
      queryType: values.queryType,
      retrievalMode: trimToUndefined(values.retrievalMode),
      status: values.status,
      llmModel: trimToUndefined(values.llmModel),
      embeddingModel: trimToUndefined(values.embeddingModel),
      window: values.window || 'day',
      from: range.from,
      to: range.to
    };
  };

  const loadCostAnalytics = async () => {
    try {
      setCostLoading(true);
      const params = costParams();
      const [trends, byModel, byTenant, anomalies] = await Promise.all([
        ragApi.getQueryCostTrends(params),
        ragApi.getQueryCostsByModel(params),
        ragApi.getQueryCostsByTenant(params),
        ragApi.getQueryCostAnomalies({ tenantId: params.tenantId, from: params.from, to: params.to })
      ]);
      setCostTrends(trends.data.data || []);
      setCostByModel(byModel.data.data || []);
      setCostByTenant(byTenant.data.data || []);
      setCostAnomalies(anomalies.data.data || []);
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setCostLoading(false);
    }
  };

  const loadMaterializedMetricWatermarks = async () => {
    try {
      setMaterializedLoading(true);
      const { data } = await ragApi.getMaterializedMetricWatermarks();
      setMaterializedWatermarks(data.data || []);
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setMaterializedLoading(false);
    }
  };

  const backfillMaterializedMetrics = async () => {
    try {
      setMaterializedLoading(true);
      const values = materializedBackfillForm.getFieldsValue();
      const range = dateRange(values.range);
      const { data } = await ragApi.backfillMaterializedMetrics(range);
      setMaterializedBackfill(data.data);
      await Promise.all([loadMaterializedMetricWatermarks(), loadCostAnalytics(), loadFeedbackAnalytics()]);
      message.success('Materialized metrics backfill completed');
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setMaterializedLoading(false);
    }
  };

  const feedbackAnalyticsParams = () => {
    const values = feedbackAnalyticsForm.getFieldsValue();
    const range = dateRange(values.range);
    return {
      tenantId: values.tenantId,
      knowledgeBaseId: values.knowledgeBaseId,
      retrievalMode: trimToUndefined(values.retrievalMode),
      queryType: values.queryType,
      feedbackRating: values.feedbackRating,
      feedbackStatus: values.feedbackStatus,
      assignee: trimToUndefined(values.assignee),
      window: values.window || 'day',
      from: range.from,
      to: range.to
    };
  };

  const loadFeedbackAnalytics = async () => {
    try {
      setFeedbackAnalyticsLoading(true);
      const params = feedbackAnalyticsParams();
      const [summaryResponse, trendsResponse, byKbResponse, byAssigneeResponse] = await Promise.all([
        ragApi.getFeedbackQualitySummary(params),
        ragApi.getFeedbackTrends(params),
        ragApi.getFeedbackByKnowledgeBase({ tenantId: params.tenantId, from: params.from, to: params.to }),
        ragApi.getFeedbackByAssignee({ tenantId: params.tenantId, from: params.from, to: params.to })
      ]);
      setFeedbackQuality(summaryResponse.data.data);
      setFeedbackTrends(trendsResponse.data.data || []);
      setFeedbackByKnowledgeBase(byKbResponse.data.data || []);
      setFeedbackByAssignee(byAssigneeResponse.data.data || []);
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setFeedbackAnalyticsLoading(false);
    }
  };

  const loadFeedbackQueue = async (
    pageNo = feedbackPagination.pageNo,
    pageSize = feedbackPagination.pageSize
  ) => {
    try {
      setFeedbackQueueLoading(true);
      const values = feedbackQueueForm.getFieldsValue();
      const { data } = await ragApi.listQueryFeedback({
        tenantId: values.tenantId,
        rating: values.rating,
        feedbackStatus: values.feedbackStatus,
        priority: values.priority,
        assignee: trimToUndefined(values.assignee),
        pageNo,
        pageSize
      });
      const page = data.data;
      setFeedbackQueue(page.records || []);
      setFeedbackPagination({ pageNo: page.pageNo, pageSize: page.pageSize, total: page.total });
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setFeedbackQueueLoading(false);
    }
  };

  const loadRevisionTasks = async (
    pageNo = revisionPagination.pageNo,
    pageSize = revisionPagination.pageSize
  ) => {
    try {
      setRevisionLoading(true);
      const values = revisionFilterForm.getFieldsValue();
      const { data } = await ragApi.listFeedbackRevisionTasks({
        feedbackId: values.feedbackId,
        tenantId: values.tenantId,
        knowledgeBaseId: values.knowledgeBaseId,
        revisionStatus: values.revisionStatus,
        assignee: trimToUndefined(values.assignee),
        pageNo,
        pageSize
      });
      const page = data.data;
      setRevisionTasks(page.records || []);
      setRevisionPagination({ pageNo: page.pageNo, pageSize: page.pageSize, total: page.total });
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setRevisionLoading(false);
    }
  };

  useEffect(() => {
    form.setFieldsValue({ tenantId: 0, visibility: 'ACTIVE' });
    summaryForm.setFieldsValue({ limit: 20 });
    costForm.setFieldsValue({ window: 'day' });
    feedbackAnalyticsForm.setFieldsValue({ window: 'day' });
    void loadLogs(1, pagination.pageSize);
    void loadSummary();
    void loadDeleteAudits(1, auditPagination.pageSize);
    void loadRetentionPolicies();
    void loadPricing();
    void loadCostAnalytics();
    void loadMaterializedMetricWatermarks();
    void loadFeedbackAnalytics();
    void loadFeedbackQueue(1, feedbackPagination.pageSize);
    void loadRevisionTasks(1, revisionPagination.pageSize);
  }, []);

  const search = async () => {
    await loadLogs(1, pagination.pageSize);
    await loadSummary();
  };

  const openDetail = async (record: RagQueryLog) => {
    try {
      setDetailLoading(true);
      const { data } = await ragApi.getQueryLog(record.id);
      setDetail(data.data);
      feedbackForm.resetFields();
      feedbackForm.setFieldsValue({ rating: 'HELPFUL' });
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setDetailLoading(false);
    }
  };

  const submitFeedback = async (values: RagQueryFeedbackRequest) => {
    if (!currentLog) {
      return;
    }
    try {
      setFeedbackSaving(true);
      await ragApi.submitQueryFeedback(currentLog.id, {
        rating: values.rating,
        createdBy: trimToUndefined(values.createdBy),
        comment: trimToUndefined(values.comment),
        correctedAnswer: trimToUndefined(values.correctedAnswer)
      });
      const { data } = await ragApi.getQueryLog(currentLog.id);
      setDetail(data.data);
      feedbackForm.resetFields();
      feedbackForm.setFieldsValue({ rating: 'HELPFUL' });
      await Promise.all([loadSummary(), loadFeedbackQueue(1, feedbackPagination.pageSize), loadFeedbackAnalytics()]);
      message.success('反馈已提交');
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setFeedbackSaving(false);
    }
  };

  const openOperation = (mode: OperationMode, ids?: number[]) => {
    const selectedIds = ids || selectedRowKeys.map((key) => Number(key)).filter((id) => Number.isFinite(id));
    if (selectedIds.length === 0) {
      message.warning('请先选择查询日志');
      return;
    }
    operationForm.resetFields();
    operationForm.setFieldsValue({ operator: 'ops' });
    setOperation({ mode, ids: selectedIds });
  };

  const submitOperation = async () => {
    if (!operation) {
      return;
    }
    try {
      const values = await operationForm.validateFields();
      setOperating(true);
      const request: RagQueryLogOperationRequest = {
        ids: operation.ids,
        operator: trimToUndefined(values.operator),
        reason: trimToUndefined(values.reason),
        retentionUntil: values.retentionUntil?.format('YYYY-MM-DDTHH:mm:ss')
      };
      const response = await runOperation(operation.mode, request);
      setOperation(undefined);
      await Promise.all([loadLogs(pagination.pageNo, pagination.pageSize), loadSummary(), loadDeleteAudits(1, auditPagination.pageSize)]);
      message.success(
        `${operationText[operation.mode]}完成：匹配 ${response.matchedCount} 条，成功 ${response.successCount} 条，审计号 ${response.deleteNo}`
      );
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setOperating(false);
    }
  };

  const runOperation = async (mode: OperationMode, request: RagQueryLogOperationRequest) => {
    if (mode === 'ARCHIVE') {
      const { data } = await ragApi.archiveQueryLogs(request);
      return data.data;
    }
    if (mode === 'RESTORE') {
      const { data } = await ragApi.restoreQueryLogs(request);
      return data.data;
    }
    if (mode === 'PURGE') {
      const { data } = await ragApi.purgeQueryLogs(request);
      return data.data;
    }
    const { data } = await ragApi.softDeleteQueryLogs(request);
    return data.data;
  };

  const openAuditDetail = async (record: RagQueryLogDeleteAudit) => {
    try {
      const { data } = await ragApi.getQueryLogDeleteAudit(record.deleteNo);
      setDeleteAuditDetail(data.data);
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  };

  const refreshAll = async () => {
    await Promise.all([
      loadLogs(pagination.pageNo, pagination.pageSize),
      loadSummary(),
      loadDeleteAudits(auditPagination.pageNo, auditPagination.pageSize),
      loadCostAnalytics(),
      loadMaterializedMetricWatermarks(),
      loadFeedbackAnalytics(),
      loadFeedbackQueue(feedbackPagination.pageNo, feedbackPagination.pageSize),
      loadRevisionTasks(revisionPagination.pageNo, revisionPagination.pageSize)
    ]);
  };

  const currentLog = detail?.log;
  const totalEstimatedCost = useMemo(
    () => costTrends.reduce((sum, row) => sum + Number(row.estimatedTotalCost || 0), 0),
    [costTrends]
  );

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <PageHeaderCard
        title="查询审计与反馈闭环"
        description="集中处理 RAG 查询日志治理、成本分析、反馈流转和知识库修订任务。"
        tags={['Governance', 'Cost', 'Feedback', 'Revision']}
      />

      <Card className="page-card" variant="borderless">
        <Form
          form={form}
          layout="inline"
          initialValues={{ tenantId: 0, visibility: 'ACTIVE' }}
          onFinish={search}
          style={{ rowGap: 12, marginBottom: 16 }}
        >
          <Form.Item label="视图" name="visibility">
            <Segmented options={visibilityOptions} onChange={() => void loadLogs(1, pagination.pageSize)} />
          </Form.Item>
          <Form.Item label="租户" name="tenantId">
            <InputNumber min={0} style={{ width: 110 }} />
          </Form.Item>
          <Form.Item label="类型" name="queryType">
            <Select
              allowClear
              style={{ width: 120 }}
              options={[
                { label: 'QUERY', value: 'QUERY' },
                { label: 'SEARCH', value: 'SEARCH' }
              ]}
            />
          </Form.Item>
          <Form.Item label="状态" name="status">
            <Select allowClear style={{ width: 120 }} options={statusOptions} />
          </Form.Item>
          <Form.Item label="问题" name="queryText">
            <Input allowClear placeholder="问题关键词" style={{ width: 180 }} />
          </Form.Item>
          <Form.Item label="会话" name="conversationId">
            <Input allowClear placeholder="conversationId" style={{ width: 170 }} />
          </Form.Item>
          <Form.Item label="Trace" name="traceId">
            <Input allowClear placeholder="traceId" style={{ width: 210 }} />
          </Form.Item>
          <Form.Item>
            <Space wrap>
              <Button type="primary" htmlType="submit" icon={<SearchOutlined />} loading={loading}>
                查询
              </Button>
              <Button icon={<ReloadOutlined />} onClick={refreshAll} loading={loading || summaryLoading}>
                刷新
              </Button>
              <Button icon={<DownloadOutlined />} onClick={() => window.open(ragApi.getQueryLogExportUrl({ ...buildLogParams(), limit: 5000 }), '_blank')}>
                导出
              </Button>
              <Button
                danger
                icon={<DeleteOutlined />}
                disabled={selectedRowKeys.length === 0}
                loading={operating}
                onClick={() => openOperation('SOFT_DELETE')}
              >
                软删除
              </Button>
              <Button
                icon={<InboxOutlined />}
                disabled={selectedRowKeys.length === 0}
                loading={operating}
                onClick={() => openOperation('ARCHIVE')}
              >
                归档
              </Button>
              <Button
                icon={<UndoOutlined />}
                disabled={selectedRowKeys.length === 0}
                loading={operating}
                onClick={() => openOperation('RESTORE')}
              >
                恢复
              </Button>
              <Button
                danger
                type="primary"
                icon={<DeleteOutlined />}
                disabled={selectedRowKeys.length === 0}
                loading={operating}
                onClick={() => openOperation('PURGE')}
              >
                物理清理
              </Button>
            </Space>
          </Form.Item>
        </Form>

        <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
          <Col xs={12} md={6}>
            <MetricBox title="反馈总数" value={summary?.totalFeedbacks || 0} loading={summaryLoading} />
          </Col>
          <Col xs={12} md={6}>
            <MetricBox title="纠错反馈" value={summary?.correctionCount || 0} loading={summaryLoading} color="#d97706" />
          </Col>
          <Col xs={12} md={6}>
            <MetricBox title="估算成本" value={totalEstimatedCost.toFixed(4)} prefix="$" loading={costLoading} color="#2563eb" />
          </Col>
          <Col xs={12} md={6}>
            <MetricBox title="待处理反馈" value={feedbackQueue.filter((row) => row.feedbackStatus !== 'CLOSED').length} loading={feedbackQueueLoading} />
          </Col>
        </Row>

        <Table<RagQueryLog>
          rowKey="id"
          loading={loading}
          dataSource={logs}
          rowSelection={{ selectedRowKeys, onChange: setSelectedRowKeys }}
          pagination={{
            current: pagination.pageNo,
            pageSize: pagination.pageSize,
            total: pagination.total,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`
          }}
          onChange={(next) => loadLogs(next.current || 1, next.pageSize || pagination.pageSize)}
          scroll={{ x: 1900 }}
          columns={[
            { title: 'ID', dataIndex: 'id', width: 86 },
            {
              title: '治理状态',
              dataIndex: 'archiveStatus',
              width: 120,
              render: (_, record) => governanceTag(record)
            },
            {
              title: '类型',
              dataIndex: 'queryType',
              width: 110,
              render: (value: string) => <Tag color={queryTypeColor[value] || 'default'}>{value}</Tag>
            },
            {
              title: '状态',
              dataIndex: 'status',
              width: 110,
              render: (value: string) => <Tag color={statusColor[value] || 'default'}>{value}</Tag>
            },
            {
              title: '问题',
              dataIndex: 'queryText',
              ellipsis: true,
              render: (value: string) => <Text>{value}</Text>
            },
            { title: '模式', dataIndex: 'retrievalMode', width: 110, render: (value?: string) => value || '-' },
            { title: '模型', dataIndex: 'llmModel', width: 160, ellipsis: true, render: (value?: string) => value || '-' },
            { title: 'Token', dataIndex: 'totalTokens', width: 90, render: emptyNumber },
            {
              title: '估算成本',
              dataIndex: 'estimatedTotalCost',
              width: 110,
              render: (value?: number) => formatMoney(value)
            },
            { title: '命中', dataIndex: 'hitCount', width: 80 },
            {
              title: '耗时',
              dataIndex: 'latencyMs',
              width: 100,
              render: (value: number) => `${value || 0} ms`
            },
            { title: '保留到', dataIndex: 'retentionUntil', width: 180, render: formatDate },
            { title: '创建时间', dataIndex: 'createdAt', width: 190, render: formatDate },
            {
              title: '操作',
              width: 190,
              fixed: 'right',
              render: (_, record) => (
                <Space size={4}>
                  <Tooltip title="详情">
                    <Button icon={<EyeOutlined />} size="small" onClick={() => openDetail(record)} loading={detailLoading} />
                  </Tooltip>
                  {record.deleted || record.archiveStatus === 'ARCHIVED' ? (
                    <>
                      <Tooltip title="恢复">
                        <Button icon={<UndoOutlined />} size="small" onClick={() => openOperation('RESTORE', [record.id])} />
                      </Tooltip>
                      <Tooltip title="物理清理">
                        <Button danger icon={<DeleteOutlined />} size="small" onClick={() => openOperation('PURGE', [record.id])} />
                      </Tooltip>
                    </>
                  ) : (
                    <>
                      <Tooltip title="软删除">
                        <Button danger icon={<DeleteOutlined />} size="small" onClick={() => openOperation('SOFT_DELETE', [record.id])} />
                      </Tooltip>
                      <Tooltip title="归档">
                        <Button icon={<InboxOutlined />} size="small" onClick={() => openOperation('ARCHIVE', [record.id])} />
                      </Tooltip>
                    </>
                  )}
                </Space>
              )
            }
          ]}
        />
      </Card>

      <Tabs
        items={[
          {
            key: 'governance',
            label: '治理与审计',
            children: (
              <Space direction="vertical" size={16} style={{ width: '100%' }}>
                <QueryLogRetentionPanel
                  policies={retentionPolicies}
                  loading={policyLoading}
                  onReload={loadRetentionPolicies}
                  onSave={async (id, request) => {
                    if (id) {
                      await ragApi.updateQueryRetentionPolicy(id, request);
                    } else {
                      await ragApi.createQueryRetentionPolicy(request);
                    }
                    await loadRetentionPolicies();
                  }}
                />
                <Card className="page-card" variant="borderless" title="删除审计">
                  <Table<RagQueryLogDeleteAudit>
                    rowKey="id"
                    loading={auditLoading}
                    dataSource={deleteAudits}
                    pagination={{
                      current: auditPagination.pageNo,
                      pageSize: auditPagination.pageSize,
                      total: auditPagination.total,
                      showSizeChanger: true
                    }}
                    onChange={(next) => loadDeleteAudits(next.current || 1, next.pageSize || auditPagination.pageSize)}
                    columns={[
                      { title: '审计号', dataIndex: 'deleteNo', width: 230 },
                      { title: '模式', dataIndex: 'deleteMode', width: 130, render: (value: string) => <Tag>{value}</Tag> },
                      { title: '操作者', dataIndex: 'operator', width: 140, render: (value?: string) => value || '-' },
                      { title: '匹配', dataIndex: 'matchedCount', width: 90 },
                      { title: '成功', dataIndex: 'successCount', width: 90 },
                      { title: '失败', dataIndex: 'failedCount', width: 90 },
                      { title: '原因', dataIndex: 'reason', ellipsis: true, render: (value?: string) => value || '-' },
                      { title: '时间', dataIndex: 'createdAt', width: 190, render: formatDate },
                      {
                        title: '操作',
                        width: 90,
                        render: (_, record) => <Button size="small" icon={<AuditOutlined />} onClick={() => openAuditDetail(record)} />
                      }
                    ]}
                  />
                </Card>
              </Space>
            )
          },
          {
            key: 'cost',
            label: '成本分析',
            children: (
              <Space direction="vertical" size={16} style={{ width: '100%' }}>
                <Card className="page-card" variant="borderless" title="成本筛选">
                  <Form form={costForm} layout="inline" initialValues={{ window: 'day' }} onFinish={loadCostAnalytics} style={{ rowGap: 12 }}>
                    <Form.Item label="租户" name="tenantId">
                      <InputNumber min={0} style={{ width: 110 }} />
                    </Form.Item>
                    <Form.Item label="知识库" name="knowledgeBaseId">
                      <InputNumber min={1} style={{ width: 120 }} />
                    </Form.Item>
                    <Form.Item label="窗口" name="window">
                      <Select style={{ width: 110 }} options={windowOptions()} />
                    </Form.Item>
                    <Form.Item label="类型" name="queryType">
                      <Select allowClear style={{ width: 120 }} options={[{ label: 'QUERY', value: 'QUERY' }, { label: 'SEARCH', value: 'SEARCH' }]} />
                    </Form.Item>
                    <Form.Item label="状态" name="status">
                      <Select allowClear style={{ width: 120 }} options={statusOptions} />
                    </Form.Item>
                    <Form.Item label="LLM" name="llmModel">
                      <Input allowClear style={{ width: 150 }} />
                    </Form.Item>
                    <Form.Item label="时间" name="range">
                      <RangePicker showTime />
                    </Form.Item>
                    <Form.Item>
                      <Space>
                        <Button type="primary" htmlType="submit" icon={<SearchOutlined />} loading={costLoading}>
                          分析
                        </Button>
                        <Button icon={<DownloadOutlined />} onClick={() => window.open(ragApi.getQueryCostExportUrl(costParams()), '_blank')}>
                          导出成本
                        </Button>
                      </Space>
                    </Form.Item>
                  </Form>
                </Card>
                <Card className="page-card" variant="borderless" title="Materialized metrics">
                  <Space direction="vertical" size={16} style={{ width: '100%' }}>
                    <Form form={materializedBackfillForm} layout="inline" onFinish={backfillMaterializedMetrics} style={{ rowGap: 12 }}>
                      <Form.Item label="Backfill window" name="range">
                        <RangePicker showTime />
                      </Form.Item>
                      <Form.Item>
                        <Space>
                          <Button type="primary" htmlType="submit" icon={<ReloadOutlined />} loading={materializedLoading}>
                            Backfill
                          </Button>
                          <Button icon={<SearchOutlined />} onClick={loadMaterializedMetricWatermarks} loading={materializedLoading}>
                            Refresh watermarks
                          </Button>
                        </Space>
                      </Form.Item>
                    </Form>
                    {materializedBackfill ? (
                      <Descriptions size="small" bordered column={{ xs: 1, md: 4 }}>
                        <Descriptions.Item label="From">{formatDate(materializedBackfill.from)}</Descriptions.Item>
                        <Descriptions.Item label="To">{formatDate(materializedBackfill.to)}</Descriptions.Item>
                        <Descriptions.Item label="Query hourly">{materializedBackfill.queryHourly}</Descriptions.Item>
                        <Descriptions.Item label="Query daily">{materializedBackfill.queryDaily}</Descriptions.Item>
                        <Descriptions.Item label="Feedback hourly">{materializedBackfill.feedbackHourly}</Descriptions.Item>
                        <Descriptions.Item label="Feedback daily">{materializedBackfill.feedbackDaily}</Descriptions.Item>
                        <Descriptions.Item label="Rerank hourly">{materializedBackfill.rerankHourly}</Descriptions.Item>
                        <Descriptions.Item label="Rerank daily">{materializedBackfill.rerankDaily}</Descriptions.Item>
                      </Descriptions>
                    ) : null}
                    <JsonBlock title="Watermarks" value={JSON.stringify(materializedWatermarks, null, 2)} />
                  </Space>
                </Card>
                <QueryCostTrendChart trends={costTrends} loading={costLoading} />
                <TokenUsageBreakdown byModel={costByModel} byTenant={costByTenant} anomalies={costAnomalies} loading={costLoading} />
                <ModelPricingPanel pricing={pricing} loading={pricingLoading} onReload={loadPricing} onSave={async (id, request) => {
                  if (id) {
                    await ragApi.updateModelPricing(id, request);
                  } else {
                    await ragApi.createModelPricing(request);
                  }
                  await loadPricing();
                }} />
              </Space>
            )
          },
          {
            key: 'feedback',
            label: '反馈闭环',
            children: (
              <Space direction="vertical" size={16} style={{ width: '100%' }}>
                <Card className="page-card" variant="borderless" title="反馈质量筛选">
                  <Form form={feedbackAnalyticsForm} layout="inline" initialValues={{ window: 'day' }} onFinish={loadFeedbackAnalytics} style={{ rowGap: 12 }}>
                    <Form.Item label="租户" name="tenantId">
                      <InputNumber min={0} style={{ width: 110 }} />
                    </Form.Item>
                    <Form.Item label="知识库" name="knowledgeBaseId">
                      <InputNumber min={1} style={{ width: 120 }} />
                    </Form.Item>
                    <Form.Item label="窗口" name="window">
                      <Select style={{ width: 110 }} options={windowOptions()} />
                    </Form.Item>
                    <Form.Item label="评分" name="feedbackRating">
                      <Select allowClear style={{ width: 130 }} options={ratingOptions} />
                    </Form.Item>
                    <Form.Item label="状态" name="feedbackStatus">
                      <Select allowClear style={{ width: 160 }} options={feedbackStatusOptions} />
                    </Form.Item>
                    <Form.Item label="负责人" name="assignee">
                      <Input allowClear style={{ width: 140 }} />
                    </Form.Item>
                    <Form.Item label="时间" name="range">
                      <RangePicker showTime />
                    </Form.Item>
                    <Form.Item>
                      <Space>
                        <Button type="primary" htmlType="submit" icon={<SearchOutlined />} loading={feedbackAnalyticsLoading}>
                          分析
                        </Button>
                        <Button icon={<DownloadOutlined />} onClick={() => window.open(ragApi.getFeedbackExportUrl(feedbackAnalyticsParams()), '_blank')}>
                          导出反馈
                        </Button>
                      </Space>
                    </Form.Item>
                  </Form>
                </Card>
                <FeedbackQualityTrendChart
                  summary={feedbackQuality}
                  trends={feedbackTrends}
                  byKnowledgeBase={feedbackByKnowledgeBase}
                  byAssignee={feedbackByAssignee}
                  loading={feedbackAnalyticsLoading}
                />
                <FeedbackWorkQueue
                  form={feedbackQueueForm}
                  feedbacks={feedbackQueue}
                  loading={feedbackQueueLoading}
                  pagination={feedbackPagination}
                  onSearch={() => loadFeedbackQueue(1, feedbackPagination.pageSize)}
                  onPage={loadFeedbackQueue}
                  onOpen={openFeedbackDetail}
                />
              </Space>
            )
          },
          {
            key: 'revision',
            label: '修订任务',
            children: (
              <RevisionTaskPanel
                form={revisionFilterForm}
                tasks={revisionTasks}
                loading={revisionLoading}
                pagination={revisionPagination}
                onSearch={() => loadRevisionTasks(1, revisionPagination.pageSize)}
                onPage={loadRevisionTasks}
                onAction={handleRevisionAction}
              />
            )
          },
          {
            key: 'recent',
            label: '最近反馈',
            children: <RecentFeedbackPanel summary={summary} loading={summaryLoading} form={summaryForm} onReload={loadSummary} />
          }
        ]}
      />

      <QueryDeleteAuditDrawer audit={deleteAuditDetail} onClose={() => setDeleteAuditDetail(undefined)} />

      <Modal
        title={`${operation ? operationText[operation.mode] : ''}确认`}
        open={!!operation}
        confirmLoading={operating}
        onCancel={() => setOperation(undefined)}
        onOk={submitOperation}
        okButtonProps={{ danger: operation?.mode === 'PURGE' || operation?.mode === 'SOFT_DELETE' }}
        okText={operation ? operationText[operation.mode] : '确认'}
      >
        <Space direction="vertical" size={12} style={{ width: '100%' }}>
          <Text type={operation?.mode === 'PURGE' ? 'danger' : undefined}>
            将处理 {operation?.ids.length || 0} 条查询日志。物理清理会永久删除主表和命中明细，后端仍会按保留期与反馈约束拦截不符合条件的记录。
          </Text>
          <Form form={operationForm} layout="vertical">
            <Form.Item label="操作者" name="operator" rules={[{ required: true, message: '请填写操作者' }]}>
              <Input placeholder="ops" maxLength={128} />
            </Form.Item>
            <Form.Item label="原因" name="reason" rules={[{ required: operation?.mode === 'PURGE', message: '高风险操作必须填写原因' }]}>
              <Input.TextArea rows={3} maxLength={1000} placeholder="记录治理、合规或误删恢复原因" />
            </Form.Item>
            {operation?.mode === 'SOFT_DELETE' ? (
              <Form.Item label="最早物理清理时间" name="retentionUntil">
                <DatePicker showTime style={{ width: '100%' }} />
              </Form.Item>
            ) : null}
          </Form>
        </Space>
      </Modal>

      <QueryLogDetailDrawer
        detail={detail}
        loading={detailLoading}
        feedbackForm={feedbackForm}
        feedbackSaving={feedbackSaving}
        onClose={() => setDetail(undefined)}
        onSubmitFeedback={submitFeedback}
      />

      <FeedbackDetailDrawer
        feedback={feedbackDetail}
        events={feedbackEvents}
        loading={feedbackDetailLoading}
        onClose={() => setFeedbackDetail(undefined)}
        onAssign={handleAssignFeedback}
        onStatus={handleChangeFeedbackStatus}
        onReview={handleReviewFeedback}
        onComment={handleCommentFeedback}
        onCloseFeedback={handleCloseFeedback}
        onReopen={handleReopenFeedback}
        onCreateRevision={handleCreateRevisionTask}
      />
    </Space>
  );

  async function openFeedbackDetail(record: RagQueryFeedback) {
    try {
      setFeedbackDetailLoading(true);
      const [feedbackResponse, eventsResponse] = await Promise.all([
        ragApi.getQueryFeedback(record.id),
        ragApi.listQueryFeedbackEvents(record.id)
      ]);
      setFeedbackDetail(feedbackResponse.data.data);
      setFeedbackEvents(eventsResponse.data.data || []);
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setFeedbackDetailLoading(false);
    }
  }

  async function refreshFeedbackDetail(id: number) {
    const [feedbackResponse, eventsResponse] = await Promise.all([
      ragApi.getQueryFeedback(id),
      ragApi.listQueryFeedbackEvents(id)
    ]);
    setFeedbackDetail(feedbackResponse.data.data);
    setFeedbackEvents(eventsResponse.data.data || []);
  }

  async function handleAssignFeedback(values: RagFeedbackAssignRequest) {
    if (!feedbackDetail) {
      return;
    }
    try {
      await ragApi.assignQueryFeedback(feedbackDetail.id, values);
      await Promise.all([refreshFeedbackDetail(feedbackDetail.id), loadFeedbackQueue(feedbackPagination.pageNo, feedbackPagination.pageSize)]);
      message.success('已分派反馈');
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  }

  async function handleChangeFeedbackStatus(values: RagFeedbackStatusRequest) {
    if (!feedbackDetail) {
      return;
    }
    try {
      await ragApi.changeQueryFeedbackStatus(feedbackDetail.id, values);
      await Promise.all([refreshFeedbackDetail(feedbackDetail.id), loadFeedbackQueue(feedbackPagination.pageNo, feedbackPagination.pageSize), loadFeedbackAnalytics()]);
      message.success('反馈状态已更新');
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  }

  async function handleReviewFeedback(values: RagFeedbackReviewRequest) {
    if (!feedbackDetail) {
      return;
    }
    try {
      await ragApi.reviewQueryFeedback(feedbackDetail.id, values);
      await Promise.all([refreshFeedbackDetail(feedbackDetail.id), loadFeedbackQueue(feedbackPagination.pageNo, feedbackPagination.pageSize), loadFeedbackAnalytics()]);
      message.success('复核结论已保存');
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  }

  async function handleCommentFeedback(values: RagFeedbackCommentRequest) {
    if (!feedbackDetail) {
      return;
    }
    try {
      await ragApi.commentQueryFeedback(feedbackDetail.id, values);
      await refreshFeedbackDetail(feedbackDetail.id);
      message.success('评论已记录');
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  }

  async function handleCloseFeedback(values: RagFeedbackCommentRequest) {
    if (!feedbackDetail) {
      return;
    }
    try {
      await ragApi.closeQueryFeedback(feedbackDetail.id, values);
      await Promise.all([refreshFeedbackDetail(feedbackDetail.id), loadFeedbackQueue(feedbackPagination.pageNo, feedbackPagination.pageSize), loadFeedbackAnalytics()]);
      message.success('反馈已关闭');
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  }

  async function handleReopenFeedback(values: RagFeedbackCommentRequest) {
    if (!feedbackDetail) {
      return;
    }
    try {
      await ragApi.reopenQueryFeedback(feedbackDetail.id, values);
      await Promise.all([refreshFeedbackDetail(feedbackDetail.id), loadFeedbackQueue(feedbackPagination.pageNo, feedbackPagination.pageSize), loadFeedbackAnalytics()]);
      message.success('反馈已重开');
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  }

  async function handleCreateRevisionTask(values: RagFeedbackRevisionTaskRequest) {
    if (!feedbackDetail) {
      return;
    }
    try {
      await ragApi.createFeedbackRevisionTask(feedbackDetail.id, values);
      await Promise.all([
        refreshFeedbackDetail(feedbackDetail.id),
        loadRevisionTasks(1, revisionPagination.pageSize),
        loadFeedbackAnalytics()
      ]);
      message.success('修订任务已创建');
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  }

  async function handleRevisionAction(id: number, action: 'apply' | 'verify' | 'reject' | 'cancel', request?: RagFeedbackRevisionTaskActionRequest) {
    try {
      if (action === 'apply') {
        await ragApi.applyFeedbackRevisionTask(id, request);
      } else if (action === 'verify') {
        await ragApi.verifyFeedbackRevisionTask(id, request);
      } else if (action === 'reject') {
        await ragApi.rejectFeedbackRevisionTask(id, request);
      } else {
        await ragApi.cancelFeedbackRevisionTask(id, request);
      }
      await Promise.all([loadRevisionTasks(revisionPagination.pageNo, revisionPagination.pageSize), loadFeedbackQueue(feedbackPagination.pageNo, feedbackPagination.pageSize), loadFeedbackAnalytics()]);
      message.success('修订任务已更新');
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  }
}

function QueryLogRetentionPanel({
  policies,
  loading,
  onReload,
  onSave
}: {
  policies: RagQueryRetentionPolicy[];
  loading: boolean;
  onReload: () => void;
  onSave: (id: number | undefined, request: RagQueryRetentionPolicyRequest) => Promise<void>;
}) {
  const { message } = App.useApp();
  const [form] = Form.useForm<RagQueryRetentionPolicyRequest>();
  const [editingId, setEditingId] = useState<number>();
  const [saving, setSaving] = useState(false);

  const save = async (values: RagQueryRetentionPolicyRequest) => {
    try {
      setSaving(true);
      await onSave(editingId, values);
      form.resetFields();
      setEditingId(undefined);
      message.success('保留策略已保存');
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Card className="page-card" variant="borderless" title="保留策略">
      <Form
        form={form}
        layout="inline"
        initialValues={{ tenantId: 0, queryType: 'ALL', statusFilter: 'ALL', retentionDays: 180, archiveBeforeDelete: true, enabled: true }}
        onFinish={save}
        style={{ rowGap: 12, marginBottom: 16 }}
      >
        <Form.Item label="租户" name="tenantId">
          <InputNumber min={0} style={{ width: 100 }} />
        </Form.Item>
        <Form.Item label="名称" name="policyName" rules={[{ required: true, message: '请填写策略名称' }]}>
          <Input style={{ width: 160 }} maxLength={128} />
        </Form.Item>
        <Form.Item label="类型" name="queryType">
          <Select style={{ width: 110 }} options={[{ label: 'ALL', value: 'ALL' }, { label: 'QUERY', value: 'QUERY' }, { label: 'SEARCH', value: 'SEARCH' }]} />
        </Form.Item>
        <Form.Item label="状态" name="statusFilter">
          <Select style={{ width: 120 }} options={[{ label: 'ALL', value: 'ALL' }, ...statusOptions]} />
        </Form.Item>
        <Form.Item label="保留天数" name="retentionDays">
          <InputNumber min={0} max={3650} style={{ width: 110 }} />
        </Form.Item>
        <Form.Item label="先归档" name="archiveBeforeDelete" valuePropName="checked">
          <Switch />
        </Form.Item>
        <Form.Item label="启用" name="enabled" valuePropName="checked">
          <Switch />
        </Form.Item>
        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={saving}>
              {editingId ? '更新策略' : '新建策略'}
            </Button>
            <Button onClick={() => { form.resetFields(); setEditingId(undefined); }}>
              清空
            </Button>
            <Button icon={<ReloadOutlined />} onClick={onReload} loading={loading}>
              刷新
            </Button>
          </Space>
        </Form.Item>
      </Form>
      <Table<RagQueryRetentionPolicy>
        rowKey="id"
        loading={loading}
        dataSource={policies}
        pagination={false}
        columns={[
          { title: '名称', dataIndex: 'policyName', ellipsis: true },
          { title: '租户', dataIndex: 'tenantId', width: 90 },
          { title: '类型', dataIndex: 'queryType', width: 100 },
          { title: '状态', dataIndex: 'statusFilter', width: 110 },
          { title: '保留天数', dataIndex: 'retentionDays', width: 100 },
          { title: '先归档', dataIndex: 'archiveBeforeDelete', width: 100, render: (value: boolean) => yesNo(value) },
          { title: '启用', dataIndex: 'enabled', width: 90, render: (value: boolean) => yesNo(value) },
          {
            title: '操作',
            width: 90,
            render: (_, record) => (
              <Button
                size="small"
                icon={<EditOutlined />}
                onClick={() => {
                  setEditingId(record.id);
                  form.setFieldsValue(record);
                }}
              />
            )
          }
        ]}
      />
    </Card>
  );
}

function QueryDeleteAuditDrawer({ audit, onClose }: { audit?: RagQueryLogDeleteAudit; onClose: () => void }) {
  return (
    <Drawer title={audit ? `删除审计 ${audit.deleteNo}` : '删除审计'} open={!!audit} width={760} onClose={onClose}>
      {audit ? (
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <Descriptions bordered size="small" column={2}>
            <Descriptions.Item label="审计号">{audit.deleteNo}</Descriptions.Item>
            <Descriptions.Item label="模式">{audit.deleteMode}</Descriptions.Item>
            <Descriptions.Item label="操作者">{audit.operator || '-'}</Descriptions.Item>
            <Descriptions.Item label="时间">{formatDate(audit.createdAt)}</Descriptions.Item>
            <Descriptions.Item label="匹配">{audit.matchedCount}</Descriptions.Item>
            <Descriptions.Item label="成功">{audit.successCount}</Descriptions.Item>
            <Descriptions.Item label="失败">{audit.failedCount}</Descriptions.Item>
            <Descriptions.Item label="原因">{audit.reason || '-'}</Descriptions.Item>
          </Descriptions>
          <JsonBlock title="筛选条件" value={audit.filterJson} />
          <JsonBlock title="影响日志" value={audit.queryLogIdsJson} />
          <JsonBlock title="结果详情" value={audit.resultJson} />
        </Space>
      ) : null}
    </Drawer>
  );
}

function QueryCostTrendChart({ trends, loading }: { trends: RagQueryCostTrendPoint[]; loading: boolean }) {
  const maxCost = Math.max(...trends.map((item) => Number(item.estimatedTotalCost || 0)), 1);
  return (
    <Card className="page-card" variant="borderless" title="成本趋势">
      <Row gutter={[16, 16]} style={{ marginBottom: 16 }}>
        <Col xs={12} md={6}>
          <MetricBox title="查询量" value={trends.reduce((sum, row) => sum + row.queryCount, 0)} loading={loading} />
        </Col>
        <Col xs={12} md={6}>
          <MetricBox title="总 Token" value={trends.reduce((sum, row) => sum + row.totalTokens, 0)} loading={loading} />
        </Col>
        <Col xs={12} md={6}>
          <MetricBox title="总成本" value={formatMoney(trends.reduce((sum, row) => sum + Number(row.estimatedTotalCost || 0), 0))} loading={loading} color="#2563eb" />
        </Col>
        <Col xs={12} md={6}>
          <MetricBox title="失败查询" value={trends.reduce((sum, row) => sum + row.failedCount, 0)} loading={loading} color="#dc2626" />
        </Col>
      </Row>
      {trends.length === 0 ? (
        <Empty description="暂无成本趋势数据" />
      ) : (
        <Space direction="vertical" size={10} style={{ width: '100%' }}>
          {trends.slice(0, 16).map((item) => (
            <div key={`${item.bucket}-${item.llmModel}-${item.status}`} style={{ display: 'grid', gridTemplateColumns: '210px 1fr 130px', gap: 12, alignItems: 'center' }}>
              <Text ellipsis>{formatDate(item.bucket)} · {item.llmModel || 'unknown'}</Text>
              <Progress percent={Math.round((Number(item.estimatedTotalCost || 0) / maxCost) * 100)} showInfo={false} strokeColor="#2563eb" />
              <Text style={{ textAlign: 'right' }}>{formatMoney(item.estimatedTotalCost)} · {item.queryCount} 次</Text>
            </div>
          ))}
        </Space>
      )}
    </Card>
  );
}

function TokenUsageBreakdown({
  byModel,
  byTenant,
  anomalies,
  loading
}: {
  byModel: RagQueryCostDimensionItem[];
  byTenant: RagQueryCostDimensionItem[];
  anomalies: RagQueryCostAnomalyItem[];
  loading: boolean;
}) {
  return (
    <Row gutter={[16, 16]}>
      <Col xs={24} lg={12}>
        <Card className="page-card" variant="borderless" title="模型维度 Token 与成本">
          <Table<RagQueryCostDimensionItem>
            rowKey={(record) => `${record.dimension}-${record.value}`}
            loading={loading}
            dataSource={byModel}
            size="small"
            pagination={false}
            columns={costDimensionColumns()}
          />
        </Card>
      </Col>
      <Col xs={24} lg={12}>
        <Card className="page-card" variant="borderless" title="租户维度 Token 与成本">
          <Table<RagQueryCostDimensionItem>
            rowKey={(record) => `${record.dimension}-${record.value}`}
            loading={loading}
            dataSource={byTenant}
            size="small"
            pagination={false}
            columns={costDimensionColumns()}
          />
        </Card>
      </Col>
      <Col span={24}>
        <Card className="page-card" variant="borderless" title="成本异常">
          <Table<RagQueryCostAnomalyItem>
            rowKey={(record, index) => `${record.anomalyType}-${record.metricName}-${index}`}
            loading={loading}
            dataSource={anomalies}
            size="small"
            pagination={false}
            columns={[
              { title: '类型', dataIndex: 'anomalyType', width: 180 },
              { title: '级别', dataIndex: 'severity', width: 100, render: (value: string) => <Tag color={value === 'HIGH' ? 'red' : 'orange'}>{value}</Tag> },
              { title: '指标', dataIndex: 'metricName', width: 140 },
              { title: '值', dataIndex: 'metricValue', width: 120, render: emptyNumber },
              { title: '基线', dataIndex: 'baselineValue', width: 120, render: emptyNumber },
              { title: '窗口开始', dataIndex: 'windowStart', width: 180, render: formatDate },
              { title: '窗口结束', dataIndex: 'windowEnd', width: 180, render: formatDate },
              { title: '元数据', dataIndex: 'metadata', ellipsis: true, render: (value?: string) => value || '-' }
            ]}
          />
        </Card>
      </Col>
    </Row>
  );
}

function ModelPricingPanel({
  pricing,
  loading,
  onReload,
  onSave
}: {
  pricing: RagModelPricing[];
  loading: boolean;
  onReload: () => void;
  onSave: (id: number | undefined, request: RagModelPricingRequest) => Promise<void>;
}) {
  const { message } = App.useApp();
  const [form] = Form.useForm<RagModelPricingRequest>();
  const [editingId, setEditingId] = useState<number>();
  const [saving, setSaving] = useState(false);

  const save = async (values: RagModelPricingRequest) => {
    try {
      setSaving(true);
      await onSave(editingId, values);
      form.resetFields();
      setEditingId(undefined);
      message.success('模型价格已保存');
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Card className="page-card" variant="borderless" title="模型价格">
      <Form form={form} layout="inline" initialValues={{ currency: 'USD', enabled: true }} onFinish={save} style={{ rowGap: 12, marginBottom: 16 }}>
        <Form.Item label="Provider" name="provider" rules={[{ required: true }]}>
          <Input style={{ width: 160 }} />
        </Form.Item>
        <Form.Item label="Model" name="model" rules={[{ required: true }]}>
          <Input style={{ width: 180 }} />
        </Form.Item>
        <Form.Item label="输入/1k" name="inputCostPer1kTokens">
          <InputNumber min={0} precision={8} style={{ width: 120 }} />
        </Form.Item>
        <Form.Item label="输出/1k" name="outputCostPer1kTokens">
          <InputNumber min={0} precision={8} style={{ width: 120 }} />
        </Form.Item>
        <Form.Item label="币种" name="currency">
          <Input style={{ width: 90 }} />
        </Form.Item>
        <Form.Item label="启用" name="enabled" valuePropName="checked">
          <Switch />
        </Form.Item>
        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={saving}>
              {editingId ? '更新价格' : '新建价格'}
            </Button>
            <Button onClick={() => { form.resetFields(); setEditingId(undefined); }}>
              清空
            </Button>
            <Button icon={<ReloadOutlined />} onClick={onReload} loading={loading}>
              刷新
            </Button>
          </Space>
        </Form.Item>
      </Form>
      <Table<RagModelPricing>
        rowKey="id"
        loading={loading}
        dataSource={pricing}
        pagination={false}
        columns={[
          { title: 'Provider', dataIndex: 'provider', width: 150 },
          { title: 'Model', dataIndex: 'model', ellipsis: true },
          { title: '输入/1k', dataIndex: 'inputCostPer1kTokens', width: 110 },
          { title: '输出/1k', dataIndex: 'outputCostPer1kTokens', width: 110 },
          { title: '币种', dataIndex: 'currency', width: 90 },
          { title: '启用', dataIndex: 'enabled', width: 90, render: (value: boolean) => yesNo(value) },
          {
            title: '操作',
            width: 90,
            render: (_, record) => (
              <Button
                size="small"
                icon={<EditOutlined />}
                onClick={() => {
                  setEditingId(record.id);
                  form.setFieldsValue(record);
                }}
              />
            )
          }
        ]}
      />
    </Card>
  );
}

function FeedbackQualityTrendChart({
  summary,
  trends,
  byKnowledgeBase,
  byAssignee,
  loading
}: {
  summary?: RagFeedbackQualitySummary;
  trends: RagFeedbackQualityTrendPoint[];
  byKnowledgeBase: RagFeedbackDimensionItem[];
  byAssignee: RagFeedbackDimensionItem[];
  loading: boolean;
}) {
  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Card className="page-card" variant="borderless" title="反馈质量摘要">
        <Row gutter={[16, 16]}>
          <Col xs={12} md={6}>
            <MetricBox title="反馈率" value={formatRate(summary?.feedbackRate)} loading={loading} />
          </Col>
          <Col xs={12} md={6}>
            <MetricBox title="有帮助率" value={formatRate(summary?.helpfulRate)} loading={loading} color="#16a34a" />
          </Col>
          <Col xs={12} md={6}>
            <MetricBox title="纠错采纳率" value={formatRate(summary?.correctionAcceptedRate)} loading={loading} color="#d97706" />
          </Col>
          <Col xs={12} md={6}>
            <MetricBox title="验证修复率" value={formatRate(summary?.verifiedFixRate)} loading={loading} color="#2563eb" />
          </Col>
        </Row>
      </Card>
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card className="page-card" variant="borderless" title="反馈趋势">
            {trends.length === 0 ? (
              <Empty description="暂无反馈趋势数据" />
            ) : (
              <Space direction="vertical" size={10} style={{ width: '100%' }}>
                {trends.slice(0, 12).map((item) => (
                  <div key={`${item.bucket}-${item.feedbackStatus}-${item.assignee}`} style={{ display: 'grid', gridTemplateColumns: '180px 1fr 120px', gap: 12, alignItems: 'center' }}>
                    <Text ellipsis>{formatDate(item.bucket)}</Text>
                    <Progress percent={ratePercent(item.helpfulRate)} showInfo={false} strokeColor="#16a34a" />
                    <Text style={{ textAlign: 'right' }}>{item.feedbackCount} 条 · {formatRate(item.helpfulRate)}</Text>
                  </div>
                ))}
              </Space>
            )}
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card className="page-card" variant="borderless" title="维度分布">
            <Tabs
              size="small"
              items={[
                {
                  key: 'kb',
                  label: '知识库',
                  children: <FeedbackDimensionTable rows={byKnowledgeBase} loading={loading} />
                },
                {
                  key: 'assignee',
                  label: '负责人',
                  children: <FeedbackDimensionTable rows={byAssignee} loading={loading} />
                }
              ]}
            />
          </Card>
        </Col>
      </Row>
    </Space>
  );
}

function FeedbackWorkQueue({
  form,
  feedbacks,
  loading,
  pagination,
  onSearch,
  onPage,
  onOpen
}: {
  form: ReturnType<typeof Form.useForm<FeedbackQueueFilterValues>>[0];
  feedbacks: RagQueryFeedback[];
  loading: boolean;
  pagination: { pageNo: number; pageSize: number; total: number };
  onSearch: () => void;
  onPage: (pageNo: number, pageSize: number) => void;
  onOpen: (feedback: RagQueryFeedback) => void;
}) {
  return (
    <Card className="page-card" variant="borderless" title="反馈工作队列">
      <Form form={form} layout="inline" onFinish={onSearch} style={{ rowGap: 12, marginBottom: 16 }}>
        <Form.Item label="租户" name="tenantId">
          <InputNumber min={0} style={{ width: 110 }} />
        </Form.Item>
        <Form.Item label="评分" name="rating">
          <Select allowClear style={{ width: 130 }} options={ratingOptions} />
        </Form.Item>
        <Form.Item label="状态" name="feedbackStatus">
          <Select allowClear style={{ width: 160 }} options={feedbackStatusOptions} />
        </Form.Item>
        <Form.Item label="优先级" name="priority">
          <Select allowClear style={{ width: 130 }} options={priorityOptions} />
        </Form.Item>
        <Form.Item label="负责人" name="assignee">
          <Input allowClear style={{ width: 140 }} />
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit" icon={<SearchOutlined />} loading={loading}>
            查询
          </Button>
        </Form.Item>
      </Form>
      <Table<RagQueryFeedback>
        rowKey="id"
        loading={loading}
        dataSource={feedbacks}
        pagination={{
          current: pagination.pageNo,
          pageSize: pagination.pageSize,
          total: pagination.total,
          showSizeChanger: true
        }}
        onChange={(next) => onPage(next.current || 1, next.pageSize || pagination.pageSize)}
        columns={[
          { title: 'ID', dataIndex: 'id', width: 80 },
          { title: '日志 ID', dataIndex: 'queryLogId', width: 100 },
          { title: '评分', dataIndex: 'rating', width: 120, render: feedbackTag },
          { title: '状态', dataIndex: 'feedbackStatus', width: 150, render: workflowTag },
          { title: '优先级', dataIndex: 'priority', width: 100, render: priorityTag },
          { title: '负责人', dataIndex: 'assignee', width: 140, render: (value?: string) => value || '-' },
          { title: '复核', dataIndex: 'reviewResult', width: 130, render: (value?: string) => value || '-' },
          { title: '备注', dataIndex: 'comment', ellipsis: true, render: (value?: string) => value || '-' },
          { title: '创建时间', dataIndex: 'createdAt', width: 180, render: formatDate },
          {
            title: '操作',
            width: 90,
            render: (_, record) => <Button size="small" icon={<EyeOutlined />} onClick={() => onOpen(record)} />
          }
        ]}
      />
    </Card>
  );
}

function RevisionTaskPanel({
  form,
  tasks,
  loading,
  pagination,
  onSearch,
  onPage,
  onAction
}: {
  form: ReturnType<typeof Form.useForm<RevisionFilterValues>>[0];
  tasks: RagFeedbackRevisionTask[];
  loading: boolean;
  pagination: { pageNo: number; pageSize: number; total: number };
  onSearch: () => void;
  onPage: (pageNo: number, pageSize: number) => void;
  onAction: (id: number, action: 'apply' | 'verify' | 'reject' | 'cancel', request?: RagFeedbackRevisionTaskActionRequest) => void;
}) {
  return (
    <Card className="page-card" variant="borderless" title="知识库修订任务">
      <Form form={form} layout="inline" onFinish={onSearch} style={{ rowGap: 12, marginBottom: 16 }}>
        <Form.Item label="反馈 ID" name="feedbackId">
          <InputNumber min={1} style={{ width: 110 }} />
        </Form.Item>
        <Form.Item label="租户" name="tenantId">
          <InputNumber min={0} style={{ width: 110 }} />
        </Form.Item>
        <Form.Item label="知识库" name="knowledgeBaseId">
          <InputNumber min={1} style={{ width: 120 }} />
        </Form.Item>
        <Form.Item label="状态" name="revisionStatus">
          <Select allowClear style={{ width: 150 }} options={revisionStatusOptions} />
        </Form.Item>
        <Form.Item label="负责人" name="assignee">
          <Input allowClear style={{ width: 140 }} />
        </Form.Item>
        <Form.Item>
          <Button type="primary" htmlType="submit" icon={<SearchOutlined />} loading={loading}>
            查询
          </Button>
        </Form.Item>
      </Form>
      <Table<RagFeedbackRevisionTask>
        rowKey="id"
        loading={loading}
        dataSource={tasks}
        pagination={{
          current: pagination.pageNo,
          pageSize: pagination.pageSize,
          total: pagination.total,
          showSizeChanger: true
        }}
        onChange={(next) => onPage(next.current || 1, next.pageSize || pagination.pageSize)}
        scroll={{ x: 1500 }}
        columns={[
          { title: '任务号', dataIndex: 'revisionNo', width: 210 },
          { title: '反馈 ID', dataIndex: 'feedbackId', width: 90 },
          { title: '知识库', dataIndex: 'knowledgeBaseId', width: 90, render: emptyNumber },
          { title: '文档', dataIndex: 'documentId', width: 90, render: emptyNumber },
          { title: 'Chunk', dataIndex: 'chunkUid', width: 160, ellipsis: true, render: (value?: string) => value || '-' },
          { title: '类型', dataIndex: 'revisionType', width: 150 },
          { title: '状态', dataIndex: 'revisionStatus', width: 130, render: revisionStatusTag },
          { title: '负责人', dataIndex: 'assignee', width: 130, render: (value?: string) => value || '-' },
          { title: '预期修复', dataIndex: 'expectedFix', ellipsis: true, render: (value?: string) => value || '-' },
          { title: '更新时间', dataIndex: 'updatedAt', width: 180, render: formatDate },
          {
            title: '操作',
            width: 260,
            fixed: 'right',
            render: (_, record) => (
              <Space size={4} wrap>
                <Popconfirm title="确认标记为已应用？" onConfirm={() => onAction(record.id, 'apply', { operator: 'ops' })}>
                  <Button size="small" icon={<CheckCircleOutlined />}>应用</Button>
                </Popconfirm>
                <Popconfirm title="验证通过并闭环反馈？" onConfirm={() => onAction(record.id, 'verify', { operator: 'ops', verified: true, verificationResultJson: '{"verified":true}' })}>
                  <Button size="small" type="primary" icon={<CheckCircleOutlined />}>通过</Button>
                </Popconfirm>
                <Popconfirm title="验证失败并退回复核？" onConfirm={() => onAction(record.id, 'verify', { operator: 'ops', verified: false, verificationResultJson: '{"verified":false}' })}>
                  <Button size="small">失败</Button>
                </Popconfirm>
                <Popconfirm title="确认驳回任务？" onConfirm={() => onAction(record.id, 'reject', { operator: 'ops' })}>
                  <Button size="small" danger icon={<CloseCircleOutlined />}>驳回</Button>
                </Popconfirm>
                <Popconfirm title="确认取消任务？" onConfirm={() => onAction(record.id, 'cancel', { operator: 'ops' })}>
                  <Button size="small">取消</Button>
                </Popconfirm>
              </Space>
            )
          }
        ]}
      />
    </Card>
  );
}

function RecentFeedbackPanel({
  summary,
  loading,
  form,
  onReload
}: {
  summary?: RagFeedbackSummaryResponse;
  loading: boolean;
  form: ReturnType<typeof Form.useForm<SummaryFilterValues>>[0];
  onReload: () => void;
}) {
  return (
    <Card className="page-card" variant="borderless" title="最近反馈">
      <Form form={form} layout="inline" initialValues={{ limit: 20 }} onFinish={onReload} style={{ rowGap: 12, marginBottom: 16 }}>
        <Form.Item label="反馈评级" name="rating">
          <Select allowClear style={{ width: 140 }} options={ratingOptions} />
        </Form.Item>
        <Form.Item label="提交人" name="createdBy">
          <Input allowClear placeholder="createdBy" style={{ width: 160 }} />
        </Form.Item>
        <Form.Item label="反馈条数" name="limit">
          <InputNumber min={1} max={200} style={{ width: 110 }} />
        </Form.Item>
        <Form.Item>
          <Button icon={<ReloadOutlined />} onClick={onReload} loading={loading}>
            刷新反馈
          </Button>
        </Form.Item>
      </Form>
      <Table<RagFeedbackSummaryItem>
        rowKey="feedbackId"
        loading={loading}
        dataSource={summary?.recentFeedbacks || []}
        pagination={false}
        size="small"
        columns={[
          { title: '评级', dataIndex: 'rating', width: 110, render: feedbackTag },
          { title: '状态', dataIndex: 'feedbackStatus', width: 150, render: workflowTag },
          { title: '优先级', dataIndex: 'priority', width: 100, render: priorityTag },
          { title: '日志 ID', dataIndex: 'queryLogId', width: 90 },
          { title: '负责人', dataIndex: 'assignee', width: 130, render: (value?: string) => value || '-' },
          { title: '问题', dataIndex: 'queryText', ellipsis: true, render: (value?: string) => value || '-' },
          { title: '备注', dataIndex: 'comment', ellipsis: true, render: (value?: string) => value || '-' },
          { title: 'Token', dataIndex: 'totalTokens', width: 90, render: emptyNumber },
          { title: '反馈时间', dataIndex: 'feedbackCreatedAt', width: 190, render: formatDate }
        ]}
      />
    </Card>
  );
}

function QueryLogDetailDrawer({
  detail,
  loading,
  feedbackForm,
  feedbackSaving,
  onClose,
  onSubmitFeedback
}: {
  detail?: RagQueryLogDetailResponse;
  loading: boolean;
  feedbackForm: ReturnType<typeof Form.useForm<RagQueryFeedbackRequest>>[0];
  feedbackSaving: boolean;
  onClose: () => void;
  onSubmitFeedback: (values: RagQueryFeedbackRequest) => void;
}) {
  const currentLog = detail?.log;
  return (
    <Drawer title={currentLog ? `查询日志 #${currentLog.id}` : '查询日志'} open={!!detail} width={980} onClose={onClose}>
      {currentLog ? (
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <Descriptions bordered size="small" column={2}>
            <Descriptions.Item label="类型">
              <Tag color={queryTypeColor[currentLog.queryType] || 'default'}>{currentLog.queryType}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="状态">
              <Tag color={statusColor[currentLog.status] || 'default'}>{currentLog.status}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="治理状态">{governanceTag(currentLog)}</Descriptions.Item>
            <Descriptions.Item label="保留到">{formatDate(currentLog.retentionUntil)}</Descriptions.Item>
            <Descriptions.Item label="Trace ID">{currentLog.traceId || '-'}</Descriptions.Item>
            <Descriptions.Item label="会话 ID">{currentLog.conversationId || '-'}</Descriptions.Item>
            <Descriptions.Item label="租户">{currentLog.tenantId}</Descriptions.Item>
            <Descriptions.Item label="检索模式">{currentLog.retrievalMode || '-'}</Descriptions.Item>
            <Descriptions.Item label="LLM">{currentLog.llmModel || '-'}</Descriptions.Item>
            <Descriptions.Item label="Embedding">{currentLog.embeddingModel || '-'}</Descriptions.Item>
            <Descriptions.Item label="Total Tokens">{emptyNumber(currentLog.totalTokens)}</Descriptions.Item>
            <Descriptions.Item label="估算成本">{formatMoney(currentLog.estimatedTotalCost)} {currentLog.costCurrency || ''}</Descriptions.Item>
            <Descriptions.Item label="命中">{currentLog.hitCount}</Descriptions.Item>
            <Descriptions.Item label="耗时">{currentLog.latencyMs || 0} ms</Descriptions.Item>
            <Descriptions.Item label="删除人">{currentLog.deletedBy || '-'}</Descriptions.Item>
            <Descriptions.Item label="删除时间">{formatDate(currentLog.deletedAt)}</Descriptions.Item>
          </Descriptions>

          <Text strong>问题</Text>
          <Paragraph copyable style={{ whiteSpace: 'pre-wrap' }}>{currentLog.queryText}</Paragraph>

          {currentLog.promptText ? (
            <>
              <Text strong>Prompt</Text>
              <Paragraph copyable style={{ whiteSpace: 'pre-wrap' }}>{currentLog.promptText}</Paragraph>
            </>
          ) : null}

          {currentLog.answerText ? (
            <>
              <Text strong>答案</Text>
              <Paragraph copyable style={{ whiteSpace: 'pre-wrap' }}>{currentLog.answerText}</Paragraph>
            </>
          ) : null}

          {currentLog.errorMessage ? (
            <>
              <Text strong type="danger">错误</Text>
              <Paragraph type="danger" copyable style={{ whiteSpace: 'pre-wrap' }}>
                {currentLog.errorCode ? `${currentLog.errorCode}: ` : ''}
                {currentLog.errorMessage}
              </Paragraph>
            </>
          ) : null}

          <Text strong>检索命中</Text>
          <Table<RagQueryHit>
            rowKey="id"
            dataSource={detail.hits || []}
            pagination={false}
            size="small"
            loading={loading}
            columns={[
              { title: '#', dataIndex: 'rankNo', width: 64 },
              { title: 'Score', dataIndex: 'score', width: 100, render: (value?: number) => (value === undefined || value === null ? '-' : value.toFixed(4)) },
              { title: 'KB', dataIndex: 'knowledgeBaseId', width: 90 },
              { title: 'Document', dataIndex: 'documentName', width: 180, ellipsis: true },
              { title: 'Chunk', dataIndex: 'chunkId', width: 180, ellipsis: true },
              { title: 'Type', dataIndex: 'contentType', width: 100 },
              { title: '内容摘要', dataIndex: 'contentSnippet', render: (value?: string) => <Paragraph ellipsis={{ rows: 2 }} style={{ marginBottom: 0 }}>{value || '-'}</Paragraph> }
            ]}
          />

          <Text strong>提交反馈</Text>
          <Form form={feedbackForm} layout="vertical" initialValues={{ rating: 'HELPFUL' }} onFinish={onSubmitFeedback}>
            <Form.Item label="评分" name="rating" rules={[{ required: true }]}>
              <Segmented
                options={[
                  { label: '有帮助', value: 'HELPFUL', icon: <LikeOutlined /> },
                  { label: '无帮助', value: 'NOT_HELPFUL', icon: <DislikeOutlined /> },
                  { label: '纠错', value: 'CORRECTION', icon: <EditOutlined /> }
                ]}
              />
            </Form.Item>
            <Form.Item label="提交人" name="createdBy">
              <Input placeholder="可选" />
            </Form.Item>
            <Form.Item label="备注" name="comment">
              <Input.TextArea rows={3} placeholder="补充问题、命中质量或答案质量说明" />
            </Form.Item>
            <Form.Item label="纠正答案" name="correctedAnswer">
              <Input.TextArea rows={4} placeholder="当评分为纠错时，可填写建议答案" />
            </Form.Item>
            <Button type="primary" htmlType="submit" loading={feedbackSaving}>提交反馈</Button>
          </Form>

          <Table<RagQueryFeedback>
            rowKey="id"
            dataSource={detail.feedbacks || []}
            pagination={false}
            size="small"
            columns={[
              { title: '评分', dataIndex: 'rating', width: 120, render: feedbackTag },
              { title: '状态', dataIndex: 'feedbackStatus', width: 150, render: workflowTag },
              { title: '优先级', dataIndex: 'priority', width: 100, render: priorityTag },
              { title: '提交人', dataIndex: 'createdBy', width: 140, render: (value?: string) => value || '-' },
              { title: '备注', dataIndex: 'comment', render: (value?: string) => <Paragraph ellipsis={{ rows: 2 }} style={{ marginBottom: 0 }}>{value || '-'}</Paragraph> },
              { title: '纠正答案', dataIndex: 'correctedAnswer', render: (value?: string) => <Paragraph ellipsis={{ rows: 2 }} style={{ marginBottom: 0 }}>{value || '-'}</Paragraph> },
              { title: '时间', dataIndex: 'createdAt', width: 180, render: formatDate }
            ]}
          />
        </Space>
      ) : null}
    </Drawer>
  );
}

function FeedbackDetailDrawer({
  feedback,
  events,
  loading,
  onClose,
  onAssign,
  onStatus,
  onReview,
  onComment,
  onCloseFeedback,
  onReopen,
  onCreateRevision
}: {
  feedback?: RagQueryFeedback;
  events: RagQueryFeedbackEvent[];
  loading: boolean;
  onClose: () => void;
  onAssign: (values: RagFeedbackAssignRequest) => void;
  onStatus: (values: RagFeedbackStatusRequest) => void;
  onReview: (values: RagFeedbackReviewRequest) => void;
  onComment: (values: RagFeedbackCommentRequest) => void;
  onCloseFeedback: (values: RagFeedbackCommentRequest) => void;
  onReopen: (values: RagFeedbackCommentRequest) => void;
  onCreateRevision: (values: RagFeedbackRevisionTaskRequest) => void;
}) {
  const [assignForm] = Form.useForm<RagFeedbackAssignRequest>();
  const [statusForm] = Form.useForm<RagFeedbackStatusRequest>();
  const [reviewForm] = Form.useForm<RagFeedbackReviewRequest>();
  const [commentForm] = Form.useForm<RagFeedbackCommentRequest>();
  const [revisionForm] = Form.useForm<RagFeedbackRevisionTaskRequest>();

  useEffect(() => {
    if (feedback) {
      assignForm.setFieldsValue({ assignee: feedback.assignee, operator: 'ops' });
      statusForm.setFieldsValue({ status: feedback.feedbackStatus || 'OPEN', operator: 'ops' });
      reviewForm.setFieldsValue({ reviewResult: feedback.reviewResult, reviewComment: feedback.reviewComment, operator: 'ops' });
      commentForm.setFieldsValue({ operator: 'ops' });
      revisionForm.setFieldsValue({ revisionType: 'UPDATE_CHUNK', createdBy: 'ops', assignee: feedback.assignee });
    }
  }, [feedback]);

  return (
    <Drawer title={feedback ? `反馈 #${feedback.id}` : '反馈详情'} open={!!feedback} width={920} onClose={onClose}>
      {feedback ? (
        <Space direction="vertical" size={16} style={{ width: '100%' }}>
          <Descriptions bordered size="small" column={2}>
            <Descriptions.Item label="查询日志">{feedback.queryLogId}</Descriptions.Item>
            <Descriptions.Item label="评分">{feedbackTag(feedback.rating)}</Descriptions.Item>
            <Descriptions.Item label="状态">{workflowTag(feedback.feedbackStatus)}</Descriptions.Item>
            <Descriptions.Item label="优先级">{priorityTag(feedback.priority)}</Descriptions.Item>
            <Descriptions.Item label="负责人">{feedback.assignee || '-'}</Descriptions.Item>
            <Descriptions.Item label="复核结论">{feedback.reviewResult || '-'}</Descriptions.Item>
            <Descriptions.Item label="解决时间">{formatDate(feedback.resolvedAt)}</Descriptions.Item>
            <Descriptions.Item label="关闭时间">{formatDate(feedback.closedAt)}</Descriptions.Item>
            <Descriptions.Item label="重开次数">{feedback.reopenedCount || 0}</Descriptions.Item>
            <Descriptions.Item label="提交人">{feedback.createdBy || '-'}</Descriptions.Item>
          </Descriptions>
          <JsonBlock title="备注" value={feedback.comment} plain />
          <JsonBlock title="纠正答案" value={feedback.correctedAnswer} plain />

          <Divider orientation="left">处理动作</Divider>
          <Row gutter={[16, 16]}>
            <Col xs={24} lg={12}>
              <Form form={assignForm} layout="vertical" onFinish={onAssign}>
                <Form.Item label="负责人" name="assignee" rules={[{ required: true }]}>
                  <Input maxLength={128} />
                </Form.Item>
                <Form.Item label="操作者" name="operator">
                  <Input maxLength={128} />
                </Form.Item>
                <Form.Item label="备注" name="comment">
                  <Input.TextArea rows={2} maxLength={2000} />
                </Form.Item>
                <Button htmlType="submit" icon={<SaveOutlined />}>分派</Button>
              </Form>
            </Col>
            <Col xs={24} lg={12}>
              <Form form={statusForm} layout="vertical" onFinish={onStatus}>
                <Form.Item label="目标状态" name="status" rules={[{ required: true }]}>
                  <Select options={feedbackStatusOptions} />
                </Form.Item>
                <Form.Item label="操作者" name="operator">
                  <Input maxLength={128} />
                </Form.Item>
                <Form.Item label="备注" name="comment">
                  <Input.TextArea rows={2} maxLength={2000} />
                </Form.Item>
                <Form.Item label="已关联修订" name="linkedRevision" valuePropName="checked">
                  <Switch />
                </Form.Item>
                <Button htmlType="submit" icon={<SaveOutlined />}>变更状态</Button>
              </Form>
            </Col>
            <Col xs={24} lg={12}>
              <Form form={reviewForm} layout="vertical" onFinish={onReview}>
                <Form.Item label="复核结论" name="reviewResult" rules={[{ required: true }]}>
                  <Select options={['VALID', 'INVALID', 'DUPLICATE', 'NEEDS_MORE_INFO'].map((value) => ({ label: value, value }))} />
                </Form.Item>
                <Form.Item label="复核说明" name="reviewComment" rules={[{ required: true }]}>
                  <Input.TextArea rows={3} maxLength={2000} />
                </Form.Item>
                <Form.Item label="操作者" name="operator">
                  <Input maxLength={128} />
                </Form.Item>
                <Button htmlType="submit" icon={<SaveOutlined />}>保存复核</Button>
              </Form>
            </Col>
            <Col xs={24} lg={12}>
              <Form form={commentForm} layout="vertical" onFinish={onComment}>
                <Form.Item label="评论" name="comment" rules={[{ required: true }]}>
                  <Input.TextArea rows={3} maxLength={2000} />
                </Form.Item>
                <Form.Item label="操作者" name="operator">
                  <Input maxLength={128} />
                </Form.Item>
                <Space>
                  <Button htmlType="submit">追加评论</Button>
                  <Popconfirm title="确认关闭反馈？" onConfirm={() => onCloseFeedback(commentForm.getFieldsValue())}>
                    <Button icon={<CheckCircleOutlined />}>关闭</Button>
                  </Popconfirm>
                  <Popconfirm title="确认重开反馈？" onConfirm={() => onReopen(commentForm.getFieldsValue())}>
                    <Button icon={<UndoOutlined />}>重开</Button>
                  </Popconfirm>
                </Space>
              </Form>
            </Col>
          </Row>

          <Divider orientation="left">创建修订任务</Divider>
          <Form form={revisionForm} layout="vertical" onFinish={onCreateRevision}>
            <Row gutter={12}>
              <Col xs={24} md={8}>
                <Form.Item label="知识库 ID" name="knowledgeBaseId">
                  <InputNumber min={1} style={{ width: '100%' }} />
                </Form.Item>
              </Col>
              <Col xs={24} md={8}>
                <Form.Item label="文档 ID" name="documentId">
                  <InputNumber min={1} style={{ width: '100%' }} />
                </Form.Item>
              </Col>
              <Col xs={24} md={8}>
                <Form.Item label="Chunk UID" name="chunkUid">
                  <Input />
                </Form.Item>
              </Col>
              <Col xs={24} md={8}>
                <Form.Item label="修订类型" name="revisionType">
                  <Select options={revisionTypeOptions} />
                </Form.Item>
              </Col>
              <Col xs={24} md={8}>
                <Form.Item label="创建人" name="createdBy">
                  <Input />
                </Form.Item>
              </Col>
              <Col xs={24} md={8}>
                <Form.Item label="负责人" name="assignee">
                  <Input />
                </Form.Item>
              </Col>
              <Col span={24}>
                <Form.Item label="预期修复" name="expectedFix">
                  <Input.TextArea rows={2} maxLength={2000} />
                </Form.Item>
              </Col>
              <Col span={24}>
                <Form.Item label="验证查询" name="verificationQuery">
                  <Input.TextArea rows={2} />
                </Form.Item>
              </Col>
            </Row>
            <Button type="primary" htmlType="submit">创建修订任务</Button>
          </Form>

          <Divider orientation="left">事件轨迹</Divider>
          <Table<RagQueryFeedbackEvent>
            rowKey="id"
            loading={loading}
            dataSource={events}
            pagination={false}
            size="small"
            columns={[
              { title: '事件', dataIndex: 'eventType', width: 170 },
              { title: 'From', dataIndex: 'fromStatus', width: 120, render: (value?: string) => value || '-' },
              { title: 'To', dataIndex: 'toStatus', width: 120, render: (value?: string) => value || '-' },
              { title: '操作者', dataIndex: 'operator', width: 130, render: (value?: string) => value || '-' },
              { title: '备注', dataIndex: 'comment', ellipsis: true, render: (value?: string) => value || '-' },
              { title: '时间', dataIndex: 'createdAt', width: 180, render: formatDate }
            ]}
          />
        </Space>
      ) : null}
    </Drawer>
  );
}

function FeedbackDimensionTable({ rows, loading }: { rows: RagFeedbackDimensionItem[]; loading: boolean }) {
  return (
    <Table<RagFeedbackDimensionItem>
      rowKey={(record) => `${record.dimension}-${record.value}`}
      loading={loading}
      dataSource={rows}
      pagination={false}
      size="small"
      columns={[
        { title: '维度值', dataIndex: 'value', ellipsis: true },
        { title: '反馈数', dataIndex: 'feedbackCount', width: 90 },
        { title: '有帮助率', dataIndex: 'helpfulRate', width: 110, render: formatRate },
        { title: '修复率', dataIndex: 'verifiedFixRate', width: 100, render: formatRate },
        { title: '修订数', dataIndex: 'linkedRevisionCount', width: 90 }
      ]}
    />
  );
}

function MetricBox({
  title,
  value,
  loading,
  color,
  prefix
}: {
  title: string;
  value: string | number;
  loading?: boolean;
  color?: string;
  prefix?: string;
}) {
  return (
    <div style={{ border: '1px solid #eef0f3', borderRadius: 8, padding: 16, minHeight: 92, background: '#fff' }}>
      <Statistic title={title} value={value} loading={loading} valueStyle={{ color }} prefix={prefix} />
    </div>
  );
}

function JsonBlock({ title, value, plain }: { title: string; value?: string; plain?: boolean }) {
  if (!value) {
    return (
      <Space direction="vertical" size={4} style={{ width: '100%' }}>
        <Text strong>{title}</Text>
        <Text type="secondary">-</Text>
      </Space>
    );
  }
  return (
    <Space direction="vertical" size={4} style={{ width: '100%' }}>
      <Text strong>{title}</Text>
      <Paragraph copyable style={{ whiteSpace: 'pre-wrap', background: plain ? undefined : '#f8fafc', padding: plain ? undefined : 12, borderRadius: plain ? undefined : 6 }}>
        {prettyJson(value)}
      </Paragraph>
    </Space>
  );
}

function costDimensionColumns() {
  return [
    { title: '维度值', dataIndex: 'value', ellipsis: true },
    { title: '查询量', dataIndex: 'queryCount', width: 90 },
    { title: 'Token', dataIndex: 'totalTokens', width: 100 },
    { title: 'P90', dataIndex: 'p90LatencyMs', width: 90, render: (value: number) => `${Math.round(value || 0)} ms` },
    { title: '成本', dataIndex: 'estimatedTotalCost', width: 110, render: formatMoney }
  ];
}

function windowOptions() {
  return [
    { label: '小时', value: 'hour' },
    { label: '天', value: 'day' },
    { label: '周', value: 'week' },
    { label: '月', value: 'month' }
  ];
}

function dateRange(range?: [Dayjs, Dayjs]) {
  return {
    from: range?.[0]?.format('YYYY-MM-DDTHH:mm:ss'),
    to: range?.[1]?.format('YYYY-MM-DDTHH:mm:ss')
  };
}

function trimToUndefined(value?: string) {
  if (!value || !value.trim()) {
    return undefined;
  }
  return value.trim();
}

function emptyNumber(value?: number) {
  return value === undefined || value === null ? '-' : value;
}

function formatDate(value?: string) {
  if (!value) {
    return '-';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString();
}

function formatMoney(value?: number) {
  const amount = Number(value || 0);
  return `$${amount.toFixed(4)}`;
}

function formatRate(value?: number) {
  return `${ratePercent(value).toFixed(1)}%`;
}

function ratePercent(value?: number) {
  const rate = Number(value || 0);
  return Math.max(0, Math.min(100, rate <= 1 ? rate * 100 : rate));
}

function feedbackTag(value?: string) {
  if (!value) {
    return '-';
  }
  return <Tag color={feedbackColor[value] || 'default'}>{feedbackLabel[value] || value}</Tag>;
}

function workflowTag(value?: string) {
  if (!value) {
    return '-';
  }
  return <Tag color={workflowStatusColor[value] || 'default'}>{value}</Tag>;
}

function priorityTag(value?: string) {
  if (!value) {
    return '-';
  }
  return <Tag color={priorityColor[value] || 'default'}>{value}</Tag>;
}

function revisionStatusTag(value?: string) {
  if (!value) {
    return '-';
  }
  return <Tag color={revisionStatusColor[value] || 'default'}>{value}</Tag>;
}

function governanceTag(record: RagQueryLog) {
  if (record.deleted) {
    return <Tag color="red">DELETED</Tag>;
  }
  if (record.archiveStatus === 'ARCHIVED') {
    return <Tag color="purple">ARCHIVED</Tag>;
  }
  if (record.archiveStatus === 'DELETE_PENDING') {
    return <Tag color="orange">DELETE_PENDING</Tag>;
  }
  return <Tag color="success">ACTIVE</Tag>;
}

function yesNo(value?: boolean) {
  return value ? <Tag color="success">是</Tag> : <Tag>否</Tag>;
}

function prettyJson(value: string) {
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}
