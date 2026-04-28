from __future__ import annotations

from dataclasses import replace
import json
import re

from fastapi.testclient import TestClient

from api.app import create_app


class _FailingChain:
    def invoke(self, payload):
        raise RuntimeError("llm unavailable")


class _CapturingChain:
    def __init__(self) -> None:
        self.payloads = []

    def invoke(self, payload):
        self.payloads.append(payload)
        return "护理等级在服务管理的护理等级页面配置。"


def test_internal_chat_retrieves_local_knowledge_base(
    configured_env,
    monkeypatch,
) -> None:
    knowledge_base_dir = configured_env["knowledge_base_dir"]
    faq_dir = knowledge_base_dir / "faq"
    faq_dir.mkdir(parents=True)
    (faq_dir / "养老院FAQ.txt").write_text(
        "\n".join(
            [
                "问：养老院收费标准是什么，费用包含哪些服务？答：收费根据房型、护理等级差异化定价，费用涵盖住宿、日常膳食、基础保洁、基础健康监测、公共文娱设施使用等基础服务。",
                "问：是否可以实地参观养老院，怎么预约参观？答：支持免费实地参观，可线上预约、电话预约两种方式。",
            ]
        ),
        encoding="utf-8",
    )
    monkeypatch.setenv("AI_SERVICE_RETRIEVAL_MIN_SCORE", "0")

    from rag.service import RagService

    monkeypatch.setattr(RagService, "_build_chain", lambda self: _FailingChain())

    app = create_app()
    with TestClient(app) as client:
        rebuild_response = client.post("/internal/kb/reindex")
        assert rebuild_response.status_code == 200

        response = client.post(
            "/internal/chat",
            json={
                "sessionId": "test-session",
                "question": "养老院收费标准是什么？",
                "history": [],
            },
        )

    assert response.status_code == 200
    payload = response.json()
    assert payload["status"] == "success"
    assert payload["hitCount"] >= 1
    assert "收费" in payload["answer"]
    assert payload["sources"][0]["sourceFile"] == "faq/养老院FAQ.txt"
    assert "questionType" in payload
    assert "confidence" in payload
    assert "retrievalTrace" in payload
    assert "selected" in payload["sources"][0]


def test_internal_chat_process_question_prefers_process_chunks(
    configured_env,
    monkeypatch,
) -> None:
    knowledge_base_dir = configured_env["knowledge_base_dir"]
    faq_dir = knowledge_base_dir / "faq"
    faq_dir.mkdir(parents=True)
    (faq_dir / "退住流程.txt").write_text(
        "\n".join(
            [
                "问：退住办理流程是什么？答：1. 发起退住申请；2. 护理员确认老人状态；3. 财务核算费用；4. 办理退房和物品交接。",
                "问：入住办理流程是什么？答：先预约参观，再提交入住申请，评估护理等级后签约入住。",
                "问：退费规则是什么？答：退费根据合同和已消费账单核算。",
            ]
        ),
        encoding="utf-8",
    )
    monkeypatch.setenv("AI_SERVICE_RETRIEVAL_MIN_SCORE", "0")

    from rag.service import RagService

    monkeypatch.setattr(RagService, "_build_chain", lambda self: _FailingChain())

    app = create_app()
    with TestClient(app) as client:
        client.post("/internal/kb/reindex")
        response = client.post(
            "/internal/chat",
            json={
                "sessionId": "process-session",
                "question": "退住办理流程是什么？",
                "history": [],
            },
        )

    payload = response.json()
    assert response.status_code == 200
    assert payload["questionType"] == "process"
    assert payload["refused"] is False
    assert payload["hitCount"] >= 1
    assert "发起退住申请" in payload["answer"]
    assert payload["sources"][0]["selected"] is True
    assert "退住" in payload["sources"][0]["matchedTerms"]


