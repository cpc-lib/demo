import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  EditOutlined,
  EyeOutlined,
  ReloadOutlined,
  SearchOutlined,
  SyncOutlined,
  ToolOutlined
} from '@ant-design/icons';
import {
  App,
  Button,
  Card,
  Descriptions,
  Drawer,
  Form,
  Image as AntImage,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Table,
  Tag,
  Typography
} from 'antd';
import { useEffect, useState } from 'react';
import { ragApi } from '../api/rag';
import PageHeaderCard from '../components/PageHeaderCard';
import type { ImageAssetReviewRequest, RagImageAsset } from '../types';
import { getErrorMessage } from '../utils/message';

const { Paragraph, Text } = Typography;
const { TextArea } = Input;

interface FilterValues {
  knowledgeBaseId?: number;
  sourceDocumentId?: string;
  contentType?: string;
  visualStatus?: string;
  reviewStatus?: string;
  ocrStatus?: string;
  imageEmbeddingStatus?: string;
  minConfidence?: number;
  limit?: number;
}

const statusColors: Record<string, string> = {
  SUCCESS: 'success',
  INVALID: 'warning',
  FAILED: 'error',
  EMPTY: 'default',
  SKIPPED: 'default',
  AUTO_APPROVED: 'success',
  REVIEW_APPROVED: 'success',
  REVIEW_PENDING: 'warning',
  REVIEW_REJECTED: 'error'
};

