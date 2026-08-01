import {
  App,
  Button,
  Card,
  Col,
  Divider,
  Form,
  Input,
  InputNumber,
  Row,
  Space,
  Switch,
  Table,
  Tabs,
  Typography
} from 'antd';
import { useState } from 'react';
import PageHeaderCard from '../components/PageHeaderCard';
import { ragApi } from '../api/rag';
import type { MilvusCollectionQueryRequest, MilvusCreateCollectionRequest } from '../types';
import { getErrorMessage } from '../utils/message';
import {
  getMilvusCollectionName,
  type MilvusCollectionRow,
  normalizeMilvusCollectionRows
} from '../utils/milvusCollections';

const { TextArea } = Input;
const { Paragraph } = Typography;

export default function CollectionsPage() {
  const { message } = App.useApp();
  const [databaseName, setDatabaseName] = useState('default');
  const [collections, setCollections] = useState<MilvusCollectionRow[]>([]);
  const [describeResult, setDescribeResult] = useState<any>();
  const [queryResult, setQueryResult] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);

  const loadCollections = async () => {
    try {
      setLoading(true);
      const { data } = await ragApi.listCollections(databaseName);
      setCollections(normalizeMilvusCollectionRows(data.data?.records || []));
      message.success('集合列表已刷新');
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  const describeCollection = async (collectionName: string) => {
    const safeCollectionName = getMilvusCollectionName(collectionName);
    if (!safeCollectionName) {
      message.warning('请选择有效的集合');
      return;
    }
    try {
      const { data } = await ragApi.describeCollection(safeCollectionName, databaseName);
      setDescribeResult(data.data);
      message.success(`已读取 ${safeCollectionName} 详情`);
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  };

  const createCollection = async (values: MilvusCreateCollectionRequest) => {
    try {
      await ragApi.createCollection(values);
      message.success('集合创建成功');
      await loadCollections();
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  };

  const queryCollection = async (values: MilvusCollectionQueryRequest & { outputFieldsText?: string; partitionNamesText?: string }) => {
    try {
      const payload: MilvusCollectionQueryRequest = {
        databaseName: values.databaseName,
        collectionName: values.collectionName,
        filter: values.filter,
        outputFields: values.outputFieldsText
          ? values.outputFieldsText.split(',').map((item) => item.trim()).filter(Boolean)
          : [],
        partitionNames: values.partitionNamesText
          ? values.partitionNamesText.split(',').map((item) => item.trim()).filter(Boolean)
          : [],
        offset: values.offset,
        limit: values.limit,
        loadBeforeQuery: values.loadBeforeQuery
      };
      const { data } = await ragApi.queryCollection(payload);
      setQueryResult(data.data || []);
      message.success('查询完成');
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  };

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <PageHeaderCard
        title="Milvus 集合管理"
        description="覆盖 /api/milvus/collections 的列表、详情、建表与 Query 接口，方便直接验证底层向量集合。"
        tags={['集合列表', 'Describe', 'Create Collection', 'Query']}
        extra={
          <Space>
            <Input
              value={databaseName}
              onChange={(e) => setDatabaseName(e.target.value)}
              placeholder="databaseName"
              style={{ width: 180 }}
            />
            <Button type="primary" onClick={loadCollections} loading={loading}>
              刷新集合
            </Button>
          </Space>
        }
      />

      <Card className="page-card" variant="borderless" title="集合列表">
        <Table
          rowKey={(record, index) => String(record.collectionName || record.name || index)}
          loading={loading}
          dataSource={collections}
          pagination={false}
          columns={[
            { title: '集合名', dataIndex: 'collectionName', render: (v, r: MilvusCollectionRow) => v || r.name || '-' },
            { title: '描述', dataIndex: 'description', render: (v) => v || '-' },
            {
              title: '操作',
              render: (_, record: MilvusCollectionRow) => (
                <Button size="small" type="link" onClick={() => describeCollection(record.collectionName)}>
                  查看详情
                </Button>
              )
            }
          ]}
        />
      </Card>

      <Tabs
        items={[
          {
            key: 'describe',
            label: '集合详情',
            children: (
              <Card className="page-card" variant="borderless">
                <pre className="code-block">{JSON.stringify(describeResult || { tip: '请先点击上方“查看详情”' }, null, 2)}</pre>
              </Card>
            )
          },
          {
            key: 'create',
            label: '创建集合',
            children: (
              <Card className="page-card" variant="borderless">
                <Form layout="vertical" onFinish={createCollection} initialValues={{ databaseName, dimension: 1024, metricType: 'COSINE', numShards: 1, enableDynamicField: true, autoId: false }}>
                  <Row gutter={16}>
                    <Col xs={24} md={8}><Form.Item label="数据库" name="databaseName"><Input /></Form.Item></Col>
                    <Col xs={24} md={8}><Form.Item label="集合名" name="collectionName" rules={[{ required: true }]}><Input /></Form.Item></Col>
                    <Col xs={24} md={8}><Form.Item label="描述" name="description"><Input /></Form.Item></Col>
                    <Col xs={24} md={8}><Form.Item label="向量维度" name="dimension" rules={[{ required: true }]}><InputNumber min={1} style={{ width: '100%' }} /></Form.Item></Col>
                    <Col xs={24} md={8}><Form.Item label="主键字段" name="primaryFieldName"><Input placeholder="id" /></Form.Item></Col>
                    <Col xs={24} md={8}><Form.Item label="ID 类型" name="idType"><Input placeholder="VarChar / Int64" /></Form.Item></Col>
                    <Col xs={24} md={8}><Form.Item label="maxLength" name="maxLength"><InputNumber min={1} style={{ width: '100%' }} /></Form.Item></Col>
                    <Col xs={24} md={8}><Form.Item label="向量字段" name="vectorFieldName"><Input placeholder="vector" /></Form.Item></Col>
                    <Col xs={24} md={8}><Form.Item label="MetricType" name="metricType"><Input placeholder="COSINE" /></Form.Item></Col>
                    <Col xs={24} md={8}><Form.Item label="分片数" name="numShards"><InputNumber min={1} style={{ width: '100%' }} /></Form.Item></Col>
                    <Col xs={12} md={4}><Form.Item label="Auto ID" name="autoId" valuePropName="checked"><Switch /></Form.Item></Col>
                    <Col xs={12} md={4}><Form.Item label="动态字段" name="enableDynamicField" valuePropName="checked"><Switch /></Form.Item></Col>
                  </Row>
                  <Button type="primary" htmlType="submit">创建集合</Button>
                </Form>
              </Card>
            )
          },
          {
            key: 'query',
            label: 'Query 测试',
            children: (
              <Card className="page-card" variant="borderless">
                <Form
                  layout="vertical"
                  onFinish={queryCollection}
                  initialValues={{ databaseName, offset: 0, limit: 10, loadBeforeQuery: true }}
                >
                  <Row gutter={16}>
                    <Col xs={24} md={8}><Form.Item label="数据库" name="databaseName"><Input /></Form.Item></Col>
                    <Col xs={24} md={8}><Form.Item label="集合名" name="collectionName" rules={[{ required: true }]}><Input /></Form.Item></Col>
                    <Col xs={24} md={8}><Form.Item label="Filter" name="filter"><Input placeholder="例如：id > 0" /></Form.Item></Col>
                    <Col xs={24} md={12}><Form.Item label="输出字段（逗号分隔）" name="outputFieldsText"><Input placeholder="id, text, metadata" /></Form.Item></Col>
                    <Col xs={24} md={12}><Form.Item label="分区名（逗号分隔）" name="partitionNamesText"><Input placeholder="partition_a, partition_b" /></Form.Item></Col>
                    <Col xs={12} md={6}><Form.Item label="Offset" name="offset"><InputNumber min={0} style={{ width: '100%' }} /></Form.Item></Col>
                    <Col xs={12} md={6}><Form.Item label="Limit" name="limit"><InputNumber min={1} style={{ width: '100%' }} /></Form.Item></Col>
                    <Col xs={24} md={6}><Form.Item label="查询前先 Load" name="loadBeforeQuery" valuePropName="checked"><Switch /></Form.Item></Col>
                  </Row>
                  <Button type="primary" htmlType="submit">执行 Query</Button>
                </Form>
                <Divider />
                <Paragraph type="secondary">Query 返回结果</Paragraph>
                <pre className="code-block">{JSON.stringify(queryResult, null, 2)}</pre>
              </Card>
            )
          }
        ]}
      />
    </Space>
  );
}
