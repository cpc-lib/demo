export type ArticleStatus = 'DRAFT' | 'SCHEDULED' | 'PUBLISHED'

export interface Article {
  id: number
  title: string
  content: string
  status: ArticleStatus
  publishAt: string | null
  publishedAt: string | null
  scheduleVersion: number
  rowVersion: number
  createdAt: string
  updatedAt: string
}

export interface ArticlePage {
  content: Article[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface CreateArticlePayload {
  title: string
  content: string
}

export interface ScheduleArticlePayload {
  publishAt: string
}
