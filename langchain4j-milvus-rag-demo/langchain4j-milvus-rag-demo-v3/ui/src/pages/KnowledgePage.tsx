import {
  DeleteOutlined,
  DownloadOutlined,
  InboxOutlined,
  ReloadOutlined,
  RetweetOutlined,
  StopOutlined,
  UploadOutlined
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
  Popconfirm,
  Progress,
  Row,
  Select,
  Space,
  Table,
  Tabs,
  Timeline,
  Tag,
  Typography,
  Upload
} from 'antd';
import { useEffect, useMemo, useState } from 'react';
import PageHeaderCard from '../components/PageHeaderCard';
import { ragApi } from '../api/rag';
import type {
  KnowledgeChunkRecord,
  IngestionTaskEventView,
  IngestionTaskProgressSnapshot,
  RagDocument,
  RagDocumentVersion,
  RagIngestionTask,
  RagIngestionTaskShard,
  RagKnowledgeBase
} from '../types';
import { getErrorMessage } from '../utils/message';

const { Dragger } = Upload;
const { TextArea } = Input;
const { Paragraph, Text } = Typography;

const taskStatusText: Record<number, string> = {
  0: 'PENDING',
  1: 'RUNNING',
  2: 'SUCCESS',
  3: 'FAILED',
  4: 'RETRY_WAIT',
  5: 'CANCELLED',
  6: 'PARTIAL_SUCCESS'
};

const stageStatusColors: Record<string, string> = {
  PENDING: 'default',
  RUNNING: 'processing',
  SUCCESS: 'success',
  FAILED: 'error',
  CANCELLED: 'default',
  SKIPPED: 'warning'
};

const shardStatusColors: Record<string, string> = {
  PENDING: 'default',
  RUNNING: 'processing',
  SUCCESS: 'success',
  FAILED_RETRYABLE: 'warning',
  FAILED_FINAL: 'error',
  CANCELLED: 'default'
};

const documentStatusText: Record<number, string> = {
  0: 'PROCESSING',
  1: 'AVAILABLE',
  2: 'FAILED',
  3: 'DISABLED'
};

const versionStatusText: Record<number, string> = {
  0: 'PROCESSING',
  1: 'AVAILABLE',
  2: 'FAILED',
  3: 'DISABLED',
  4: 'DELETED'
};

const chunkStatusColors: Record<string, string> = {
  ACTIVE: 'success',
  SUPERSEDED: 'default',
  DISABLED: 'warning',
  DELETED: 'error'
};

function taskStatusColor(status?: number) {
  if (status === 2) return 'success';
  if (status === 3) return 'error';
  if (status === 4) return 'warning';
  if (status === 5) return 'default';
  if (status === 6) return 'warning';
  return 'processing';
}

function documentStatusColor(status?: number) {
  if (status === 1) return 'success';
  if (status === 2) return 'error';
  if (status === 3 || status === 4) return 'warning';
  return 'processing';
}

function canCancelTask(status?: number) {
  return status === 0 || status === 1 || status === 3 || status === 4;
}

function canRetryTask(status?: number) {
  return status === 3 || status === 6;
}

