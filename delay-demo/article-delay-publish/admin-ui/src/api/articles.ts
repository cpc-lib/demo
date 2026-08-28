import { http } from './http'
import type {
  Article,
  ArticlePage,
  ArticleStatus,
  CreateArticlePayload,
  ScheduleArticlePayload,
} from '../types/article'

export interface ListArticlesParams {
  keyword?: string
  status?: ArticleStatus
  page: number
  size: number
}

export async function listArticles(params: ListArticlesParams): Promise<ArticlePage> {
  const response = await http.get<ArticlePage>('/api/articles', { params })
  return response.data
}

export async function getArticle(id: number): Promise<Article> {
  const response = await http.get<Article>(`/api/articles/${id}`)
  return response.data
}

export async function createArticle(payload: CreateArticlePayload): Promise<Article> {
  const response = await http.post<Article>('/api/articles', payload)
  return response.data
}

export async function scheduleArticle(
  id: number,
  payload: ScheduleArticlePayload,
): Promise<Article> {
  const response = await http.post<Article>(`/api/articles/${id}/schedule`, payload)
  return response.data
}

export async function cancelArticleSchedule(id: number): Promise<Article> {
  const response = await http.post<Article>(`/api/articles/${id}/cancel-schedule`)
  return response.data
}
