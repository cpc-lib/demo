import { Form, Input, Modal } from 'antd'
import { useEffect } from 'react'
import type { CreateArticlePayload } from '../../types/article'

interface CreateArticleModalProps {
  open: boolean
  loading: boolean
  onCancel: () => void
  onSubmit: (values: CreateArticlePayload) => Promise<void>
}

export function CreateArticleModal({
  open,
  loading,
  onCancel,
  onSubmit,
}: CreateArticleModalProps) {
  const [form] = Form.useForm<CreateArticlePayload>()

  useEffect(() => {
    if (!open) {
      form.resetFields()
    }
  }, [form, open])

  return (
    <Modal
      open={open}
      title="创建文章草稿"
      okText="创建草稿"
      cancelText="取消"
      confirmLoading={loading}
      onCancel={onCancel}
      onOk={() => form.submit()}
      destroyOnClose
    >
      <Form
        form={form}
        layout="vertical"
        requiredMark="optional"
        onFinish={onSubmit}
      >
        <Form.Item
          name="title"
          label="文章标题"
          rules={[
            { required: true, message: '请输入文章标题' },
            { max: 255, message: '标题不能超过 255 个字符' },
          ]}
        >
          <Input placeholder="例如：Redis + Kafka 延时发布实践" maxLength={255} showCount />
        </Form.Item>

        <Form.Item
          name="content"
          label="文章正文"
          rules={[{ required: true, message: '请输入文章正文' }]}
        >
          <Input.TextArea
            placeholder="请输入文章正文"
            autoSize={{ minRows: 8, maxRows: 16 }}
            showCount
          />
        </Form.Item>
      </Form>
    </Modal>
  )
}
