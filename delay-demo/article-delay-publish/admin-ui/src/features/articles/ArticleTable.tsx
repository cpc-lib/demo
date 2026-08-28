import {
  ClockCircleOutlined,
  EyeOutlined,
  StopOutlined,
} from '@ant-design/icons'
import { Button, Popconfirm, Space, Table, Tooltip, Typography } from 'antd'
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table'
import dayjs from 'dayjs'
import { StatusTag } from '../../components/StatusTag'
import type { Article } from '../../types/article'

interface ArticleTableProps {
  data: Article[]
  loading: boolean
  page: number
  pageSize: number
  total: number
  cancellingId: number | null
  onPageChange: (page: number, pageSize: number) => void
  onView: (article: Article) => void
  onSchedule: (article: Article) => void
  onCancelSchedule: (article: Article) => Promise<void>
}

const formatDateTime = (value: string | null) => value
  ? dayjs(value).format('YYYY-MM-DD HH:mm:ss')
  : '—'

export function ArticleTable({
  data,
  loading,
  page,
  pageSize,
  total,
  cancellingId,
  onPageChange,
  onView,
  onSchedule,
  onCancelSchedule,
}: ArticleTableProps) {
  const columns: ColumnsType<Article> = [
    {
      title: 'ID',
      dataIndex: 'id',
      width: 80,
      fixed: 'left',
    },
    {
      title: '文章标题',
      dataIndex: 'title',
      width: 300,
      render: (title: string) => (
        <Typography.Text ellipsis={{ tooltip: title }} strong>
          {title}
        </Typography.Text>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 110,
      render: (_, article) => <StatusTag status={article.status} />,
    },
    {
      title: '计划发布时间',
      dataIndex: 'publishAt',
      width: 190,
      render: (value: string | null) => formatDateTime(value),
    },
    {
      title: '实际发布时间',
      dataIndex: 'publishedAt',
      width: 190,
      render: (value: string | null) => formatDateTime(value),
    },
    {
      title: '调度版本',
      dataIndex: 'scheduleVersion',
      width: 110,
      render: (value: number) => `v${value}`,
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      width: 190,
      render: (value: string) => formatDateTime(value),
    },
    {
      title: '操作',
      key: 'actions',
      width: 250,
      fixed: 'right',
      render: (_, article) => (
        <Space size={4} wrap>
          <Tooltip title="查看详情">
            <Button type="text" icon={<EyeOutlined />} onClick={() => onView(article)}>
              查看
            </Button>
          </Tooltip>

          {article.status !== 'PUBLISHED' ? (
            <Button
              type="link"
              icon={<ClockCircleOutlined />}
              onClick={() => onSchedule(article)}
            >
              {article.status === 'SCHEDULED' ? '改时间' : '定时'}
            </Button>
          ) : null}

          {article.status === 'SCHEDULED' ? (
            <Popconfirm
              title="取消定时发布？"
              description="文章将恢复为草稿状态，旧调度任务会自动失效。"
              okText="确认取消"
              cancelText="保留定时"
              onConfirm={() => onCancelSchedule(article)}
            >
              <Button
                type="text"
                danger
                icon={<StopOutlined />}
                loading={cancellingId === article.id}
              >
                取消定时
              </Button>
            </Popconfirm>
          ) : null}
        </Space>
      ),
    },
  ]

  const pagination: TablePaginationConfig = {
    current: page,
    pageSize,
    total,
    showSizeChanger: true,
    showQuickJumper: true,
    pageSizeOptions: [10, 20, 50, 100],
    showTotal: (value) => `共 ${value} 篇`,
  }

  return (
    <Table<Article>
      rowKey="id"
      columns={columns}
      dataSource={data}
      loading={loading}
      pagination={pagination}
      scroll={{ x: 1450 }}
      onChange={(nextPagination) => {
        onPageChange(nextPagination.current ?? 1, nextPagination.pageSize ?? 10)
      }}
    />
  )
}