def test_internal_chat_formats_process_answer_without_markdown_noise(
    configured_env,
    monkeypatch,
) -> None:
    knowledge_base_dir = configured_env["knowledge_base_dir"]
    faq_dir = knowledge_base_dir / "faq"
    faq_dir.mkdir(parents=True)
    (faq_dir / "养老院FAQ.md").write_text(
        "\n".join(
            [
                "Q1: 入住流程是怎样的？",
                "A:入住流程如下：",
                "1. **预约参观**：拨打服务热线或在线预约参观时间",
                "2. **健康评估**：携带近期体检报告，由本院医护人员进行健康评估",
                "3. **确定房型**：根据评估结果和老人需求选择合适的房型",
            ]
        ),
        encoding="utf-8",
    )
    monkeypatch.setenv("AI_SERVICE_RETRIEVAL_MIN_SCORE", "0")

    from rag.service import RagService

    monkeypatch.setattr(RagService, "_build_chain", lambda self: _FailingChain())

    app = create_app()
    with TestClient(app) as client:
        client.post("/internal/kb/reindex")
        response = client.post(
            "/internal/chat",
            json={
                "sessionId": "format-session",
                "question": "入住流程是怎样的",
                "history": [],
            },
        )

    payload = response.json()
    answer = payload["answer"]

    assert response.status_code == 200
    assert payload["refused"] is False
    assert "**" not in answer
    assert "1. 入住流程如下" not in answer
    assert "1. 预约参观" in answer
    assert "\n2. 健康评估" in answer
    assert "\n3. 确定房型" in answer


def test_internal_chat_answers_staff_system_feature_question(
    configured_env,
    monkeypatch,
) -> None:
    knowledge_base_dir = configured_env["knowledge_base_dir"]
    manual_dir = knowledge_base_dir / "system-manuals"
    manual_dir.mkdir(parents=True)
    (manual_dir / "中州养老系统使用手册.md").write_text(
        "\n".join(
            [
                "# 中州养老系统使用手册",
                "",
                "## 核心功能模块",
                "",
                "中州养老管理系统涵盖老人入住管理、护理服务、床位管理、预约来访、订单财务等核心业务模块。",
            ]
        ),
        encoding="utf-8",
    )
    monkeypatch.setenv("AI_SERVICE_RETRIEVAL_MIN_SCORE", "0")

    from rag.service import RagService

    monkeypatch.setattr(RagService, "_build_chain", lambda self: _FailingChain())

    app = create_app()
    with TestClient(app) as client:
        client.post("/internal/kb/reindex")
        response = client.post(
            "/internal/chat",
            json={
                "sessionId": "system-feature-session",
                "question": "本系统有哪些功能",
                "history": [],
            },
        )

    payload = response.json()
    assert response.status_code == 200
    assert payload["refused"] is False
    assert payload["questionType"] == "list"
    assert "入住管理" in payload["answer"]
    assert "护理服务" in payload["answer"]


def test_internal_chat_rebuilds_when_knowledge_base_changes_after_indexing(
    configured_env,
    monkeypatch,
) -> None:
    knowledge_base_dir = configured_env["knowledge_base_dir"]
    faq_dir = knowledge_base_dir / "faq"
    faq_dir.mkdir(parents=True)
    (faq_dir / "养老院FAQ.txt").write_text(
        "问：养老院有哪些娱乐活动？答：本院定期组织棋牌、书法、唱歌等文娱活动。",
        encoding="utf-8",
    )
    monkeypatch.setenv("AI_SERVICE_RETRIEVAL_MIN_SCORE", "0")

    from rag.service import RagService

    monkeypatch.setattr(RagService, "_build_chain", lambda self: _FailingChain())

    app = create_app()
    with TestClient(app) as client:
        rebuild_response = client.post("/internal/kb/reindex")
        assert rebuild_response.status_code == 200

        manual_dir = knowledge_base_dir / "system-manuals"
        manual_dir.mkdir(parents=True)
        (manual_dir / "中州养老系统使用手册.md").write_text(
            "\n".join(
                [
                    "# 中州养老系统使用手册",
                    "",
                    "## 核心功能模块",
                    "",
                    "中州养老管理系统涵盖老人入住管理、护理服务、床位管理、预约来访、订单财务等核心业务模块。",
                ]
            ),
            encoding="utf-8",
        )

        response = client.post(
            "/internal/chat",
            json={
                "sessionId": "stale-index-session",
                "question": "本系统有哪些功能",
                "history": [],
            },
        )

    payload = response.json()
    selected_sources = [source for source in payload["sources"] if source["selected"]]

    assert response.status_code == 200
    assert payload["refused"] is False
    assert "入住管理" in payload["answer"]
    assert "护理服务" in payload["answer"]
    assert any(source["sourceFile"] == "system-manuals/中州养老系统使用手册.md" for source in selected_sources)


