import {
  BarChartOutlined,
  DeleteOutlined,
  EditOutlined,
  EyeOutlined,
  PlayCircleOutlined,
  ReloadOutlined,
  SaveOutlined,
  SearchOutlined
} from '@ant-design/icons';
import {
  App,
  Button,
  Card,
  Col,
  Descriptions,
  Drawer,
  Form,
  Input,
  InputNumber,
  Popconfirm,
  Row,
  Select,
  Space,
  Statistic,
  Switch,
  Table,
  Tabs,
  Tag,
  Typography
} from 'antd';
import type { Key } from 'react';
import { useEffect, useState } from 'react';
import { ragApi } from '../api/rag';
import PageHeaderCard from '../components/PageHeaderCard';
import type {
  KeywordIndexHealthResponse,
  KeywordIndexTemplatesResponse,
  KeywordReindexJobRequest,
  RagRerankCallLog,
  RagRerankObservationDimensionItem,
  RagRerankObservationSummary,
  RagRerankObservationTrendPoint,
  RagKeywordReindexJob,
  RagRetrievalEvalCase,
  RagRetrievalEvalCaseResult,
  RagRetrievalEvalRun,
  RagRetrievalEvaluationCaseUpsertRequest,
  RagRetrievalEvaluationReportItem,
  RagRetrievalEvaluationResponse,
  RagRetrievalEvaluationRunDetailResponse,
  RagRetrievalEvaluationSliceItem,
  RagRetrievalEvaluationTrendPoint,
  RagRetrievalFailureClusterItem
} from '../types';
import { getErrorMessage } from '../utils/message';

const { Text, Paragraph } = Typography;

interface FilterValues {
  tenantId?: number;
  knowledgeBaseId?: number;
  versionTag?: string;
  retrievalMode?: string;
}

interface CaseFormValues {
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
  contentTypes?: string;
  permissionTags?: string;
  expectedChunkIds: string;
  enabled?: boolean;
  metadataJson?: string;
}

interface ReindexFormValues extends KeywordReindexJobRequest {}

type KeywordReindexAction = 'run' | 'switch' | 'rollback' | 'cancel';

const modeOptions = [
  { label: 'hybrid', value: 'hybrid' },
  { label: 'vector', value: 'vector' },
  { label: 'keyword', value: 'keyword' }
];

