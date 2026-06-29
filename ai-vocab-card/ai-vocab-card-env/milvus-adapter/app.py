import hashlib
import json
import os
from typing import Any, Dict, List, Optional

import numpy as np
from fastapi import FastAPI
from pydantic import BaseModel
from pymilvus import (
    Collection,
    CollectionSchema,
    DataType,
    FieldSchema,
    connections,
    utility,
)

MILVUS_HOST = os.getenv("MILVUS_HOST", "milvus")
MILVUS_PORT = os.getenv("MILVUS_PORT", "19530")
DIMENSION = int(os.getenv("MILVUS_DIMENSION", "1024"))

app = FastAPI(title="AI Vocabulary Milvus Adapter", version="1.0.0")
_collections: Dict[str, Collection] = {}


class UpsertRequest(BaseModel):
    collection: str
    id: int
    text: str
    metadata: Optional[Dict[str, Any]] = None


class SearchRequest(BaseModel):
    collection: str
    query: str
    topK: int = 10


def connect() -> None:
    connections.connect(alias="default", host=MILVUS_HOST, port=MILVUS_PORT)


def embed(text: str) -> List[float]:
    """Deterministic local embedding used by the adapter.
    In production, replace this with the same embedding model used by the Java service.
    """
    vector = np.zeros(DIMENSION, dtype=np.float32)
    for token in (text or "").lower().split():
        digest = hashlib.sha256(token.encode("utf-8")).digest()
        idx = int.from_bytes(digest[:4], "big") % DIMENSION
        sign = 1.0 if digest[4] % 2 == 0 else -1.0
        vector[idx] += sign
    norm = np.linalg.norm(vector)
    if norm == 0:
        vector[0] = 1.0
    else:
        vector = vector / norm
    return vector.tolist()


def get_collection(name: str) -> Collection:
    if name in _collections:
        return _collections[name]
    connect()
    if not utility.has_collection(name):
        fields = [
            FieldSchema(name="id", dtype=DataType.INT64, is_primary=True, auto_id=False),
            FieldSchema(name="vector", dtype=DataType.FLOAT_VECTOR, dim=DIMENSION),
            FieldSchema(name="text", dtype=DataType.VARCHAR, max_length=4096),
            FieldSchema(name="metadata_json", dtype=DataType.VARCHAR, max_length=8192),
        ]
        schema = CollectionSchema(fields=fields, description="AI Vocabulary word card vectors")
        collection = Collection(name=name, schema=schema)
        collection.create_index(
            field_name="vector",
            index_params={"index_type": "HNSW", "metric_type": "COSINE", "params": {"M": 16, "efConstruction": 200}},
        )
    else:
        collection = Collection(name)
    collection.load()
    _collections[name] = collection
    return collection


@app.get("/health")
def health() -> Dict[str, str]:
    connect()
    return {"status": "UP", "milvus": f"{MILVUS_HOST}:{MILVUS_PORT}"}


@app.post("/vectors/upsert")
def upsert(req: UpsertRequest) -> Dict[str, Any]:
    collection = get_collection(req.collection)
    metadata_json = json.dumps(req.metadata or {}, ensure_ascii=False)
    collection.upsert([[req.id], [embed(req.text)], [req.text[:4096]], [metadata_json[:8192]]])
    collection.flush()
    return {"id": req.id, "collection": req.collection, "status": "UPSERTED"}


@app.post("/vectors/search")
def search(req: SearchRequest) -> List[Dict[str, Any]]:
    collection = get_collection(req.collection)
    result = collection.search(
        data=[embed(req.query)],
        anns_field="vector",
        param={"metric_type": "COSINE", "params": {"ef": 64}},
        limit=req.topK,
        output_fields=["text", "metadata_json"],
    )
    items: List[Dict[str, Any]] = []
    for hit in result[0]:
        metadata_raw = hit.entity.get("metadata_json") or "{}"
        try:
            detail = json.loads(metadata_raw)
        except json.JSONDecodeError:
            detail = {}
        items.append({
            "id": int(hit.id),
            "word": detail.get("word"),
            "chineseMeaning": detail.get("chineseMeaning"),
            "englishDefinition": detail.get("englishDefinition"),
            "score": float(hit.score),
            "source": "milvus",
            "detail": detail,
        })
    return items
