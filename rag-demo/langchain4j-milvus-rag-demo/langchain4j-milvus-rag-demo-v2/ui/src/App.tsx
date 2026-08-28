import { Navigate, Route, Routes } from 'react-router-dom';
import AppLayout from './layouts/AppLayout';
import ChatPage from './pages/ChatPage';
import CollectionsPage from './pages/CollectionsPage';
import KnowledgePage from './pages/KnowledgePage';
import VectorStoresPage from './pages/VectorStoresPage';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<AppLayout />}>
        <Route index element={<ChatPage />} />
        <Route path="knowledge" element={<KnowledgePage />} />
        <Route path="vector-stores" element={<VectorStoresPage />} />
        <Route path="collections" element={<CollectionsPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
