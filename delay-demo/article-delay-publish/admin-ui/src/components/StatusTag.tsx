import { Tag } from 'antd'
import type { ArticleStatus } from '../types/article'

const statusMeta: Record<ArticleStatus, { color: string; text: string }> = {
  DRAFT: { color: 'default', text: '草稿' },
  SCHEDULED: { color: 'processing', text: '已定时' },
  PUBLISHED: { color: 'success', text: '已发布' },
}

interface StatusTagProps {
  status: ArticleStatus
}

export function StatusTag({ status }: StatusTagProps) {
  const meta = statusMeta[status]
  return <Tag color={meta.color}>{meta.text}</Tag>
}
