package com.zzyl.test;

import com.zzyl.assistant.entity.AssistantHistory;
import com.zzyl.assistant.mapper.AssistantHistoryMapper;
import com.zzyl.assistant.service.AssistantHistoryService;
import com.zzyl.assistant.vo.AssistantChatVo;
import com.zzyl.assistant.vo.AssistantHistoryMessageVo;
import com.zzyl.assistant.vo.AssistantRetrievalTraceVo;
import com.zzyl.assistant.vo.AssistantSessionVo;
import com.zzyl.assistant.vo.AssistantSourceVo;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AssistantHistoryServiceTest {

    @Test
    public void shouldRestoreAssistantSourcePayloadFromHistory() {
        FakeAssistantHistoryMapper mapper = new FakeAssistantHistoryMapper();
        AssistantHistoryService service = new AssistantHistoryService(mapper);

        AssistantSourceVo source = new AssistantSourceVo();
        source.setDocumentTitle("退住流程");
        source.setSourceFile("faq/退住流程.txt");
        source.setSelected(true);
        source.setMatchedTerms(Collections.singletonList("退住"));
        source.setSnippet("发起退住申请");

        AssistantRetrievalTraceVo trace = new AssistantRetrievalTraceVo();
        trace.setTotalCandidates(2);
        trace.setSelectedCount(1);
        trace.setDiscardedCount(1);
        trace.setRewrittenQuery("退住流程是什么？");
        trace.setRequiredTerms(Collections.singletonList("退住"));

        AssistantChatVo response = new AssistantChatVo();
        response.setSessionId("session-test");
        response.setAnswer("根据知识库依据，流程可按以下步骤处理。");
        response.setStatus("success");
        response.setRefused(false);
        response.setHitCount(1);
        response.setQuestionType("process");
        response.setConfidence(0.86);
        response.setSources(Collections.singletonList(source));
        response.setRetrievalTrace(trace);

        service.recordAssistantMessage(1L, "tester", "session-test", response);
        List<AssistantHistoryMessageVo> messages = service.messages(1L, "session-test");

        assertEquals(1, messages.size());
        AssistantHistoryMessageVo message = messages.get(0);
        assertEquals("process", message.getQuestionType());
        assertEquals(Double.valueOf(0.86), message.getConfidence());
        assertNotNull(message.getRetrievalTrace());
        assertEquals(Integer.valueOf(2), message.getRetrievalTrace().getTotalCandidates());
        assertEquals("退住流程是什么？", message.getRetrievalTrace().getRewrittenQuery());
        assertEquals(1, message.getSources().size());
        assertEquals("退住流程", message.getSources().get(0).getDocumentTitle());
        assertEquals(Collections.singletonList("退住"), message.getSources().get(0).getMatchedTerms());
    }

    private static class FakeAssistantHistoryMapper implements AssistantHistoryMapper {
        private final List<AssistantHistory> histories = new ArrayList<>();

        @Override
        public int insert(AssistantHistory history) {
            history.setId((long) histories.size() + 1);
            histories.add(history);
            return 1;
        }

        @Override
        public List<AssistantHistory> findBySession(Long userId, String sessionId) {
            return histories;
        }

        @Override
        public List<AssistantHistory> findRecentMessages(Long userId, String sessionId, int limit) {
            return histories;
        }

        @Override
        public List<AssistantSessionVo> findSessions(Long userId, int limit) {
            return Collections.emptyList();
        }

        @Override
        public int deleteSession(Long userId, String sessionId) {
            histories.clear();
            return 1;
        }
    }
}
