import {
  CheckCircleOutlined,
  CopyOutlined,
  EditOutlined,
  EyeOutlined,
  FileTextOutlined,
  PlusOutlined,
  ReloadOutlined,
  RollbackOutlined,
  SaveOutlined,
  StopOutlined
} from '@ant-design/icons';
import {
  Alert,
  App,
  Button,
  Card,
  Col,
  Descriptions,
  Drawer,
  Empty,
  Form,
  Input,
  Popconfirm,
  Row,
  Space,
  Switch,
  Table,
  Tag,
  Typography
} from 'antd';
import { useEffect, useMemo, useState } from 'react';


import http from '../api/http';
import PageHeaderCard from '../components/PageHeaderCard';
import type {
  AgentPromptRollbackRequest,
  AgentPromptUpdateRequest,
  RagAgentPrompt,
  RagApiResponse
} from '../types';
import {
  buildAgentPromptSaveRequest,
  canDisableAgentPrompt,
  canEditAgentPrompt,
  canEnableAgentPrompt,
  getAgentPromptSourceLabel,
  getPromptTextStats
} from '../utils/agentPrompts';
import { getErrorMessage } from '../utils/message';

const { TextArea } = Input;
const { Paragraph, Text } = Typography;

type PromptFormValues = {
  promptName?: string;
  promptContent: string;
  enabled?: boolean;
};

