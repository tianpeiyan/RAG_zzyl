from __future__ import annotations

from dataclasses import dataclass
from enum import StrEnum
import re


class Intent(StrEnum):
    SMALL_TALK = "small_talk"
    DOMAIN_QA = "domain_qa"
    OUT_OF_SCOPE = "out_of_scope"


class QuestionType(StrEnum):
    FACT = "fact"
    PROCESS = "process"
    LIST = "list"
    COMPARISON = "comparison"
    CONDITION = "condition"


@dataclass(frozen=True)
class IntentResult:
    intent: Intent
    reason: str
    question_type: QuestionType | None = None
    expanded_queries: list[str] | None = None
    required_terms: list[str] | None = None
    matched_terms: list[str] | None = None
    focus_terms: list[str] | None = None
    action_terms: list[str] | None = None


@dataclass(frozen=True)
class QueryFocus:
    focus_terms: list[str]
    action_terms: list[str]


DOMAIN_KEYWORDS = {
    "中州养老",
    "养老",
    "老人",
    "院区",
    "系统",
    "功能",
    "模块",
    "操作",
    "使用",
    "管理",
    "入住",
    "退住",
    "护理",
    "护工",
    "照护",
    "照料",
    "康养",
    "失能",
    "半失能",
    "失智",
    "费用",
    "收费",
    "价格",
    "房型",
    "床位",
    "餐食",
    "饮食",
    "医保",
    "长护险",
    "报销",
    "接送",
    "参观",
    "预约",
    "探望",
    "陪护",
    "医疗",
    "医务室",
    "服药",
    "健康",
    "血压",
    "血糖",
    "文娱",
    "活动",
    "安全",
    "监控",
    "托管",
    "寄养",
    "日间",
    "智能助手",
    "知识库",
    "依据",
    "索引",
    "重建索引",
    "命中",
    "命中详情",
    "片段",
    "分片",
    "置信度",
    "RAG",
    "检索增强",
}

PROCESS_TERMS = {"流程", "步骤", "办理", "怎么做", "如何办理", "怎么处理", "怎么办", "处理方式", "操作流程"}
LIST_TERMS = {"哪些", "有什么", "有几种", "列表", "清单", "包括", "包含", "查看哪些"}
COMPARISON_TERMS = {"区别", "差异", "不同", "对比", "比较", "哪个更"}
CONDITION_TERMS = {"条件", "要求", "需要满足", "什么情况下", "是否可以", "能否", "如果"}
LOCATION_TERMS = {"在哪", "哪里", "哪儿", "地址", "位置", "院址"}

BUSINESS_TERMS = {
    "系统功能",
    "功能模块",
    "使用手册",
    "操作流程",
    "入住",
    "退住",
    "退费",
    "护理等级",
    "护理计划",
    "智能床位",
    "床位",
    "房型",
    "账单",
    "入账",
    "预约",
    "来访",
    "参观",
    "配置",
    "设置",
    "入口",
    "页面",
    "菜单",
    "新增",
    "创建",
    "查询",
    "编辑",
    "删除",
    "知识库",
    "索引",
    "重建索引",
    "命中详情",
    "置信度",
    "片段",
}

LOW_SIGNAL_REQUIRED_TERMS = {
    "养老",
    "老人",
    "院区",
    "服务",
    "康养",
    "系统",
    "功能",
    "模块",
    "操作",
    "使用",
    "管理",
}

QUESTION_STOP_TERMS = {
    "什么",
    "怎么",
    "如何",
    "哪些",
    "是否",
    "可以",
    "能否",
    "哪里",
    "哪儿",
    "在哪",
    "查看",
    "一下",
    "请问",
    "本系统",
    "系统",
    "功能",
    "模块",
}

ACTION_TERMS_BY_TYPE = {
    QuestionType.PROCESS: ["流程", "步骤", "办理", "处理"],
    QuestionType.LIST: ["数据", "查看", "包括", "包含"],
    QuestionType.COMPARISON: ["区别", "差异", "对比"],
    QuestionType.CONDITION: ["条件", "要求", "规则"],
    QuestionType.FACT: ["配置", "位置", "地址", "说明"],
}

SMALL_TALK_PATTERNS = {
    "你好",
    "您好",
    "你好啊",
    "您好啊",
    "hi",
    "hello",
    "在吗",
    "在不在",
    "早上好",
    "中午好",
    "下午好",
    "晚上好",
    "谢谢",
    "感谢",
}


def classify_intent(question: str) -> IntentResult:
    normalized = question.strip().lower()
    compact = "".join(normalized.split())

    matched_domain_terms = _matched_terms(question, DOMAIN_KEYWORDS | BUSINESS_TERMS)
    if matched_domain_terms:
        question_type = classify_question_type(question)
        focus = extract_query_focus(question, question_type, matched_domain_terms)
        required_terms = _required_terms(question, question_type, matched_domain_terms, focus)
        expanded_queries = expand_query(question, question_type, required_terms)
        return IntentResult(
            Intent.DOMAIN_QA,
            "包含养老院领域关键词",
            question_type=question_type,
            expanded_queries=expanded_queries,
            required_terms=required_terms,
            matched_terms=matched_domain_terms,
            focus_terms=focus.focus_terms,
            action_terms=focus.action_terms,
        )

    stripped = compact.strip("，。！？!?~～ ")
    if stripped in SMALL_TALK_PATTERNS or (
        len(stripped) <= 12 and any(pattern in stripped for pattern in SMALL_TALK_PATTERNS)
    ):
        return IntentResult(Intent.SMALL_TALK, "短问候或礼貌表达")

    return IntentResult(Intent.OUT_OF_SCOPE, "不属于养老院业务范围")


