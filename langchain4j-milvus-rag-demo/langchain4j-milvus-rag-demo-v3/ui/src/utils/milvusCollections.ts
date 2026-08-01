export type MilvusCollectionRow = {
  collectionName: string;
  name?: string;
  description?: string;
  raw?: unknown;
};

export function getMilvusCollectionName(row: unknown): string | undefined {
  if (typeof row === 'string') {
    return safeCollectionName(row);
  }
  if (!row || typeof row !== 'object') {
    return undefined;
  }

  const record = row as Record<string, unknown>;
  return safeCollectionName(record.collectionName) || safeCollectionName(record.name);
}

export function normalizeMilvusCollectionRows(rows: unknown[]): MilvusCollectionRow[] {
  return rows.reduce<MilvusCollectionRow[]>((result, row) => {
    const collectionName = getMilvusCollectionName(row);
    if (!collectionName) {
      return result;
    }
    const record = row && typeof row === 'object' ? (row as Record<string, unknown>) : {};
    result.push({
      collectionName,
      name: safeCollectionName(record.name),
      description: typeof record.description === 'string' ? record.description : undefined,
      raw: row
    });
    return result;
  }, []);
}

export function buildMilvusCollectionDetailPath(collectionName: unknown): string {
  const safeName = getMilvusCollectionName(collectionName);
  if (!safeName) {
    throw new Error('collectionName is required');
  }
  return `/api/milvus/collections/${encodeURIComponent(safeName)}`;
}

function safeCollectionName(value: unknown): string | undefined {
  if (typeof value !== 'string') {
    return undefined;
  }
  const trimmed = value.trim();
  if (!trimmed || trimmed === 'undefined' || trimmed === 'null') {
    return undefined;
  }
  return trimmed;
}
