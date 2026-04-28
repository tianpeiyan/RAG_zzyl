package com.zzyl.assistant.service;

import com.zzyl.assistant.dto.AssistantChatRequest;
import com.zzyl.assistant.vo.AssistantChatVo;
import com.zzyl.assistant.vo.AssistantHistoryMessageVo;
import com.zzyl.assistant.vo.AssistantSessionVo;
import com.zzyl.assistant.vo.KbDocumentVo;
import com.zzyl.assistant.vo.KbIndexStatusVo;
import com.zzyl.assistant.vo.KbRebuildVo;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface AssistantService {

    AssistantChatVo chat(AssistantChatRequest request);

    SseEmitter chatStream(AssistantChatRequest request);

    List<AssistantSessionVo> sessions();

    List<AssistantHistoryMessageVo> messages(String sessionId);

    void deleteSession(String sessionId);

    List<KbDocumentVo> documents();

    KbIndexStatusVo status();

    KbRebuildVo rebuild();
}