export default function AgentPromptsPage() {
  const { message } = App.useApp();
  const [form] = Form.useForm<PromptFormValues>();
  const [activePrompt, setActivePrompt] = useState<RagAgentPrompt | null>();
  const [prompts, setPrompts] = useState<RagAgentPrompt[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editingPrompt, setEditingPrompt] = useState<RagAgentPrompt>();
  const [drawerTitle, setDrawerTitle] = useState('新增系统提示词');
  const [previewPrompt, setPreviewPrompt] = useState<RagAgentPrompt>();
  const promptContent = Form.useWatch('promptContent', form) || '';

  const editorStats = useMemo(() => getPromptTextStats(promptContent), [promptContent]);
  const activeStats = useMemo(
    () => getPromptTextStats(activePrompt?.promptContent),
    [activePrompt?.promptContent]
  );
  const activeSourceLabel = getAgentPromptSourceLabel(activePrompt);
  const activeIsGlobal = activePrompt?.tenantId === 0;

  const loadData = async () => {
    try {
      setLoading(true);
      const [activeResult, listResult] = await Promise.all([
        getActiveAgentPromptRequest(),
        listAgentPromptVersionsRequest()
      ]);
      setActivePrompt(activeResult.data.data || null);
      setPrompts(listResult.data.data || []);
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadData();
  }, []);

  const openCreate = (content = '', enabled = false, title = '新增系统提示词') => {
    setEditingPrompt(undefined);
    setDrawerTitle(title);
    form.resetFields();
    form.setFieldsValue({
      promptName: 'default',
      promptContent: content,
      enabled
    });
    setDrawerOpen(true);
  };

  const openEdit = (record: RagAgentPrompt) => {
    if (!canEditAgentPrompt(record)) {
      openCreate(record.promptContent, true, '基于全局默认创建租户提示词');
      return;
    }
    setEditingPrompt(record);
    setDrawerTitle(record.status === 1 ? '编辑当前系统提示词' : `编辑系统提示词 v${record.version ?? '-'}`);
    form.resetFields();
    form.setFieldsValue({
      promptName: record.promptName || 'default',
      promptContent: record.promptContent,
      enabled: record.status === 1
    });
    setDrawerOpen(true);
  };

  const openEditCurrent = () => {
    if (!activePrompt) {
      openCreate('', true, '新增并启用系统提示词');
      return;
    }
    openEdit(activePrompt);
  };

  const savePrompt = async (values: PromptFormValues) => {
    try {
      setSaving(true);
      const request = buildAgentPromptSaveRequest(values);
      if (editingPrompt?.id) {
        await updateAgentPromptByIdRequest(editingPrompt.id, request);
        message.success(editingPrompt.status === 1 ? '当前系统提示词已保存为新版本' : '系统提示词已更新');
      } else {
        await createAgentPromptRequest(request);
        message.success(values.enabled ? '系统提示词已新增并启用' : '系统提示词已新增');
      }
      setDrawerOpen(false);
      setEditingPrompt(undefined);
      form.resetFields();
      await loadData();
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setSaving(false);
    }
  };

  const enablePrompt = async (record: RagAgentPrompt) => {
    if (!record.id) return;
    try {
      setLoading(true);
      await enableAgentPromptRequest(record.id);
      message.success(`系统提示词 v${record.version ?? '-'} 已启用`);
      await loadData();
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  const disablePrompt = async (record: RagAgentPrompt) => {
    if (!record.id) return;
    try {
      setLoading(true);
      await disableAgentPromptRequest(record.id);
      message.success(`系统提示词 v${record.version ?? '-'} 已停用`);
      await loadData();
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  const rollbackPrompt = async (version?: number) => {
    if (version === undefined) return;
    try {
      setLoading(true);
      await rollbackAgentPromptRequest({ version });
      message.success(`已回滚到版本 ${version}`);
      await loadData();
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  const copyPrompt = async (content?: string) => {
    if (!content) {
      message.warning('没有可复制的提示词内容');
      return;
    }
    try {
      await navigator.clipboard.writeText(content);
      message.success('提示词已复制');
    } catch {
      message.error('复制失败，请手动复制');
    }
  };

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <PageHeaderCard
        title="系统提示词管理"
        description="新增、编辑、启用和停用当前租户聊天 Agent 的系统提示词。启用后会立即影响运行时对话。"
        tags={['Tenant Scoped', 'Versioned', 'Runtime Prompt']}
        extra={
          <Space wrap>
            <Button icon={<ReloadOutlined />} onClick={loadData} loading={loading}>
              刷新
            </Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={() => openCreate()}>
              新增系统提示词
            </Button>
          </Space>
        }
      />

      {activeIsGlobal ? (
        <Alert
          type="info"
          showIcon
          message="当前租户正在使用全局默认提示词"
          description="点击“编辑当前”会创建当前租户自己的启用版本，不会修改全局默认配置。"
        />
      ) : null}

      <Row gutter={[16, 16]}>
        <Col xs={24} xl={9}>
          <Card
            className="page-card"
            variant="borderless"
            title="当前生效提示词"
            loading={loading}
            extra={<Tag color={activeIsGlobal ? 'blue' : 'success'}>{activeSourceLabel}</Tag>}
          >
            {activePrompt ? (
              <Descriptions bordered column={1} size="small">
                <Descriptions.Item label="名称">{activePrompt.promptName || 'default'}</Descriptions.Item>
                <Descriptions.Item label="版本">{activePrompt.version ?? '-'}</Descriptions.Item>
                <Descriptions.Item label="状态">
                  <Tag color={activePrompt.status === 1 ? 'success' : 'default'}>
                    {activePrompt.status === 1 ? '启用' : '停用'}
                  </Tag>
                </Descriptions.Item>
                <Descriptions.Item label="字符 / 行">
                  {activeStats.characters} / {activeStats.lines}
                </Descriptions.Item>
                <Descriptions.Item label="更新人">{activePrompt.updatedBy || '-'}</Descriptions.Item>
                <Descriptions.Item label="更新时间">{activePrompt.updatedAt || '-'}</Descriptions.Item>
              </Descriptions>
            ) : (
              <Empty description="暂无生效提示词" />
            )}
            <Space wrap style={{ marginTop: 16 }}>
              <Button icon={<EditOutlined />} onClick={openEditCurrent}>
                编辑当前
              </Button>
              <Button icon={<EyeOutlined />} onClick={() => activePrompt && setPreviewPrompt(activePrompt)} disabled={!activePrompt}>
                查看当前
              </Button>
              <Button icon={<CopyOutlined />} onClick={() => copyPrompt(activePrompt?.promptContent)} disabled={!activePrompt}>
                复制当前
              </Button>
              <Popconfirm
                title="停用当前租户提示词后，将回退到全局默认提示词。确认停用？"
                onConfirm={() => activePrompt && disablePrompt(activePrompt)}
                disabled={!canDisableAgentPrompt(activePrompt)}
              >
                <Button danger icon={<StopOutlined />} disabled={!canDisableAgentPrompt(activePrompt)}>
                  停用当前
                </Button>
              </Popconfirm>
            </Space>
          </Card>
        </Col>

        <Col xs={24} xl={15}>
          <Card className="page-card" variant="borderless" title="当前提示词内容" loading={loading}>
            {activePrompt?.promptContent ? (
              <Paragraph
                style={{
                  whiteSpace: 'pre-wrap',
                  marginBottom: 0,
                  lineHeight: 1.7,
                  fontFamily: "'JetBrains Mono', 'Cascadia Code', Consolas, monospace"
                }}
                ellipsis={{ rows: 16, expandable: true, symbol: '展开' }}
              >
                {activePrompt.promptContent}
              </Paragraph>
            ) : (
              <Empty description="暂无提示词内容" />
            )}
          </Card>
        </Col>
      </Row>

      <Card className="page-card" variant="borderless" title="租户系统提示词">
        <Table<RagAgentPrompt>
          rowKey={(record) => String(record.id ?? `${record.promptName}-${record.version}`)}
          loading={loading}
          dataSource={prompts}
          pagination={{ pageSize: 8, showSizeChanger: true }}
          columns={[
            {
              title: '版本',
              dataIndex: 'version',
              width: 100,
              render: (value?: number) => <Tag color="blue">v{value ?? '-'}</Tag>
            },
            {
              title: '状态',
              dataIndex: 'status',
              width: 110,
              render: (value?: number) => (
                <Tag color={value === 1 ? 'success' : 'default'}>{value === 1 ? '启用' : '停用'}</Tag>
              )
            },
            { title: '名称', dataIndex: 'promptName', width: 130, render: fallback },
            {
              title: '内容摘要',
              dataIndex: 'promptContent',
              ellipsis: true,
              render: (value?: string) => (
                <Paragraph ellipsis={{ rows: 2 }} style={{ marginBottom: 0 }}>
                  {value || '-'}
                </Paragraph>
              )
            },
            {
              title: '字符',
              dataIndex: 'promptContent',
              width: 90,
              render: (value?: string) => getPromptTextStats(value).characters
            },
            { title: '更新人', dataIndex: 'updatedBy', width: 130, render: fallback },
            { title: '更新时间', dataIndex: 'updatedAt', width: 190, render: fallback },
            {
              title: '操作',
              fixed: 'right',
              width: 300,
              render: (_, record) => (
                <Space size={4}>
                  <Button size="small" type="link" icon={<EyeOutlined />} onClick={() => setPreviewPrompt(record)}>
                    查看
                  </Button>
                  <Button size="small" type="link" icon={<EditOutlined />} onClick={() => openEdit(record)} disabled={!canEditAgentPrompt(record)}>
                    编辑
                  </Button>
                  <Button size="small" type="link" icon={<CheckCircleOutlined />} onClick={() => enablePrompt(record)} disabled={!canEnableAgentPrompt(record)}>
                    启用
                  </Button>
                  <Popconfirm title="确认停用该系统提示词？" onConfirm={() => disablePrompt(record)} disabled={!canDisableAgentPrompt(record)}>
                    <Button size="small" type="link" danger icon={<StopOutlined />} disabled={!canDisableAgentPrompt(record)}>
                      停用
                    </Button>
                  </Popconfirm>
                  <Button size="small" type="link" icon={<CopyOutlined />} onClick={() => copyPrompt(record.promptContent)}>
                    复制
                  </Button>
                  <Popconfirm
                    title={`确认回滚到版本 ${record.version}？`}
                    onConfirm={() => rollbackPrompt(record.version)}
                    disabled={record.status === 1}
                  >
                    <Button size="small" type="link" icon={<RollbackOutlined />} disabled={record.status === 1}>
                      回滚
                    </Button>
                  </Popconfirm>
                </Space>
              )
            }
          ]}
        />
      </Card>

      <Drawer
        title={drawerTitle}
        open={drawerOpen}
        onClose={() => {
          setDrawerOpen(false);
          setEditingPrompt(undefined);
          form.resetFields();
        }}
        width={820}
        destroyOnClose
      >
        <Form layout="vertical" form={form} onFinish={savePrompt}>
          <Form.Item name="promptName" initialValue="default" hidden>
            <Input />
          </Form.Item>
          <Form.Item
            label="系统提示词内容"
            name="promptContent"
            rules={[{ required: true, message: '请输入系统提示词内容' }]}
          >
            <TextArea
              rows={24}
              placeholder="请输入系统提示词"
              style={{
                fontFamily: "'JetBrains Mono', 'Cascadia Code', Consolas, monospace",
                lineHeight: 1.6,
                resize: 'vertical'
              }}
            />
          </Form.Item>
          <Space direction="vertical" size={12} style={{ width: '100%' }}>
            <Space wrap style={{ justifyContent: 'space-between', width: '100%' }}>
              <Form.Item
                label="保存后启用"
                name="enabled"
                valuePropName="checked"
                style={{ marginBottom: 0 }}
              >
                <Switch
                  checkedChildren="启用"
                  unCheckedChildren="停用"
                  disabled={editingPrompt?.status === 1}
                />
              </Form.Item>
              <Text type="secondary">
                {editorStats.characters} chars / {editorStats.lines} lines
              </Text>
            </Space>
            {editingPrompt?.status === 1 ? (
              <Alert
                type="warning"
                showIcon
                message="当前启用提示词保存后会生成新版本"
                description="旧版本会自动停用，新版本会保持启用，运行时提示词立即切换到新内容。"
              />
            ) : null}
            <Space>
              <Button type="primary" htmlType="submit" icon={<SaveOutlined />} loading={saving}>
                保存
              </Button>
              <Button
                onClick={() => {
                  setDrawerOpen(false);
                  setEditingPrompt(undefined);
                  form.resetFields();
                }}
              >
                取消
              </Button>
            </Space>
          </Space>
        </Form>
      </Drawer>

      <Drawer
        title={`提示词内容 ${previewPrompt?.version ? `v${previewPrompt.version}` : ''}`}
        open={Boolean(previewPrompt)}
        onClose={() => setPreviewPrompt(undefined)}
        width={760}
      >
        <Space direction="vertical" size={12} style={{ width: '100%' }}>
          <Space wrap>
            <Tag icon={<FileTextOutlined />}>{getAgentPromptSourceLabel(previewPrompt)}</Tag>
            <Tag color={previewPrompt?.status === 1 ? 'success' : 'default'}>
              {previewPrompt?.status === 1 ? '启用' : '停用'}
            </Tag>
            <Text type="secondary">
              {getPromptTextStats(previewPrompt?.promptContent).characters} chars
            </Text>
          </Space>
          <TextArea
            value={previewPrompt?.promptContent || ''}
            readOnly
            rows={24}
            style={{
              fontFamily: "'JetBrains Mono', 'Cascadia Code', Consolas, monospace",
              lineHeight: 1.6
            }}
          />
          <Button icon={<CopyOutlined />} onClick={() => copyPrompt(previewPrompt?.promptContent)}>
            复制内容
          </Button>
        </Space>
      </Drawer>
    </Space>
  );
}

function fallback(value?: string) {
  return value || '-';
}

function createAgentPromptRequest(data: AgentPromptUpdateRequest) {
  return http.post<RagApiResponse<RagAgentPrompt>>('/api/admin/agent-prompts', data);
}

function getActiveAgentPromptRequest() {
  return http.get<RagApiResponse<RagAgentPrompt | null>>('/api/admin/agent-prompts');
}

function listAgentPromptVersionsRequest() {
  return http.get<RagApiResponse<RagAgentPrompt[]>>('/api/admin/agent-prompts/versions');
}

function updateAgentPromptByIdRequest(id: number, data: AgentPromptUpdateRequest) {
  return http.put<RagApiResponse<RagAgentPrompt>>(`/api/admin/agent-prompts/${id}`, data);
}

function enableAgentPromptRequest(id: number) {
  return http.put<RagApiResponse<RagAgentPrompt>>(`/api/admin/agent-prompts/${id}/enable`);
}

function disableAgentPromptRequest(id: number) {
  return http.put<RagApiResponse<RagAgentPrompt>>(`/api/admin/agent-prompts/${id}/disable`);
}

function rollbackAgentPromptRequest(data: AgentPromptRollbackRequest) {
  return http.post<RagApiResponse<RagAgentPrompt>>('/api/admin/agent-prompts/rollback', data);
}