def test_internal_chat_rewrites_colloquial_retreat_question_before_retrieval(
    configured_env,
    monkeypatch,
) -> None:
    knowledge_base_dir = configured_env["knowledge_base_dir"]
    faq_dir = knowledge_base_dir / "faq"
    faq_dir.mkdir(parents=True)
    (faq_dir / "退住流程.txt").write_text(
        "\n".join(
            [
                "问：退住流程是什么？答：1. 发起退住申请；2. 财务核算费用；3. 办理退房和物品交接。",
                "问：入住流程是什么？答：先预约参观，再提交入住申请，评估护理等级后签约入住。",
            ]
        ),
        encoding="utf-8",
    )
    monkeypatch.setenv("AI_SERVICE_RETRIEVAL_MIN_SCORE", "0")

    from rag.service import RagService

    monkeypatch.setattr(RagService, "_build_chain", lambda self: _FailingChain())

    app = create_app()
    with TestClient(app) as client:
        client.post("/internal/kb/reindex")
        response = client.post(
            "/internal/chat",
            json={
                "sessionId": "rewrite-session",
                "question": "如何退住",
                "history": [],
            },
        )

    payload = response.json()
    assert response.status_code == 200
    assert payload["refused"] is False
    assert payload["questionType"] == "process"
    assert payload["hitCount"] >= 1
    assert payload["retrievalTrace"]["rewrittenQuery"] == "退住流程是什么？"
    assert "如何退住" in payload["retrievalTrace"]["expandedQueries"]
    assert "退住流程是什么？" in payload["retrievalTrace"]["expandedQueries"]
    assert "发起退住申请" in payload["answer"]


def test_internal_chat_answers_kb_no_evidence_troubleshooting_question(
    configured_env,
    monkeypatch,
) -> None:
    knowledge_base_dir = configured_env["knowledge_base_dir"]
    manual_dir = knowledge_base_dir / "system-manuals"
    manual_dir.mkdir(parents=True)
    (manual_dir / "中州养老系统使用手册.md").write_text(
        "\n".join(
            [
                "# 中州养老系统使用手册",
                "",
                "## 知识库暂无依据怎么办",
                "",
                "当智能助手回答知识库暂无依据时，说明当前问题在知识库文档中没有相关内容。处理方式：",
                "",
                "1. 确认问题表述是否准确，尝试换一种提问方式",
                "2. 检查知识库是否包含相关文档",
                "3. 如需补充知识，联系管理员在知识目录添加相关文档",
                "4. 添加文档后在知识库管理页面重建索引",
            ]
        ),
        encoding="utf-8",
    )
    monkeypatch.setenv("AI_SERVICE_RETRIEVAL_MIN_SCORE", "0")

    from rag.service import RagService

    monkeypatch.setattr(RagService, "_build_chain", lambda self: _FailingChain())

    app = create_app()
    with TestClient(app) as client:
        client.post("/internal/kb/reindex")
        response = client.post(
            "/internal/chat",
            json={
                "sessionId": "kb-no-evidence-session",
                "question": "知识库暂无依据时应该怎么处理？",
                "history": [],
            },
        )

    payload = response.json()

    assert response.status_code == 200
    assert payload["refused"] is False
    assert payload["questionType"] == "process"
    assert payload["hitCount"] >= 1
    assert "确认问题表述" in payload["answer"]
    assert "重建索引" in payload["answer"]
    assert payload["retrievalTrace"]["requiredTerms"] == ["知识库", "依据", "处理"]


