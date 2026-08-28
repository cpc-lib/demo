import http from './http';
import type {
  ChatAnswer,
  CurrentVectorStoreResponse,
  IngestTextRequest,
  MilvusCollectionQueryRequest,
  MilvusCreateCollectionRequest,
  VectorStoreSaveRequest
} from '../types';

export const ragApi = {
  chat(question: string, conversationId?: string) {
    return http.get<string>('/api/chat', {
      params: { question, conversationId: conversationId || undefined }
    });
  },
  chatDetail(question: string, conversationId?: string) {
    return http.get<ChatAnswer>('/api/chat/detail', {
      params: { question, conversationId: conversationId || undefined }
    });
  },
  ingestText(data: IngestTextRequest) {
    return http.post('/api/ingest/text', data);
  },
  ingestFile(file: File) {
    const formData = new FormData();
    formData.append('file', file);
    return http.post('/api/ingest/file', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
  },
  getCurrentVectorStore() {
    return http.get<CurrentVectorStoreResponse>('/api/vector-stores/current');
  },
  listVectorStores() {
    return http.get('/api/vector-stores');
  },
  saveVectorStore(data: VectorStoreSaveRequest) {
    return http.post('/api/vector-stores', data);
  },
  switchVectorStore(alias: string) {
    return http.post('/api/vector-stores/switch', null, { params: { alias } });
  },
  listCollections(databaseName?: string) {
    return http.get('/api/milvus/collections', { params: { databaseName: databaseName || undefined } });
  },
  describeCollection(collectionName: string, databaseName?: string) {
    return http.get(`/api/milvus/collections/${collectionName}`, {
      params: { databaseName: databaseName || undefined }
    });
  },
  queryCollection(data: MilvusCollectionQueryRequest) {
    return http.post('/api/milvus/collections/query', data);
  },
  createCollection(data: MilvusCreateCollectionRequest) {
    return http.post('/api/milvus/collections', data);
  }
};
