# Multi-stage: uv 로 의존성 설치 + 슬림 런타임.
FROM python:3.12-slim AS builder
ENV PYTHONUNBUFFERED=1 PIP_DISABLE_PIP_VERSION_CHECK=1

# uv (Astral) — 빠른 의존성 설치
RUN pip install --no-cache-dir uv==0.5.4

WORKDIR /build
COPY pyproject.toml ./
# uv 는 pyproject.toml 의 [project].dependencies 를 시스템 site-packages 에 설치
RUN uv pip install --system --no-cache .

FROM python:3.12-slim
ENV PYTHONUNBUFFERED=1
WORKDIR /app

COPY --from=builder /usr/local/lib/python3.12/site-packages /usr/local/lib/python3.12/site-packages
COPY --from=builder /usr/local/bin /usr/local/bin
COPY app ./app

EXPOSE 8000

# Healthcheck — docker compose / Caddy 에서 활용
HEALTHCHECK --interval=10s --timeout=3s --start-period=15s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/health', timeout=2).read()" || exit 1

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
