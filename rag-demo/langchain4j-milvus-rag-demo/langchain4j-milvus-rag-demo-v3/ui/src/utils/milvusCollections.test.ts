import {
  buildMilvusCollectionDetailPath,
  getMilvusCollectionName,
  normalizeMilvusCollectionRows
} from './milvusCollections';

const stringRowName = getMilvusCollectionName('demo_kb');
if (stringRowName !== 'demo_kb') {
  throw new Error('string collection rows should resolve to the collection name');
}

const objectRowName = getMilvusCollectionName({ collectionName: 'rag_chunks' });
if (objectRowName !== 'rag_chunks') {
  throw new Error('object collection rows should resolve from collectionName');
}

const fallbackObjectRowName = getMilvusCollectionName({ name: 'rag_images' });
if (fallbackObjectRowName !== 'rag_images') {
  throw new Error('object collection rows should resolve from name');
}

if (getMilvusCollectionName({ collectionName: 'undefined' }) !== undefined) {
  throw new Error('literal undefined should not be treated as a collection name');
}

const normalizedRows = normalizeMilvusCollectionRows(['demo_kb', { name: 'rag_images' }, undefined]);
if (normalizedRows.length !== 2) {
  throw new Error('normalization should remove invalid collection rows');
}

if (normalizedRows[0].collectionName !== 'demo_kb' || normalizedRows[1].collectionName !== 'rag_images') {
  throw new Error('normalization should preserve valid collection names');
}

const detailPath = buildMilvusCollectionDetailPath('rag chunks');
if (detailPath !== '/api/milvus/collections/rag%20chunks') {
  throw new Error('collection detail path should encode collection names');
}

try {
  buildMilvusCollectionDetailPath('undefined');
  throw new Error('invalid collection names should be rejected before making an API call');
} catch (error) {
  if (!(error instanceof Error) || !error.message.includes('collectionName')) {
    throw error;
  }
}
