import { App, Button, Card, Col, Descriptions, Form, Input, InputNumber, Row, Space, Table, Tag } from 'antd';
import { useEffect, useState } from 'react';
import PageHeaderCard from '../components/PageHeaderCard';
import { ragApi } from '../api/rag';
import type { CurrentVectorStoreResponse, VectorStoreConfig } from '../types';
import { getErrorMessage } from '../utils/message';

export default function VectorStoresPage() {
  const { message } = App.useApp();
  const [form] = Form.useForm<VectorStoreConfig>();
  const [current, setCurrent] = useState<CurrentVectorStoreResponse>();
  const [stores, setStores] = useState<VectorStoreConfig[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  const loadData = async () => {
    try {
      setLoading(true);
      const [currentRes, listRes] = await Promise.all([
        ragApi.getCurrentVectorStore(),
        ragApi.listVectorStores()
      ]);
      setCurrent(currentRes.data.data);
      setStores(listRes.data.data?.records || []);
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const onFinish = async (values: VectorStoreConfig) => {
    try {
      setSaving(true);
      await ragApi.saveVectorStore(values);
      message.success('向量库配置保存成功');
      form.resetFields();
      await loadData();
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setSaving(false);
    }
  };

  const switchStore = async (alias: string) => {
    try {
      await ragApi.switchVectorStore(alias);
      message.success(`已切换到 ${alias}`);
      await loadData();
    } catch (error) {
      message.error(getErrorMessage(error));
    }
  };

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <PageHeaderCard
        title="向量库配置"
        description="对应 /api/vector-stores 系列接口，可新增多个 Milvus 配置并动态切换当前生效的集合。"
        tags={['Milvus', '动态切换', 'topK', 'minScore']}
      />
      <Row gutter={[16, 16]}>
        <Col xs={24} xl={10}>
          <Card className="page-card" variant="borderless" title="新增 / 更新配置">
            <Form layout="vertical" form={form} onFinish={onFinish}>
              <Form.Item label="别名" name="alias" rules={[{ required: true }]}>
                <Input placeholder="prod-local" />
              </Form.Item>
              <Form.Item label="主机" name="host" rules={[{ required: true }]}>
                <Input placeholder="127.0.0.1" />
              </Form.Item>
              <Form.Item label="端口" name="port" rules={[{ required: true }]}>
                <InputNumber min={1} style={{ width: '100%' }} placeholder="19530" />
              </Form.Item>
              <Form.Item label="集合名" name="collection" rules={[{ required: true }]}>
                <Input placeholder="rag_demo_collection" />
              </Form.Item>
              <Form.Item label="TopK" name="topK" rules={[{ required: true }]}>
                <InputNumber min={1} style={{ width: '100%' }} placeholder="5" />
              </Form.Item>
              <Form.Item label="最小分数" name="minScore" rules={[{ required: true }]}>
                <InputNumber min={0} max={1} step={0.01} style={{ width: '100%' }} placeholder="0.5" />
              </Form.Item>
              <Button type="primary" htmlType="submit" loading={saving}>
                保存配置
              </Button>
            </Form>
          </Card>
        </Col>
        <Col xs={24} xl={14}>
          <Card className="page-card" variant="borderless" title="当前生效配置" loading={loading}>
            {current ? (
              <Descriptions bordered column={1} size="small">
                <Descriptions.Item label="当前别名">{current.activeAlias}</Descriptions.Item>
                <Descriptions.Item label="主机">{current.config?.host}</Descriptions.Item>
                <Descriptions.Item label="端口">{current.config?.port}</Descriptions.Item>
                <Descriptions.Item label="集合">{current.config?.collection}</Descriptions.Item>
                <Descriptions.Item label="TopK">{current.config?.topK}</Descriptions.Item>
                <Descriptions.Item label="MinScore">{current.config?.minScore}</Descriptions.Item>
              </Descriptions>
            ) : null}
          </Card>
          <Card className="page-card" variant="borderless" title="全部向量库配置" style={{ marginTop: 16 }}>
            <Table
              loading={loading}
              rowKey="alias"
              dataSource={stores}
              pagination={false}
              scroll={{ x: 800 }}
              columns={[
                {
                  title: '别名',
                  dataIndex: 'alias',
                  render: (value: string) => (
                    <Space>
                      <Tag color={value === current?.activeAlias ? 'success' : 'default'}>{value}</Tag>
                    </Space>
                  )
                },
                { title: '主机', dataIndex: 'host' },
                { title: '端口', dataIndex: 'port' },
                { title: '集合', dataIndex: 'collection' },
                { title: 'TopK', dataIndex: 'topK' },
                { title: 'MinScore', dataIndex: 'minScore' },
                {
                  title: '操作',
                  render: (_, record: VectorStoreConfig) => (
                    <Button size="small" type="link" onClick={() => switchStore(record.alias)}>
                      切换
                    </Button>
                  )
                }
              ]}
            />
          </Card>
        </Col>
      </Row>
    </Space>
  );
}
