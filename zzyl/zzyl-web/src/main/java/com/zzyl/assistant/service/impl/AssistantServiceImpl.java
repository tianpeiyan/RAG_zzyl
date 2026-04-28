package com.zzyl.assistant.service.impl;

import cn.hutool.json.JSONUtil;
import com.zzyl.assistant.client.AssistantPythonClient;
import com.zzyl.assistant.dto.AssistantChatRequest;
import com.zzyl.assistant.dto.InternalAssistantChatRequest;
import com.zzyl.assistant.exception.AssistantServiceException;
import com.zzyl.assistant.service.AssistantAuditService;
import com.zzyl.assistant.service.AssistantHistoryService;
import com.zzyl.assistant.service.AssistantService;
import com.zzyl.assistant.vo.AssistantChatVo;
import com.zzyl.assistant.vo.AssistantHistoryMessageVo;
import com.zzyl.assistant.vo.AssistantSessionVo;
import com.zzyl.assistant.vo.KbDocumentVo;
import com.zzyl.assistant.vo.KbIndexStatusVo;
import com.zzyl.assistant.vo.KbRebuildVo;
import com.zzyl.utils.StringUtils;
import com.zzyl.utils.UserThreadLocal;
import com.zzyl.vo.UserVo;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.UUID;

@Service
public class AssistantServiceImpl implements AssistantService {

    private static final int STREAM_CHUNK_SIZE = 6;
    private static final ExecutorService STREAM_EXECUTOR = Executors.newCachedThreadPool();

    private final AssistantPythonClient assistantPythonClient;
    private final AssistantAuditService assistantAuditService;
    private final AssistantHistoryService assistantHistoryService;

    public AssistantServiceImpl(AssistantPythonClient assistantPythonClient,
                                AssistantAuditService assistantAuditService,
                                AssistantHistoryService assistantHistoryService) {
        this.assistantPythonClient = assistantPythonClient;
        this.assistantAuditService = assistantAuditService;
        this.assistantHistoryService = assistantHistoryService;
    }

    @Override
    public AssistantChatVo chat(AssistantChatRequest request) {
        validateQuestion(request);
        UserVo user = currentUser();
        String sessionId = normalizeSessionId(request.getSessionId());
        String username = displayUsername(user);
        InternalAssistantChatRequest internalRequest = new InternalAssistantChatRequest();
        internalRequest.setSessionId(sessionId);
        internalRequest.setQuestion(request.getQuestion());
        internalRequest.setHistory(assistantHistoryService.recentContext(user.getId(), sessionId));
        internalRequest.setUserId(user.getId());
        internalRequest.setUsername(username);

        long start = System.currentTimeMillis();
        assistantHistoryService.recordUserMessage(user.getId(), username, sessionId, request.getQuestion().trim());
        try {
            AssistantChatVo response = assistantPythonClient.chat(internalRequest);
            if (StringUtils.isEmpty(response.getSessionId())) {
                response.setSessionId(sessionId);
            }
            assistantHistoryService.recordAssistantMessage(user.getId(), username, response.getSessionId(), response);
            assistantAuditService.recordChat(
                    user.getId(),
                    username,
                    request.getQuestion(),
                    System.currentTimeMillis() - start,
                    "success",
                    response,
                    null
            );
            return response;
        } catch (AssistantServiceException exception) {
            assistantHistoryService.recordFailedAssistantMessage(user.getId(), username, sessionId, exception.getMessage());
            assistantAuditService.recordChat(
                    user.getId(),
                    username,
                    request.getQuestion(),
                    System.currentTimeMillis() - start,
                    "failed",
                    null,
                    exception.getMessage()
            );
            throw exception;
        }
    }

    @Override
    public SseEmitter chatStream(AssistantChatRequest request) {
        validateQuestion(request);
        UserVo user = currentUser();
        String sessionId = normalizeSessionId(request.getSessionId());
        String username = displayUsername(user);
        InternalAssistantChatRequest internalRequest = new InternalAssistantChatRequest();
        internalRequest.setSessionId(sessionId);
        internalRequest.setQuestion(request.getQuestion());
        internalRequest.setHistory(assistantHistoryService.recentContext(user.getId(), sessionId));
        internalRequest.setUserId(user.getId());
        internalRequest.setUsername(username);

        SseEmitter emitter = new SseEmitter(0L);
        STREAM_EXECUTOR.execute(() -> {
            long start = System.currentTimeMillis();
            assistantHistoryService.recordUserMessage(user.getId(), username, sessionId, request.getQuestion().trim());
            try {
                AssistantChatVo response = assistantPythonClient.chat(internalRequest);
                if (StringUtils.isEmpty(response.getSessionId())) {
                    response.setSessionId(sessionId);
                }
                streamAnswer(emitter, response.getAnswer());
                sendEvent(emitter, "done", donePayload(response));
                assistantHistoryService.recordAssistantMessage(user.getId(), username, response.getSessionId(), response);
                assistantAuditService.recordChat(
                        user.getId(),
                        username,
                        request.getQuestion(),
                        System.currentTimeMillis() - start,
                        "success",
                        response,
                        null
                );
                emitter.complete();
            } catch (AssistantServiceException exception) {
                assistantHistoryService.recordFailedAssistantMessage(user.getId(), username, sessionId, exception.getMessage());
                assistantAuditService.recordChat(
                        user.getId(),
                        username,
                        request.getQuestion(),
                        System.currentTimeMillis() - start,
                        "failed",
                        null,
                        exception.getMessage()
                );
                safeSendEvent(emitter, "error", errorPayload(exception.getMessage()));
                emitter.complete();
            } catch (Exception exception) {
                safeSendEvent(emitter, "error", errorPayload("智能助手暂不可用"));
                emitter.completeWithError(exception);
            }
        });
        return emitter;
    }

