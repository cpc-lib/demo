import { DeleteOutlined, EditOutlined, HistoryOutlined, ReloadOutlined, StopOutlined } from '@ant-design/icons';
import {
  App,
  Button,
  Card,
  Col,
  Form,
  Input,
  InputNumber,
  Modal,
  Popconfirm,
  Row,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Typography
} from 'antd';
import { useEffect, useState } from 'react';
import PageHeaderCard from '../components/PageHeaderCard';
import { ragApi } from '../api/rag';
import type { KnowledgeChunkCreateRequest, KnowledgeChunkRecord, KnowledgeChunkUpdateRequest } from '../types';
import { getErrorMessage } from '../utils/message';

const { TextArea } = Input;
const { Paragraph, Text } = Typography;

const statusColors: Record<string, string> = {
  ACTIVE: 'success',
  SUPERSEDED: 'default',
  DISABLED: 'warning',
  DELETED: 'error'
};

export default function ChunksPage() {
  const { message } = App.useApp();
  const [createForm] = Form.useForm<KnowledgeChunkCreateRequest>();
  const [editForm] = Form.useForm<KnowledgeChunkUpdateRequest>();
  const [chunks, setChunks] = useState<KnowledgeChunkRecord[]>([]);
  const [versions, setVersions] = useState<KnowledgeChunkRecord[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [includeHistory, setIncludeHistory] = useState(false);
  const [documentId, setDocumentId] = useState('');
  const [contentType, setContentType] = useState<string>();
  const [status, setStatus] = useState<string>();
  const [editing, setEditing] = useState<KnowledgeChunkRecord>();
  const [versionChunk, setVersionChunk] = useState<KnowledgeChunkRecord>();

  const loadChunks = async () => {
    try {
      setLoading(true);
      const { data } = await ragApi.listKnowledgeChunks({
        documentId: documentId || undefined,
        contentType,
        status,
        includeHistory
      });
      setChunks(data.data?.records || []);
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadChunks();
  }, []);

  const createChunk = async (values: KnowledgeChunkCreateRequest) => {
    try {
      setSaving(true);
      await ragApi.createKnowledgeChunk(normalizePayload(values));
      message.success('Chunk 已创建');
      createForm.resetFields();
      await loadChunks();
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setSaving(false);
    }
  };

  const openEdit = (record: KnowledgeChunkRecord) => {
    setEditing(record);
    editForm.setFieldsValue({
      textContent: record.textContent,
      contentType: record.contentType,
      imageUrl: record.imageUrl,
      pageNo: record.pageNo,
      sectionTitle: record.sectionTitle,
      imageCaption: record.imageCaption,
      imageNumber: record.imageNumber,
      parentChunkId: record.parentChunkId,
      tenantId: record.tenantId,
      metadataJson: record.metadataJson
    });
  };

  const updateChunk = async () => {
    if (!editing) return;
    try {
      setSaving(true);
      const values = await editForm.validateFields();
      await ragApi.updateKnowledgeChunk(editing.chunkId, normalizePayload(values));
      message.success('Chunk 已更新并生成新版本');
      setEditing(undefined);
      await loadChunks();
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setSaving(false);
    }
  };

  const loadVersions = async (record: KnowledgeChunkRecord) => {
    try {
      setVersionChunk(record);
      const { data } = await ragApi.listKnowledgeChunkVersions(record.chunkId);
      setVersions(data.data?.records || []);
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  };

  const rollback = async (chunkId: string, version: number) => {
    try {
      await ragApi.rollbackKnowledgeChunk(chunkId, version);
      message.success(`已回滚到 v${version} 并生成新版本`);
      if (versionChunk) await loadVersions(versionChunk);
      await loadChunks();
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  };

  const disableChunk = async (chunkId: string) => {
    try {
      await ragApi.disableKnowledgeChunk(chunkId);
      message.success('Chunk 已禁用');
      await loadChunks();
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  };

  const deleteChunk = async (chunkId: string) => {
    try {
      await ragApi.deleteKnowledgeChunk(chunkId);
      message.success('Chunk 已删除');
      await loadChunks();
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  };

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <PageHeaderCard
        title="Chunk 管理"
        description="管理 Redis chunk registry 与 Milvus vector_id 映射，支持版本、禁用、删除和回滚。"
        tags={['Chunk Version', 'Vector ID', 'Rollback', 'Metadata']}
      />

      <Row gutter={[16, 16]}>
        <Col xs={24} xl={8}>
          <Card className="page-card" variant="borderless" title="创建手工 Chunk">
            <Form layout="vertical" form={createForm} onFinish={createChunk} initialValues={{ contentType: 'text' }}>
              <Form.Item label="Document ID" name="documentId">
                <Input placeholder="manual-doc" />
              </Form.Item>
              <Form.Item label="Content Type" name="contentType">
                <Select
                  options={['text', 'image', 'chart', 'flowchart', 'architecture'].map((value) => ({
                    value,
                    label: value
                  }))}
                />
              </Form.Item>
              <Form.Item label="文本内容" name="textContent" rules={[{ required: true }]}>
                <TextArea rows={8} />
              </Form.Item>
              <Form.Item label="章节标题" name="sectionTitle">
                <Input />
              </Form.Item>
              <Form.Item label="图片 URL" name="imageUrl">
                <Input />
              </Form.Item>
              <Form.Item label="页码" name="pageNo">
                <InputNumber min={1} style={{ width: '100%' }} />
              </Form.Item>
              <Form.Item label="metadataJson" name="metadataJson" initialValue="{}">
                <TextArea rows={3} />
              </Form.Item>
              <Button type="primary" htmlType="submit" loading={saving}>
                创建并写入向量
              </Button>
            </Form>
          </Card>
        </Col>

        <Col xs={24} xl={16}>
          <Card
            className="page-card"
            variant="borderless"
            title="Chunk 列表"
            extra={<Button icon={<ReloadOutlined />} onClick={loadChunks} />}
          >
            <Space wrap style={{ marginBottom: 16 }}>
              <Input
                allowClear
                placeholder="Document ID"
                value={documentId}
                onChange={(event) => setDocumentId(event.target.value)}
                style={{ width: 220 }}
              />
              <Select
                allowClear
                placeholder="Content Type"
                value={contentType}
                onChange={setContentType}
                style={{ width: 160 }}
                options={['text', 'image', 'chart', 'flowchart', 'architecture'].map((value) => ({ value, label: value }))}
              />
              <Select
                allowClear
                placeholder="Status"
                value={status}
                onChange={setStatus}
                style={{ width: 160 }}
                options={['ACTIVE', 'SUPERSEDED', 'DISABLED', 'DELETED'].map((value) => ({ value, label: value }))}
              />
              <Space>
                <Text type="secondary">历史版本</Text>
                <Switch checked={includeHistory} onChange={setIncludeHistory} />
              </Space>
              <Button type="primary" onClick={loadChunks}>
                查询
              </Button>
            </Space>

            <Table
              rowKey={(record) => `${record.chunkId}-${record.version}`}
              loading={loading}
              dataSource={chunks}
              scroll={{ x: 1300 }}
              columns={[
                {
                  title: 'Chunk ID',
                  dataIndex: 'chunkId',
                  width: 260,
                  render: (value: string) => <Text copyable>{value}</Text>
                },
                { title: 'Doc', dataIndex: 'documentId', width: 160 },
                {
                  title: 'Type',
                  dataIndex: 'contentType',
                  width: 120,
                  render: (value: string) => <Tag>{value || 'text'}</Tag>
                },
                {
                  title: 'Status',
                  dataIndex: 'status',
                  width: 120,
                  render: (value: string) => <Tag color={statusColors[value] || 'default'}>{value}</Tag>
                },
                { title: 'Version', dataIndex: 'version', width: 90 },
                { title: 'Page', dataIndex: 'pageNo', width: 80, render: (value: number) => value || '-' },
                { title: 'Section', dataIndex: 'sectionTitle', width: 180, render: (value: string) => value || '-' },
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
                  width: 230,
                  fixed: 'right',
                  render: (_, record) => (
                    <Space size={4}>
                      <Button icon={<EditOutlined />} size="small" onClick={() => openEdit(record)} />
                      <Button icon={<HistoryOutlined />} size="small" onClick={() => loadVersions(record)} />
                      <Button icon={<StopOutlined />} size="small" onClick={() => disableChunk(record.chunkId)} />
                      <Popconfirm title="确认删除该 chunk？" onConfirm={() => deleteChunk(record.chunkId)}>
                        <Button danger icon={<DeleteOutlined />} size="small" />
                      </Popconfirm>
                    </Space>
                  )
                }
              ]}
            />
          </Card>
        </Col>
      </Row>

      <Modal
        open={!!editing}
        title={editing ? `编辑 ${editing.chunkId}` : '编辑 Chunk'}
        onCancel={() => setEditing(undefined)}
        onOk={updateChunk}
        confirmLoading={saving}
        width={820}
      >
        <Form layout="vertical" form={editForm}>
          <Form.Item label="Content Type" name="contentType">
            <Input />
          </Form.Item>
          <Form.Item label="文本内容" name="textContent" rules={[{ required: true }]}>
            <TextArea rows={8} />
          </Form.Item>
          <Row gutter={12}>
            <Col span={12}>
              <Form.Item label="章节标题" name="sectionTitle">
                <Input />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item label="页码" name="pageNo">
                <InputNumber min={1} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>
          <Form.Item label="图片 URL" name="imageUrl">
            <Input />
          </Form.Item>
          <Form.Item label="图注" name="imageCaption">
            <Input />
          </Form.Item>
          <Form.Item label="metadataJson" name="metadataJson">
            <TextArea rows={4} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        open={!!versionChunk}
        title={versionChunk ? `版本历史 ${versionChunk.chunkId}` : '版本历史'}
        onCancel={() => {
          setVersionChunk(undefined);
          setVersions([]);
        }}
        footer={null}
        width={1000}
      >
        <Table
          rowKey={(record) => `${record.chunkId}-${record.version}`}
          dataSource={versions}
          scroll={{ x: 900 }}
          columns={[
            { title: 'Version', dataIndex: 'version', width: 90 },
            {
              title: 'Status',
              dataIndex: 'status',
              width: 120,
              render: (value: string) => <Tag color={statusColors[value] || 'default'}>{value}</Tag>
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
              render: (_, record) => (
                <Button size="small" onClick={() => rollback(record.chunkId, record.version)}>
                  回滚
                </Button>
              )
            }
          ]}
        />
      </Modal>
    </Space>
  );
}

function normalizePayload<T extends KnowledgeChunkCreateRequest | KnowledgeChunkUpdateRequest>(values: T): T {
  const next = { ...values };
  if (typeof next.metadataJson === 'string' && next.metadataJson.trim() === '') {
    next.metadataJson = '{}';
  }
  return next;
}
