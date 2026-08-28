import { DatePicker, Form, Modal, Typography } from 'antd'
import dayjs, { Dayjs } from 'dayjs'
import { useEffect } from 'react'
import type { Article } from '../../types/article'

interface ScheduleFormValues {
  publishAt: Dayjs
}

interface ScheduleArticleModalProps {
  article: Article | null
  open: boolean
  loading: boolean
  onCancel: () => void
  onSubmit: (publishAt: string) => Promise<void>
}

export function ScheduleArticleModal({
  article,
  open,
  loading,
  onCancel,
  onSubmit,
}: ScheduleArticleModalProps) {
  const [form] = Form.useForm<ScheduleFormValues>()

  useEffect(() => {
    if (!open) {
      form.resetFields()
      return
    }

    form.setFieldsValue({
      publishAt: article?.publishAt ? dayjs(article.publishAt) : dayjs().add(10, 'minute'),
    })
  }, [article, form, open])

  return (
    <Modal
      open={open}
      title={article?.status === 'SCHEDULED' ? '修改发布时间' : '设置定时发布'}
      okText={article?.status === 'SCHEDULED' ? '保存新时间' : '确认定时'}
      cancelText="取消"
      confirmLoading={loading}
      onCancel={onCancel}
      onOk={() => form.submit()}
      destroyOnClose
    >
      <Typography.Paragraph type="secondary" className="modal-hint">
        {article?.title}
      </Typography.Paragraph>

      <Form
        form={form}
        layout="vertical"
        onFinish={(values) => onSubmit(values.publishAt.toISOString())}
      >
        <Form.Item
          name="publishAt"
          label="发布时间"
          rules={[
            { required: true, message: '请选择发布时间' },
            {
              validator: (_, value: Dayjs | undefined) => {
                if (!value || value.isAfter(dayjs())) {
                  return Promise.resolve()
                }
                return Promise.reject(new Error('发布时间必须晚于当前时间'))
              },
            },
          ]}
        >
          <DatePicker
            showTime
            format="YYYY-MM-DD HH:mm:ss"
            style={{ width: '100%' }}
            disabledDate={(current) => current && current.endOf('day').isBefore(dayjs())}
          />
        </Form.Item>
      </Form>
    </Modal>
  )
}
