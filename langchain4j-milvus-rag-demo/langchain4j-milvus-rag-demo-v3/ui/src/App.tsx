import { lazy, Suspense } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import AppLayout from './layouts/AppLayout';

const ChatPage = lazy(() => import('./pages/ChatPage'));
const ChunksPage = lazy(() => import('./pages/ChunksPage'));
const CollectionsPage = lazy(() => import('./pages/CollectionsPage'));
const ImageAssetsPage = lazy(() => import('./pages/ImageAssetsPage'));
const KnowledgePage = lazy(() => import('./pages/KnowledgePage'));
const ModelConfigsPage = lazy(() => import('./pages/ModelConfigsPage'));
const AgentPromptsPage = lazy(() => import('./pages/AgentPromptsPage'));
const ChangePasswordPage = lazy(() => import('./pages/ChangePasswordPage'));
const QueryLogsPage = lazy(() => import('./pages/QueryLogsPage'));
const RetrievalEvaluationsPage = lazy(() => import('./pages/RetrievalEvaluationsPage'));
const TenantAccessPage = lazy(() => import('./pages/TenantAccessPage'));
const VectorStoresPage = lazy(() => import('./pages/VectorStoresPage'));
const LoginPage = lazy(() => import('./pages/LoginPage'));

const routeFallback = <div style={{ padding: 24 }}>Loading...</div>;

export default function App() {
  return (
    <Suspense fallback={routeFallback}>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/" element={<AppLayout />}>
          <Route index element={<ChatPage />} />
          <Route path="knowledge" element={<KnowledgePage />} />
          <Route path="image-assets" element={<ImageAssetsPage />} />
          <Route path="query-logs" element={<QueryLogsPage />} />
          <Route path="retrieval-evaluations" element={<RetrievalEvaluationsPage />} />
          <Route path="model-configs" element={<ModelConfigsPage />} />
          <Route path="agent-prompts" element={<AgentPromptsPage />} />
          <Route path="chunks" element={<ChunksPage />} />
          <Route path="tenant-access" element={<TenantAccessPage />} />
          <Route path="change-password" element={<ChangePasswordPage />} />
          <Route path="vector-stores" element={<VectorStoresPage />} />
          <Route path="collections" element={<CollectionsPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Suspense>
  );
}