export default function KnowledgePage() {
  const { message } = App.useApp();
  const [kbForm] = Form.useForm();
  const [knowledgeBases, setKnowledgeBases] = useState<RagKnowledgeBase[]>([]);
  const [selectedKbId, setSelectedKbId] = useState<number>();
  const [documents, setDocuments] = useState<RagDocument[]>([]);
  const [tasks, setTasks] = useState<RagIngestionTask[]>([]);
  const [documentVersions, setDocumentVersions] = useState<RagDocumentVersion[]>([]);
  const [documentChunks, setDocumentChunks] = useState<KnowledgeChunkRecord[]>([]);
  const [chunkVersions, setChunkVersions] = useState<KnowledgeChunkRecord[]>([]);
  const [selectedChunk, setSelectedChunk] = useState<KnowledgeChunkRecord>();
  const [selectedTaskProgress, setSelectedTaskProgress] = useState<IngestionTaskProgressSnapshot>();
  const [taskEvents, setTaskEvents] = useState<IngestionTaskEventView[]>([]);
  const [taskShards, setTaskShards] = useState<RagIngestionTaskShard[]>([]);
  const [taskProgressLoading, setTaskProgressLoading] = useState(false);
  const [selectedDocument, setSelectedDocument] = useState<RagDocument>();
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState<string>();
  const [loading, setLoading] = useState(false);
  const [creatingKb, setCreatingKb] = useState(false);
  const [uploading, setUploading] = useState(false);

  const selectedKb = useMemo(
    () => knowledgeBases.find((item) => item.id === selectedKbId),
    [knowledgeBases, selectedKbId]
  );

  const loadKnowledgeBases = async () => {
    const { data } = await ragApi.listKnowledgeBases();
    const rows = data.data?.records || [];
    setKnowledgeBases(rows);
    setSelectedKbId((current) => current || rows[0]?.id);
  };

  const loadDocuments = async (kbId = selectedKbId) => {
    if (!kbId) {
      setDocuments([]);
      return;
    }
    const { data } = await ragApi.listRagDocuments(kbId);
    setDocuments(data.data?.records || []);
  };

  const loadAll = async () => {
    try {
      setLoading(true);
      await loadKnowledgeBases();
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAll();
  }, []);

  useEffect(() => {
    loadDocuments(selectedKbId).catch((error) => message.error(getErrorMessage(error)));
  }, [selectedKbId]);

  const createKnowledgeBase = async (values: { kbCode: string; name: string; description?: string }) => {
    try {
      setCreatingKb(true);
      const { data } = await ragApi.createKnowledgeBase(values);
      message.success(`知识库已就绪：${data.data?.name}`);
      kbForm.resetFields();
      await loadKnowledgeBases();
      if (data.data?.id) setSelectedKbId(data.data.id);
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setCreatingKb(false);
    }
  };

  const loadDocumentDetail = async (document: RagDocument) => {
    try {
      setDetailOpen(true);
      setDetailLoading(true);
      const [documentResult, versionResult, taskResult] = await Promise.all([
        ragApi.getRagDocument(document.id),
        ragApi.listRagDocumentVersions(document.id),
        ragApi.listDocumentTasks(document.id)
      ]);
      const freshDocument = documentResult.data.data;
      const [chunkResult] = await Promise.all([
        ragApi.listKnowledgeChunks({
          documentId: freshDocument.documentUid,
          includeHistory: false
        })
      ]);
      setSelectedDocument(freshDocument);
      setDocumentVersions(versionResult.data.data?.records || []);
      setTasks(taskResult.data.data?.records || []);
      setDocumentChunks(chunkResult.data.data?.records || []);
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setDetailLoading(false);
    }
  };

  const refreshDocumentDetail = async () => {
    if (!selectedDocument) return;
    await loadDocumentDetail(selectedDocument);
    await loadDocuments(selectedKbId);
  };

  const runDocumentAction = async (
    key: string,
    action: () => Promise<unknown>,
    successMessage: string,
    closeAfterSuccess = false
  ) => {
    try {
      setActionLoading(key);
      await action();
      message.success(successMessage);
      if (closeAfterSuccess) {
        setDetailOpen(false);
        setSelectedDocument(undefined);
        setDocumentVersions([]);
        setDocumentChunks([]);
        setSelectedChunk(undefined);
        setChunkVersions([]);
        setTasks([]);
        await loadDocuments(selectedKbId);
      } else {
        await refreshDocumentDetail();
      }
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setActionLoading(undefined);
    }
  };

  const replaceDocument = async (file: File) => {
    if (!selectedDocument) return;
    await runDocumentAction(
      'replace-document',
      () => ragApi.replaceRagDocument(selectedDocument.id, file),
      '替换任务已提交'
    );
  };

  const rollbackVersion = async (versionNo: number) => {
    if (!selectedDocument) return;
    await runDocumentAction(
      `rollback-version-${versionNo}`,
      () => ragApi.rollbackRagDocument(selectedDocument.id, versionNo),
      `已提交回滚到 v${versionNo} 的解析任务`
    );
  };

  const cancelTask = async (taskId: number) => {
    await runDocumentAction(`cancel-task-${taskId}`, () => ragApi.cancelIngestionTask(taskId), '任务已取消');
  };

  const retryTask = async (taskId: number) => {
    await runDocumentAction(`retry-task-${taskId}`, async () => {
      const { data } = await ragApi.retryIngestionTask(taskId);
      if (!data.data?.published) {
        message.warning('任务已进入重试等待，但消息发布未确认');
      }
    }, '任务已重新入队');
  };

  const loadTaskProgress = async (taskId: number, showLoading = true) => {
    try {
      if (showLoading) setTaskProgressLoading(true);
      const [progressResult, eventResult, shardResult] = await Promise.all([
        ragApi.getIngestionTaskProgress(taskId),
        ragApi.listIngestionTaskEvents(taskId, { pageSize: 50, sortBy: 'id', sortDirection: 'DESC' }),
        ragApi.listIngestionTaskShards(taskId, { pageSize: 100, sortBy: 'id', sortDirection: 'ASC' })
      ]);
      setSelectedTaskProgress(progressResult.data.data);
      setTaskEvents(eventResult.data.data?.records || []);
      setTaskShards(shardResult.data.data?.records || []);
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      if (showLoading) setTaskProgressLoading(false);
    }
  };

  const retryFailedShards = async () => {
    if (!selectedTaskProgress) return;
    try {
      setActionLoading(`retry-shards-${selectedTaskProgress.taskId}`);
      const { data } = await ragApi.retryIngestionTaskShards(selectedTaskProgress.taskId);
      if (!data.data?.published) {
        message.warning('没有可重试分片，或消息发布未确认');
      } else {
        message.success(`已重置 ${data.data.resetCount} 个失败分片`);
      }
      await loadTaskProgress(selectedTaskProgress.taskId, false);
      await refreshDocumentDetail();
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setActionLoading(undefined);
    }
  };

  useEffect(() => {
    const taskId = selectedTaskProgress?.taskId;
    const status = selectedTaskProgress?.taskStatus;
    if (!taskId || (status !== 0 && status !== 1 && status !== 4)) {
      return;
    }
    const terminalEvents = ['TASK_SUCCEEDED', 'TASK_PARTIAL_SUCCESS', 'TASK_FAILED', 'TASK_CANCELLED'];
    let source: EventSource | null = null;
    let reconnectTimer: ReturnType<typeof setTimeout> | null = null;
    let retryCount = 0;
    const maxRetries = 5;
    const baseDelay = 2000;

    const connect = () => {
      source = new EventSource(ragApi.getIngestionTaskEventsUrl(taskId));
      const refresh = () => {
        retryCount = 0;
        loadTaskProgress(taskId, false).catch((error) => message.error(getErrorMessage(error)));
      };
      [
        'TASK_STARTED',
        'STAGE_STARTED',
        'STAGE_PROGRESS',
        'STAGE_COMPLETED',
        'STAGE_SKIPPED',
        'STAGE_FAILED',
        'TASK_CANCEL_REQUESTED',
        'TASK_SUCCEEDED',
        'TASK_PARTIAL_SUCCESS',
        'TASK_FAILED',
        'TASK_CANCELLED',
        'SHARD_RETRY_REQUESTED'
      ].forEach((eventName) =>
        source!.addEventListener(eventName, (e: MessageEvent) => {
          refresh();
          if (terminalEvents.includes(eventName)) {
            source?.close();
          }
        })
      );
      // On error, attempt reconnection with exponential backoff.
      // EventSource automatically sends Last-Event-ID header on reconnect,
      // enabling server-side resume from the last received event.
      source.onerror = () => {
        source?.close();
        if (retryCount < maxRetries) {
          const delay = baseDelay * Math.pow(2, retryCount);
          retryCount++;
          reconnectTimer = setTimeout(connect, delay);
        } else {
          message.warning('任务事件连接中断，请手动刷新查看进度');
        }
      };
    };

    connect();
    return () => {
      source?.close();
      if (reconnectTimer) clearTimeout(reconnectTimer);
    };
  }, [selectedTaskProgress?.taskId, selectedTaskProgress?.taskStatus]);

  const openDocumentDownload = (versionNo?: number) => {
    if (!selectedDocument) return;
    window.open(ragApi.getRagDocumentDownloadUrl(selectedDocument.id, versionNo), '_blank', 'noopener,noreferrer');
  };

  const loadChunkVersions = async (chunk: KnowledgeChunkRecord) => {
    try {
      setSelectedChunk(chunk);
      const { data } = await ragApi.listKnowledgeChunkVersions(chunk.chunkId);
      setChunkVersions(data.data?.records || []);
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  };

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <PageHeaderCard
        title="RAG 知识库"
        description="适配新的 MySQL 元数据、RabbitMQ 异步任务和 Milvus vector_id 回写流程。"
        tags={['Knowledge Base', 'Async Ingestion', 'RabbitMQ', 'Chunk Metadata']}
      />

      <Tabs
        items={[
          {
            key: 'async',
            label: '异步文档入库',
            children: (
              <Row gutter={[16, 16]}>
                <Col xs={24} xl={8}>
                  <Card className="page-card" variant="borderless" title="知识库">
                    <Form layout="vertical" form={kbForm} onFinish={createKnowledgeBase}>
                      <Form.Item label="知识库编码" name="kbCode" rules={[{ required: true }]}>
                        <Input placeholder="default" />
                      </Form.Item>
                      <Form.Item label="知识库名称" name="name" rules={[{ required: true }]}>
                        <Input placeholder="Default Knowledge Base" />
                      </Form.Item>
                      <Form.Item label="描述" name="description">
                        <TextArea rows={3} />
                      </Form.Item>
                      <Button type="primary" htmlType="submit" loading={creatingKb}>
                        创建或复用知识库
                      </Button>
                    </Form>

                    <Table
                      style={{ marginTop: 16 }}
                      size="small"
                      loading={loading}
                      rowKey="id"
                      dataSource={knowledgeBases}
                      pagination={false}
                      columns={[
                        { title: '编码', dataIndex: 'kbCode' },
                        { title: '名称', dataIndex: 'name' },
                        {
                          title: '操作',
                          width: 90,
                          render: (_, record) => (
                            <Button type="link" size="small" onClick={() => setSelectedKbId(record.id)}>
                              选择
                            </Button>
                          )
                        }
                      ]}
                    />
                  </Card>
                </Col>

                <Col xs={24} xl={16}>
                  <Card
                    className="page-card"
                    variant="borderless"
                    title="上传到 RAG 元数据链路"
                    extra={<Button icon={<ReloadOutlined />} onClick={() => loadDocuments()} />}
                  >
                    <Space direction="vertical" size={16} style={{ width: '100%' }}>
                      <Select
                        placeholder="选择知识库"
                        value={selectedKbId}
                        onChange={setSelectedKbId}
                        style={{ width: '100%' }}
                        options={knowledgeBases.map((kb) => ({
                          value: kb.id,
                          label: `${kb.name} (${kb.kbCode})`
                        }))}
                      />

                      {selectedKb ? (
                        <Descriptions bordered size="small" column={{ xs: 1, md: 2 }}>
                          <Descriptions.Item label="Collection">{selectedKb.vectorCollection}</Descriptions.Item>
                          <Descriptions.Item label="Embedding">{selectedKb.embeddingModel}</Descriptions.Item>
                          <Descriptions.Item label="Chunk">{`${selectedKb.chunkSize}/${selectedKb.chunkOverlap}`}</Descriptions.Item>
                          <Descriptions.Item label="TopK/Score">{`${selectedKb.retrievalTopK}/${selectedKb.minScore}`}</Descriptions.Item>
                        </Descriptions>
                      ) : null}

                      <Dragger
                        multiple={false}
                        disabled={!selectedKbId}
                        customRequest={async ({ file, onSuccess, onError }) => {
                          try {
                            setUploading(true);
                            const { data } = await ragApi.ingestRagDocument(file as File, selectedKbId);
                            message.success(`任务已提交：${data.data?.taskNo}`);
                            onSuccess?.(data);
                            await loadDocuments(selectedKbId);
                          } catch (error) {
                            const msg = getErrorMessage(error);
                            message.error(msg);
                            onError?.(new Error(msg));
                          } finally {
                            setUploading(false);
                          }
                        }}
                      >
                        <p className="ant-upload-drag-icon">
                          <InboxOutlined />
                        </p>
                        <p className="ant-upload-text">拖拽或点击上传文件</p>
                        <p className="ant-upload-hint">后端会先写 MySQL 元数据，再投递 RabbitMQ 任务。</p>
                      </Dragger>
                      <Text type="secondary">{uploading ? '正在提交任务...' : '支持 PDF、DOCX、Markdown、TXT 等文件。'}</Text>
                    </Space>
                  </Card>

                  <Card className="page-card" variant="borderless" title="文档状态" style={{ marginTop: 16 }}>
                    <Table
                      rowKey="id"
                      dataSource={documents}
                      loading={loading}
                      scroll={{ x: 1100 }}
                      columns={[
                        { title: 'ID', dataIndex: 'id', width: 80 },
                        { title: '文档', dataIndex: 'documentName', width: 220 },
                        {
                          title: '状态',
                          dataIndex: 'documentStatus',
                          width: 130,
                          render: (value: number) => (
                            <Tag color={documentStatusColor(value)}>{documentStatusText[value] || value}</Tag>
                          )
                        },
                        { title: 'Chunks', dataIndex: 'chunkCount', width: 100 },
                        { title: '文件大小', dataIndex: 'fileSize', width: 120 },
                        {
                          title: 'Hash',
                          dataIndex: 'fileHash',
                          width: 180,
                          render: (value: string) => value?.slice(0, 12) || '-'
                        },
                        {
                          title: '错误',
                          dataIndex: 'errorMessage',
                          render: (value: string) => (
                            <Paragraph ellipsis={{ rows: 2, expandable: true }} style={{ margin: 0 }}>
                              {value || '-'}
                            </Paragraph>
                          )
                        },
                        {
                          title: '操作',
                          width: 120,
                          fixed: 'right',
                          render: (_, record) => (
                            <Button size="small" onClick={() => loadDocumentDetail(record)}>
                              详情
                            </Button>
                          )
                        }
                      ]}
                    />
                  </Card>

                  <Card
                    className="page-card"
                    variant="borderless"
                    title={selectedDocument ? `任务状态：${selectedDocument.documentName}` : '任务状态'}
                    style={{ marginTop: 16 }}
                  >
                    <Table
                      rowKey="id"
                      dataSource={tasks}
                      pagination={false}
                      scroll={{ x: 900 }}
                      columns={[
                        { title: 'Task ID', dataIndex: 'id', width: 90 },
                        { title: 'Task No', dataIndex: 'taskNo', width: 180 },
                        { title: 'Type', dataIndex: 'taskType', width: 100 },
                        {
                          title: 'Status',
                          dataIndex: 'taskStatus',
                          width: 120,
                          render: (value: number) => <Tag color={taskStatusColor(value)}>{taskStatusText[value] || value}</Tag>
                        },
                        {
                          title: 'Progress',
                          dataIndex: 'progress',
                          width: 140,
                          render: (value: number) => <Progress percent={value || 0} size="small" />
                        },
                        { title: 'Stage', dataIndex: 'currentStage', width: 150, render: (value: string) => value || '-' },
                        { title: 'Success', dataIndex: 'successCount', width: 100 },
                        { title: 'Failed', dataIndex: 'failedCount', width: 100 },
                        {
                          title: 'Cancel',
                          dataIndex: 'cancelRequested',
                          width: 100,
                          render: (value: boolean) => (value ? <Tag color="warning">REQUESTED</Tag> : '-')
                        },
                        {
                          title: 'Error',
                          dataIndex: 'errorMessage',
                          render: (value: string) => (
                            <Paragraph ellipsis={{ rows: 2, expandable: true }} style={{ margin: 0 }}>
                              {value || '-'}
                            </Paragraph>
                          )
                        },
                        {
                          title: '操作',
                          width: 100,
                          fixed: 'right',
                          render: (_, record) => (
                            <Button size="small" onClick={() => loadTaskProgress(record.id)}>
                              进度
                            </Button>
                          )
                        }
                      ]}
                    />
                  </Card>
                </Col>
              </Row>
            )
          },
        ]}
      />

      <Drawer
        title={selectedDocument ? `文档详情：${selectedDocument.documentName}` : '文档详情'}
        width={980}
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
        destroyOnClose={false}
        extra={
          selectedDocument ? (
            <Space wrap>
              <Button icon={<ReloadOutlined />} onClick={refreshDocumentDetail} loading={detailLoading}>
                刷新
              </Button>
              <Button icon={<DownloadOutlined />} onClick={() => openDocumentDownload()}>
                下载当前版本
              </Button>
              <Button
                icon={<RetweetOutlined />}
                loading={actionLoading === 'reparse-document'}
                onClick={() =>
                  runDocumentAction(
                    'reparse-document',
                    () => ragApi.reparseRagDocument(selectedDocument.id),
                    '重新解析任务已提交'
                  )
                }
              >
                重新解析
              </Button>
              <Upload
                showUploadList={false}
                customRequest={async ({ file, onSuccess, onError }) => {
                  try {
                    await replaceDocument(file as File);
                    onSuccess?.({});
                  } catch (error) {
                    onError?.(new Error(getErrorMessage(error)));
                  }
                }}
              >
                <Button icon={<UploadOutlined />} loading={actionLoading === 'replace-document'}>
                  替换上传
                </Button>
              </Upload>
              {selectedDocument.documentStatus === 3 ? (
                <Button
                  loading={actionLoading === 'enable-document'}
                  onClick={() =>
                    runDocumentAction(
                      'enable-document',
                      () => ragApi.enableRagDocument(selectedDocument.id),
                      '文档已启用'
                    )
                  }
                >
                  启用
                </Button>
              ) : (
                <Button
                  icon={<StopOutlined />}
                  loading={actionLoading === 'disable-document'}
                  onClick={() =>
                    runDocumentAction(
                      'disable-document',
                      () => ragApi.disableRagDocument(selectedDocument.id),
                      '文档已禁用'
                    )
                  }
                >
                  禁用
                </Button>
              )}
              <Popconfirm
                title="确认删除该文档？"
                description="后端当前会软删除文档和版本记录。"
                okText="删除"
                cancelText="取消"
                onConfirm={() =>
                  runDocumentAction(
                    'delete-document',
                    () => ragApi.deleteRagDocument(selectedDocument.id),
                    '文档已删除',
                    true
                  )
                }
              >
                <Button danger icon={<DeleteOutlined />} loading={actionLoading === 'delete-document'}>
                  删除
                </Button>
              </Popconfirm>
            </Space>
          ) : null
        }
      >
        {selectedDocument ? (
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            <Descriptions bordered size="small" column={{ xs: 1, md: 2 }}>
              <Descriptions.Item label="Document ID">{selectedDocument.id}</Descriptions.Item>
              <Descriptions.Item label="UID">{selectedDocument.documentUid}</Descriptions.Item>
              <Descriptions.Item label="当前版本">v{selectedDocument.currentVersionNo}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={documentStatusColor(selectedDocument.documentStatus)}>
                  {documentStatusText[selectedDocument.documentStatus] || selectedDocument.documentStatus}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="文件名">{selectedDocument.originalFilename || selectedDocument.documentName}</Descriptions.Item>
              <Descriptions.Item label="MIME">{selectedDocument.mimeType || '-'}</Descriptions.Item>
              <Descriptions.Item label="大小">{selectedDocument.fileSize || 0}</Descriptions.Item>
              <Descriptions.Item label="Chunks">{selectedDocument.chunkCount}</Descriptions.Item>
              <Descriptions.Item label="Hash" span={2}>
                <Text copyable>{selectedDocument.fileHash || '-'}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="错误" span={2}>
                {selectedDocument.errorMessage || '-'}
              </Descriptions.Item>
            </Descriptions>

            <Card className="page-card" variant="borderless" title="版本">
              <Table
                rowKey="id"
                size="small"
                loading={detailLoading}
                dataSource={documentVersions}
                scroll={{ x: 980 }}
                pagination={{ pageSize: 5 }}
                columns={[
                  { title: '版本', dataIndex: 'versionNo', width: 80, render: (value: number) => `v${value}` },
                  {
                    title: '当前',
                    dataIndex: 'currentFlag',
                    width: 80,
                    render: (value: boolean) => (value ? <Tag color="success">当前</Tag> : '-')
                  },
                  {
                    title: '状态',
                    dataIndex: 'versionStatus',
                    width: 120,
                    render: (value: number) => (
                      <Tag color={documentStatusColor(value)}>{versionStatusText[value] || value}</Tag>
                    )
                  },
                  { title: '文件', dataIndex: 'originalFilename', width: 180 },
                  { title: 'Chunks', dataIndex: 'chunkCount', width: 90 },
                  {
                    title: 'Hash',
                    dataIndex: 'fileHash',
                    width: 140,
                    render: (value: string) => value?.slice(0, 12) || '-'
                  },
                  {
                    title: '错误',
                    dataIndex: 'errorMessage',
                    render: (value: string) => (
                      <Paragraph ellipsis={{ rows: 2, expandable: true }} style={{ margin: 0 }}>
                        {value || '-'}
                      </Paragraph>
                    )
                  },
                  {
                    title: '操作',
                    width: 170,
                    fixed: 'right',
                    render: (_, record) => (
                      <Space>
                        <Button size="small" onClick={() => openDocumentDownload(record.versionNo)}>
                          下载
                        </Button>
                        <Popconfirm
                          title={`回滚到 v${record.versionNo}？`}
                          okText="回滚"
                          cancelText="取消"
                          disabled={record.currentFlag || record.versionStatus > 1}
                          onConfirm={() => rollbackVersion(record.versionNo)}
                        >
                          <Button
                            size="small"
                            disabled={record.currentFlag || record.versionStatus > 1}
                            loading={actionLoading === `rollback-version-${record.versionNo}`}
                          >
                            回滚
                          </Button>
                        </Popconfirm>
                      </Space>
                    )
                  }
                ]}
              />
            </Card>

            <Card className="page-card" variant="borderless" title="Chunks">
              <Table
                rowKey={(record) => `${record.chunkId}-${record.version}`}
                size="small"
                loading={detailLoading}
                dataSource={documentChunks}
                scroll={{ x: 1180 }}
                pagination={{ pageSize: 6 }}
                columns={[
                  {
                    title: 'Chunk ID',
                    dataIndex: 'chunkId',
                    width: 240,
                    render: (value: string) => <Text copyable>{value}</Text>
                  },
                  {
                    title: 'Type',
                    dataIndex: 'contentType',
                    width: 110,
                    render: (value: string) => <Tag>{value || 'text'}</Tag>
                  },
                  {
                    title: 'Status',
                    dataIndex: 'status',
                    width: 120,
                    render: (value: string) => <Tag color={chunkStatusColors[value] || 'default'}>{value}</Tag>
                  },
                  { title: 'Version', dataIndex: 'version', width: 90 },
                  { title: 'Page', dataIndex: 'pageNo', width: 80, render: (value: number) => value || '-' },
                  {
                    title: 'Section',
                    dataIndex: 'sectionTitle',
                    width: 150,
                    render: (value: string) => value || '-'
                  },
                  {
                    title: 'Image',
                    dataIndex: 'imageUrl',
                    width: 130,
                    render: (value: string) =>
                      value ? (
                        <Button type="link" size="small" href={value} target="_blank" rel="noreferrer">
                          打开
                        </Button>
                      ) : (
                        '-'
                      )
                  },
                  {
                    title: 'Content',
                    dataIndex: 'textContent',
                    render: (value: string) => (
                      <Paragraph ellipsis={{ rows: 2, expandable: true }} style={{ margin: 0 }}>
                        {value || '-'}
                      </Paragraph>
                    )
                  },
                  {
                    title: '操作',
                    width: 100,
                    fixed: 'right',
                    render: (_, record) => (
                      <Button size="small" onClick={() => loadChunkVersions(record)}>
                        版本
                      </Button>
                    )
                  }
                ]}
              />
            </Card>

            <Card className="page-card" variant="borderless" title="任务">
              <Table
                rowKey="id"
                size="small"
                loading={detailLoading}
                dataSource={tasks}
                scroll={{ x: 1050 }}
                pagination={{ pageSize: 5 }}
                columns={[
                  { title: 'Task ID', dataIndex: 'id', width: 90 },
                  { title: 'Task No', dataIndex: 'taskNo', width: 180 },
                  { title: 'Type', dataIndex: 'taskType', width: 110 },
                  {
                    title: 'Status',
                    dataIndex: 'taskStatus',
                    width: 120,
                    render: (value: number) => <Tag color={taskStatusColor(value)}>{taskStatusText[value] || value}</Tag>
                  },
                  {
                    title: 'Progress',
                    dataIndex: 'progress',
                    width: 140,
                    render: (value: number) => <Progress percent={value || 0} size="small" />
                  },
                  { title: 'Stage', dataIndex: 'currentStage', width: 150, render: (value: string) => value || '-' },
                  {
                    title: 'Cancel',
                    dataIndex: 'cancelRequested',
                    width: 100,
                    render: (value: boolean) => (value ? <Tag color="warning">REQUESTED</Tag> : '-')
                  },
                  { title: 'Retry', dataIndex: 'retryCount', width: 80 },
                  {
                    title: 'Error',
                    dataIndex: 'errorMessage',
                    render: (value: string) => (
                      <Paragraph ellipsis={{ rows: 2, expandable: true }} style={{ margin: 0 }}>
                        {value || '-'}
                      </Paragraph>
                    )
                  },
                  {
                    title: '操作',
                    width: 210,
                    fixed: 'right',
                    render: (_, record) => (
                      <Space>
                        <Button size="small" onClick={() => loadTaskProgress(record.id)}>
                          进度
                        </Button>
                        <Button
                          size="small"
                          disabled={!canCancelTask(record.taskStatus)}
                          loading={actionLoading === `cancel-task-${record.id}`}
                          onClick={() => cancelTask(record.id)}
                        >
                          取消
                        </Button>
                        <Button
                          size="small"
                          disabled={!canRetryTask(record.taskStatus)}
                          loading={actionLoading === `retry-task-${record.id}`}
                          onClick={() => retryTask(record.id)}
                        >
                          重试
                        </Button>
                      </Space>
                    )
                  }
                ]}
              />
            </Card>
          </Space>
        ) : null}
      </Drawer>

      <Drawer
        title={selectedTaskProgress ? `任务进度：${selectedTaskProgress.task.taskNo}` : '任务进度'}
        width={1040}
        open={!!selectedTaskProgress}
        onClose={() => {
          setSelectedTaskProgress(undefined);
          setTaskEvents([]);
          setTaskShards([]);
        }}
        extra={
          selectedTaskProgress ? (
            <Space>
              <Button
                icon={<ReloadOutlined />}
                loading={taskProgressLoading}
                onClick={() => loadTaskProgress(selectedTaskProgress.taskId)}
              >
                刷新
              </Button>
              <Button
                loading={actionLoading === `retry-shards-${selectedTaskProgress.taskId}`}
                disabled={selectedTaskProgress.shardSummary.failedRetryable <= 0 || selectedTaskProgress.taskStatus === 1}
                onClick={retryFailedShards}
              >
                重试失败分片
              </Button>
            </Space>
          ) : null
        }
      >
        {selectedTaskProgress ? (
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            <Descriptions bordered size="small" column={{ xs: 1, md: 3 }}>
              <Descriptions.Item label="Task ID">{selectedTaskProgress.taskId}</Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={taskStatusColor(selectedTaskProgress.taskStatus)}>
                  {taskStatusText[selectedTaskProgress.taskStatus] || selectedTaskProgress.taskStatus}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="当前阶段">{selectedTaskProgress.currentStage || '-'}</Descriptions.Item>
              <Descriptions.Item label="总进度" span={2}>
                <Progress percent={selectedTaskProgress.progress || 0} size="small" />
              </Descriptions.Item>
              <Descriptions.Item label="阶段进度">
                <Progress percent={selectedTaskProgress.stageProgress || 0} size="small" />
              </Descriptions.Item>
              <Descriptions.Item label="取消请求">
                {selectedTaskProgress.cancelRequested ? <Tag color="warning">REQUESTED</Tag> : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="分片">
                {`total=${selectedTaskProgress.shardSummary.total}, success=${selectedTaskProgress.shardSummary.success}, retryable=${selectedTaskProgress.shardSummary.failedRetryable}, final=${selectedTaskProgress.shardSummary.failedFinal}`}
              </Descriptions.Item>
              <Descriptions.Item label="Last Event">{selectedTaskProgress.lastEventId || '-'}</Descriptions.Item>
            </Descriptions>

            <Card className="page-card" variant="borderless" title="阶段时间线">
              <Timeline
                items={selectedTaskProgress.stages.map((stage) => ({
                  color:
                    stage.stageStatus === 'SUCCESS' || stage.stageStatus === 'SKIPPED'
                      ? 'green'
                      : stage.stageStatus === 'FAILED'
                        ? 'red'
                        : stage.stageStatus === 'RUNNING'
                          ? 'blue'
                          : 'gray',
                  children: (
                    <Space direction="vertical" size={4} style={{ width: '100%' }}>
                      <Space wrap>
                        <Text strong>{stage.stageName || stage.stageCode}</Text>
                        <Tag color={stageStatusColors[stage.stageStatus] || 'default'}>{stage.stageStatus}</Tag>
                        <Text type="secondary">{stage.stageCode}</Text>
                      </Space>
                      <Progress percent={stage.progress || 0} size="small" />
                      <Text type="secondary">
                        {`success=${stage.successCount || 0}, failed=${stage.failedCount || 0}, total=${stage.totalCount || 0}`}
                      </Text>
                      {stage.errorMessage ? (
                        <Paragraph ellipsis={{ rows: 2, expandable: true }} style={{ margin: 0 }}>
                          {stage.errorMessage}
                        </Paragraph>
                      ) : null}
                    </Space>
                  )
                }))}
              />
            </Card>

            <Card className="page-card" variant="borderless" title="分片">
              <Table
                rowKey="id"
                size="small"
                loading={taskProgressLoading}
                dataSource={taskShards}
                scroll={{ x: 1120 }}
                pagination={{ pageSize: 8 }}
                columns={[
                  { title: 'ID', dataIndex: 'id', width: 80 },
                  { title: 'Stage', dataIndex: 'stageCode', width: 150 },
                  { title: 'Type', dataIndex: 'shardType', width: 130 },
                  {
                    title: 'Status',
                    dataIndex: 'shardStatus',
                    width: 150,
                    render: (value: string) => <Tag color={shardStatusColors[value] || 'default'}>{value}</Tag>
                  },
                  { title: 'Retry', dataIndex: 'retryCount', width: 80 },
                  {
                    title: 'Shard Key',
                    dataIndex: 'shardKey',
                    width: 260,
                    render: (value: string) => <Text copyable>{value}</Text>
                  },
                  {
                    title: 'Output',
                    dataIndex: 'outputRef',
                    width: 160,
                    render: (value: string) => value || '-'
                  },
                  {
                    title: 'Error',
                    dataIndex: 'errorMessage',
                    render: (value: string) => (
                      <Paragraph ellipsis={{ rows: 2, expandable: true }} style={{ margin: 0 }}>
                        {value || '-'}
                      </Paragraph>
                    )
                  }
                ]}
              />
            </Card>

            <Card className="page-card" variant="borderless" title="事件历史">
              <Table
                rowKey="id"
                size="small"
                loading={taskProgressLoading}
                dataSource={taskEvents}
                pagination={{ pageSize: 8 }}
                columns={[
                  { title: 'Event ID', dataIndex: 'id', width: 90 },
                  { title: 'Type', dataIndex: 'eventType', width: 180 },
                  { title: 'Stage', dataIndex: 'stageCode', width: 150, render: (value: string) => value || '-' },
                  { title: 'Progress', dataIndex: 'progress', width: 100, render: (value: number) => value ?? '-' },
                  {
                    title: 'Message',
                    dataIndex: 'message',
                    render: (value: string) => (
                      <Paragraph ellipsis={{ rows: 2, expandable: true }} style={{ margin: 0 }}>
                        {value || '-'}
                      </Paragraph>
                    )
                  },
                  { title: 'Created', dataIndex: 'createdAt', width: 190 }
                ]}
              />
            </Card>
          </Space>
        ) : null}
      </Drawer>

      <Drawer
        title={selectedChunk ? `Chunk 版本：${selectedChunk.chunkId}` : 'Chunk 版本'}
        width={820}
        open={!!selectedChunk}
        onClose={() => {
          setSelectedChunk(undefined);
          setChunkVersions([]);
        }}
      >
        <Table
          rowKey={(record) => `${record.chunkId}-${record.version}`}
          size="small"
          dataSource={chunkVersions}
          scroll={{ x: 820 }}
          columns={[
            { title: 'Version', dataIndex: 'version', width: 90 },
            {
              title: 'Current',
              dataIndex: 'current',
              width: 90,
              render: (value: boolean) => (value ? <Tag color="success">当前</Tag> : '-')
            },
            {
              title: 'Status',
              dataIndex: 'status',
              width: 120,
              render: (value: string) => <Tag color={chunkStatusColors[value] || 'default'}>{value}</Tag>
            },
            {
              title: 'Content',
              dataIndex: 'textContent',
              render: (value: string) => (
                <Paragraph ellipsis={{ rows: 4, expandable: true }} style={{ margin: 0 }}>
                  {value || '-'}
                </Paragraph>
              )
            }
          ]}
        />
      </Drawer>
    </Space>
  );
}