def classify_question_type(question: str) -> QuestionType:
    compact = "".join(question.strip().lower().split())
    if any(term in compact for term in PROCESS_TERMS):
        return QuestionType.PROCESS
    if any(term in compact for term in LIST_TERMS):
        return QuestionType.LIST
    if any(term in compact for term in COMPARISON_TERMS):
        return QuestionType.COMPARISON
    if any(term in compact for term in CONDITION_TERMS):
        return QuestionType.CONDITION
    return QuestionType.FACT


def expand_query(
    question: str,
    question_type: QuestionType,
    required_terms: list[str] | None = None,
) -> list[str]:
    compact_question = "".join(question.strip().split())
    terms = list(dict.fromkeys(required_terms or []))
    type_terms = {
        QuestionType.PROCESS: ["流程", "步骤", "办理", "处理", "方式"],
        QuestionType.LIST: ["哪些", "包括", "数据", "列表"],
        QuestionType.COMPARISON: ["区别", "差异", "对比"],
        QuestionType.CONDITION: ["条件", "要求", "规则"],
        QuestionType.FACT: ["位置", "地址", "配置", "说明"],
    }[question_type]

    expanded = [question.strip()]
    if terms:
        expanded.append(" ".join([*terms, *type_terms]))
        if len(terms) >= 2:
            expanded.append("".join(terms[:2]))
        if "功能模块" in terms:
            expanded.append("中州养老管理系统 核心业务模块 核心功能模块")
    else:
        expanded.append(" ".join([compact_question, *type_terms]))
    return [item for item in dict.fromkeys(expanded) if item]


def extract_query_focus(
    question: str,
    question_type: QuestionType,
    matched_domain_terms: list[str],
) -> QueryFocus:
    compact = "".join(question.strip().lower().split())
    has_configuration_action = any(term in compact for term in {"配置", "设置", "维护"})
    is_system_feature_question = _is_system_feature_question(compact)
    focus_terms = [
        term
        for term in matched_domain_terms
        if term not in LOW_SIGNAL_REQUIRED_TERMS and term not in QUESTION_STOP_TERMS
    ]
    if is_system_feature_question:
        focus_terms = ["功能模块"]
    action_terms = [
        term
        for term in ACTION_TERMS_BY_TYPE[question_type]
        if term in compact
        or (
            term == "位置"
            and not has_configuration_action
            and any(location in compact for location in LOCATION_TERMS)
        )
    ]

    if not focus_terms:
        for term in _fallback_cjk_terms(question):
            if term in QUESTION_STOP_TERMS or term in LOW_SIGNAL_REQUIRED_TERMS:
                continue
            if any(stop in term for stop in QUESTION_STOP_TERMS):
                continue
            focus_terms.append(term)

    return QueryFocus(
        focus_terms=_remove_redundant_terms(list(dict.fromkeys(focus_terms))),
        action_terms=_remove_redundant_terms(list(dict.fromkeys(action_terms))),
    )


def _required_terms(
    question: str,
    question_type: QuestionType,
    matched_domain_terms: list[str],
    focus: QueryFocus | None = None,
) -> list[str]:
    compact = "".join(question.strip().lower().split())
    focus = focus or extract_query_focus(question, question_type, matched_domain_terms)
    terms = [*focus.focus_terms, *focus.action_terms]
    has_configuration_action = any(term in compact for term in {"配置", "设置", "维护"})
    if (
        question_type == QuestionType.FACT
        and not has_configuration_action
        and any(term in compact for term in LOCATION_TERMS)
    ):
        terms.append("位置")
    if question_type == QuestionType.PROCESS and not any(term in terms for term in {"流程", "步骤", "办理", "处理"}):
        terms.append("流程")
    return _remove_redundant_terms(list(dict.fromkeys(terms)))


def _matched_terms(text: str, terms: set[str]) -> list[str]:
    compact = "".join(text.strip().lower().split())
    matched = [term for term in terms if term.lower() in compact]
    matched.sort(key=len, reverse=True)
    return matched


def _remove_redundant_terms(terms: list[str]) -> list[str]:
    result: list[str] = []
    for term in terms:
        if any(term != other and term in other for other in terms):
            continue
        result.append(term)
    return result


def _fallback_cjk_terms(text: str) -> list[str]:
    ignored = {"什么", "怎么", "如何", "哪些", "是否", "可以", "查看"}
    candidates = re.findall(r"[\u4e00-\u9fff]{2,}", text)
    result: list[str] = []
    for candidate in candidates:
        if candidate in ignored:
            continue
        if len(candidate) > 8:
            candidate = candidate[:8]
        result.append(candidate)
    return list(dict.fromkeys(result))


def _is_system_feature_question(compact_question: str) -> bool:
    return (
        any(term in compact_question for term in {"系统", "本系统", "管理系统"})
        and any(term in compact_question for term in {"功能", "模块", "能做什么"})
        and any(term in compact_question for term in {"哪些", "有什么", "包括", "包含", "能做什么"})
    )
