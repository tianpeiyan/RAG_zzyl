package com.zzyl.assistant.dto;

import lombok.Data;

import java.util.List;

@Data
public class InternalAssistantChatRequest {

    private String sessionId;

    private String question;

    private List<AssistantMessageDto> history;

    private Long userId;

    private String username;
}