export default function ImageAssetsPage() {
  const { message } = App.useApp();
  const [filterForm] = Form.useForm<FilterValues>();
  const [reviewForm] = Form.useForm<ImageAssetReviewRequest>();
  const [assets, setAssets] = useState<RagImageAsset[]>([]);
  const [detail, setDetail] = useState<RagImageAsset>();
  const [loading, setLoading] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState<number>();

  const loadAssets = async (pendingOnly = false) => {
    try {
      setLoading(true);
      const values = filterForm.getFieldsValue();
      const request = {
        knowledgeBaseId: values.knowledgeBaseId,
        sourceDocumentId: trimToUndefined(values.sourceDocumentId),
        contentType: values.contentType,
        visualStatus: values.visualStatus,
        reviewStatus: values.reviewStatus,
        ocrStatus: values.ocrStatus,
        imageEmbeddingStatus: values.imageEmbeddingStatus,
        minConfidence: values.minConfidence,
        limit: values.limit,
        sortBy: 'updatedAt',
        sortDirection: 'DESC'
      };
      const response = pendingOnly
        ? await ragApi.listReviewPendingImageAssets(request)
        : await ragApi.listImageAssets(request);
      setAssets(response.data.data?.records || []);
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAssets();
  }, []);

  const openDetail = async (record: RagImageAsset) => {
    try {
      setDetailLoading(true);
      const { data } = await ragApi.getImageAsset(record.id);
      setDetail(data.data);
      reviewForm.setFieldsValue({
        updatedVisualJson: formatJson(data.data.reviewUpdatedVisualJson || data.data.visualJson),
        updatedOcrText: data.data.reviewUpdatedOcrText || data.data.ocrText,
        operator: data.data.reviewedBy,
        comment: data.data.reviewComment
      });
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setDetailLoading(false);
    }
  };

  const refreshDetail = async (id: number) => {
    const { data } = await ragApi.getImageAsset(id);
    setDetail(data.data);
  };

  const approve = (record: RagImageAsset) => {
    Modal.confirm({
      title: `Approve image asset #${record.id}`,
      onOk: () => runAssetAction(record.id, () => ragApi.approveImageAsset(record.id, { operator: 'frontend' }))
    });
  };

  const reject = (record: RagImageAsset) => {
    Modal.confirm({
      title: `Reject image asset #${record.id}`,
      onOk: () => runAssetAction(record.id, () => ragApi.rejectImageAsset(record.id, { operator: 'frontend' }))
    });
  };

  const reprocess = (record: RagImageAsset) => {
    Modal.confirm({
      title: `Reprocess image asset #${record.id}`,
      content: 'OCR, visual analysis and image embedding will be retried when configured.',
      onOk: () =>
        runAssetAction(record.id, () =>
          ragApi.reprocessImageAsset(record.id, {
            ocr: true,
            visionAnalysis: true,
            imageEmbedding: true,
            operator: 'frontend'
          })
        )
    });
  };

  const updateReview = async () => {
    if (!detail) return;
    const values = await reviewForm.validateFields();
    await runAssetAction(detail.id, () => ragApi.updateImageAssetReview(detail.id, values));
  };

  const runAssetAction = async (id: number, action: () => Promise<unknown>) => {
    try {
      setActionLoading(id);
      await action();
      message.success('Done');
      await loadAssets();
      await refreshDetail(id).catch(() => undefined);
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setActionLoading(undefined);
    }
  };

  const ensureCollection = async () => {
    try {
      setLoading(true);
      const { data } = await ragApi.ensureMultimodalCollection();
      message.info(`${data.data.status}: ${data.data.message || data.data.collection}`);
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <PageHeaderCard
        title="Multimodal Image Assets"
        description="Review extracted image knowledge, OCR output, visual JSON, image embeddings and retrieval readiness."
        tags={['Image Asset', 'OCR', 'Image Vector', 'Review']}
      />

      <Card className="page-card" variant="borderless">
        <Form
          form={filterForm}
          layout="inline"
          initialValues={{ limit: 100 }}
          onFinish={() => loadAssets()}
          style={{ rowGap: 12, marginBottom: 16 }}
        >
          <Form.Item label="KB" name="knowledgeBaseId">
            <InputNumber min={0} style={{ width: 110 }} />
          </Form.Item>
          <Form.Item label="Document" name="sourceDocumentId">
            <Input allowClear placeholder="document_uid" style={{ width: 200 }} />
          </Form.Item>
          <Form.Item label="Type" name="contentType">
            <Select
              allowClear
              style={{ width: 140 }}
              options={['image', 'chart', 'table', 'flowchart', 'architecture'].map((value) => ({ label: value, value }))}
            />
          </Form.Item>
          <Form.Item label="Visual" name="visualStatus">
            <Select
              allowClear
              style={{ width: 140 }}
              options={['SUCCESS', 'INVALID', 'FAILED', 'EMPTY'].map((value) => ({ label: value, value }))}
            />
          </Form.Item>
          <Form.Item label="Review" name="reviewStatus">
            <Select
              allowClear
              style={{ width: 160 }}
              options={['AUTO_APPROVED', 'REVIEW_PENDING', 'REVIEW_APPROVED', 'REVIEW_REJECTED', 'FAILED', 'EMPTY'].map(
                (value) => ({ label: value, value })
              )}
            />
          </Form.Item>
          <Form.Item label="OCR" name="ocrStatus">
            <Select
              allowClear
              style={{ width: 130 }}
              options={['SUCCESS', 'FAILED', 'SKIPPED'].map((value) => ({ label: value, value }))}
            />
          </Form.Item>
          <Form.Item label="Embedding" name="imageEmbeddingStatus">
            <Select
              allowClear
              style={{ width: 140 }}
              options={['SUCCESS', 'FAILED', 'SKIPPED'].map((value) => ({ label: value, value }))}
            />
          </Form.Item>
          <Form.Item label="Min conf" name="minConfidence">
            <InputNumber min={0} max={1} step={0.05} style={{ width: 110 }} />
          </Form.Item>
          <Form.Item label="Limit" name="limit">
            <InputNumber min={1} max={200} style={{ width: 90 }} />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit" icon={<SearchOutlined />} loading={loading}>
                Search
              </Button>
              <Button icon={<ReloadOutlined />} onClick={() => loadAssets()} loading={loading} />
              <Button icon={<ToolOutlined />} onClick={() => loadAssets(true)} loading={loading}>
                Pending
              </Button>
              <Button icon={<SyncOutlined />} onClick={ensureCollection} loading={loading}>
                Ensure Collection
              </Button>
            </Space>
          </Form.Item>
        </Form>

        <Table<RagImageAsset>
          rowKey="id"
          loading={loading}
          dataSource={assets}
          pagination={{ pageSize: 10 }}
          scroll={{ x: 1800 }}
          columns={[
            { title: 'ID', dataIndex: 'id', width: 80 },
            {
              title: 'Preview',
              dataIndex: 'imageUrl',
              width: 96,
              render: (value?: string) =>
                value ? <AntImage width={56} height={56} src={value} style={{ objectFit: 'cover' }} /> : '-'
            },
            {
              title: 'Image ID',
              dataIndex: 'imageId',
              width: 220,
              ellipsis: true,
              render: (value: string) => <Text copyable>{value}</Text>
            },
            { title: 'Chunk', dataIndex: 'chunkUid', width: 180, ellipsis: true },
            { title: 'Type', dataIndex: 'contentType', width: 110, render: tag },
            { title: 'Visual', dataIndex: 'visualStatus', width: 120, render: tag },
            { title: 'OCR', dataIndex: 'ocrStatus', width: 110, render: tag },
            { title: 'Embedding', dataIndex: 'imageEmbeddingStatus', width: 130, render: tag },
            { title: 'Review', dataIndex: 'reviewStatus', width: 150, render: tag },
            {
              title: 'Conf',
              dataIndex: 'visualConfidence',
              width: 90,
              render: (value?: number) => (value === undefined || value === null ? '-' : value.toFixed(2))
            },
            { title: 'Page', dataIndex: 'pageNo', width: 80, render: (value?: number) => value || '-' },
            { title: 'Section', dataIndex: 'sectionTitle', width: 180, ellipsis: true, render: fallback },
            { title: 'Caption', dataIndex: 'imageCaption', ellipsis: true, render: fallback },
            { title: 'Updated', dataIndex: 'updatedAt', width: 180, render: formatDate },
            {
              title: 'Actions',
              width: 330,
              fixed: 'right',
              render: (_, record) => (
                <Space>
                  <Button icon={<EyeOutlined />} loading={detailLoading} onClick={() => openDetail(record)}>
                    Detail
                  </Button>
                  <Button
                    icon={<CheckCircleOutlined />}
                    loading={actionLoading === record.id}
                    onClick={() => approve(record)}
                  />
                  <Button
                    icon={<CloseCircleOutlined />}
                    loading={actionLoading === record.id}
                    onClick={() => reject(record)}
                  />
                  <Button
                    icon={<SyncOutlined />}
                    loading={actionLoading === record.id}
                    onClick={() => reprocess(record)}
                  />
                </Space>
              )
            }
          ]}
        />
      </Card>

      <Drawer
        title={detail ? `Image asset #${detail.id}` : 'Image asset'}
        open={!!detail}
        width={980}
        onClose={() => setDetail(undefined)}
      >
        {detail ? (
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
            {detail.imageUrl ? <AntImage src={detail.imageUrl} style={{ maxHeight: 360, objectFit: 'contain' }} /> : null}

            <Descriptions bordered size="small" column={2}>
              <Descriptions.Item label="Image ID">{detail.imageId}</Descriptions.Item>
              <Descriptions.Item label="Chunk UID">{detail.chunkUid || '-'}</Descriptions.Item>
              <Descriptions.Item label="Tenant">{detail.tenantId}</Descriptions.Item>
              <Descriptions.Item label="Knowledge Base">{detail.knowledgeBaseId}</Descriptions.Item>
              <Descriptions.Item label="Document UID">{detail.sourceDocumentId || '-'}</Descriptions.Item>
              <Descriptions.Item label="Version">{detail.documentVersionId ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="Type">{detail.contentType}</Descriptions.Item>
              <Descriptions.Item label="Page">{detail.pageNo ?? '-'}</Descriptions.Item>
              <Descriptions.Item label="Visual">{tag(detail.visualStatus)}</Descriptions.Item>
              <Descriptions.Item label="Schema">
                <Tag color={detail.visualSchemaValid ? 'success' : 'warning'}>
                  {detail.visualSchemaValid ? 'valid' : 'invalid'}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="OCR">{tag(detail.ocrStatus)}</Descriptions.Item>
              <Descriptions.Item label="Embedding">{tag(detail.imageEmbeddingStatus)}</Descriptions.Item>
              <Descriptions.Item label="Review">{tag(detail.reviewStatus)}</Descriptions.Item>
              <Descriptions.Item label="Reviewed by">{detail.reviewedBy || '-'}</Descriptions.Item>
              <Descriptions.Item label="Reviewed at">{formatDate(detail.reviewedAt)}</Descriptions.Item>
              <Descriptions.Item label="Updated">{formatDate(detail.updatedAt)}</Descriptions.Item>
            </Descriptions>

            <Section title="OCR text" value={detail.reviewUpdatedOcrText || detail.ocrText} />
            <Section title="Visual JSON" value={formatJson(detail.reviewUpdatedVisualJson || detail.visualJson)} />
            <Section title="Schema errors" value={formatJson(detail.visualSchemaErrors)} />
            <Section title="Text vector IDs" value={formatJson(detail.textVectorIds)} />
            <Section title="Image vector IDs" value={formatJson(detail.imageVectorIds)} />
            <Section title="Coordinate" value={formatJson(detail.coordinateJson)} />

            <Form form={reviewForm} layout="vertical">
              <Form.Item label="Operator" name="operator">
                <Input placeholder="reviewer" />
              </Form.Item>
              <Form.Item label="Comment" name="comment">
                <Input placeholder="review comment" />
              </Form.Item>
              <Form.Item label="Corrected OCR text" name="updatedOcrText">
                <TextArea rows={4} />
              </Form.Item>
              <Form.Item label="Corrected visual JSON" name="updatedVisualJson">
                <TextArea rows={8} />
              </Form.Item>
              <Button
                type="primary"
                icon={<EditOutlined />}
                loading={actionLoading === detail.id}
                onClick={updateReview}
              >
                Save Review Update
              </Button>
            </Form>
          </Space>
        ) : null}
      </Drawer>
    </Space>
  );
}

function Section({ title, value }: { title: string; value?: string }) {
  return (
    <div>
      <Text strong>{title}</Text>
      <Paragraph copyable style={{ whiteSpace: 'pre-wrap', marginTop: 8 }}>
        {value || '-'}
      </Paragraph>
    </div>
  );
}

function trimToUndefined(value?: string) {
  if (!value || !value.trim()) {
    return undefined;
  }
  return value.trim();
}

function tag(value?: string) {
  if (!value) {
    return '-';
  }
  return <Tag color={statusColors[value] || 'default'}>{value}</Tag>;
}

function fallback(value?: string) {
  return value || '-';
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

function formatJson(value?: string) {
  if (!value) {
    return '-';
  }
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
}
