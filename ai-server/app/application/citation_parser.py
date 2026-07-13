"""프롬프트가 강제한 `[뉴스 N번]` / `[뉴스 1번, 3번]` 마커를 파싱해 인덱스로 변환.

프롬프트 규칙상 모델은 key_reasons 각 항목에 반드시 마커를 넣어야 하지만, LLM 이 가끔 누락한다.
누락 시 빈 리스트 반환 (프론트는 unattributed reason 으로 표시하거나 필터링).
"""

import re

from app.domain.models.ai_summary import KeyReason

# `[뉴스 3번]`, `[뉴스 1번, 3번]`, `[뉴스 1번 , 2번 ,4번]` 등을 매치.
_MARKER_RE = re.compile(r"\[뉴스\s*([0-9]+(?:\s*번\s*,\s*[0-9]+)*)\s*번?\s*\]")
_NUMBER_RE = re.compile(r"[0-9]+")


def parse_key_reasons(raw: list[str], max_source_index: int) -> list[KeyReason]:
    """모델이 반환한 텍스트 리스트를 KeyReason 으로 변환.

    max_source_index: sources 배열 길이. 마커에서 이 범위를 벗어난 인덱스는 무시 (환각 방어).
    """
    parsed: list[KeyReason] = []
    for text in raw:
        indices = _extract_indices(text, max_source_index)
        parsed.append(KeyReason(text=text, source_indices=indices))
    return parsed


def _extract_indices(text: str, max_index: int) -> list[int]:
    seen: set[int] = set()
    for match in _MARKER_RE.finditer(text):
        for num in _NUMBER_RE.findall(match.group(1)):
            i = int(num)
            if 1 <= i <= max_index:
                seen.add(i)
    return sorted(seen)