export default function RetrievalEvaluationsPage() {
  const { message } = App.useApp();
  const [filterForm] = Form.useForm<FilterValues>();
  const [caseForm] = Form.useForm<CaseFormValues>();
  const [reindexForm] = Form.useForm<ReindexFormValues>();
  const [cases, setCases] = useState<RagRetrievalEvalCase[]>([]);
  const [runs, setRuns] = useState<RagRetrievalEvalRun[]>([]);
  const [report, setReport] = useState<RagRetrievalEvaluationReportItem[]>([]);
  const [rerankLogs, setRerankLogs] = useState<RagRerankCallLog[]>([]);
  const [rerankSummary, setRerankSummary] = useState<RagRerankObservationSummary>();
  const [keywordHealth, setKeywordHealth] = useState<KeywordIndexHealthResponse>();
  const [keywordTemplates, setKeywordTemplates] = useState<KeywordIndexTemplatesResponse>();
  const [keywordReindexJobs, setKeywordReindexJobs] = useState<RagKeywordReindexJob[]>([]);
  const [keywordReindexPlan, setKeywordReindexPlan] = useState<unknown>();
  const [evaluationTrends, setEvaluationTrends] = useState<RagRetrievalEvaluationTrendPoint[]>([]);
  const [evaluationSlices, setEvaluationSlices] = useState<RagRetrievalEvaluationSliceItem[]>([]);
  const [failureClusters, setFailureClusters] = useState<RagRetrievalFailureClusterItem[]>([]);
  const [rerankTrends, setRerankTrends] = useState<RagRerankObservationTrendPoint[]>([]);
  const [rerankErrorBreakdown, setRerankErrorBreakdown] = useState<RagRerankObservationDimensionItem[]>([]);
  const [rerankApiKeyBreakdown, setRerankApiKeyBreakdown] = useState<RagRerankObservationDimensionItem[]>([]);
  const [latestResult, setLatestResult] = useState<RagRetrievalEvaluationResponse>();
  const [runDetail, setRunDetail] = useState<RagRetrievalEvaluationRunDetailResponse>();
  const [selectedCaseIds, setSelectedCaseIds] = useState<Key[]>([]);
  const [casePagination, setCasePagination] = useState({ pageNo: 1, pageSize: 10, total: 0 });
  const [runPagination, setRunPagination] = useState({ pageNo: 1, pageSize: 10, total: 0 });
  const [rerankPagination, setRerankPagination] = useState({ pageNo: 1, pageSize: 10, total: 0 });
  const [caseDrawerOpen, setCaseDrawerOpen] = useState(false);
  const [detailOpen, setDetailOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [running, setRunning] = useState(false);
  const [rerankLoading, setRerankLoading] = useState(false);
  const [diagnosticsLoading, setDiagnosticsLoading] = useState(false);
  const [keywordReindexLoading, setKeywordReindexLoading] = useState(false);
  const [keywordReindexActing, setKeywordReindexActing] = useState<number>();

  const filterParams = () => {
    const values = filterForm.getFieldsValue();
    return {
      tenantId: values.tenantId,
      knowledgeBaseId: values.knowledgeBaseId,
      versionTag: trimToUndefined(values.versionTag),
      retrievalMode: trimToUndefined(values.retrievalMode)
    };
  };

  const loadCases = async (pageNo = casePagination.pageNo, pageSize = casePagination.pageSize) => {
    try {
      setLoading(true);
      const { data } = await ragApi.listRetrievalEvaluationCases({
        ...filterParams(),
        pageNo,
        pageSize
      });
      const page = data.data;
      setCases(page.records || []);
      setCasePagination({ pageNo: page.pageNo, pageSize: page.pageSize, total: page.total });
      setSelectedCaseIds([]);
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  const loadRuns = async (pageNo = runPagination.pageNo, pageSize = runPagination.pageSize) => {
    try {
      setLoading(true);
      const { data } = await ragApi.listRetrievalEvaluationRuns({
        ...filterParams(),
        pageNo,
        pageSize
      });
      const page = data.data;
      setRuns(page.records || []);
      setRunPagination({ pageNo: page.pageNo, pageSize: page.pageSize, total: page.total });
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  const loadReport = async () => {
    try {
      const { data } = await ragApi.getRetrievalEvaluationReport(filterParams());
      setReport(data.data || []);
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  };

  const loadRerank = async (pageNo = rerankPagination.pageNo, pageSize = rerankPagination.pageSize) => {
    try {
      setRerankLoading(true);
      const params = filterParams();
      const [summaryResponse, logsResponse] = await Promise.all([
        ragApi.getRerankObservationSummary({ tenantId: params.tenantId, provider: 'dashscope' }),
        ragApi.listRerankObservationLogs({ tenantId: params.tenantId, provider: 'dashscope', pageNo, pageSize })
      ]);
      setRerankSummary(summaryResponse.data.data);
      const page = logsResponse.data.data;
      setRerankLogs(page.records || []);
      setRerankPagination({ pageNo: page.pageNo, pageSize: page.pageSize, total: page.total });
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setRerankLoading(false);
    }
  };

  const loadDiagnostics = async () => {
    try {
      setDiagnosticsLoading(true);
      const params = filterParams();
      const [
        healthResponse,
        templatesResponse,
        trendsResponse,
        slicesResponse,
        clustersResponse,
        rerankTrendsResponse,
        rerankErrorsResponse,
        rerankKeysResponse
      ] = await Promise.all([
        ragApi.getKeywordIndexHealth(),
        ragApi.getKeywordIndexTemplates(),
        ragApi.getRetrievalEvaluationTrends({ ...params, window: 'day' }),
        ragApi.getRetrievalEvaluationSlices({ ...params, dimension: 'queryCategory' }),
        ragApi.getRetrievalFailureClusters(params),
        ragApi.getRerankObservationTrends({ tenantId: params.tenantId, provider: 'dashscope', window: 'day' }),
        ragApi.getRerankByErrorCode({ tenantId: params.tenantId, provider: 'dashscope' }),
        ragApi.getRerankByApiKey({ tenantId: params.tenantId, provider: 'dashscope' })
      ]);
      setKeywordHealth(healthResponse.data.data);
      setKeywordTemplates(templatesResponse.data.data);
      setEvaluationTrends(trendsResponse.data.data || []);
      setEvaluationSlices(slicesResponse.data.data || []);
      setFailureClusters(clustersResponse.data.data || []);
      setRerankTrends(rerankTrendsResponse.data.data || []);
      setRerankErrorBreakdown(rerankErrorsResponse.data.data || []);
      setRerankApiKeyBreakdown(rerankKeysResponse.data.data || []);
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setDiagnosticsLoading(false);
    }
  };

  const loadKeywordReindexJobs = async () => {
    try {
      setKeywordReindexLoading(true);
      const params = filterParams();
      const { data } = await ragApi.listKeywordReindexJobs({ tenantId: params.tenantId, limit: 20 });
      setKeywordReindexJobs(data.data || []);
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setKeywordReindexLoading(false);
    }
  };

  const createKeywordReindexJob = async (values: ReindexFormValues) => {
    try {
      setKeywordReindexLoading(true);
      const payload: KeywordReindexJobRequest = {
        tenantId: values.tenantId ?? filterParams().tenantId,
        sourceIndex: trimToUndefined(values.sourceIndex),
        targetIndex: trimToUndefined(values.targetIndex),
        aliasName: trimToUndefined(values.aliasName),
        templateVersion: trimToUndefined(values.templateVersion)
      };
      await ragApi.createKeywordReindexJob(payload);
      reindexForm.resetFields();
      await loadKeywordReindexJobs();
      message.success('Reindex job created');
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setKeywordReindexLoading(false);
    }
  };

  const previewKeywordReindexJob = async (jobId: number) => {
    try {
      setKeywordReindexActing(jobId);
      const { data } = await ragApi.previewKeywordReindexJob(jobId);
      setKeywordReindexPlan(data.data);
      message.success('Reindex plan loaded');
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setKeywordReindexActing(undefined);
    }
  };

  const runKeywordReindexAction = async (jobId: number, action: KeywordReindexAction) => {
    try {
      setKeywordReindexActing(jobId);
      if (action === 'run') {
        await ragApi.runKeywordReindexJob(jobId);
      } else if (action === 'switch') {
        await ragApi.switchKeywordReindexAlias(jobId);
      } else if (action === 'rollback') {
        await ragApi.rollbackKeywordReindexJob(jobId);
      } else {
        await ragApi.cancelKeywordReindexJob(jobId);
      }
      await loadKeywordReindexJobs();
      message.success(`Reindex ${action} completed`);
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setKeywordReindexActing(undefined);
    }
  };

  const refreshAll = async () => {
    await Promise.all([loadCases(1, casePagination.pageSize), loadRuns(1, runPagination.pageSize), loadReport()]);
    await loadRerank(1, rerankPagination.pageSize);
    await loadDiagnostics();
    await loadKeywordReindexJobs();
  };

  useEffect(() => {
    refreshAll();
  }, []);

  const openCreate = () => {
    caseForm.resetFields();
    caseForm.setFieldsValue({
      tenantId: 0,
      versionTag: 'default',
      retrievalMode: 'hybrid',
      queryCategory: 'definition',
      difficultyLevel: 'easy',
      language: 'mixed',
      expectedAnswerType: 'fact',
      topK: 6,
      enabled: true
    });
    setCaseDrawerOpen(true);
  };

  const openEdit = (record: RagRetrievalEvalCase) => {
    caseForm.setFieldsValue({
      id: record.id,
      tenantId: record.tenantId,
      knowledgeBaseId: record.knowledgeBaseId,
      versionTag: record.versionTag,
      caseId: record.caseId,
      query: record.queryText,
      retrievalMode: record.retrievalMode,
      queryCategory: record.queryCategory,
      difficultyLevel: record.difficultyLevel,
      language: record.language,
      expectedAnswerType: record.expectedAnswerType,
      topK: record.topK,
      minScore: record.minScore,
      contentTypes: parseJsonList(record.contentTypesJson).join(','),
      permissionTags: parseJsonList(record.permissionTagsJson).join(','),
      expectedChunkIds: parseJsonList(record.expectedChunkIdsJson).join(','),
      enabled: record.enabled,
      metadataJson: record.metadataJson
    });
    setCaseDrawerOpen(true);
  };

  const saveCase = async (values: CaseFormValues) => {
    const payload: RagRetrievalEvaluationCaseUpsertRequest = {
      id: values.id,
      tenantId: values.tenantId,
      knowledgeBaseId: values.knowledgeBaseId,
      versionTag: trimToUndefined(values.versionTag),
      caseId: trimToUndefined(values.caseId),
      query: values.query,
      retrievalMode: values.retrievalMode,
      queryCategory: trimToUndefined(values.queryCategory),
      difficultyLevel: trimToUndefined(values.difficultyLevel),
      language: trimToUndefined(values.language),
      expectedAnswerType: trimToUndefined(values.expectedAnswerType),
      topK: values.topK,
      minScore: values.minScore,
      contentTypes: splitList(values.contentTypes),
      permissionTags: splitList(values.permissionTags),
      expectedChunkIds: splitList(values.expectedChunkIds),
      enabled: values.enabled,
      metadataJson: trimToUndefined(values.metadataJson)
    };
    try {
      setSaving(true);
      await ragApi.saveRetrievalEvaluationCase(payload);
      setCaseDrawerOpen(false);
      await loadCases(1, casePagination.pageSize);
      message.success('评估用例已保存');
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setSaving(false);
    }
  };

  const deleteCase = async (record: RagRetrievalEvalCase) => {
    try {
      setLoading(true);
      await ragApi.deleteRetrievalEvaluationCase(record.id);
      await loadCases(casePagination.pageNo, casePagination.pageSize);
      message.success('评估用例已删除');
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  const runEvaluation = async (selectedOnly = false) => {
    try {
      setRunning(true);
      const params = filterParams();
      const { data } = await ragApi.runRetrievalEvaluation({
        ...params,
        caseIds: selectedOnly ? selectedCaseIds.map((key) => Number(key)).filter(Number.isFinite) : undefined
      });
      setLatestResult(data.data);
      await Promise.all([loadRuns(1, runPagination.pageSize), loadReport()]);
      message.success('评估运行完成');
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setRunning(false);
    }
  };

  const openRunDetail = async (record: RagRetrievalEvalRun) => {
    try {
      setLoading(true);
      const { data } = await ragApi.getRetrievalEvaluationRun(record.id);
      setRunDetail(data.data);
      setDetailOpen(true);
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  const onSearch = async () => {
    await refreshAll();
  };

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <PageHeaderCard
        title="检索评估"
        description="管理检索评估集，运行评估历史，按知识库和版本对比指标，并观察 DashScope Rerank 调用质量。"
        tags={['BM25', 'Evaluation', 'Rerank Observability']}
        extra={
          <Space>
            <Button icon={<ReloadOutlined />} onClick={refreshAll} loading={loading || rerankLoading} />
            <Button type="primary" icon={<PlayCircleOutlined />} onClick={() => runEvaluation(false)} loading={running}>
              运行评估
            </Button>
          </Space>
        }
      />

      <Card className="page-card" variant="borderless">
        <Form form={filterForm} layout="inline" initialValues={{ tenantId: 0, retrievalMode: 'hybrid' }} onFinish={onSearch}>
          <Form.Item label="租户" name="tenantId">
            <InputNumber min={0} style={{ width: 110 }} />
          </Form.Item>
          <Form.Item label="知识库" name="knowledgeBaseId">
            <InputNumber min={0} style={{ width: 120 }} />
          </Form.Item>
          <Form.Item label="版本" name="versionTag">
            <Input allowClear placeholder="default / v1" style={{ width: 150 }} />
          </Form.Item>
          <Form.Item label="模式" name="retrievalMode">
            <Select allowClear options={modeOptions} style={{ width: 130 }} />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" icon={<SearchOutlined />} loading={loading}>
                查询
              </Button>
              <Button icon={<ReloadOutlined />} onClick={refreshAll} loading={loading || rerankLoading} />
            </Space>
          </Form.Item>
        </Form>
      </Card>

      <Tabs
        items={[
          {
            key: 'cases',
            label: '评估集',
            children: (
              <Space direction="vertical" size={16} style={{ width: '100%' }}>
                {latestResult ? <LatestResultCard result={latestResult} /> : null}
                <Card
                  className="page-card"
                  variant="borderless"
                  title="评估用例"
                  extra={
                    <Space>
                      <Button onClick={openCreate} icon={<EditOutlined />}>
                        新增用例
                      </Button>
                      <Button
                        icon={<PlayCircleOutlined />}
                        disabled={selectedCaseIds.length === 0}
                        loading={running}
                        onClick={() => runEvaluation(true)}
                      >
                        运行选中
                      </Button>
                    </Space>
                  }
                >
                  <Table<RagRetrievalEvalCase>
                    rowKey="id"
                    loading={loading}
                    dataSource={cases}
                    rowSelection={{ selectedRowKeys: selectedCaseIds, onChange: setSelectedCaseIds }}
                    pagination={{
                      current: casePagination.pageNo,
                      pageSize: casePagination.pageSize,
                      total: casePagination.total,
                      showSizeChanger: true
                    }}
                    onChange={(pagination) => loadCases(pagination.current || 1, pagination.pageSize || casePagination.pageSize)}
                    columns={[
                      { title: 'ID', dataIndex: 'id', width: 80 },
                      { title: 'Case ID', dataIndex: 'caseId', width: 180, ellipsis: true },
                      { title: '知识库', dataIndex: 'knowledgeBaseId', width: 100, render: emptyText },
                      { title: '版本', dataIndex: 'versionTag', width: 120 },
                      { title: '模式', dataIndex: 'retrievalMode', width: 100 },
                      { title: 'Category', dataIndex: 'queryCategory', width: 130, render: emptyText },
                      { title: 'Lang', dataIndex: 'language', width: 90, render: emptyText },
                      { title: 'Difficulty', dataIndex: 'difficultyLevel', width: 110, render: emptyText },
                      { title: '问题', dataIndex: 'queryText', ellipsis: true },
                      {
                        title: '期望 Chunk',
                        dataIndex: 'expectedChunkIdsJson',
                        width: 180,
                        render: (value: string) => parseJsonList(value).join(', ')
                      },
                      {
                        title: '状态',
                        dataIndex: 'enabled',
                        width: 90,
                        render: (value: boolean) => <Tag color={value ? 'success' : 'default'}>{value ? '启用' : '停用'}</Tag>
                      },
                      { title: '更新时间', dataIndex: 'updatedAt', width: 180, render: formatDate },
                      {
                        title: '操作',
                        width: 150,
                        fixed: 'right',
                        render: (_, record) => (
                          <Space>
                            <Button size="small" icon={<EditOutlined />} onClick={() => openEdit(record)} />
                            <Popconfirm title="确认删除该评估用例？" onConfirm={() => deleteCase(record)}>
                              <Button danger size="small" icon={<DeleteOutlined />} />
                            </Popconfirm>
                          </Space>
                        )
                      }
                    ]}
                    scroll={{ x: 1200 }}
                  />
                </Card>
              </Space>
            )
          },
          {
            key: 'runs',
            label: '历史记录',
            children: (
              <Card className="page-card" variant="borderless" title="评估历史">
                <Table<RagRetrievalEvalRun>
                  rowKey="id"
                  loading={loading}
                  dataSource={runs}
                  pagination={{
                    current: runPagination.pageNo,
                    pageSize: runPagination.pageSize,
                    total: runPagination.total,
                    showSizeChanger: true
                  }}
                  onChange={(pagination) => loadRuns(pagination.current || 1, pagination.pageSize || runPagination.pageSize)}
                  columns={[
                    { title: 'Run No', dataIndex: 'runNo', width: 230, ellipsis: true },
                    { title: '知识库', dataIndex: 'knowledgeBaseId', width: 100, render: emptyText },
                    { title: '版本', dataIndex: 'versionTag', width: 120 },
                    { title: '模式', dataIndex: 'retrievalMode', width: 100 },
                    { title: '用例数', dataIndex: 'totalCases', width: 90 },
                    { title: 'Hit Rate', dataIndex: 'hitRate', width: 110, render: percent },
                    { title: 'MRR', dataIndex: 'meanReciprocalRank', width: 100, render: decimal },
                    { title: 'Recall', dataIndex: 'meanRecall', width: 100, render: percent },
                    { title: '来源', dataIndex: 'source', width: 90 },
                    { title: '时间', dataIndex: 'createdAt', width: 180, render: formatDate },
                    {
                      title: '详情',
                      width: 90,
                      render: (_, record) => (
                        <Button size="small" icon={<EyeOutlined />} onClick={() => openRunDetail(record)} />
                      )
                    }
                  ]}
                  scroll={{ x: 1200 }}
                />
              </Card>
            )
          },
          {
            key: 'report',
            label: '对比报表',
            children: (
              <Card className="page-card" variant="borderless" title="知识库 / 版本对比">
                <Table<RagRetrievalEvaluationReportItem>
                  rowKey={(record) => `${record.knowledgeBaseId || 0}-${record.versionTag}-${record.retrievalMode}`}
                  dataSource={report}
                  pagination={false}
                  columns={[
                    { title: '知识库', dataIndex: 'knowledgeBaseId', width: 100, render: emptyText },
                    { title: '版本', dataIndex: 'versionTag', width: 140 },
                    { title: '模式', dataIndex: 'retrievalMode', width: 100 },
                    { title: '运行次数', dataIndex: 'runCount', width: 100 },
                    { title: '累计用例', dataIndex: 'totalCases', width: 100 },
                    { title: '平均 Hit Rate', dataIndex: 'avgHitRate', width: 140, render: percent },
                    { title: '平均 MRR', dataIndex: 'avgMeanReciprocalRank', width: 120, render: decimal },
                    { title: '平均 Recall', dataIndex: 'avgMeanRecall', width: 130, render: percent },
                    { title: '最近运行', dataIndex: 'latestRunNo', ellipsis: true },
                    { title: '最近时间', dataIndex: 'latestCreatedAt', width: 180, render: formatDate }
                  ]}
                  scroll={{ x: 1100 }}
                />
              </Card>
            )
          },
          {
            key: 'diagnostics',
            label: 'Diagnostics',
            children: (
              <Space direction="vertical" size={16} style={{ width: '100%' }}>
                <Card
                  className="page-card"
                  variant="borderless"
                  title="Keyword index"
                  extra={<Button icon={<ReloadOutlined />} onClick={loadDiagnostics} loading={diagnosticsLoading} />}
                >
                  <Descriptions column={{ xs: 1, md: 3 }} size="small" bordered>
                    <Descriptions.Item label="Status">{keywordHealth?.status || '-'}</Descriptions.Item>
                    <Descriptions.Item label="Provider">{keywordHealth?.provider || '-'}</Descriptions.Item>
                    <Descriptions.Item label="Engine">{keywordHealth?.engineCompatible || '-'}</Descriptions.Item>
                    <Descriptions.Item label="Index">{keywordHealth?.indexName || '-'}</Descriptions.Item>
                    <Descriptions.Item label="Alias">{keywordHealth?.indexAlias || '-'}</Descriptions.Item>
                    <Descriptions.Item label="Profile">{keywordHealth?.analyzerProfile || '-'}</Descriptions.Item>
                    <Descriptions.Item label="Template">{keywordTemplates?.current?.templateName || '-'}</Descriptions.Item>
                    <Descriptions.Item label="Version">{keywordHealth?.indexVersion || '-'}</Descriptions.Item>
                    <Descriptions.Item label="Managed">{keywordHealth?.templateManaged ? 'true' : 'false'}</Descriptions.Item>
                  </Descriptions>
                </Card>

                <Card
                  className="page-card"
                  variant="borderless"
                  title="Keyword reindex jobs"
                  extra={
                    <Button icon={<ReloadOutlined />} onClick={loadKeywordReindexJobs} loading={keywordReindexLoading} />
                  }
                >
                  <Space direction="vertical" size={16} style={{ width: '100%' }}>
                    <Form form={reindexForm} layout="inline" onFinish={createKeywordReindexJob} style={{ rowGap: 12 }}>
                      <Form.Item label="Tenant" name="tenantId">
                        <InputNumber min={0} style={{ width: 110 }} />
                      </Form.Item>
                      <Form.Item label="Source" name="sourceIndex">
                        <Input allowClear placeholder="current index" style={{ width: 180 }} />
                      </Form.Item>
                      <Form.Item label="Target" name="targetIndex">
                        <Input allowClear placeholder="new index" style={{ width: 180 }} />
                      </Form.Item>
                      <Form.Item label="Alias" name="aliasName">
                        <Input allowClear placeholder="search alias" style={{ width: 180 }} />
                      </Form.Item>
                      <Form.Item label="Template" name="templateVersion">
                        <Input allowClear placeholder="optional" style={{ width: 140 }} />
                      </Form.Item>
                      <Form.Item>
                        <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={keywordReindexLoading}>
                          Create job
                        </Button>
                      </Form.Item>
                    </Form>

                    <Table<RagKeywordReindexJob>
                      rowKey="id"
                      loading={keywordReindexLoading}
                      dataSource={keywordReindexJobs}
                      pagination={{ pageSize: 8 }}
                      columns={[
                        { title: 'Job', dataIndex: 'jobNo', width: 190, ellipsis: true },
                        {
                          title: 'Status',
                          dataIndex: 'jobStatus',
                          width: 150,
                          render: (value: string) => <Tag color={keywordJobStatusColor(value)}>{value || '-'}</Tag>
                        },
                        { title: 'Target', dataIndex: 'targetIndex', width: 210, ellipsis: true },
                        { title: 'Alias', dataIndex: 'aliasName', width: 180, ellipsis: true },
                        {
                          title: 'Progress',
                          dataIndex: 'progress',
                          width: 110,
                          render: (value?: number) => `${Math.round(Number(value || 0))}%`
                        },
                        { title: 'Total', dataIndex: 'totalCount', width: 90, render: emptyText },
                        { title: 'Success', dataIndex: 'successCount', width: 90, render: emptyText },
                        { title: 'Failed', dataIndex: 'failedCount', width: 90, render: emptyText },
                        {
                          title: 'Validation',
                          dataIndex: 'sampleValidationJson',
                          width: 220,
                          ellipsis: true,
                          render: (value?: string) => value || '-'
                        },
                        { title: 'Updated', dataIndex: 'updatedAt', width: 180, render: formatDate },
                        {
                          title: 'Actions',
                          fixed: 'right',
                          width: 340,
                          render: (_, record) => (
                            <Space>
                              <Button
                                size="small"
                                onClick={() => previewKeywordReindexJob(record.id)}
                                loading={keywordReindexActing === record.id}
                              >
                                Preview
                              </Button>
                              <Button
                                size="small"
                                icon={<PlayCircleOutlined />}
                                onClick={() => runKeywordReindexAction(record.id, 'run')}
                                loading={keywordReindexActing === record.id}
                              >
                                Run
                              </Button>
                              <Popconfirm title="Switch keyword alias to this target index?" onConfirm={() => runKeywordReindexAction(record.id, 'switch')}>
                                <Button size="small" type="primary" loading={keywordReindexActing === record.id}>
                                  Switch
                                </Button>
                              </Popconfirm>
                              <Popconfirm title="Rollback keyword alias to previous index?" onConfirm={() => runKeywordReindexAction(record.id, 'rollback')}>
                                <Button size="small" loading={keywordReindexActing === record.id}>
                                  Rollback
                                </Button>
                              </Popconfirm>
                              <Button
                                size="small"
                                danger
                                onClick={() => runKeywordReindexAction(record.id, 'cancel')}
                                loading={keywordReindexActing === record.id}
                              >
                                Cancel
                              </Button>
                            </Space>
                          )
                        }
                      ]}
                      scroll={{ x: 1700 }}
                    />

                    {keywordReindexPlan ? <JsonPreview title="Latest reindex plan" value={keywordReindexPlan} /> : null}
                  </Space>
                </Card>

                <Row gutter={[16, 16]}>
                  <Col xs={24} lg={12}>
                    <Card className="page-card" variant="borderless" title="Evaluation trends">
                      <Table<RagRetrievalEvaluationTrendPoint>
                        rowKey={(record) =>
                          `${record.bucket}-${record.retrievalMode}-${record.queryCategory}-${record.language}-${record.difficultyLevel}`
                        }
                        loading={diagnosticsLoading}
                        dataSource={evaluationTrends}
                        pagination={{ pageSize: 8 }}
                        columns={[
                          { title: 'Bucket', dataIndex: 'bucket', width: 170, render: formatDate },
                          { title: 'Mode', dataIndex: 'retrievalMode', width: 90 },
                          { title: 'Category', dataIndex: 'queryCategory', width: 130 },
                          { title: 'Cases', dataIndex: 'totalCases', width: 80 },
                          { title: 'Hit', dataIndex: 'hitRate', width: 90, render: percent },
                          { title: 'MRR', dataIndex: 'meanReciprocalRank', width: 90, render: decimal },
                          { title: 'Recall', dataIndex: 'meanRecall', width: 90, render: percent },
                          { title: 'Failure', dataIndex: 'failureRate', width: 90, render: percent }
                        ]}
                        scroll={{ x: 900 }}
                      />
                    </Card>
                  </Col>
                  <Col xs={24} lg={12}>
                    <Card className="page-card" variant="borderless" title="Evaluation slices">
                      <Table<RagRetrievalEvaluationSliceItem>
                        rowKey={(record) => `${record.dimension}-${record.value}`}
                        loading={diagnosticsLoading}
                        dataSource={evaluationSlices}
                        pagination={{ pageSize: 8 }}
                        columns={[
                          { title: 'Dimension', dataIndex: 'dimension', width: 120 },
                          { title: 'Value', dataIndex: 'value', ellipsis: true },
                          { title: 'Cases', dataIndex: 'totalCases', width: 80 },
                          { title: 'Hit', dataIndex: 'hitRate', width: 90, render: percent },
                          { title: 'Recall', dataIndex: 'meanRecall', width: 90, render: percent },
                          { title: 'Failure', dataIndex: 'failureRate', width: 90, render: percent }
                        ]}
                        scroll={{ x: 760 }}
                      />
                    </Card>
                  </Col>
                </Row>

                <Card className="page-card" variant="borderless" title="Failure clusters">
                  <Table<RagRetrievalFailureClusterItem>
                    rowKey="clusterKey"
                    loading={diagnosticsLoading}
                    dataSource={failureClusters}
                    pagination={{ pageSize: 8 }}
                    columns={[
                      { title: 'Cluster', dataIndex: 'clusterLabel', width: 220, ellipsis: true },
                      { title: 'Type', dataIndex: 'failureType', width: 150 },
                      { title: 'Cases', dataIndex: 'caseCount', width: 90 },
                      {
                        title: 'Samples',
                        dataIndex: 'sampleCaseIds',
                        width: 220,
                        render: (value: string[]) => (value || []).join(', ')
                      },
                      { title: 'Suggestion', dataIndex: 'suggestion', ellipsis: true }
                    ]}
                    scroll={{ x: 1000 }}
                  />
                </Card>

                <Row gutter={[16, 16]}>
                  <Col xs={24} lg={12}>
                    <Card className="page-card" variant="borderless" title="Rerank trends">
                      <Table<RagRerankObservationTrendPoint>
                        rowKey={(record) => `${record.bucket}-${record.provider}-${record.model}-${record.tenantId || 0}`}
                        loading={diagnosticsLoading}
                        dataSource={rerankTrends}
                        pagination={{ pageSize: 8 }}
                        columns={[
                          { title: 'Bucket', dataIndex: 'bucket', width: 170, render: formatDate },
                          { title: 'Requests', dataIndex: 'requestCount', width: 100 },
                          { title: 'Failure', dataIndex: 'failureRate', width: 90, render: percent },
                          { title: 'Fallback', dataIndex: 'fallbackRate', width: 90, render: percent },
                          { title: 'P90', dataIndex: 'p90LatencyMs', width: 90, render: (value: number) => `${value || 0} ms` },
                          { title: 'Cost', dataIndex: 'estimatedCost', width: 110, render: money }
                        ]}
                        scroll={{ x: 760 }}
                      />
                    </Card>
                  </Col>
                  <Col xs={24} lg={12}>
                    <Card className="page-card" variant="borderless" title="Rerank errors">
                      <Table<RagRerankObservationDimensionItem>
                        rowKey={(record) => `${record.dimension}-${record.value}`}
                        loading={diagnosticsLoading}
                        dataSource={rerankErrorBreakdown}
                        pagination={{ pageSize: 8 }}
                        columns={[
                          { title: 'Error', dataIndex: 'value', ellipsis: true },
                          { title: 'Requests', dataIndex: 'requestCount', width: 100 },
                          { title: 'Failure', dataIndex: 'failureRate', width: 90, render: percent },
                          { title: 'Fallback', dataIndex: 'fallbackRate', width: 90, render: percent },
                          { title: 'P99', dataIndex: 'p99LatencyMs', width: 90, render: (value: number) => `${value || 0} ms` }
                        ]}
                        scroll={{ x: 680 }}
                      />
                    </Card>
                  </Col>
                </Row>

                <Card className="page-card" variant="borderless" title="Rerank API keys">
                  <Table<RagRerankObservationDimensionItem>
                    rowKey={(record) => `${record.dimension}-${record.value}`}
                    loading={diagnosticsLoading}
                    dataSource={rerankApiKeyBreakdown}
                    pagination={{ pageSize: 8 }}
                    columns={[
                      { title: 'API key hash', dataIndex: 'value', width: 180, ellipsis: true },
                      { title: 'Requests', dataIndex: 'requestCount', width: 100 },
                      { title: 'Failure', dataIndex: 'failureRate', width: 90, render: percent },
                      { title: 'Fallback', dataIndex: 'fallbackRate', width: 90, render: percent },
                      { title: 'Tokens', dataIndex: 'totalTokens', width: 100 },
                      { title: 'Cost', dataIndex: 'estimatedCost', width: 120, render: money }
                    ]}
                    scroll={{ x: 760 }}
                  />
                </Card>
              </Space>
            )
          },
          {
            key: 'rerank',
            label: 'Rerank 观测',
            children: (
              <Space direction="vertical" size={16} style={{ width: '100%' }}>
                <Row gutter={[16, 16]}>
                  <Col xs={24} md={6}>
                    <Card>
                      <Statistic title="请求数" value={rerankSummary?.totalRequests || 0} loading={rerankLoading} />
                    </Card>
                  </Col>
                  <Col xs={24} md={6}>
                    <Card>
                      <Statistic title="失败率" value={percentValue(rerankSummary?.failureRate)} suffix="%" loading={rerankLoading} />
                    </Card>
                  </Col>
                  <Col xs={24} md={6}>
                    <Card>
                      <Statistic title="降级次数" value={rerankSummary?.degradedCount || 0} loading={rerankLoading} />
                    </Card>
                  </Col>
                  <Col xs={24} md={6}>
                    <Card>
                      <Statistic
                        title="估算费用"
                        value={Number(rerankSummary?.estimatedCost || 0)}
                        precision={6}
                        prefix="¥"
                        loading={rerankLoading}
                      />
                    </Card>
                  </Col>
                </Row>
                <Card
                  className="page-card"
                  variant="borderless"
                  title={
                    <Space>
                      <BarChartOutlined />
                      <span>DashScope Rerank 调用日志</span>
                    </Space>
                  }
                  extra={<Button icon={<ReloadOutlined />} onClick={() => loadRerank(1, rerankPagination.pageSize)} />}
                >
                  <Table<RagRerankCallLog>
                    rowKey="id"
                    loading={rerankLoading}
                    dataSource={rerankLogs}
                    pagination={{
                      current: rerankPagination.pageNo,
                      pageSize: rerankPagination.pageSize,
                      total: rerankPagination.total,
                      showSizeChanger: true
                    }}
                    onChange={(pagination) =>
                      loadRerank(pagination.current || 1, pagination.pageSize || rerankPagination.pageSize)
                    }
                    columns={[
                      { title: 'ID', dataIndex: 'id', width: 80 },
                      { title: '模型', dataIndex: 'model', width: 150 },
                      { title: 'API key', dataIndex: 'apiKeyHash', width: 140, ellipsis: true, render: emptyText },
                      { title: '候选', dataIndex: 'candidateCount', width: 80 },
                      { title: 'TopK', dataIndex: 'topK', width: 80 },
                      { title: '耗时', dataIndex: 'latencyMs', width: 100, render: (value: number) => `${value || 0} ms` },
                      { title: 'Tokens', dataIndex: 'totalTokens', width: 100 },
                      { title: '费用', dataIndex: 'estimatedCost', width: 120, render: money },
                      {
                        title: '状态',
                        dataIndex: 'success',
                        width: 90,
                        render: (value: boolean) => <Tag color={value ? 'success' : 'error'}>{value ? '成功' : '失败'}</Tag>
                      },
                      {
                        title: '降级',
                        dataIndex: 'fallback',
                        width: 90,
                        render: (value: boolean) => <Tag color={value ? 'warning' : 'default'}>{value ? '是' : '否'}</Tag>
                      },
                      { title: 'HTTP', dataIndex: 'httpStatus', width: 90, render: emptyText },
                      { title: '错误', dataIndex: 'errorCodeNormalized', width: 150, render: emptyText },
                      { title: '降级原因', dataIndex: 'degradedReason', width: 130, render: emptyText },
                      { title: '时间', dataIndex: 'createdAt', width: 180, render: formatDate }
                    ]}
                    scroll={{ x: 1350 }}
                  />
                </Card>
              </Space>
            )
          }
        ]}
      />

      <Drawer
        title="评估用例"
        width={560}
        open={caseDrawerOpen}
        onClose={() => setCaseDrawerOpen(false)}
        destroyOnClose
      >
        <Form form={caseForm} layout="vertical" onFinish={saveCase}>
          <Form.Item name="id" hidden>
            <Input />
          </Form.Item>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item label="租户" name="tenantId" rules={[{ required: true }]}>
                <InputNumber min={0} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="知识库" name="knowledgeBaseId">
                <InputNumber min={0} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item label="版本标签" name="versionTag">
                <Input placeholder="default / v1" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="检索模式" name="retrievalMode">
                <Select options={modeOptions} />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item label="Query category" name="queryCategory">
                <Select
                  options={[
                    { label: 'definition', value: 'definition' },
                    { label: 'how_to', value: 'how_to' },
                    { label: 'comparison', value: 'comparison' },
                    { label: 'numeric', value: 'numeric' },
                    { label: 'troubleshooting', value: 'troubleshooting' },
                    { label: 'multimodal', value: 'multimodal' }
                  ]}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="Difficulty" name="difficultyLevel">
                <Select
                  options={[
                    { label: 'easy', value: 'easy' },
                    { label: 'medium', value: 'medium' },
                    { label: 'hard', value: 'hard' }
                  ]}
                />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item label="Language" name="language">
                <Select
                  options={[
                    { label: 'zh', value: 'zh' },
                    { label: 'en', value: 'en' },
                    { label: 'mixed', value: 'mixed' }
                  ]}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="Answer type" name="expectedAnswerType">
                <Select
                  options={[
                    { label: 'fact', value: 'fact' },
                    { label: 'list', value: 'list' },
                    { label: 'step', value: 'step' },
                    { label: 'table', value: 'table' },
                    { label: 'image', value: 'image' },
                    { label: 'code', value: 'code' }
                  ]}
                />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item label="Case ID" name="caseId">
            <Input placeholder="留空自动生成" />
          </Form.Item>
          <Form.Item label="问题" name="query" rules={[{ required: true, message: '请输入问题' }]}>
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item label="期望 Chunk ID" name="expectedChunkIds" rules={[{ required: true, message: '请输入期望 Chunk ID' }]}>
            <Input.TextArea rows={3} placeholder="多个值用逗号或换行分隔" />
          </Form.Item>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item label="Top K" name="topK">
                <InputNumber min={1} max={50} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="最小分数" name="minScore">
                <InputNumber min={0} max={100} step={0.01} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item label="内容类型" name="contentTypes">
            <Input placeholder="text,image" />
          </Form.Item>
          <Form.Item label="权限标签" name="permissionTags">
            <Input placeholder="internal,finance" />
          </Form.Item>
          <Form.Item label="元数据 JSON" name="metadataJson">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item label="启用" name="enabled" valuePropName="checked">
            <Switch />
          </Form.Item>
          <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={saving}>
            保存
          </Button>
        </Form>
      </Drawer>

      <Drawer title="评估运行详情" width={720} open={detailOpen} onClose={() => setDetailOpen(false)}>
        {runDetail ? (
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            <LatestResultCard
              result={{
                runId: runDetail.run.id,
                runNo: runDetail.run.runNo,
                knowledgeBaseId: runDetail.run.knowledgeBaseId,
                versionTag: runDetail.run.versionTag,
                retrievalMode: runDetail.run.retrievalMode,
                totalCases: runDetail.run.totalCases,
                hitRate: runDetail.run.hitRate,
                meanReciprocalRank: runDetail.run.meanReciprocalRank,
                meanRecall: runDetail.run.meanRecall,
                results: []
              }}
            />
            <Table<RagRetrievalEvalCaseResult>
              rowKey="id"
              dataSource={runDetail.results}
              pagination={false}
              columns={[
                { title: 'Case ID', dataIndex: 'caseId', width: 150, ellipsis: true },
                { title: '问题', dataIndex: 'queryText', ellipsis: true },
                {
                  title: '命中',
                  dataIndex: 'hit',
                  width: 80,
                  render: (value: boolean) => <Tag color={value ? 'success' : 'error'}>{value ? '是' : '否'}</Tag>
                },
                { title: 'MRR', dataIndex: 'reciprocalRank', width: 90, render: decimal },
                { title: 'Recall', dataIndex: 'recall', width: 90, render: percent },
                { title: 'Failure', dataIndex: 'failureType', width: 140, render: emptyText },
                { title: 'Cluster', dataIndex: 'clusterKey', width: 220, ellipsis: true, render: emptyText },
                { title: '召回 Chunk', dataIndex: 'retrievedChunkIdsJson', width: 220, render: (value: string) => parseJsonList(value).join(', ') }
              ]}
              scroll={{ x: 1200 }}
            />
          </Space>
        ) : null}
      </Drawer>
    </Space>
  );
}

function JsonPreview({ title, value }: { title: string; value: unknown }) {
  return (
    <Space direction="vertical" size={4} style={{ width: '100%' }}>
      <Text strong>{title}</Text>
      <Paragraph copyable style={{ whiteSpace: 'pre-wrap', background: '#f8fafc', padding: 12, borderRadius: 6 }}>
        {JSON.stringify(value, null, 2)}
      </Paragraph>
    </Space>
  );
}

function LatestResultCard({ result }: { result: RagRetrievalEvaluationResponse }) {
  return (
    <Card className="page-card" variant="borderless">
      <Row gutter={[16, 16]}>
        <Col xs={24} md={6}>
          <Statistic title="用例数" value={result.totalCases} />
        </Col>
        <Col xs={24} md={6}>
          <Statistic title="Hit Rate" value={percentValue(result.hitRate)} suffix="%" />
        </Col>
        <Col xs={24} md={6}>
          <Statistic title="MRR" value={result.meanReciprocalRank} precision={3} />
        </Col>
        <Col xs={24} md={6}>
          <Statistic title="Recall" value={percentValue(result.meanRecall)} suffix="%" />
        </Col>
      </Row>
      {result.runNo ? (
        <Paragraph type="secondary" style={{ margin: '12px 0 0' }}>
          Run: <Text code>{result.runNo}</Text>
        </Paragraph>
      ) : null}
    </Card>
  );
}

function splitList(value?: string) {
  if (!value) return [];
  return value
    .split(/[\n,，]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function parseJsonList(value?: string) {
  if (!value) return [];
  try {
    const parsed = JSON.parse(value);
    return Array.isArray(parsed) ? parsed.map(String) : [];
  } catch {
    return [];
  }
}

function trimToUndefined(value?: string) {
  const trimmed = value?.trim();
  return trimmed ? trimmed : undefined;
}

function percent(value?: number) {
  return `${percentValue(value).toFixed(1)}%`;
}

function percentValue(value?: number) {
  return Number(((value || 0) * 100).toFixed(2));
}

function decimal(value?: number) {
  return Number(value || 0).toFixed(3);
}

function money(value?: number) {
  return `¥${Number(value || 0).toFixed(6)}`;
}

function emptyText(value?: number | string) {
  return value === undefined || value === null || value === '' ? '-' : value;
}

function keywordJobStatusColor(value?: string) {
  if (!value) return 'default';
  if (value === 'SUCCEEDED' || value === 'READY_TO_SWITCH' || value === 'ROLLED_BACK') return 'success';
  if (value === 'FAILED') return 'error';
  if (value === 'BACKFILLING' || value === 'VALIDATING' || value === 'SWITCHING_ALIAS' || value === 'ROLLING_BACK') {
    return 'processing';
  }
  return 'default';
}

function formatDate(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-';
}
