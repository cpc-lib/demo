import {
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
} from '@ant-design/icons'
import {
  Button,
  Card,
  Input,
  Select,
  Space,
  Typography,
  message,
} from 'antd'
import { useCallback, useEffect, useState } from 'react'
import {
  cancelArticleSchedule,
  createArticle,
  getArticle,
  listArticles,
  scheduleArticle,
} from '../../api/articles'
import { getErrorMessage } from '../../api/http'
import type { Article, ArticleStatus, CreateArticlePayload } from '../../types/article'
import { ArticleDetailDrawer } from './ArticleDetailDrawer'
import { ArticleTable } from './ArticleTable'
import { CreateArticleModal } from './CreateArticleModal'
import { ScheduleArticleModal } from './ScheduleArticleModal'

const PAGE_SIZE = 10

export function ArticlesPage() {
  const [messageApi, contextHolder] = message.useMessage()
  const [articles, setArticles] = useState<Article[]>([])
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(PAGE_SIZE)
  const [total, setTotal] = useState(0)
  const [keyword, setKeyword] = useState('')
  const [queryKeyword, setQueryKeyword] = useState('')
  const [status, setStatus] = useState<ArticleStatus | undefined>()

  const [createOpen, setCreateOpen] = useState(false)
  const [creating, setCreating] = useState(false)
  const [scheduleTarget, setScheduleTarget] = useState<Article | null>(null)
  const [scheduling, setScheduling] = useState(false)
  const [detail, setDetail] = useState<Article | null>(null)
  const [detailOpen, setDetailOpen] = useState(false)
  const [cancellingId, setCancellingId] = useState<number | null>(null)

  const loadArticles = useCallback(async () => {
    setLoading(true)
    try {
      const data = await listArticles({
        keyword: queryKeyword || undefined,
        status,
        page: page - 1,
        size: pageSize,
      })
      setArticles(data.content)
      setTotal(data.totalElements)
    } catch (error) {
      messageApi.error(getErrorMessage(error))
    } finally {
      setLoading(false)
    }
  }, [messageApi, page, pageSize, queryKeyword, status])

  useEffect(() => {
    void loadArticles()
  }, [loadArticles])

  const handleSearch = () => {
    setPage(1)
    setQueryKeyword(keyword.trim())
  }

  const handleCreate = async (values: CreateArticlePayload) => {
    setCreating(true)
    try {
      const article = await createArticle(values)
      setCreateOpen(false)
      messageApi.success(`文章 #${article.id} 已创建`)
      setPage(1)
      await loadArticles()
    } catch (error) {
      messageApi.error(getErrorMessage(error))
    } finally {
      setCreating(false)
    }
  }

  const handleSchedule = async (publishAt: string) => {
    if (!scheduleTarget) return

    setScheduling(true)
    try {
      await scheduleArticle(scheduleTarget.id, { publishAt })
      messageApi.success(scheduleTarget.status === 'SCHEDULED' ? '发布时间已更新' : '定时发布已设置')
      setScheduleTarget(null)
      await loadArticles()
    } catch (error) {
      messageApi.error(getErrorMessage(error))
    } finally {
      setScheduling(false)
    }
  }

  const handleCancelSchedule = async (article: Article) => {
    setCancellingId(article.id)
    try {
      await cancelArticleSchedule(article.id)
      messageApi.success('已取消定时发布')
      await loadArticles()
    } catch (error) {
      messageApi.error(getErrorMessage(error))
    } finally {
      setCancellingId(null)
    }
  }

  const handleView = async (article: Article) => {
    setDetail(article)
    setDetailOpen(true)
    try {
      setDetail(await getArticle(article.id))
    } catch (error) {
      messageApi.error(getErrorMessage(error))
    }
  }

  return (
    <>
      {contextHolder}

      <div className="page-heading">
        <div>
          <Typography.Title level={2}>文章发布管理</Typography.Title>
          <Typography.Text type="secondary">
            管理草稿、定时任务和文章发布状态。
          </Typography.Text>
        </div>
        <Button type="primary" size="large" icon={<PlusOutlined />} onClick={() => setCreateOpen(true)}>
          新建文章
        </Button>
      </div>

      <Card className="content-card" bordered={false}>
        <div className="table-toolbar">
          <Space wrap size={12}>
            <Input
              allowClear
              value={keyword}
              prefix={<SearchOutlined />}
              placeholder="搜索文章标题"
              style={{ width: 280 }}
              onChange={(event) => setKeyword(event.target.value)}
              onPressEnter={handleSearch}
              onClear={() => {
                setKeyword('')
                setQueryKeyword('')
                setPage(1)
              }}
            />
            <Select<ArticleStatus>
              allowClear
              placeholder="全部状态"
              value={status}
              style={{ width: 150 }}
              options={[
                { value: 'DRAFT', label: '草稿' },
                { value: 'SCHEDULED', label: '已定时' },
                { value: 'PUBLISHED', label: '已发布' },
              ]}
              onChange={(value) => {
                setStatus(value)
                setPage(1)
              }}
            />
            <Button onClick={handleSearch}>查询</Button>
          </Space>

          <Button icon={<ReloadOutlined />} onClick={() => void loadArticles()}>
            刷新
          </Button>
        </div>

        <ArticleTable
          data={articles}
          loading={loading}
          page={page}
          pageSize={pageSize}
          total={total}
          cancellingId={cancellingId}
          onPageChange={(nextPage, nextSize) => {
            setPage(nextSize !== pageSize ? 1 : nextPage)
            setPageSize(nextSize)
          }}
          onView={handleView}
          onSchedule={setScheduleTarget}
          onCancelSchedule={handleCancelSchedule}
        />
      </Card>

      <CreateArticleModal
        open={createOpen}
        loading={creating}
        onCancel={() => setCreateOpen(false)}
        onSubmit={handleCreate}
      />

      <ScheduleArticleModal
        article={scheduleTarget}
        open={Boolean(scheduleTarget)}
        loading={scheduling}
        onCancel={() => setScheduleTarget(null)}
        onSubmit={handleSchedule}
      />

      <ArticleDetailDrawer
        article={detail}
        open={detailOpen}
        onClose={() => setDetailOpen(false)}
      />
    </>
  )
}
