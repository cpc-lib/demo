import { InboxOutlined } from '@ant-design/icons';
import { App, Button, Card, Col, Form, Input, Row, Space, Typography, Upload } from 'antd';
import { useState } from 'react';
import PageHeaderCard from '../components/PageHeaderCard';
import { ragApi } from '../api/rag';
import { getErrorMessage } from '../utils/message';

const { Dragger } = Upload;
const { TextArea } = Input;
const { Paragraph } = Typography;

export default function KnowledgePage() {
  const { message } = App.useApp();
  const [textLoading, setTextLoading] = useState(false);
  const [fileLoading, setFileLoading] = useState(false);

  const onTextFinish = async (values: { text: string }) => {
    try {
      setTextLoading(true);
      const { data } = await ragApi.ingestText(values);
      message.success(`文本导入成功，切分块数：${data.chunks}`);
    } catch (error) {
      message.error(getErrorMessage(error));
    } finally {
      setTextLoading(false);
    }
  };

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <PageHeaderCard
        title="知识库导入"
        description="覆盖 /api/ingest/text 与 /api/ingest/file，适合测试文本导入与文件解析入库流程。"
        tags={['文本导入', '文件导入', 'Tika 解析', 'Milvus 入库']}
      />
      <Row gutter={[16, 16]}>
        <Col xs={24} xl={12}>
          <Card className="page-card" bordered={false} title="导入纯文本">
            <Form layout="vertical" onFinish={onTextFinish}>
              <Form.Item
                label="文本内容"
                name="text"
                rules={[{ required: true, message: '请输入要导入的文本' }]}
              >
                <TextArea
                  rows={12}
                  placeholder="例如：Java线程池的核心参数包括 corePoolSize、maximumPoolSize、keepAliveTime、workQueue 等。"
                />
              </Form.Item>
              <Button type="primary" htmlType="submit" loading={textLoading}>
                导入文本
              </Button>
            </Form>
          </Card>
        </Col>
        <Col xs={24} xl={12}>
          <Card className="page-card" bordered={false} title="导入文件">
            <Paragraph type="secondary">
              支持将 Markdown、PDF、Word、TXT 等文件通过后端解析后写入向量库。
            </Paragraph>
            <Dragger
              multiple={false}
              showUploadList
              customRequest={async ({ file, onSuccess, onError }) => {
                try {
                  setFileLoading(true);
                  const realFile = file as File;
                  const { data } = await ragApi.ingestFile(realFile);
                  message.success(`文件 ${data.fileName} 导入成功，切分块数：${data.chunks}`);
                  onSuccess?.(data);
                } catch (error) {
                  message.error(getErrorMessage(error));
                  onError?.(new Error(getErrorMessage(error)));
                } finally {
                  setFileLoading(false);
                }
              }}
            >
              <p className="ant-upload-drag-icon">
                <InboxOutlined />
              </p>
              <p className="ant-upload-text">点击或拖拽文件到这里上传</p>
              <p className="ant-upload-hint">后端接口：POST /api/ingest/file</p>
            </Dragger>
            <Space style={{ marginTop: 16 }}>
              <Button loading={fileLoading}>上传状态</Button>
            </Space>
          </Card>
        </Col>
      </Row>
    </Space>
  );
}
