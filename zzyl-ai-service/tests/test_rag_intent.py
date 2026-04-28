from __future__ import annotations

from rag.intent import Intent, QuestionType, classify_intent


def test_greeting_does_not_require_retrieval() -> None:
    assert classify_intent("你好").intent == Intent.SMALL_TALK


def test_domain_question_requires_retrieval() -> None:
    assert classify_intent("养老院收费标准是什么").intent == Intent.DOMAIN_QA


def test_staff_system_feature_question_requires_retrieval() -> None:
    result = classify_intent("本系统有哪些功能")

    assert result.intent == Intent.DOMAIN_QA
    assert result.question_type == QuestionType.LIST
    assert result.required_terms == ["功能模块"]


def test_location_question_requires_position_evidence() -> None:
    result = classify_intent("养老院在哪")

    assert result.intent == Intent.DOMAIN_QA
    assert result.question_type == QuestionType.FACT
    assert "位置" in (result.required_terms or [])
    assert result.action_terms == ["位置"]


def test_configuration_question_requires_subject_and_configuration_action() -> None:
    result = classify_intent("护理等级在哪里配置？")

    assert result.intent == Intent.DOMAIN_QA
    assert result.question_type == QuestionType.FACT
    assert "护理等级" in (result.required_terms or [])
    assert "配置" in (result.required_terms or [])


def test_process_question_type_is_detected() -> None:
    result = classify_intent("退住办理流程是什么？")

    assert result.intent == Intent.DOMAIN_QA
    assert result.question_type == QuestionType.PROCESS
    assert "退住" in (result.required_terms or [])
    assert any("流程" in query for query in result.expanded_queries or [])


def test_kb_troubleshooting_question_requires_retrieval() -> None:
    result = classify_intent("知识库暂无依据时应该怎么处理？")

    assert result.intent == Intent.DOMAIN_QA
    assert result.question_type == QuestionType.PROCESS
    assert "知识库" in (result.required_terms or [])
    assert "依据" in (result.required_terms or [])
    assert "处理" in (result.required_terms or [])
    assert "流程" not in (result.required_terms or [])


def test_list_question_type_is_detected() -> None:
    result = classify_intent("智能床位能查看哪些数据？")

    assert result.intent == Intent.DOMAIN_QA
    assert result.question_type == QuestionType.LIST
    assert result.focus_terms == ["智能床位"]
    assert "数据" in (result.action_terms or [])


def test_out_of_scope_question_is_rejected() -> None:
    assert classify_intent("今天天气怎么样").intent == Intent.OUT_OF_SCOPE
