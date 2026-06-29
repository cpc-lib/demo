import axios from 'axios';
import type { WordCardDTO, WordSearchPageDTO } from '../types/word';

export const api = axios.create({ baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api' });
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export const register = async (username: string, userCode: string, password: string, confirmPassword: string) =>
  (await api.post('/auth/register', { username, userCode, password, confirmPassword })).data;
export const login = async (username: string, password: string) => (await api.post('/auth/login', { username, password })).data;
export const changePassword = async (oldPassword: string, newPassword: string, confirmNewPassword: string) =>
  (await api.post('/auth/change-password', { oldPassword, newPassword, confirmNewPassword })).data;
export const generateWord = async (word: string) => (await api.post<WordCardDTO>('/words/generate', { word })).data;
export const saveWord = async (data: WordCardDTO) => (await api.post<{id: number}>('/words', data)).data;
export const searchWords = async (keyword: string) => (await api.get<WordCardDTO[]>('/words/search', { params: { keyword, page: 1, size: 20 } })).data;
export const searchWordsPage = async (keyword: string) => (await api.get<WordSearchPageDTO>('/words/search/page', { params: { keyword, page: 1, size: 20 } })).data;
export const getWordDetail = async (id: number) => (await api.get<WordCardDTO>(`/words/${id}`)).data;
export const addToWordBook = async (wordCardId: number) => (await api.post('/wordbook/add', { wordCardId })).data;
export const getDueWords = async () => (await api.get<WordCardDTO[]>('/wordbook/due', { params: { limit: 20 } })).data;
export const submitReview = async (wordCardId: number, result: 0 | 1 | 2) => (await api.post('/wordbook/review', { wordCardId, result })).data;
export const createAnkiExportTask = async () => (await api.post('/export/anki')).data as Promise<{taskId:number;status:string}>;
export const getExportTask = async (taskId: number) => (await api.get(`/export/${taskId}`)).data as Promise<{taskId:number;status:string;fileName?:string;fileUrl?:string;errorMessage?:string}>;
export const exportAnkiBlob = async () => (await api.get('/wordbook/export/anki', { responseType: 'blob' })).data as Blob;

export const semanticSearch = async (query: string, topK = 10) => (await api.post('/search/semantic', { query, topK })).data;
export const keywordSearch = async (keyword: string) => (await api.get('/search/keyword', { params: { keyword, page: 1, size: 20 } })).data;
export const listPrompts = async () => (await api.get('/prompts')).data;
export const savePrompt = async (data: {id?:number;code:string;version:string;title?:string;content:string;enabled?:number}) => (await api.post('/prompts', data)).data;
