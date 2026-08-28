import { DeleteOutlined, PictureOutlined, SearchOutlined, SendOutlined, UploadOutlined } from '@ant-design/icons';
import {
  App,
  Badge,
  Button,
  Card,
  Checkbox,
  Col,
  Descriptions,
  Empty,
  Form,
  Image as AntImage,
  Input,
  InputNumber,
  Row,
  Select,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
  Upload
} from 'antd';
import { useEffect, useMemo, useState } from 'react';
import { ragApi } from '../api/rag';
import PageHeaderCard from '../components/PageHeaderCard';
import type {
  ChatAnswer,
  RagImageSearchRequest,
  RagImageSearchResponse,
  RagKnowledgeBase,
  RagQueryResponse,
  RagSearchItem,
  RagSearchRequest,
  RagSearchResponse,
  SourceItem
} from '../types';
import { shouldRouteToAgentChat } from '../utils/chatRouting';
import {
  defaultImageOnlyFields,
  hasImageQueryInput,
  imageQueryFileError,
  readImageFileAsDataUrl
} from '../utils/imageQuery';
import { knowledgeBaseOptionLabel } from '../utils/knowledgeBases';
import { getErrorMessage } from '../utils/message';

const { TextArea } = Input;
const { Paragraph, Text } = Typography;

interface ChatFormValues {
  knowledgeBaseIds: number[];
  question?: string;
  imageUrl?: string;
  imageAssetId?: number;
  imageBase64?: string;
  modalities?: string[];
  includeReviewPending?: boolean;
  conversationId?: string;
  retrievalMode: string;
  topK: number;
  minScore: number;
  textVectorWeight?: number;
  imageVectorWeight?: number;
  keywordWeight?: number;
  contentTypes: string[];
  permissionTagsText?: string;
}

const contentTypeOptions = [
  { label: 'Text', value: 'text' },
  { label: 'Image', value: 'image' },
  { label: 'Chart', value: 'chart' },
  { label: 'Table', value: 'table' },
  { label: 'Flowchart', value: 'flowchart' },
  { label: 'Architecture', value: 'architecture' }
];

const retrievalModeOptions = [
  { label: 'Vector', value: 'vector' },
  { label: 'Hybrid', value: 'hybrid' },
  { label: 'Keyword', value: 'keyword' }
];

const modalityOptions = [
  { label: 'Text', value: 'text' },
  { label: 'Image', value: 'image' },
  { label: 'Multimodal', value: 'multimodal' }
];

