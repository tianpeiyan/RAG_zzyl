from __future__ import annotations

from langchain_text_splitters import RecursiveCharacterTextSplitter

from rag.chunker import split_knowledge_text


def test_faq_markdown_is_split_by_question_and_answer_blocks() -> None:
    chunks = split_knowledge_text(
        "\n".join(
            [
                "Q1: 入住流程是怎样的？",
                "A:入住流程如下：",
                "1. 预约参观",
                "2. 健康评估",
                "",
                "Q2: 退住时押金如何退还？",
                "A: 退住押金退还规则：",
                "- 提前30天提出退住申请",
                "- 结清所有费用",
            ]
        ),
        category="faq",
        source_file="faq/养老院FAQ.md",
        chunk_size=80,
        chunk_overlap=10,
        splitter_cls=RecursiveCharacterTextSplitter,
    )

    assert len(chunks) == 2
    assert chunks[0].metadata["chunkType"] == "faq"
    assert chunks[0].metadata["question"] == "入住流程是怎样的？"
    assert "问：入住流程是怎样的？" in chunks[0].content
    assert "答：入住流程如下" in chunks[0].content
    assert "问：退住时押金如何退还？" in chunks[1].content
    assert "提前30天提出退住申请" in chunks[1].content


def test_system_manual_markdown_keeps_section_titles_in_chunks() -> None:
    chunks = split_knowledge_text(
        "\n".join(
            [
                "# 使用手册",
                "",
                "## 退住管理",
                "",
                "#### 功能入口",
                "",
                "菜单路径：入住管理 > 退住办理",
                "",
                "#### 操作流程",
                "",
                "1. 发起退住申请",
                "2. 财务核算费用",
            ]
        ),
        category="system-manuals",
        source_file="system-manuals/手册.md",
        chunk_size=120,
        chunk_overlap=10,
        splitter_cls=RecursiveCharacterTextSplitter,
    )

    assert any(chunk.metadata["chunkType"] == "markdown_section" for chunk in chunks)
    assert any("章节：使用手册 > 退住管理 > 功能入口" in chunk.content for chunk in chunks)
    assert any("菜单路径：入住管理 > 退住办理" in chunk.content for chunk in chunks)
    assert any("章节：使用手册 > 退住管理 > 操作流程" in chunk.content for chunk in chunks)
