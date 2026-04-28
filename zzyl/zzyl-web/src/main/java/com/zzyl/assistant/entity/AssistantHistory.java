package com.zzyl.assistant.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AssistantHistory {

    private Long id;

    private String sessionId;

    private Long userId;

    private String username;

    private String role;

    private String content;

    private String traceId;

    private Integer hitCount;

    private Boolean refused;

    private String status;

    private String sourceJson;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createBy;

    private Long updateBy;
}
