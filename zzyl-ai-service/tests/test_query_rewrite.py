from __future__ import annotations

from rag.query_rewrite import QueryRewriteService


def test_query_rewrite_standardizes_colloquial_retreat_question(configured_env) -> None:
    from config.settings import get_settings

    service = QueryRewriteService(get_settings())
    result = service.rewrite("如何退住")

    assert result.rewritten_question == "退住流程是什么？"
    assert result.expanded_queries == ["如何退住", "退住流程是什么？"]


def test_query_rewrite_standardizes_care_level_configuration_question(configured_env) -> None:
    from config.settings import get_settings

    service = QueryRewriteService(get_settings())
    result = service.rewrite("护理等级怎么设")

    assert result.rewritten_question == "护理等级在哪里配置？"


def test_query_rewrite_standardizes_system_feature_question(configured_env) -> None:
    from config.settings import get_settings

    service = QueryRewriteService(get_settings())
    result = service.rewrite("本系统有哪些功能")

    assert result.rewritten_question == "中州养老管理系统功能模块有哪些？"