    @Override
    public List<AssistantSessionVo> sessions() {
        UserVo user = currentUser();
        return assistantHistoryService.sessions(user.getId());
    }

    @Override
    public List<AssistantHistoryMessageVo> messages(String sessionId) {
        if (StringUtils.isEmpty(sessionId)) {
            throw new AssistantServiceException(400, "会话 ID 不能为空");
        }
        UserVo user = currentUser();
        return assistantHistoryService.messages(user.getId(), sessionId);
    }

    @Override
    public void deleteSession(String sessionId) {
        if (StringUtils.isEmpty(sessionId)) {
            throw new AssistantServiceException(400, "会话 ID 不能为空");
        }
        UserVo user = currentUser();
        assistantHistoryService.deleteSession(user.getId(), sessionId);
    }

    @Override
    public List<KbDocumentVo> documents() {
        UserVo user = currentUser();
        return assistantPythonClient.documents(user.getId(), displayUsername(user));
    }

    @Override
    public KbIndexStatusVo status() {
        UserVo user = currentUser();
        return assistantPythonClient.status(user.getId(), displayUsername(user));
    }

    @Override
    public KbRebuildVo rebuild() {
        UserVo user = currentUser();
        return assistantPythonClient.rebuild(user.getId(), displayUsername(user));
    }

    private void validateQuestion(AssistantChatRequest request) {
        if (request == null || StringUtils.isEmpty(request.getQuestion())) {
            throw new AssistantServiceException(400, "请输入要咨询的问题");
        }
        if (request.getQuestion().trim().length() > 1000) {
            throw new AssistantServiceException(400, "问题内容不能超过 1000 个字符");
        }
    }

    private UserVo currentUser() {
        String subject = UserThreadLocal.getSubject();
        if (StringUtils.isEmpty(subject)) {
            throw new AssistantServiceException(401, "登录状态失效，请重新登录");
        }
        return JSONUtil.toBean(subject, UserVo.class);
    }

    private String normalizeSessionId(String sessionId) {
        if (StringUtils.isNotEmpty(sessionId)) {
            return sessionId.trim();
        }
        return "session-" + UUID.randomUUID();
    }

    private String displayUsername(UserVo user) {
        if (user == null) {
            return "";
        }
        if (StringUtils.isNotEmpty(user.getUsername())) {
            return user.getUsername();
        }
        if (StringUtils.isNotEmpty(user.getRealName())) {
            return user.getRealName();
        }
        return user.getNickName();
    }

    private void streamAnswer(SseEmitter emitter, String answer) throws IOException, InterruptedException {
        String content = StringUtils.isEmpty(answer) ? "知识库暂无依据" : answer;
        for (int index = 0; index < content.length(); index += STREAM_CHUNK_SIZE) {
            String delta = content.substring(index, Math.min(index + STREAM_CHUNK_SIZE, content.length()));
            sendEvent(emitter, "delta", deltaPayload(delta));
            Thread.sleep(25L);
        }
    }

    private void sendEvent(SseEmitter emitter, String eventName, Map<String, Object> payload) throws IOException {
        emitter.send(SseEmitter.event().name(eventName).data(JSONUtil.toJsonStr(payload)));
    }

    private void safeSendEvent(SseEmitter emitter, String eventName, Map<String, Object> payload) {
        try {
            sendEvent(emitter, eventName, payload);
        } catch (IOException ignored) {
            // Client already disconnected.
        }
    }

    private Map<String, Object> deltaPayload(String delta) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "delta");
        payload.put("delta", delta);
        return payload;
    }

    private Map<String, Object> donePayload(AssistantChatVo response) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "done");
        payload.put("data", response);
        return payload;
    }

    private Map<String, Object> errorPayload(String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "error");
        payload.put("message", message);
        return payload;
    }
}
