import {
  Badge,
  Button,
  Card,
  Col,
  Descriptions,
  Empty,
  Form,
  Input,
  Row,
  Space,
  Spin,
  Table,
  Tag,
  Typography,
  App
} from 'antd';
import { useState } from 'react';
import PageHeaderCard from '../components/PageHeaderCard';
import { ragApi } from '../api/rag';
import type { ChatAnswer } from '../types';
import { getErrorMessage } from '../utils/message';

const { TextArea } = Input;
const { Paragraph, Text } = Typography;

interface ChatFormValues {
  question: string;
  conversationId?: string;
}

export default function ChatPage() {
  const [form] = Form.useForm<ChatFormValues>();
  const { message } = App.useApp();
  const [detail, setDetail] = useState<ChatAnswer>();
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (values: ChatFormValues) => {
    try {
      setLoading(true);
      const { data } = await ragApi.chatDetail(values.question, values.conversationId);
      setDetail(data);
      message.success('问答完成');
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setLoading(false);
    }
  };

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <PageHeaderCard
        title="智能问答控制台"
        description="调用 /api/chat/detail，展示答案、来源、工具轨迹与命中情况，适合演示 Agent + RAG 后端能力。"
        tags={['多轮会话', '知识库优先', '联网搜索回退', '天气工具']}
      />

      <Card className="page-card" bordered={false}>
        <Form layout="vertical" form={form} onFinish={handleSubmit} initialValues={{ conversationId: 'demo-001' }}>
          <Row gutter={16}>
            <Col xs={24} md={8}>
              <Form.Item label="会话 ID" name="conversationId" extra="留空则按单轮请求处理">
                <Input placeholder="例如：demo-001" />
              </Form.Item>
            </Col>
            <Col xs={24} md={16}>
              <Form.Item
                label="问题"
                name="question"
                rules={[{ required: true, message: '请输入问题' }]}
              >
                <TextArea rows={4} placeholder="例如：请解释一下 JVM Full GC 的常见原因" />
              </Form.Item>
            </Col>
          </Row>
          <Space>
            <Button type="primary" htmlType="submit" loading={loading}>
              开始提问
            </Button>
            <Button
              onClick={() =>
                form.setFieldsValue({ question: '明天上海天气如何？', conversationId: 'weather-demo' })
              }
            >
              填入天气示例
            </Button>
            <Button
              onClick={() =>
                form.setFieldsValue({ question: 'Java线程池有哪些核心参数？', conversationId: 'kb-demo' })
              }
            >
              填入知识库示例
            </Button>
          </Space>
        </Form>
      </Card>

      <Spin spinning={loading}>
        {detail ? (
          <Row gutter={[16, 16]}>
            <Col xs={24} xl={16}>
              <Card className="page-card" bordered={false} title="回答结果">
                <Descriptions column={{ xs: 1, md: 2 }} bordered size="small">
                  <Descriptions.Item label="Conversation ID">
                    {detail.conversationId || '-'}
                  </Descriptions.Item>
                  <Descriptions.Item label="问题">{detail.question}</Descriptions.Item>
                  <Descriptions.Item label="知识库命中">
                    <Badge status={detail.knowledgeHit ? 'success' : 'default'} text={String(detail.knowledgeHit)} />
                  </Descriptions.Item>
                  <Descriptions.Item label="联网搜索">
                    <Badge status={detail.webSearchUsed ? 'processing' : 'default'} text={String(detail.webSearchUsed)} />
                  </Descriptions.Item>
                  <Descriptions.Item label="天气工具">
                    <Badge status={detail.weatherUsed ? 'warning' : 'default'} text={String(detail.weatherUsed)} />
                  </Descriptions.Item>
                  <Descriptions.Item label="来源数">{detail.sources?.length || 0}</Descriptions.Item>
                </Descriptions>
                <div style={{ marginTop: 16 }} className="answer-box">
                  {detail.answer}
                </div>
              </Card>
            </Col>
            <Col xs={24} xl={8}>
              <Card className="page-card metric-card" bordered={false} title="工具调用轨迹">
                {detail.toolTraces?.length ? (
                  <Space direction="vertical" style={{ width: '100%' }}>
                    {detail.toolTraces.map((trace, index) => (
                      <Card key={`${trace.toolName}-${index}`} size="small">
                        <Space direction="vertical" size={4}>
                          <Text strong>{trace.toolName}</Text>
                          <Text type="secondary">{trace.summary}</Text>
                        </Space>
                      </Card>
                    ))}
                  </Space>
                ) : (
                  <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无工具轨迹" />
                )}
              </Card>
            </Col>
            <Col span={24}>
              <Card className="page-card" bordered={false} title="数据来源">
                <Table
                  rowKey={(_, index) => String(index)}
                  pagination={false}
                  dataSource={detail.sources || []}
                  scroll={{ x: 900 }}
                  columns={[
                    {
                      title: '类型',
                      dataIndex: 'type',
                      width: 120,
                      render: (value: string) => <Tag color="blue">{value}</Tag>
                    },
                    { title: '标题', dataIndex: 'title', render: (value: string) => value || '-' },
                    { title: '文件名', dataIndex: 'fileName', render: (value: string) => value || '-' },
                    { title: 'Chunk', dataIndex: 'chunkId', width: 100, render: (value: number) => value ?? '-' },
                    {
                      title: 'Score',
                      dataIndex: 'score',
                      width: 120,
                      render: (value: number) => (typeof value === 'number' ? value.toFixed(4) : '-')
                    },
                    {
                      title: 'URL',
                      dataIndex: 'url',
                      render: (value: string) =>
                        value ? (
                          <a href={value} target="_blank" rel="noreferrer">
                            打开链接
                          </a>
                        ) : (
                          '-'
                        )
                    },
                    {
                      title: '摘要',
                      dataIndex: 'content',
                      render: (value: string) => (
                        <Paragraph ellipsis={{ rows: 3, expandable: true, symbol: '展开' }} style={{ marginBottom: 0 }}>
                          {value || '-'}
                        </Paragraph>
                      )
                    }
                  ]}
                />
              </Card>
            </Col>
          </Row>
        ) : (
          <Card className="page-card" bordered={false}>
            <Empty description="先发起一次问答，这里会展示详细结果" />
          </Card>
        )}
      </Spin>
    </Space>
  );
}
