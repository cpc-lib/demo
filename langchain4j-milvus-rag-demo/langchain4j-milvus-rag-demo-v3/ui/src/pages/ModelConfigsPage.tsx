import {
  App,
  Badge,
  Button,
  Card,
  Col,
  Descriptions,
  Drawer,
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
import {
  CheckCircleOutlined,
  DeleteOutlined,
  EditOutlined,
  PictureOutlined,
  PlusOutlined,
  ReloadOutlined,
  StopOutlined
} from '@ant-design/icons';
import { useEffect, useState } from 'react';
import { ragApi } from '../api/rag';
import PageHeaderCard from '../components/PageHeaderCard';
import type { ModelCacheStats, RagModelType, RagTenantModelConfig } from '../types';
import { getErrorMessage } from '../utils/message';
import {
  buildModelConfigFormValues,
  canDisableModelConfig,
  canEditModelConfig,
  canManageModelConfigApiKey,
  type ModelConfigFormValues,
  modelTypeColor,
  normalizeModelConfigFormRequest
} from '../utils/modelConfigs';

const { Paragraph, Text } = Typography;

const modelTypeOptions = [
  { label: 'LLM', value: 'LLM' },
  { label: 'Embedding', value: 'EMBEDDING' },
  { label: 'Image', value: 'IMAGE' }
];

type ApiKeyTarget = {
  id?: number;
  modelType: RagModelType;
  title: string;
  source: 'active' | 'config';
};

export default function ModelConfigsPage() {
  const { message } = App.useApp();
  const [form] = Form.useForm<ModelConfigFormValues>();
  const [keyForm] = Form.useForm<{ apiKeySecretRef: string }>();
  const [configs, setConfigs] = useState<RagTenantModelConfig[]>([]);
  const [activeLlm, setActiveLlm] = useState<RagTenantModelConfig>();
  const [activeEmbedding, setActiveEmbedding] = useState<RagTenantModelConfig>();
  const [activeImage, setActiveImage] = useState<RagTenantModelConfig>();
  const [cacheStats, setCacheStats] = useState<ModelCacheStats>();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [editingId, setEditingId] = useState<number>();
  const [editingConfig, setEditingConfig] = useState<RagTenantModelConfig>();
  const [apiKeyModalOpen, setApiKeyModalOpen] = useState(false);
  const [apiKeyTarget, setApiKeyTarget] = useState<ApiKeyTarget>();
  const [apiKeyLoading, setApiKeyLoading] = useState(false);
  const [apiKeySaving, setApiKeySaving] = useState(false);
  const selectedModelType = Form.useWatch('modelType', form) || 'LLM';
  const changeApiKey = Form.useWatch('changeApiKey', form) || false;
  const currentApiKeyValue = Form.useWatch('apiKeySecretRef', keyForm) || '';
  const isEditing = editingId !== undefined;

  const fetchModelConfigApiKey = async (config: RagTenantModelConfig) => {
    const modelType = config.modelType as RagModelType;
    const { data } = config.id
      ? await ragApi.getModelConfigApiKey(config.id)
      : await ragApi.getActiveModelConfigApiKey(modelType);
    return data.data?.apiKeySecretRef || '';
  };

  const loadData = async () => {
    try {
      setLoading(true);
      const [listRes, llmRes, embeddingRes, imageRes, statsRes] = await Promise.all([
        ragApi.listModelConfigs(),
        ragApi.getActiveLlmConfig(),
        ragApi.getActiveEmbeddingConfig(),
        ragApi.getActiveImageConfig(),
        ragApi.getModelCacheStats()
      ]);
      setConfigs(listRes.data.data || []);
      setActiveLlm(llmRes.data.data);
      setActiveEmbedding(embeddingRes.data.data);
      setActiveImage(imageRes.data.data);
      setCacheStats(statsRes.data.data);
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadData();
  }, []);

  const openCreate = (modelType: RagModelType = 'LLM') => {
    setEditingId(undefined);
    setEditingConfig(undefined);
    form.resetFields();
    form.setFieldsValue({
      modelType,
      provider: 'openai-compatible',
      changeApiKey: true,
      enabled: false,
      temperature: modelType === 'LLM' ? 0.2 : undefined,
      dimension: modelType === 'EMBEDDING' ? 1024 : undefined,
      imageSize: modelType === 'IMAGE' ? '1024x1024' : undefined,
      imageQuality: modelType === 'IMAGE' ? 'standard' : undefined,
      pollIntervalMillis: modelType === 'IMAGE' ? 2000 : undefined,
      timeoutSeconds: 60,
      maxRetries: modelType === 'IMAGE' ? 0 : 2
    });
    setDrawerOpen(true);
  };

  const openEdit = (record: RagTenantModelConfig) => {
    if (record.enabled) {
      message.warning('请先停用该模型配置，再编辑参数');
      return;
    }
    if (!canEditModelConfig(record)) {
      message.warning('Fallback config cannot be edited directly');
      return;
    }
    void openParameterEditor(record);
  };

  const openParameterEditor = async (record: RagTenantModelConfig) => {
    setEditingId(record.id);
    setEditingConfig(record);
    form.resetFields();
    try {
      setLoading(true);
      const apiKey = await fetchModelConfigApiKey(record);
      form.setFieldsValue(buildModelConfigFormValues(record, apiKey));
      setDrawerOpen(true);
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  const saveConfig = async (values: ModelConfigFormValues) => {
    try {
      setSaving(true);
      const request = normalizeModelConfigFormRequest(values, isEditing);
      if (editingId !== undefined) {
        await ragApi.updateModelConfig(editingId, request);
        message.success('模型配置已更新');
      } else {
        await ragApi.upsertModelConfig(request);
        message.success('模型配置已保存');
      }
      setDrawerOpen(false);
      setEditingId(undefined);
      setEditingConfig(undefined);
      form.resetFields();
      await loadData();
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setSaving(false);
    }
  };

  const enableConfig = async (id?: number) => {
    if (!id) return;
    try {
      await ragApi.enableModelConfig(id);
      message.success('模型配置已启用');
      await loadData();
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  };

  const disableConfig = async (id?: number) => {
    if (!id) return undefined;
    try {
      const { data } = await ragApi.disableModelConfig(id);
      message.success('模型配置已停用');
      await loadData();
      return data.data;
    } catch (error) {
      message.error(getErrorMessage(error));
      return undefined;
    }
  };

  const disableAndEdit = async (record?: RagTenantModelConfig) => {
    if (!record?.id) {
      return;
    }
    const disabled = await disableConfig(record.id);
    if (disabled) {
      await openParameterEditor(disabled);
    }
  };

  const openApiKeyModal = async (target: ApiKeyTarget) => {
    setApiKeyTarget(target);
    setApiKeyModalOpen(true);
    keyForm.resetFields();
    try {
      setApiKeyLoading(true);
      const { data } =
        target.source === 'config' && target.id
          ? await ragApi.getModelConfigApiKey(target.id)
          : await ragApi.getActiveModelConfigApiKey(target.modelType);
      keyForm.setFieldsValue({ apiKeySecretRef: data.data?.apiKeySecretRef || '' });
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setApiKeyLoading(false);
    }
  };

  const saveApiKey = async (values: { apiKeySecretRef: string }) => {
    if (!apiKeyTarget) {
      return;
    }
    try {
      setApiKeySaving(true);
      const request = { apiKeySecretRef: values.apiKeySecretRef.trim() };
      if (apiKeyTarget.source === 'config' && apiKeyTarget.id) {
        await ragApi.updateModelConfigApiKey(apiKeyTarget.id, request);
      } else {
        await ragApi.updateActiveModelConfigApiKey(apiKeyTarget.modelType, request);
      }
      message.success('API Key 已更新');
      setApiKeyModalOpen(false);
      setApiKeyTarget(undefined);
      keyForm.resetFields();
      await loadData();
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setApiKeySaving(false);
    }
  };

  const copyApiKey = async () => {
    const value = keyForm.getFieldValue('apiKeySecretRef');
    if (!value) {
      message.warning('当前没有可复制的 API Key');
      return;
    }
    try {
      await navigator.clipboard.writeText(value);
      message.success('API Key 已复制');
    } catch {
      message.error('复制失败，请手动复制');
    }
  };

  const deleteConfig = async (id?: number) => {
    if (!id) return;
    try {
      await ragApi.deleteModelConfig(id);
      message.success('模型配置已删除');
      await loadData();
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  };

  const findOwnedConfig = (config?: RagTenantModelConfig) => {
    if (!config?.id) {
      return undefined;
    }
    return configs.find((item) => item.id === config.id);
  };

  const runCacheAction = async (action: () => Promise<unknown>, successText: string) => {
    try {
      setLoading(true);
      await action();
      message.success(successText);
      await loadData();
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <PageHeaderCard
        title="模型配置管理"
        description="管理当前租户的 LLM、Embedding 与文生图模型配置，并控制动态模型缓存。"
        tags={['LLM', 'Embedding', 'Image', 'Tenant Scoped', 'Cache']}
        extra={
          <Space wrap>
            <Button icon={<ReloadOutlined />} onClick={loadData} loading={loading}>
              刷新
            </Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={() => openCreate('LLM')}>
              新增配置
            </Button>
          </Space>
        }
      />

      <Row gutter={[16, 16]}>
        <Col xs={24} xl={16}>
          <Card className="page-card" variant="borderless" title="当前生效模型" loading={loading}>
            <Row gutter={[16, 16]}>
              <Col xs={24} lg={8}>
                <ConfigSummary
                  title="LLM"
                  config={activeLlm}
                  ownedConfig={findOwnedConfig(activeLlm)}
                  onCreate={() => openCreate('LLM')}
                  onDisableAndEdit={disableAndEdit}
                  onEdit={openEdit}
                  onEditParameters={(record) => void openParameterEditor(record)}
                  onManageApiKey={(target) => void openApiKeyModal(target)}
                />
              </Col>
              <Col xs={24} lg={8}>
                <ConfigSummary
                  title="Embedding"
                  config={activeEmbedding}
                  ownedConfig={findOwnedConfig(activeEmbedding)}
                  onCreate={() => openCreate('EMBEDDING')}
                  onDisableAndEdit={disableAndEdit}
                  onEdit={openEdit}
                  onEditParameters={(record) => void openParameterEditor(record)}
                  onManageApiKey={(target) => void openApiKeyModal(target)}
                />
              </Col>
              <Col xs={24} lg={8}>
                <ConfigSummary
                  title="Image"
                  config={activeImage}
                  ownedConfig={findOwnedConfig(activeImage)}
                  onCreate={() => openCreate('IMAGE')}
                  onDisableAndEdit={disableAndEdit}
                  onEdit={openEdit}
                  onEditParameters={(record) => void openParameterEditor(record)}
                  onManageApiKey={(target) => void openApiKeyModal(target)}
                />
              </Col>
            </Row>
          </Card>
        </Col>
        <Col xs={24} xl={8}>
          <Card className="page-card" variant="borderless" title="模型缓存" loading={loading}>
            <Descriptions bordered column={2} size="small">
              <Descriptions.Item label="Total">{cacheStats?.totalCacheSize ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="LLM">{cacheStats?.llmCacheSize ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="Streaming">{cacheStats?.streamingLlmCacheSize ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="Embedding">{cacheStats?.embeddingCacheSize ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="LLM Hit">{formatRate(cacheStats?.llmHitRate)}</Descriptions.Item>
              <Descriptions.Item label="Embedding Hit">{formatRate(cacheStats?.embeddingHitRate)}</Descriptions.Item>
            </Descriptions>
            <Space wrap style={{ marginTop: 16 }}>
              <Button onClick={() => runCacheAction(ragApi.reloadModelConfigs, '配置缓存已重新加载')}>
                Reload Config
              </Button>
              <Button onClick={() => runCacheAction(ragApi.invalidateTenantModels, '当前租户模型缓存已失效')}>
                Invalidate Tenant
              </Button>
              <Popconfirm
                title="确认清空所有租户的动态模型缓存？"
                onConfirm={() => runCacheAction(ragApi.invalidateAllModels, '全部模型缓存已失效')}
              >
                <Button danger>Invalidate All</Button>
              </Popconfirm>
              <Button onClick={() => runCacheAction(ragApi.resetModelCacheStats, '缓存统计已重置')}>
                Reset Stats
              </Button>
            </Space>
          </Card>
        </Col>
      </Row>

      <Card className="page-card" variant="borderless" title="全部模型配置">
        <Table<RagTenantModelConfig>
          rowKey={(record) => String(record.id ?? `${record.modelType}-${record.provider}-${record.modelName}`)}
          loading={loading}
          dataSource={configs}
          scroll={{ x: 1400 }}
          pagination={{ pageSize: 8, showSizeChanger: true }}
          columns={[
            {
              title: '类型',
              dataIndex: 'modelType',
              width: 120,
              render: (value: string) => <Tag color={modelTypeColor(value)}>{value}</Tag>
            },
            {
              title: '状态',
              dataIndex: 'enabled',
              width: 110,
              render: (value?: boolean, record?: RagTenantModelConfig) => (
                <Badge
                  status={value ? 'success' : 'default'}
                  text={!record?.id && value ? 'Fallback' : value ? 'Enabled' : 'Disabled'}
                />
              )
            },
            { title: 'Provider', dataIndex: 'provider', width: 160 },
            { title: 'Model', dataIndex: 'modelName', width: 210, ellipsis: true },
            { title: 'Base URL', dataIndex: 'baseUrl', width: 280, ellipsis: true, render: fallback },
            {
              title: 'Key',
              dataIndex: 'apiKeyConfigured',
              width: 160,
              render: (value: boolean | undefined, record) => (
                <Space size={4}>
                  {value ? <Tag color="success">Set</Tag> : <Tag>None</Tag>}
                  <Button
                    size="small"
                    type="link"
                    disabled={!canManageModelConfigApiKey(record)}
                    onClick={() =>
                      openApiKeyModal({
                        id: record.id,
                        modelType: record.modelType as RagModelType,
                        title: `${record.modelType} / ${record.modelName}`,
                        source: record.id ? 'config' : 'active'
                      })
                    }
                  >
                    只改 Key
                  </Button>
                </Space>
              )
            },
            { title: 'Temp', dataIndex: 'temperature', width: 90, render: numberFallback },
            { title: 'Dim', dataIndex: 'dimension', width: 90, render: numberFallback },
            { title: 'Image Size', dataIndex: 'imageSize', width: 130, render: fallback },
            { title: 'Quality', dataIndex: 'imageQuality', width: 110, render: fallback },
            { title: 'Poll ms', dataIndex: 'pollIntervalMillis', width: 100, render: numberFallback },
            { title: 'Timeout', dataIndex: 'timeoutSeconds', width: 100, render: numberFallback },
            { title: 'Retries', dataIndex: 'maxRetries', width: 90, render: numberFallback },
            { title: 'Max Tokens', dataIndex: 'maxTokens', width: 110, render: numberFallback },
            { title: 'Top P', dataIndex: 'topP', width: 90, render: numberFallback },
            { title: 'Updated', dataIndex: 'updatedAt', width: 180, render: fallback },
            {
              title: '操作',
              fixed: 'right',
              width: 230,
              render: (_, record) => (
                <Space size={4}>
                  <Button
                    size="small"
                    type="link"
                    icon={<CheckCircleOutlined />}
                    disabled={record.enabled || !record.id}
                    onClick={() => enableConfig(record.id)}
                  >
                    启用
                  </Button>
                  <Button
                    size="small"
                    type="link"
                    icon={<StopOutlined />}
                    disabled={!canDisableModelConfig(record)}
                    onClick={() => disableConfig(record.id)}
                  >
                    停用
                  </Button>
                  <Button
                    size="small"
                    type="link"
                    icon={<EditOutlined />}
                    onClick={() =>
                      record.id && record.enabled
                        ? void disableAndEdit(record)
                        : record.id
                          ? openEdit(record)
                          : void openParameterEditor(record)
                    }
                  >
                    {record.id && record.enabled ? '停用编辑' : '参数'}
                  </Button>
                  <Popconfirm title="确认删除该模型配置？" onConfirm={() => deleteConfig(record.id)}>
                    <Button size="small" type="link" danger icon={<DeleteOutlined />} disabled={!record.id || record.enabled}>
                      删除
                    </Button>
                  </Popconfirm>
                </Space>
              )
            }
          ]}
        />
      </Card>

      <Drawer
        title={editingId ? '编辑模型配置' : '新增模型配置'}
        open={drawerOpen}
        onClose={() => {
          setDrawerOpen(false);
          setEditingId(undefined);
          setEditingConfig(undefined);
        }}
        width={720}
        destroyOnClose
      >
        <Form layout="vertical" form={form} onFinish={saveConfig}>
          <Row gutter={16}>
            <Col xs={24} md={8}>
              <Form.Item label="模型类型" name="modelType" rules={[{ required: true }]}>
                <Select options={modelTypeOptions} />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item label="Provider" name="provider">
                <Input placeholder="openai-compatible" />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item label="模型名称" name="modelName" rules={[{ required: true }]}>
                <Input placeholder="qwen-plus" />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item label="保存后启用" name="enabled" valuePropName="checked">
                <Switch checkedChildren="启用" unCheckedChildren="停用" />
              </Form.Item>
            </Col>
            <Col xs={24}>
              <Form.Item label="Base URL" name="baseUrl">
                <Input placeholder="https://dashscope.aliyuncs.com/compatible-mode/v1" />
              </Form.Item>
            </Col>
            <Col xs={24}>
        {isEditing ? (
                <Form.Item label="API Key" name="changeApiKey" valuePropName="checked">
                  <Switch
                    checkedChildren="替换"
                    unCheckedChildren={editingConfig?.apiKeyConfigured ? '保持已配置' : '未配置'}
                  />
                </Form.Item>
              ) : null}
              {!isEditing || changeApiKey ? (
                <Form.Item
                  label={isEditing ? '新的 API Key / Secret Ref' : 'API Key / Secret Ref'}
                  name="apiKeySecretRef"
                >
                  <Input.Password placeholder="sk-... 或密钥引用" autoComplete="new-password" />
                </Form.Item>
              ) : (
                <Text type="secondary">{editingConfig?.apiKeyConfigured ? '当前 API Key 已配置' : '当前未配置 API Key'}</Text>
              )}
            </Col>
            {selectedModelType === 'EMBEDDING' ? (
              <Col xs={24} md={8}>
                <Form.Item label="Embedding 维度" name="dimension" rules={[{ required: true }]}>
                  <InputNumber min={1} max={16384} style={{ width: '100%' }} />
                </Form.Item>
              </Col>
            ) : selectedModelType === 'IMAGE' ? (
              <>
                <Col xs={24} md={8}>
                  <Form.Item label="图片尺寸" name="imageSize" rules={[{ required: true }]}>
                    <Input placeholder="1024x1024" />
                  </Form.Item>
                </Col>
                <Col xs={24} md={8}>
                  <Form.Item label="图片质量" name="imageQuality">
                    <Input placeholder="standard / hd" />
                  </Form.Item>
                </Col>
                <Col xs={24} md={8}>
                  <Form.Item label="轮询间隔 ms" name="pollIntervalMillis">
                    <InputNumber min={500} max={60000} step={100} style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
              </>
            ) : (
              <>
                <Col xs={24} md={8}>
                  <Form.Item label="Temperature" name="temperature">
                    <InputNumber min={0} max={2} step={0.01} style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
                <Col xs={24} md={8}>
                  <Form.Item label="Max Tokens" name="maxTokens">
                    <InputNumber min={1} max={32768} style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
                <Col xs={24} md={8}>
                  <Form.Item label="Top P" name="topP">
                    <InputNumber min={0} max={1} step={0.01} style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
                <Col xs={24} md={8}>
                  <Form.Item label="Frequency Penalty" name="frequencyPenalty">
                    <InputNumber min={-2} max={2} step={0.01} style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
                <Col xs={24} md={8}>
                  <Form.Item label="Presence Penalty" name="presencePenalty">
                    <InputNumber min={-2} max={2} step={0.01} style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
              </>
            )}
            <Col xs={24} md={8}>
              <Form.Item label="Timeout 秒" name="timeoutSeconds">
                <InputNumber min={1} max={600} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item label="Max Retries" name="maxRetries">
                <InputNumber min={0} max={10} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item label="Rate Limit QPS" name="rateLimitQps">
                <InputNumber min={1} max={10000} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item label="Monthly Budget Cents" name="monthlyBudgetCents">
                <InputNumber min={0} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
          </Row>
          <Space>
            <Button type="primary" htmlType="submit" loading={saving}>
              保存
            </Button>
            <Button onClick={() => setDrawerOpen(false)}>取消</Button>
          </Space>
        </Form>
      </Drawer>

      <Modal
        title={`${apiKeyTarget?.title || ''} API Key`}
        open={apiKeyModalOpen}
        onCancel={() => {
          setApiKeyModalOpen(false);
          setApiKeyTarget(undefined);
          keyForm.resetFields();
        }}
        onOk={() => keyForm.submit()}
        confirmLoading={apiKeySaving}
        okText="保存"
        cancelText="取消"
      >
        <Form layout="vertical" form={keyForm} onFinish={saveApiKey}>
          <Form.Item
            label="API Key / Secret Ref"
            name="apiKeySecretRef"
            rules={[{ required: true, message: '请输入 API Key / Secret Ref' }]}
          >
            <Input.Password placeholder="sk-... 或密钥引用" autoComplete="new-password" disabled={apiKeyLoading} />
          </Form.Item>
          <Space>
            <Button onClick={copyApiKey} disabled={!currentApiKeyValue || apiKeyLoading}>
              复制
            </Button>
            <Text type="secondary">保存只会替换 API Key，不会修改其它模型参数。</Text>
          </Space>
        </Form>
      </Modal>
    </Space>
  );
}

function ConfigSummary({
  title,
  config,
  ownedConfig,
  onCreate,
  onDisableAndEdit,
  onEdit,
  onEditParameters,
  onManageApiKey
}: {
  title: string;
  config?: RagTenantModelConfig;
  ownedConfig?: RagTenantModelConfig;
  onCreate: () => void;
  onDisableAndEdit: (record?: RagTenantModelConfig) => void;
  onEdit: (record: RagTenantModelConfig) => void;
  onEditParameters: (record: RagTenantModelConfig) => void;
  onManageApiKey: (target: ApiKeyTarget) => void;
}) {
  const modelType = (config?.modelType || titleToModelType(title)) as RagModelType;
  return (
    <Space direction="vertical" size={8} style={{ width: '100%' }}>
      <Space>
        <Tag color={modelTypeColor(modelType)}>
          {modelType === 'IMAGE' ? <PictureOutlined /> : null} {title}
        </Tag>
        <Badge status={config?.enabled ? 'success' : 'default'} text={config?.enabled ? 'Enabled' : 'Fallback'} />
      </Space>
      <Descriptions bordered column={1} size="small">
        <Descriptions.Item label="Provider">{config?.provider || '-'}</Descriptions.Item>
        <Descriptions.Item label="Model">{config?.modelName || '-'}</Descriptions.Item>
        <Descriptions.Item label="Base URL">
          <Paragraph ellipsis={{ rows: 2, expandable: true, symbol: 'More' }} style={{ marginBottom: 0 }}>
            {config?.baseUrl || '-'}
          </Paragraph>
        </Descriptions.Item>
        <Descriptions.Item label={modelType === 'EMBEDDING' ? 'Dimension' : modelType === 'IMAGE' ? 'Image Size' : 'Temperature'}>
          {modelType === 'EMBEDDING'
            ? config?.dimension ?? '-'
            : modelType === 'IMAGE'
              ? config?.imageSize ?? '-'
              : config?.temperature ?? '-'}
        </Descriptions.Item>
        {modelType === 'IMAGE' ? (
          <Descriptions.Item label="Quality / Poll">
            {config?.imageQuality || '-'} / {config?.pollIntervalMillis ?? '-'} ms
          </Descriptions.Item>
        ) : null}
        <Descriptions.Item label="API Key">
          <Space size={8}>
            <Text type={config?.apiKeyConfigured ? 'success' : 'secondary'}>
              {config?.apiKeyConfigured ? 'Configured' : 'Not configured'}
            </Text>
            {canManageModelConfigApiKey(config) ? (
              <Button
                size="small"
                type="link"
                onClick={() =>
                  onManageApiKey({
                    modelType,
                    title,
                    source: 'active'
                  })
                }
              >
                只改 Key
              </Button>
            ) : null}
          </Space>
        </Descriptions.Item>
      </Descriptions>
      <Space wrap>
        {canDisableModelConfig(ownedConfig) ? (
          <Button size="small" icon={<StopOutlined />} onClick={() => onDisableAndEdit(ownedConfig)}>
            停用并编辑
          </Button>
        ) : null}
        {canEditModelConfig(ownedConfig) ? (
          <Button size="small" icon={<EditOutlined />} onClick={() => onEdit(ownedConfig)}>
            编辑参数
          </Button>
        ) : null}
        {!ownedConfig && config ? (
          <Button size="small" type="primary" icon={<EditOutlined />} onClick={() => onEditParameters(config)}>
            查看/编辑参数
          </Button>
        ) : null}
        {!ownedConfig && !config ? (
          <Button size="small" type="primary" icon={<PlusOutlined />} onClick={onCreate}>
            新增配置
          </Button>
        ) : null}
      </Space>
    </Space>
  );
}

function titleToModelType(title: string): RagModelType {
  if (title === 'Embedding') {
    return 'EMBEDDING';
  }
  if (title === 'Image') {
    return 'IMAGE';
  }
  return 'LLM';
}

function formatRate(value?: number) {
  return typeof value === 'number' ? `${(value * 100).toFixed(1)}%` : '-';
}

function numberFallback(value?: number) {
  return typeof value === 'number' ? value : '-';
}

function fallback(value?: string) {
  return value || '-';
}
