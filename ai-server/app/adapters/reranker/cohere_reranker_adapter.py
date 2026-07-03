"""Cohere Rerank v2 API 어댑터.

Endpoint: POST https://api.cohere.com/v2/rerank
Model 기본: rerank-v3.5 (2024-12, 다국어/한국어 강함).

문서에 title 을 앞에 붙여 넘기면 성능이 좋다는 Cohere 권고를 따른다. body 는 1500자 컷 —
Cohere 는 문서당 4096 토큰까지 받지만 우리 뉴스/공시는 통상 그 안이라 body 컷은 안전 margin.
"""

import httpx

from app.config.settings import settings
from app.domain.models.retrieved_news import RetrievedNews


class CohereRerankerAdapter:
    def __init__(self) -> None:
        self._client = httpx.AsyncClient(
            base_url="https://api.cohere.com",
            headers={
                "Authorization": f"Bearer {settings.cohere_api_key}",
                "Content-Type": "application/json",
            },
            timeout=30.0,
        )
        self._model = settings.cohere_rerank_model

    async def rerank(
        self, query: str, candidates: list[RetrievedNews], top_n: int
    ) -> list[RetrievedNews]:
        if not candidates:
            return []
        n = min(top_n, len(candidates))
        docs = [f"{c.title}\n{c.body[:1500]}" for c in candidates]
        payload = {
            "model": self._model,
            "query": query,
            "documents": docs,
            "top_n": n,
        }
        resp = await self._client.post("/v2/rerank", json=payload)
        resp.raise_for_status()
        data = resp.json()
        results = data.get("results", [])
        return [candidates[r["index"]] for r in results]