export default function ChatPage() {
  const [form] = Form.useForm<ChatFormValues>();
  const { message } = App.useApp();
  const [detail, setDetail] = useState<RagQueryResponse>();
  const [agentDetail, setAgentDetail] = useState<ChatAnswer>();
  const [traceId, setTraceId] = useState<string>();
  const [searchResult, setSearchResult] = useState<RagSearchResponse>();
  const [imageSearchResult, setImageSearchResult] = useState<RagImageSearchResponse>();
  const [searchTraceId, setSearchTraceId] = useState<string>();
  const [knowledgeBases, setKnowledgeBases] = useState<RagKnowledgeBase[]>([]);
  const [knowledgeBaseLoading, setKnowledgeBaseLoading] = useState(false);
  const [loading, setLoading] = useState(false);
  const [searchLoading, setSearchLoading] = useState(false);
  const [imageReading, setImageReading] = useState(false);
  const [queryImagePreview, setQueryImagePreview] = useState<string>();
  const [queryImageFile, setQueryImageFile] = useState<File>();
  const [queryImageName, setQueryImageName] = useState<string>();

  useEffect(() => {
    let mounted = true;
    setKnowledgeBaseLoading(true);
    ragApi
      .listKnowledgeBases()
      .then(({ data }) => {
        if (!mounted) return;
        const items = data.data?.records || [];
        setKnowledgeBases(items);
        if (items.length && !form.getFieldValue('knowledgeBaseIds')?.length) {
          form.setFieldsValue({
            knowledgeBaseIds: [items[0].id]
          });
        }
      })
      .catch((error: unknown) => {
        if (mounted) message.warning(`Knowledge base load failed: ${getErrorMessage(error)}`);
      })
      .finally(() => {
        if (mounted) setKnowledgeBaseLoading(false);
      });

    return () => {
      mounted = false;
    };
  }, [form, message]);

  const knowledgeBaseOptions = useMemo(
    () =>
      knowledgeBases.map((kb) => ({
        label: knowledgeBaseOptionLabel(kb),
        value: kb.id
      })),
    [knowledgeBases]
  );

  const buildSearchRequest = (values: ChatFormValues): RagSearchRequest => ({
    knowledgeBaseIds: values.knowledgeBaseIds,
    query: trimToUndefined(values.question),
    imageUrl: trimToUndefined(values.imageUrl),
    imageAssetId: values.imageAssetId,
    imageBase64: trimToUndefined(values.imageBase64),
    modalities: values.modalities,
    includeReviewPending: values.includeReviewPending,
    retrievalMode: values.retrievalMode,
    topK: values.topK,
    minScore: values.minScore,
    textVectorWeight: values.textVectorWeight,
    imageVectorWeight: values.imageVectorWeight,
    keywordWeight: values.keywordWeight,
    contentTypes: values.contentTypes,
    permissionTags: splitTags(values.permissionTagsText)
  });

  const buildImageSearchRequest = (values: ChatFormValues): RagImageSearchRequest => ({
    knowledgeBaseIds: values.knowledgeBaseIds,
    question: trimToUndefined(values.question),
    retrievalMode: values.retrievalMode,
    topK: values.topK,
    minScore: values.minScore,
    includeReviewPending: values.includeReviewPending,
    contentTypes: values.contentTypes,
    permissionTags: splitTags(values.permissionTagsText)
  });

  const handleSubmit = async (values: ChatFormValues) => {
    try {
      setLoading(true);
      const question = trimToUndefined(values.question);
      if (shouldRouteToAgentChat(values) && question) {
        const { data } = await ragApi.chatDetail({
          conversationId: values.conversationId,
          question
        });
        if (!data.ok) throw new Error(data.error?.message || 'Agent chat failed');
        setTraceId(data.traceId);
        setAgentDetail(data.data);
        setDetail(undefined);
        setSearchResult(undefined);
        setImageSearchResult(undefined);
        message.success('Chat completed');
        return;
      }

      const request = buildSearchRequest(values);
      const { data } = await ragApi.ragQuery({
        knowledgeBaseIds: request.knowledgeBaseIds,
        question: request.query,
        imageUrl: request.imageUrl,
        imageAssetId: request.imageAssetId,
        imageBase64: request.imageBase64,
        modalities: request.modalities,
        includeReviewPending: request.includeReviewPending,
        conversationId: values.conversationId,
        retrievalMode: request.retrievalMode,
        topK: request.topK,
        minScore: request.minScore,
        textVectorWeight: request.textVectorWeight,
        imageVectorWeight: request.imageVectorWeight,
        keywordWeight: request.keywordWeight,
        contentTypes: request.contentTypes,
        permissionTags: request.permissionTags,
        enableRewrite: false,
        enableRerank: false,
        includeSources: true
      });
      if (!data.ok) throw new Error(data.error?.message || 'RAG query failed');
      setTraceId(data.traceId);
      setDetail(data.data);
      setAgentDetail(undefined);
      message.success('Query completed');
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  const handleSearchOnly = async () => {
    try {
      const values = await form.validateFields();
      await runSearchOnly(values);
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  };

  const handleImageSearchOnly = async () => {
    const currentValues = form.getFieldsValue();
    if (!queryImageFile && !hasImageQueryInput(currentValues)) {
      message.warning('Upload an image or provide an image URL/asset ID first');
      return;
    }
    try {
      const values = await form.validateFields();
      const imageDefaults = defaultImageOnlyFields();
      const imageOnlyValues = {
        ...values,
        ...imageDefaults,
        question: values.question
      };
      form.setFieldsValue({
        ...imageDefaults,
        question: values.question
      });
      await runImageSearchOnly(imageOnlyValues);
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  };

  const runSearchOnly = async (values: ChatFormValues) => {
    try {
      setSearchLoading(true);
      const { data } = await ragApi.ragSearch(buildSearchRequest(values));
      if (!data.ok) throw new Error(data.error?.message || 'RAG search failed');
      setSearchTraceId(data.traceId);
      setImageSearchResult(undefined);
      setSearchResult(data.data);
      message.success('Retrieval completed');
    } finally {
      setSearchLoading(false);
    }
  };

  const runImageSearchOnly = async (values: ChatFormValues) => {
    try {
      setSearchLoading(true);
      const request = buildImageSearchRequest(values);
      const { data } = queryImageFile
        ? await ragApi.imageSearchByFile(queryImageFile, request)
        : await ragApi.imageSearchByReference({
            ...request,
            imageUrl: trimToUndefined(values.imageUrl),
            imageAssetId: values.imageAssetId,
            imageBase64: trimToUndefined(values.imageBase64)
          });
      if (!data.ok) throw new Error(data.error?.message || 'Image search failed');
      setSearchTraceId(data.traceId);
      setSearchResult(undefined);
      setImageSearchResult(data.data);
      message.success('Image search completed');
    } finally {
      setSearchLoading(false);
    }
  };

  const handleImageFile = async (file: File) => {
    const error = imageQueryFileError(file);
    if (error) {
      message.warning(error);
      return;
    }
    try {
      setImageReading(true);
      const dataUrl = await readImageFileAsDataUrl(file);
      setQueryImagePreview(dataUrl);
      setQueryImageFile(file);
      setQueryImageName(file.name);
      form.setFieldsValue({
        ...defaultImageOnlyFields(),
        imageAssetId: undefined,
        imageBase64: dataUrl,
        imageUrl: undefined
      });
      message.success('Image loaded for retrieval');
    } catch (readError) {
      message.error(getErrorMessage(readError));
    } finally {
      setImageReading(false);
    }
  };

  const clearImageInput = () => {
    setQueryImagePreview(undefined);
    setQueryImageFile(undefined);
    setQueryImageName(undefined);
    setImageSearchResult(undefined);
    form.setFieldsValue({
      imageBase64: undefined
    });
  };

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <PageHeaderCard
        title="RAG Query Console"
        description="Run text, image or multimodal retrieval through /api/rag/query and /api/rag/search."
        tags={['Text Vector', 'Image Vector', 'Keyword', 'Fusion']}
      />

      <Card className="page-card" variant="borderless">
        <Form
          layout="vertical"
          form={form}
          onFinish={handleSubmit}
          initialValues={{
            conversationId: 'demo-001',
            retrievalMode: 'vector',
            topK: 8,
            minScore: 0.55,
            modalities: ['text', 'image'],
            includeReviewPending: false,
            textVectorWeight: 0.4,
            imageVectorWeight: 0.4,
            keywordWeight: 0.2,
            contentTypes: ['text', 'image', 'chart', 'table', 'flowchart', 'architecture']
          }}
        >
          <Row gutter={16}>
            <Col xs={24} md={12}>
              <Form.Item label="Conversation ID" name="conversationId">
                <Input placeholder="demo-001" />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item label="Retrieval mode" name="retrievalMode">
                <Select options={retrievalModeOptions} />
              </Form.Item>
            </Col>
            <Col xs={24}>
              <Form.Item
                label="Knowledge bases"
                name="knowledgeBaseIds"
                rules={[{ required: true, message: 'Select at least one knowledge base' }]}
              >
                <Select
                  mode="multiple"
                  loading={knowledgeBaseLoading}
                  options={knowledgeBaseOptions}
                  placeholder="Select knowledge bases"
                />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item label="Top K" name="topK" rules={[{ required: true, message: 'Top K is required' }]}>
                <InputNumber min={1} max={50} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item label="Min score" name="minScore" rules={[{ required: true, message: 'Min score is required' }]}>
                <InputNumber min={0} max={1} step={0.01} style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item label="Permission tags" name="permissionTagsText">
                <Input placeholder="internal, partner" />
              </Form.Item>
            </Col>
            <Col xs={24}>
              <Form.Item label="Content types" name="contentTypes">
                <Checkbox.Group options={contentTypeOptions} />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Form.Item
                label="Question"
                name="question"
                rules={[
                  ({ getFieldValue }) => ({
                    validator(_, value) {
                      if (
                        hasText(value) ||
                        hasText(getFieldValue('imageUrl')) ||
                        hasText(getFieldValue('imageBase64')) ||
                        getFieldValue('imageAssetId')
                      ) {
                        return Promise.resolve();
                      }
                      return Promise.reject(new Error('Provide a question or image input'));
                    }
                  })
                ]}
              >
                <TextArea rows={5} placeholder="Ask a question or leave empty for image-only retrieval." />
              </Form.Item>
            </Col>
            <Col xs={24} md={12}>
              <Row gutter={12}>
                <Col span={24}>
                  <Form.Item label="Upload image">
                    <Space direction="vertical" size={8} style={{ width: '100%' }}>
                      <Space wrap>
                        <Upload
                          accept="image/*"
                          beforeUpload={(file) => {
                            void handleImageFile(file);
                            return Upload.LIST_IGNORE;
                          }}
                          showUploadList={false}
                        >
                          <Button icon={<UploadOutlined />} loading={imageReading}>
                            Select Image
                          </Button>
                        </Upload>
                        {queryImagePreview ? (
                          <Button icon={<DeleteOutlined />} onClick={clearImageInput}>
                            Clear
                          </Button>
                        ) : null}
                      </Space>
                      {queryImagePreview ? (
                        <Space align="start">
                          <AntImage
                            width={112}
                            height={84}
                            src={queryImagePreview}
                            style={{ objectFit: 'cover', borderRadius: 6 }}
                          />
                          <Space direction="vertical" size={2}>
                            <Text strong>{queryImageName || 'Selected image'}</Text>
                            <Text type="secondary">Ready for image vector retrieval</Text>
                          </Space>
                        </Space>
                      ) : null}
                    </Space>
                  </Form.Item>
                </Col>
                <Col span={24}>
                  <Form.Item label="Image URL" name="imageUrl">
                    <Input placeholder="https://... or /api/knowledge/assets/..." />
                  </Form.Item>
                </Col>
                <Col xs={24} md={12}>
                  <Form.Item label="Image asset ID" name="imageAssetId">
                    <InputNumber min={1} style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
                <Col xs={24} md={12}>
                  <Form.Item label="Include review pending" name="includeReviewPending" valuePropName="checked">
                    <Checkbox>Allow pending image assets</Checkbox>
                  </Form.Item>
                </Col>
                <Col span={24}>
                  <Form.Item label="Image base64 or data URL" name="imageBase64">
                    <TextArea rows={2} placeholder="Optional image base64/data URL for query embedding." />
                  </Form.Item>
                </Col>
              </Row>
            </Col>
            <Col xs={24} md={8}>
              <Form.Item label="Modalities" name="modalities">
                <Select mode="multiple" options={modalityOptions} />
              </Form.Item>
            </Col>
            <Col xs={24} md={16}>
              <Row gutter={12}>
                <Col xs={24} md={8}>
                  <Form.Item label="Text vector weight" name="textVectorWeight">
                    <InputNumber min={0} max={1} step={0.05} style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
                <Col xs={24} md={8}>
                  <Form.Item label="Image vector weight" name="imageVectorWeight">
                    <InputNumber min={0} max={1} step={0.05} style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
                <Col xs={24} md={8}>
                  <Form.Item label="Keyword weight" name="keywordWeight">
                    <InputNumber min={0} max={1} step={0.05} style={{ width: '100%' }} />
                  </Form.Item>
                </Col>
              </Row>
            </Col>
          </Row>
          <Space wrap>
            <Button type="primary" htmlType="submit" loading={loading} icon={<SendOutlined />}>
              Ask
            </Button>
            <Button onClick={handleSearchOnly} loading={searchLoading} icon={<SearchOutlined />}>
              Retrieval Debug
            </Button>
            <Button onClick={handleImageSearchOnly} loading={searchLoading} icon={<PictureOutlined />}>
              Search by Image
            </Button>
            <Button
              onClick={() =>
                form.setFieldsValue({
                  question: 'Explain the system architecture diagram in the uploaded document.',
                  conversationId: 'rag-query-demo',
                  retrievalMode: 'hybrid',
                  modalities: ['text', 'image'],
                  contentTypes: ['text', 'image', 'chart', 'table', 'flowchart', 'architecture']
                })
              }
            >
              Architecture Example
            </Button>
            <Button
              onClick={() =>
                form.setFieldsValue({
                  ...defaultImageOnlyFields(),
                  imageAssetId: 1,
                  imageBase64: undefined,
                  imageUrl: undefined
                })
              }
            >
              Image Only Example
            </Button>
          </Space>
        </Form>
      </Card>

      <Spin spinning={loading || searchLoading}>
        {detail || agentDetail ? (
          <Row gutter={[16, 16]}>
            <Col xs={24} xl={16}>
              <Card className="page-card" variant="borderless" title="Answer">
                <Descriptions column={{ xs: 1, md: 2 }} bordered size="small">
                  <Descriptions.Item label="Answer path">{agentDetail ? 'Agent tools' : 'RAG query'}</Descriptions.Item>
                  <Descriptions.Item label="Trace ID">{traceId || '-'}</Descriptions.Item>
                  <Descriptions.Item label="Conversation ID">
                    {agentDetail?.conversationId || detail?.conversationId || '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="Query Log ID">{detail?.queryLogId ?? '-'}</Descriptions.Item>
                  <Descriptions.Item label="Knowledge hit">
                    <Badge
                      status={(agentDetail?.knowledgeHit ?? detail?.knowledgeHit) ? 'success' : 'default'}
                      text={String(agentDetail?.knowledgeHit ?? detail?.knowledgeHit ?? false)}
                    />
                  </Descriptions.Item>
                  {agentDetail ? (
                    <Descriptions.Item label="Weather tool">
                      <Badge
                        status={agentDetail.weatherUsed ? 'success' : 'default'}
                        text={String(agentDetail.weatherUsed)}
                      />
                    </Descriptions.Item>
                  ) : null}
                  <Descriptions.Item label="Source count">
                    {agentDetail?.sources?.length ?? detail?.sources?.length ?? 0}
                  </Descriptions.Item>
                  <Descriptions.Item label="Tokens">{detail?.usage?.totalTokens ?? '-'}</Descriptions.Item>
                </Descriptions>
                <div style={{ marginTop: 16 }} className="answer-box">
                  {agentDetail?.answer || detail?.answer}
                </div>
                {agentDetail?.toolTraces?.length ? (
                  <Space direction="vertical" size={8} style={{ marginTop: 16 }}>
                    {agentDetail.toolTraces.map((trace, index) => (
                      <Space key={`${trace.toolName}-${index}`} wrap>
                        <Tag color="processing">{trace.toolName}</Tag>
                        <Text type="secondary">{trace.summary}</Text>
                      </Space>
                    ))}
                  </Space>
                ) : null}
              </Card>
            </Col>
            <Col xs={24} xl={8}>
              <Card className="page-card metric-card" variant="borderless" title="Retrieval Trace">
                <Space direction="vertical" size={8}>
                  <Text strong>{agentDetail ? '/api/chat/detail' : '/api/rag/query'}</Text>
                  <Text type="secondary">
                    {agentDetail
                      ? 'Agent chat can invoke configured tools such as weatherForecast, webSearch and knowledgeSearch.'
                      : 'Sources include modality, retrieval source and fusion score when multimodal retrieval is active.'}
                  </Text>
                </Space>
              </Card>
            </Col>
            <Col span={24}>
              <Card className="page-card" variant="borderless" title="Sources">
                {agentDetail ? (
                  <AgentSourcesTable items={agentDetail.sources || []} />
                ) : (
                  <SearchItemsTable items={detail?.sources || []} />
                )}
              </Card>
            </Col>
          </Row>
        ) : (
          <Card className="page-card" variant="borderless">
            <Empty description="Run a query to view the answer and retrieved sources." />
          </Card>
        )}
      </Spin>

      {searchResult ? (
        <Card className="page-card" variant="borderless" title="Retrieval Debug Results">
          <Descriptions column={{ xs: 1, md: 3 }} bordered size="small" style={{ marginBottom: 16 }}>
            <Descriptions.Item label="Trace ID">{searchTraceId || '-'}</Descriptions.Item>
            <Descriptions.Item label="Query Log ID">{searchResult.queryLogId ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="Hit Count">{searchResult.items?.length || 0}</Descriptions.Item>
          </Descriptions>
          <SearchItemsTable items={searchResult.items || []} />
        </Card>
      ) : null}

      {imageSearchResult ? (
        <Card className="page-card" variant="borderless" title="Image Search Results">
          <Descriptions column={{ xs: 1, md: 4 }} bordered size="small" style={{ marginBottom: 16 }}>
            <Descriptions.Item label="Trace ID">{searchTraceId || '-'}</Descriptions.Item>
            <Descriptions.Item label="Query Log ID">{imageSearchResult.queryLogId ?? '-'}</Descriptions.Item>
            <Descriptions.Item label="Similar Images">
              {imageSearchResult.similarImages?.length || 0}
            </Descriptions.Item>
            <Descriptions.Item label="Related Knowledge">
              {imageSearchResult.relatedKnowledge?.length || 0}
            </Descriptions.Item>
          </Descriptions>
          <Typography.Title level={5}>Similar Images</Typography.Title>
          <SearchItemsTable items={imageSearchResult.similarImages || []} />
          <Typography.Title level={5} style={{ marginTop: 20 }}>
            Related Knowledge
          </Typography.Title>
          <SearchItemsTable items={imageSearchResult.relatedKnowledge || []} />
        </Card>
      ) : null}
    </Space>
  );
}

function AgentSourcesTable({ items }: { items: SourceItem[] }) {
  return (
    <Table<SourceItem>
      rowKey={(record, index) => `${record.type}-${record.title || record.url || index}`}
      pagination={{ pageSize: 5, showSizeChanger: true }}
      dataSource={items}
      columns={[
        { title: 'Type', dataIndex: 'type', width: 120, render: tag },
        { title: 'Title', dataIndex: 'title', width: 220, render: fallback },
        {
          title: 'URL',
          dataIndex: 'url',
          width: 260,
          render: (value?: string) =>
            value ? (
              <a href={value} target="_blank" rel="noreferrer">
                {value}
              </a>
            ) : (
              '-'
            )
        },
        {
          title: 'Content',
          dataIndex: 'content',
          render: (value?: string) => (
            <Paragraph ellipsis={{ rows: 3, expandable: true, symbol: 'More' }} style={{ marginBottom: 0 }}>
              {value || '-'}
            </Paragraph>
          )
        }
      ]}
    />
  );
}

function SearchItemsTable({ items }: { items: RagSearchItem[] }) {
  return (
    <Table<RagSearchItem>
      rowKey={(record) => `${record.rank}-${record.chunkId || record.documentId || record.imageAssetId || 'source'}`}
      pagination={{ pageSize: 5, showSizeChanger: true }}
      dataSource={items}
      scroll={{ x: 1500 }}
      columns={[
        { title: 'Rank', dataIndex: 'rank', width: 80 },
        {
          title: 'Score',
          dataIndex: 'score',
          width: 110,
          render: (value?: number) => (typeof value === 'number' ? value.toFixed(4) : '-')
        },
        {
          title: 'Fusion',
          dataIndex: 'fusionScore',
          width: 110,
          render: (value?: number) => (typeof value === 'number' ? value.toFixed(4) : '-')
        },
        { title: 'Modality', dataIndex: 'modality', width: 120, render: tag },
        { title: 'Source', dataIndex: 'retrievalSource', width: 150, render: tag },
        { title: 'Image Asset', dataIndex: 'imageAssetId', width: 120, render: (value?: number) => value ?? '-' },
        { title: 'KB', dataIndex: 'knowledgeBaseId', width: 90, render: (value?: number) => value ?? '-' },
        { title: 'Document', dataIndex: 'documentName', width: 180, render: fallback },
        { title: 'Document ID', dataIndex: 'documentId', width: 170, render: fallback },
        { title: 'Chunk', dataIndex: 'chunkId', width: 190, render: fallback },
        { title: 'Version', dataIndex: 'version', width: 90, render: (value?: number) => value ?? '-' },
        { title: 'Type', dataIndex: 'contentType', width: 110, render: tag },
        { title: 'Page', dataIndex: 'pageNo', width: 80, render: (value?: number) => value ?? '-' },
        { title: 'Section', dataIndex: 'sectionTitle', width: 180, render: fallback },
        {
          title: 'Image',
          dataIndex: 'imageUrl',
          width: 130,
          render: (value?: string) =>
            value ? (
              <Space direction="vertical" size={4}>
                <AntImage width={72} height={54} src={value} style={{ objectFit: 'cover', borderRadius: 6 }} />
                <a href={value} target="_blank" rel="noreferrer">
                  Open
                </a>
              </Space>
            ) : (
              '-'
            )
        },
        {
          title: 'Content',
          dataIndex: 'content',
          render: (value?: string) => (
            <Paragraph ellipsis={{ rows: 3, expandable: true, symbol: 'More' }} style={{ marginBottom: 0 }}>
              {value || '-'}
            </Paragraph>
          )
        }
      ]}
    />
  );
}

function splitTags(value?: string) {
  if (!value) return [];
  return value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean);
}

function trimToUndefined(value?: string) {
  if (!value || !value.trim()) return undefined;
  return value.trim();
}

function hasText(value?: string) {
  return !!value && !!value.trim();
}

function tag(value?: string) {
  return value ? <Tag>{value}</Tag> : '-';
}

function fallback(value?: string) {
  return value || '-';
}
