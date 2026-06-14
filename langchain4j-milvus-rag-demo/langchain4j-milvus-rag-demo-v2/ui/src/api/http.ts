import axios from 'axios';
import type { ApiError } from '../types';

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 60000
});

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.data) {
      return Promise.reject(error.response.data as ApiError);
    }
    return Promise.reject({
      ok: false,
      code: 500,
      message: error.message || '请求失败',
      path: '',
      timestamp: new Date().toISOString()
    } as ApiError);
  }
);

export default http;
