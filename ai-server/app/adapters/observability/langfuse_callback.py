"""LangChain CallbackHandler 싱글톤.

env 의 LANGFUSE_PUBLIC_KEY / LANGFUSE_SECRET_KEY / LANGFUSE_HOST 를 자동으로 사용한다.
키가 없으면 handler 가 dummy 로 동작 (Langfuse SDK 가 silently no-op).
"""

from functools import lru_cache

from langfuse.langchain import CallbackHandler


@lru_cache(maxsize=1)
def langfuse_callback_handler() -> CallbackHandler:
    return CallbackHandler()