def test_internal_chat_location_question_filters_generic_faq_chunks(
    configured_env,
    monkeypatch,
) -> None:
    knowledge_base_dir = configured_env["knowledge_base_dir"]
    faq_dir = knowledge_base_dir / "faq"
    faq_dir.mkdir(parents=True)
    (faq_dir / "养老院FAQ.txt").write_text(
        "\n".join(
            [
                "问：养老院有哪些安全防护保障措施？答：全域高清监控、走廊卫生间防滑扶手、紧急呼叫铃、防火消防设施、夜间巡逻等多重安全防护配置。",
                "问：养老院具体位置在哪，是否提供接送服务？答：本院有明确固定院区地址，支持预约上门接送、院内参观接送服务，可联系客服预约出行安排。",
                "问：养老院有哪些房型可以选择？答：设有单人房、双人标间、多人康养房多种户型，可根据老人自理情况、居住需求自由选择。",
                "问：是否可以实地参观养老院，怎么预约参观？答：支持免费实地参观，可线上预约、电话预约两种方式。",
                "问：养老院收费标准是什么，费用包含哪些服务？答：收费根据房型、护理等级差异化定价，费用涵盖住宿、日常膳食、基础保洁、基础健康监测、公共文娱设施使用等基础服务。",
            ]
        ),
        encoding="utf-8",
    )
    monkeypatch.setenv("AI_SERVICE_RETRIEVAL_MIN_SCORE", "0")

    from rag.service import RagService

    monkeypatch.setattr(RagService, "_build_chain", lambda self: _FailingChain())

    app = create_app()
    with TestClient(app) as client:
        client.post("/internal/kb/reindex")
        response = client.post(
            "/internal/chat",
            json={
                "sessionId": "location-session",
                "question": "养老院在哪",
                "history": [],
            },
        )

    payload = response.json()
    selected_sources = [source for source in payload["sources"] if source["selected"]]
    selected_content = "\n".join(source["content"] for source in selected_sources)

    assert response.status_code == 200
    assert payload["refused"] is False
    assert payload["hitCount"] == 1
    assert payload["retrievalTrace"]["requiredTerms"] == ["位置"]
    assert "固定院区地址" in selected_content
    assert "安全防护" not in selected_content
    assert "房型" not in selected_content
    assert "收费" not in selected_content


def test_internal_chat_list_question_returns_list_type(
    configured_env,
    monkeypatch,
) -> None:
    knowledge_base_dir = configured_env["knowledge_base_dir"]
    faq_dir = knowledge_base_dir / "faq"
    faq_dir.mkdir(parents=True)
    (faq_dir / "智能床位.txt").write_text(
        "问：智能床位能查看哪些数据？答：可以查看在床状态、离床提醒、心率、呼吸、睡眠趋势和异常告警数据。",
        encoding="utf-8",
    )
    monkeypatch.setenv("AI_SERVICE_RETRIEVAL_MIN_SCORE", "0")

    from rag.service import RagService

    monkeypatch.setattr(RagService, "_build_chain", lambda self: _FailingChain())

    app = create_app()
    with TestClient(app) as client:
        client.post("/internal/kb/reindex")
        response = client.post(
            "/internal/chat",
            json={
                "sessionId": "list-session",
                "question": "智能床位能查看哪些数据？",
                "history": [],
            },
        )

    payload = response.json()
    assert response.status_code == 200
    assert payload["questionType"] == "list"
    assert payload["refused"] is False
    assert "在床状态" in payload["answer"]


def test_internal_chat_refuses_when_evidence_is_weak(
    configured_env,
    monkeypatch,
) -> None:
    knowledge_base_dir = configured_env["knowledge_base_dir"]
    faq_dir = knowledge_base_dir / "faq"
    faq_dir.mkdir(parents=True)
    (faq_dir / "餐食.txt").write_text(
        "问：养老院餐食如何安排？答：每日提供三餐两点，并根据老人健康情况调整饮食。",
        encoding="utf-8",
    )
    monkeypatch.setenv("AI_SERVICE_RETRIEVAL_MIN_SCORE", "0")

    from rag.service import RagService

    monkeypatch.setattr(RagService, "_build_chain", lambda self: _FailingChain())

    app = create_app()
    with TestClient(app) as client:
        client.post("/internal/kb/reindex")
        response = client.post(
            "/internal/chat",
            json={
                "sessionId": "weak-session",
                "question": "护理等级在哪里配置？",
                "history": [],
            },
        )

    payload = response.json()
    assert response.status_code == 200
    assert payload["refused"] is True
    assert payload["confidence"] < 0.32
    assert "暂未找到能直接回答" in payload["answer"] or "暂未检索到" in payload["answer"]


