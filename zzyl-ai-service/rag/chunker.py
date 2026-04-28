from __future__ import annotations

from dataclasses import dataclass, field
import re
from typing import Any


@dataclass(frozen=True)
class KnowledgeChunk:
    content: str
    metadata: dict[str, Any] = field(default_factory=dict)


_QUESTION_RE = re.compile(r"^\s*(?:问|问题|Q\d*)\s*[:：]\s*(.+?)\s*$", re.IGNORECASE)
_ANSWER_RE = re.compile(r"^\s*(?:答|答案|A)\s*[:：]\s*(.*)\s*$", re.IGNORECASE)
_INLINE_QA_RE = re.compile(
    r"^\s*(?:问|问题|Q\d*)\s*[:：]\s*(?P<question>.+?)\s*(?:答|答案|A)\s*[:：]\s*(?P<answer>.+?)\s*$",
    re.IGNORECASE,
)
_HEADING_RE = re.compile(r"^(#{1,6})\s+(.+?)\s*$")


def split_knowledge_text(
    text: str,
    *,
    category: str,
    source_file: str,
    chunk_size: int,
    chunk_overlap: int,
    splitter_cls: type,
) -> list[KnowledgeChunk]:
    normalized = text.strip()
    if not normalized:
        return []

    qa_chunks = _split_faq(normalized)
    if category.lower() == "faq" or qa_chunks:
        if qa_chunks:
            return qa_chunks

    if _should_split_by_markdown_sections(normalized, category, source_file):
        section_chunks = _split_markdown_sections(
            normalized,
            chunk_size=chunk_size,
            chunk_overlap=chunk_overlap,
            splitter_cls=splitter_cls,
        )
        if section_chunks:
            return section_chunks

    return [
        KnowledgeChunk(content=chunk, metadata={"chunkType": "text"})
        for chunk in _recursive_split(
            normalized,
            chunk_size=chunk_size,
            chunk_overlap=chunk_overlap,
            splitter_cls=splitter_cls,
        )
    ]


def _split_faq(text: str) -> list[KnowledgeChunk]:
    inline_chunks = _split_inline_faq(text)
    block_chunks = _split_block_faq(text)
    chunks = block_chunks if len(block_chunks) >= len(inline_chunks) else inline_chunks
    return chunks if chunks else []


def _split_inline_faq(text: str) -> list[KnowledgeChunk]:
    chunks: list[KnowledgeChunk] = []
    for line in text.splitlines():
        line = line.strip()
        if not line:
            continue
        match = _INLINE_QA_RE.match(line)
        if not match:
            continue
        question = _clean_text(match.group("question"))
        answer = _clean_text(match.group("answer"))
        if question and answer:
            chunks.append(_faq_chunk(question, answer))
    return chunks


def _split_block_faq(text: str) -> list[KnowledgeChunk]:
    chunks: list[KnowledgeChunk] = []
    question: str | None = None
    answer_lines: list[str] = []
    pending_question_lines: list[str] = []
    in_answer = False

    def flush() -> None:
        nonlocal question, answer_lines, pending_question_lines, in_answer
        if question and answer_lines:
            answer = _clean_multiline(answer_lines)
            if answer:
                chunks.append(_faq_chunk(question, answer))
        question = None
        answer_lines = []
        pending_question_lines = []
        in_answer = False

    for raw_line in text.splitlines():
        line = raw_line.rstrip()
        inline_match = _INLINE_QA_RE.match(line)
        if inline_match:
            flush()
            chunks.append(
                _faq_chunk(
                    _clean_text(inline_match.group("question")),
                    _clean_text(inline_match.group("answer")),
                )
            )
            continue

        question_match = _QUESTION_RE.match(line)
        if question_match:
            flush()
            question = _clean_text(question_match.group(1))
            pending_question_lines = []
            in_answer = False
            continue

        answer_match = _ANSWER_RE.match(line)
        if answer_match and question:
            in_answer = True
            first_answer_line = answer_match.group(1).strip()
            if first_answer_line:
                answer_lines.append(first_answer_line)
            continue

        if question is None:
            continue
        if in_answer:
            answer_lines.append(line)
        elif line.strip():
            pending_question_lines.append(line.strip())
            question = _clean_text(" ".join([question, *pending_question_lines]))

    flush()
    return chunks


def _faq_chunk(question: str, answer: str) -> KnowledgeChunk:
    return KnowledgeChunk(
        content=f"问：{question}\n答：{answer}",
        metadata={
            "chunkType": "faq",
            "question": question,
        },
    )


def _should_split_by_markdown_sections(text: str, category: str, source_file: str) -> bool:
    if not source_file.lower().endswith(".md"):
        return False
    heading_count = sum(1 for line in text.splitlines() if _HEADING_RE.match(line))
    if heading_count >= 2:
        return True
    return category.lower() in {"system-manuals", "manuals", "manual", "docs"}


def _split_markdown_sections(
    text: str,
    *,
    chunk_size: int,
    chunk_overlap: int,
    splitter_cls: type,
) -> list[KnowledgeChunk]:
    sections: list[tuple[list[str], list[str]]] = []
    heading_stack: list[tuple[int, str]] = []
    body_lines: list[str] = []

    def flush() -> None:
        if not heading_stack and not any(line.strip() for line in body_lines):
            return
        headings = [title for _, title in heading_stack]
        body = _clean_multiline(body_lines)
        if body:
            sections.append((headings, body_lines.copy()))

    for line in text.splitlines():
        match = _HEADING_RE.match(line)
        if match:
            flush()
            level = len(match.group(1))
            title = _clean_text(match.group(2))
            heading_stack = [(item_level, item_title) for item_level, item_title in heading_stack if item_level < level]
            heading_stack.append((level, title))
            body_lines = []
            continue
        body_lines.append(line)
    flush()

    chunks: list[KnowledgeChunk] = []
    for headings, lines in sections:
        section_title = " > ".join(headings)
        body = _clean_multiline(lines)
        content = f"章节：{section_title}\n{body}".strip() if section_title else body
        if not content:
            continue
        if len(content) <= chunk_size:
            chunks.append(
                KnowledgeChunk(
                    content=content,
                    metadata={"chunkType": "markdown_section", "sectionTitle": section_title},
                )
            )
            continue
        for index, part in enumerate(
            _recursive_split(
                body or content,
                chunk_size=chunk_size,
                chunk_overlap=chunk_overlap,
                splitter_cls=splitter_cls,
            ),
            start=1,
        ):
            chunks.append(
                KnowledgeChunk(
                    content=f"章节：{section_title}\n{part}".strip(),
                    metadata={
                        "chunkType": "markdown_section",
                        "sectionTitle": section_title,
                        "sectionPart": index,
                    },
                )
            )
    return chunks


def _recursive_split(
    text: str,
    *,
    chunk_size: int,
    chunk_overlap: int,
    splitter_cls: type,
) -> list[str]:
    return splitter_cls(
        chunk_size=chunk_size,
        chunk_overlap=chunk_overlap,
        separators=["\n\n", "\n", "。", "！", "？", ".", "!", "?", "；", ";", " ", ""],
        length_function=len,
    ).split_text(text)


def _clean_text(text: str) -> str:
    return " ".join(text.strip().split())


def _clean_multiline(lines: list[str]) -> str:
    return "\n".join(line.rstrip() for line in lines).strip()
