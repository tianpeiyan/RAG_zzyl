from __future__ import annotations

from dataclasses import dataclass
import re

from config.settings import Settings


@dataclass(frozen=True)
class QueryRewriteResult:
    original_question: str
    rewritten_question: str
    expanded_queries: list[str]


class QueryRewriteService:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings

    def rewrite(self, question: str) -> QueryRewriteResult:
        original = question.strip()
        if not self.settings.query_rewrite_enabled or not original:
            return QueryRewriteResult(original, original, [original] if original else [])

        rule_rewritten = self._rewrite_by_rules(original)
        rewritten = rule_rewritten
        if self._should_use_llm(original, rule_rewritten):
            rewritten = self._rewrite_by_llm(original, rule_rewritten)

        queries = [original, rewritten]
        if rule_rewritten not in queries:
            queries.append(rule_rewritten)
        return QueryRewriteResult(
            original_question=original,
            rewritten_question=rewritten,
            expanded_queries=[item for item in dict.fromkeys(queries) if item],
        )

    def _should_use_llm(self, original: str, rule_rewritten: str) -> bool:
        if not self.settings.query_rewrite_llm_enabled:
            return False
        if self.settings.app_env.lower() == "test":
            return False
        if self.settings.bailian_api_key.strip().lower() in {"", "dummy-key", "test", "test-key"}:
            return False
        compact = "".join(original.lower().split())
        if self._contains_any(compact, {"你好", "您好", "谢谢", "感谢"}):
            return False
        if self._contains_any(compact, {"在哪里", "在哪儿", "在哪", "地址", "位置"}) and "配置" not in compact:
            return False
        return original == rule_rewritten

    def _rewrite_by_llm(self, question: str, fallback: str) -> str:
        try:
            from langchain_community.chat_models import ChatTongyi
            from langchain_core.output_parsers import StrOutputParser
            from langchain_core.prompts import ChatPromptTemplate

            prompt = ChatPromptTemplate.from_messages(
                [
                    (
                        "system",
                        "你是中州养老知识库检索前的查询改写器。"
                        "只输出一个适合知识库检索的标准中文问句，不要回答问题。"
                        "规则：保留用户核心业务对象；把口语化表达改成知识库常见问法；"
                        "涉及办理动作时优先补成“流程是什么”；涉及位置补成“具体位置在哪”；"
                        "涉及配置补成“在哪里配置”；无法判断时原样输出。",
                    ),
                    ("human", "{question}"),
                ]
            )
            chat_model = ChatTongyi(
                model=self.settings.bailian_chat_model,
                api_key=self.settings.bailian_api_key,
            )
            rewritten = (prompt | chat_model | StrOutputParser()).invoke({"question": question})
        except Exception:
            return fallback
        return self._sanitize_llm_rewrite(question, rewritten) or fallback

    def _sanitize_llm_rewrite(self, original: str, rewritten: str) -> str:
        line = " ".join(str(rewritten).strip().splitlines()).strip()
        line = re.sub(r"^(标准问法|改写后|输出)\s*[:：]\s*", "", line).strip()
        if not line:
            return original
        if len(line) > 80:
            return original
        if any(marker in line for marker in {"答：", "答案：", "根据知识库"}):
            return original
        return line

    def _rewrite_by_rules(self, question: str) -> str:
        compact = "".join(question.lower().split())

        if (
            self._contains_any(compact, {"系统", "本系统", "管理系统"})
            and self._contains_any(compact, {"哪些功能", "有什么功能", "功能模块", "包括哪些", "能做什么"})
        ):
            return "中州养老管理系统功能模块有哪些？"

        if self._contains_any(compact, {"如何退住", "怎么退住", "退住怎么办", "退住咋办", "我要退住", "想退住", "办理退住"}):
            return "退住流程是什么？"
        if "退住" in compact and not self._contains_any(compact, {"流程", "步骤", "规则", "押金", "退费"}):
            return "退住流程是什么？"

        if self._contains_any(compact, {"如何入住", "怎么入住", "入住怎么办", "入住咋办", "我要入住", "想入住", "办理入住"}):
            return "入住流程是什么？"
        if "入住" in compact and not self._contains_any(compact, {"流程", "步骤", "条件", "材料", "资料", "收费"}):
            return "入住流程是什么？"

        if self._contains_any(compact, {"怎么收费", "收费多少", "多少钱", "价格多少", "费用多少"}):
            return "养老院收费标准是什么？"

        if "护理等级" in compact and self._contains_any(compact, {"怎么配", "怎么设", "设置", "配置", "维护"}):
            return "护理等级在哪里配置？"

        if "智能床位" in compact and self._contains_any(compact, {"看什么", "查什么", "有哪些", "哪些数据"}):
            return "智能床位能查看哪些数据？"

        return question

    def _contains_any(self, text: str, terms: set[str]) -> bool:
        return any(term in text for term in terms)