def test_internal_chat_refuses_when_question_action_is_not_covered(
    configured_env,
    monkeypatch,
) -> None:
    knowledge_base_dir = configured_env["knowledge_base_dir"]
    faq_dir = knowledge_base_dir / "faq"
    faq_dir.mkdir(parents=True)
    (faq_dir / "收费标准.txt").write_text(
        "问：养老院收费标准是什么？答：收费根据房型、护理等级差异化定价，费用涵盖住宿、日常膳食、基础保洁、基础健康监测等服务。",
        encoding="utf-8",
    )
    monkeypatch.setenv("AI_SERVICE_RETRIEVAL_MIN_SCORE", "0")

    from rag.service import RagService

    monkeypatch.setattr(RagService, "_build_chain", lambda self: _FailingChain())

    app = create_app()
    with TestClient(app) as client:
        client.post("/internal/kb/reindex")
        response = client.post(
            "/internal/chat",
            json={
                "sessionId": "config-miss-session",
                "question": "护理等级在哪里配置？",
                "history": [],
            },
        )

    payload = response.json()
    assert response.status_code == 200
    assert payload["refused"] is True
    assert payload["hitCount"] == 0
    assert payload["sources"][0]["reason"] == "关键问题词覆盖不足"


def test_internal_chat_answers_when_configuration_evidence_is_covered(
    configured_env,
    monkeypatch,
) -> None:
    knowledge_base_dir = configured_env["knowledge_base_dir"]
    faq_dir = knowledge_base_dir / "faq"
    faq_dir.mkdir(parents=True)
    (faq_dir / "护理等级配置.txt").write_text(
        "问：护理等级在哪里配置？答：护理等级在服务管理的护理等级页面配置，可维护等级名称、护理项目和收费规则。",
        encoding="utf-8",
    )
    monkeypatch.setenv("AI_SERVICE_RETRIEVAL_MIN_SCORE", "0")

    from rag.service import RagService

    monkeypatch.setattr(RagService, "_build_chain", lambda self: _FailingChain())

    app = create_app()
    with TestClient(app) as client:
        client.post("/internal/kb/reindex")
        response = client.post(
            "/internal/chat",
            json={
                "sessionId": "config-hit-session",
                "question": "护理等级在哪里配置？",
                "history": [],
            },
        )

    payload = response.json()
    assert response.status_code == 200
    assert payload["refused"] is False
    assert payload["hitCount"] >= 1
    assert "服务管理" in payload["answer"]
    assert payload["sources"][0]["scoreBreakdown"]["answerabilityScore"] >= 0.5


def test_internal_chat_uses_rerank_score_before_final_context_selection(
    configured_env,
    monkeypatch,
) -> None:
    knowledge_base_dir = configured_env["knowledge_base_dir"]
    faq_dir = knowledge_base_dir / "faq"
    faq_dir.mkdir(parents=True)
    (faq_dir / "护理等级配置.txt").write_text(
        "\n".join(
            [
                "问：护理等级有什么作用？答：护理等级用于评估老人照护强度，并影响护理计划和收费规则。",
                "问：护理等级在哪里配置？答：护理等级在服务管理的护理等级页面配置，可维护等级名称、护理项目和收费规则。",
            ]
        ),
        encoding="utf-8",
    )
    monkeypatch.setenv("AI_SERVICE_RETRIEVAL_MIN_SCORE", "0")

    from rag.service import RagService
    from retrievers.rerank import DashScopeReranker

    def _fake_rerank(self, query, chunks, top_n=None):
        ranked = sorted(
            chunks,
            key=lambda chunk: "页面配置" in chunk.page_content,
            reverse=True,
        )
        limit = top_n or len(ranked)
        return [
            replace(chunk, score=0.92 if index == 0 else 0.21, rerank_score=0.92 if index == 0 else 0.21)
            for index, chunk in enumerate(ranked[:limit])
        ]

    monkeypatch.setattr(RagService, "_build_chain", lambda self: _FailingChain())
    monkeypatch.setattr(DashScopeReranker, "rerank", _fake_rerank)

    app = create_app()
    with TestClient(app) as client:
        client.post("/internal/kb/reindex")
        response = client.post(
            "/internal/chat",
            json={
                "sessionId": "rerank-session",
                "question": "护理等级在哪里配置？",
                "history": [],
            },
        )

    payload = response.json()
    selected_sources = [source for source in payload["sources"] if source["selected"]]

    assert response.status_code == 200
    assert payload["refused"] is False
    assert selected_sources[0]["scoreBreakdown"]["rerankScore"] == 0.92
    assert "服务管理" in selected_sources[0]["content"]
    assert selected_sources[0]["reason"] == "重排判断可回答且综合得分较高"


