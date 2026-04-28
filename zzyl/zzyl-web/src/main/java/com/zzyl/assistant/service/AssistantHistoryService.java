package com.zzyl.assistant.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zzyl.assistant.dto.AssistantMessageDto;
import com.zzyl.assistant.entity.AssistantHistory;
import com.zzyl.assistant.mapper.AssistantHistoryMapper;
import com.zzyl.assistant.vo.AssistantChatVo;
import com.zzyl.assistant.vo.AssistantHistoryMessageVo;
import com.zzyl.assistant.vo.AssistantRetrievalTraceVo;
import com.zzyl.assistant.vo.AssistantSessionVo;
import com.zzyl.assistant.vo.AssistantSourceVo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AssistantHistoryService {

    private static final int CONTEXT_MESSAGE_LIMIT = 10;
    private static final int SESSION_LIMIT = 50;

    private final AssistantHistoryMapper assistantHistoryMapper;

    public AssistantHistoryService(AssistantHistoryMapper assistantHistoryMapper) {
        this.assistantHistoryMapper = assistantHistoryMapper;
    }

    public List<AssistantMessageDto> recentContext(Long userId, String sessionId) {
        return assistantHistoryMapper.findRecentMessages(userId, sessionId, CONTEXT_MESSAGE_LIMIT)
                .stream()
                .map(this::toMessageDto)
                .collect(Collectors.toList());
    }

    public void recordUserMessage(Long userId, String username, String sessionId, String question) {
        AssistantHistory history = baseHistory(userId, username, sessionId, "user", question);
        history.setStatus("success");
        assistantHistoryMapper.insert(history);
    }

    public void recordAssistantMessage(Long userId, String username, String sessionId, AssistantChatVo response) {
        AssistantHistory history = baseHistory(userId, username, sessionId, "assistant", response.getAnswer());
        history.setTraceId(response.getTraceId());
        history.setHitCount(response.getHitCount());
        history.setRefused(response.getRefused());
        history.setStatus(response.getStatus());
        if (response.getSources() != null || response.getRetrievalTrace() != null) {
            Map<String, Object> sourcePayload = new HashMap<>();
            sourcePayload.put("sources", response.getSources());
            sourcePayload.put("questionType", response.getQuestionType());
            sourcePayload.put("confidence", response.getConfidence());
            sourcePayload.put("retrievalTrace", response.getRetrievalTrace());
            history.setSourceJson(JSONUtil.toJsonStr(sourcePayload));
        }
        assistantHistoryMapper.insert(history);
    }

    public void recordFailedAssistantMessage(Long userId, String username, String sessionId, String failReason) {
        AssistantHistory history = baseHistory(userId, username, sessionId, "assistant", failReason);
        history.setStatus("failed");
        history.setRefused(true);
        assistantHistoryMapper.insert(history);
    }

    public List<AssistantSessionVo> sessions(Long userId) {
        return assistantHistoryMapper.findSessions(userId, SESSION_LIMIT);
    }

    public List<AssistantHistoryMessageVo> messages(Long userId, String sessionId) {
        return assistantHistoryMapper.findBySession(userId, sessionId)
                .stream()
                .map(this::toMessageVo)
                .collect(Collectors.toList());
    }

    public void deleteSession(Long userId, String sessionId) {
        assistantHistoryMapper.deleteSession(userId, sessionId);
    }

    private AssistantHistory baseHistory(Long userId, String username, String sessionId, String role, String content) {
        LocalDateTime now = LocalDateTime.now();
        AssistantHistory history = new AssistantHistory();
        history.setUserId(userId);
        history.setUsername(username);
        history.setSessionId(sessionId);
        history.setRole(role);
        history.setContent(content == null ? "" : content);
        history.setCreateTime(now);
        history.setUpdateTime(now);
        return history;
    }

    private AssistantMessageDto toMessageDto(AssistantHistory history) {
        AssistantMessageDto message = new AssistantMessageDto();
        message.setRole(history.getRole());
        message.setContent(history.getContent());
        return message;
    }

    private AssistantHistoryMessageVo toMessageVo(AssistantHistory history) {
        AssistantHistoryMessageVo message = new AssistantHistoryMessageVo();
        message.setId(history.getId());
        message.setSessionId(history.getSessionId());
        message.setRole(history.getRole());
        message.setContent(history.getContent());
        message.setTraceId(history.getTraceId());
        message.setHitCount(history.getHitCount());
        message.setRefused(history.getRefused());
        message.setStatus(history.getStatus());
        fillSourcePayload(message, history.getSourceJson());
        message.setCreateTime(history.getCreateTime());
        return message;
    }

    private void fillSourcePayload(AssistantHistoryMessageVo message, String sourceJson) {
        if (sourceJson == null || sourceJson.trim().isEmpty()) {
            return;
        }
        try {
            Object parsed = JSONUtil.parse(sourceJson);
            if (parsed instanceof JSONArray) {
                message.setSources(JSONUtil.toList((JSONArray) parsed, AssistantSourceVo.class));
                return;
            }
            if (!(parsed instanceof JSONObject)) {
                return;
            }
            JSONObject object = (JSONObject) parsed;
            Object sources = object.get("sources");
            if (sources instanceof JSONArray) {
                message.setSources(JSONUtil.toList((JSONArray) sources, AssistantSourceVo.class));
            }
            message.setQuestionType(object.getStr("questionType"));
            message.setConfidence(object.getDouble("confidence"));
            Object trace = object.get("retrievalTrace");
            if (trace instanceof JSONObject) {
                message.setRetrievalTrace(JSONUtil.toBean((JSONObject) trace, AssistantRetrievalTraceVo.class));
            }
        } catch (RuntimeException ignored) {
            // 历史来源解析失败不影响会话消息展示。
        }
    }
}
