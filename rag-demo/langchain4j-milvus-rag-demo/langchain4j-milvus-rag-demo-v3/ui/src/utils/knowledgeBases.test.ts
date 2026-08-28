import { knowledgeBaseOptionLabel } from './knowledgeBases';

const tenantScopedLabel = knowledgeBaseOptionLabel({
  id: 1,
  tenantId: 1,
  kbCode: 'default',
  name: 'Default Knowledge Base',
  vectorStoreType: 'milvus',
  vectorCollection: 'demo_kb_tenant_1',
  embeddingModel: 'text-embedding',
  embeddingDimension: 1024,
  chunkStrategy: 'recursive',
  chunkSize: 800,
  chunkOverlap: 120,
  retrievalTopK: 8,
  minScore: 0.55,
  status: 1
});

if (tenantScopedLabel !== 'Default Knowledge Base (demo_kb_tenant_1)') {
  throw new Error('knowledge base labels should show the tenant-scoped vector collection');
}

const fallbackLabel = knowledgeBaseOptionLabel({
  id: 2,
  tenantId: 1,
  kbCode: 'kb_a',
  name: 'KB A',
  vectorStoreType: 'milvus',
  vectorCollection: '',
  embeddingModel: 'text-embedding',
  embeddingDimension: 1024,
  chunkStrategy: 'recursive',
  chunkSize: 800,
  chunkOverlap: 120,
  retrievalTopK: 8,
  minScore: 0.55,
  status: 1
});

if (fallbackLabel !== 'KB A (kb_a)') {
  throw new Error('knowledge base labels should fall back to kbCode when collection is absent');
}
