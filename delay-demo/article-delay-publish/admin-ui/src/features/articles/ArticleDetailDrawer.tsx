import { Descriptions, Drawer, Empty, Typography } from 'antd'
import dayjs from 'dayjs'
import { StatusTag } from '../../components/StatusTag'
import type { Article } from '../../types/article'

interface ArticleDetailDrawerProps {
  article: Article | null
  open: boolean
  onClose: () => void
}

const formatDateTime = (value: string | null) => value
  ? dayjs(value).format('YYYY-MM-DD HH:mm:ss')
  : '—'

export function ArticleDetailDrawer({ article, open, onClose }: ArticleDetailDrawerProps) {
  return (
    <Drawer
      open={open}
      title="文章详情"
      width={640}
      onClose={onClose}
    >
      {!article ? (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} />
      ) : (
        <>
          <Descriptions column={1} bordered size="small">
            <Descriptions.Item label="文章 ID">{article.id}</Descriptions.Item>
            <Descriptions.Item label="标题">{article.title}</Descriptions.Item>
            <Descriptions.Item label="状态"><StatusTag status={article.status} /></Descriptions.Item>
            <Descriptions.Item label="计划发布时间">{formatDateTime(article.publishAt)}</Descriptions.Item>
            <Descriptions.Item label="实际发布时间">{formatDateTime(article.publishedAt)}</Descriptions.Item>
            <Descriptions.Item label="调度版本">v{article.scheduleVersion}</Descriptions.Item>
            <Descriptions.Item label="数据版本">v{article.rowVersion}</Descriptions.Item>
            <Descriptions.Item label="创建时间">{formatDateTime(article.createdAt)}</Descriptions.Item>
            <Descriptions.Item label="更新时间">{formatDateTime(article.updatedAt)}</Descriptions.Item>
          </Descriptions>

          <Typography.Title level={5} className="detail-content-title">文章正文</Typography.Title>
          <div className="article-content">{article.content}</div>
        </>
      )}
    </Drawer>
  )
}
