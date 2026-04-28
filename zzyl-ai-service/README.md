# zzyl-ai-service

Python AI service for the ZZYL RAG assistant. The FastAPI routes are backed by
LangChain, DashScope embeddings/chat models, and a persisted Chroma knowledge
base.

Production retrieval uses DashScope semantic embeddings by default. The local
hash embedding provider is kept only for offline tests or temporary local
debugging and should not be used as the main production index.

## Quick start

1. Copy `.env.example` to `.env`.
2. Fill in the required configuration items.
3. Put `.txt`, `.md`, `.pdf`, or `.docx` files under `knowledge-base/`.
4. Run `uv run uvicorn main:app --host 127.0.0.1 --port 8000`.
5. Call `POST /assistant/kb/index/rebuild` once to rebuild the Chroma index.

## Retrieval behavior

- FAQ documents are indexed by one question-answer pair per chunk.
- Markdown manuals are indexed by heading section, preserving the section path in
  each chunk.
- Before retrieval, colloquial user questions are rewritten into standard
  knowledge-base questions, for example `如何退住` -> `退住流程是什么？`.

## Health check

- `GET /health`

## Assistant APIs

- `POST /assistant/chat`
- `GET /assistant/kb/documents`
- `GET /assistant/kb/index/status`
- `POST /assistant/kb/index/rebuild`