def test_internal_chat_prompt_context_excludes_discarded_chunks(
    configured_env,
    monkeypatch,
) -> None:
    knowledge_base_dir = configured_env["knowledge_base_dir"]
    faq_dir = knowledge_base_dir / "faq"
    faq_dir.mkdir(parents=True)
    (faq_dir / "护理等级配置.txt").write_text(
        "\n".join(
            [
                "问：护理等级有什么作用？答：护理等级用于评估老人照护强度，并影响护理计划和收费规则。",
                "问：护理等级在哪里配置？答：护理等级在服务管理的护理等级页面配置，可维护等级名称、护理项目和收费规则。",
            ]
        ),
        encoding="utf-8",
    )
    monkeypatch.setenv("AI_SERVICE_RETRIEVAL_MIN_SCORE", "0")

    from rag.service import RagService

    chain = _CapturingChain()
    monkeypatch.setattr(RagService, "_build_chain", lambda self: chain)

    app = create_app()
    with TestClient(app) as client:
        client.post("/internal/kb/reindex")
        response = client.post(
            "/internal/chat",
            json={
                "sessionId": "adopted-context-session",
                "question": "护理等级在哪里配置？",
                "history": [],
            },
        )

    payload = response.json()
    selected_sources = [source for source in payload["sources"] if source["selected"]]
    discarded_sources = [source for source in payload["sources"] if not source["selected"]]
    prompt_context = chain.payloads[0]["context"]

    assert response.status_code == 200
    assert payload["refused"] is False
    assert len(selected_sources) == payload["hitCount"]
    assert discarded_sources
    assert "服务管理" in prompt_context
    assert "评估老人照护强度" not in prompt_context


def test_internal_chat_stream_returns_delta_and_done_events(
    configured_env,
    monkeypatch,
) -> None:
    knowledge_base_dir = configured_env["knowledge_base_dir"]
    faq_dir = knowledge_base_dir / "faq"
    faq_dir.mkdir(parents=True)
    (faq_dir / "护理等级配置.txt").write_text(
        "问：护理等级在哪里配置？答：护理等级在服务管理的护理等级页面配置。",
        encoding="utf-8",
    )
    monkeypatch.setenv("AI_SERVICE_RETRIEVAL_MIN_SCORE", "0")

    from rag.service import RagService

    monkeypatch.setattr(RagService, "_build_chain", lambda self: _FailingChain())

    app = create_app()
    with TestClient(app) as client:
        client.post("/internal/kb/reindex")
        with client.stream(
            "POST",
            "/internal/chat/stream",
            json={
                "sessionId": "stream-session",
                "question": "护理等级在哪里配置？",
                "history": [],
            },
        ) as response:
            body = "".join(response.iter_text())

    assert response.status_code == 200
    assert "event: delta" in body
    assert "event: done" in body
    assert "服务管理" in body

    done_match = re.search(r"event: done\r?\ndata: (.+?)(?:\r?\n\r?\n|$)", body, flags=re.S)
    assert done_match is not None
    done_payload = json.loads(done_match.group(1))
    done_data = done_payload["data"]
    selected_count = len([source for source in done_data["sources"] if source["selected"]])
    assert done_data["hitCount"] == selected_count
    assert done_data["retrievalTrace"]["selectedCount"] == selected_count
