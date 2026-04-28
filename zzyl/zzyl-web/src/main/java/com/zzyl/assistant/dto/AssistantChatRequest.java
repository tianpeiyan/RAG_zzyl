package com.zzyl.assistant.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("智能助手提问请求")
public class AssistantChatRequest {

    @ApiModelProperty("会话 ID，前端本地生成或为空")
    private String sessionId;

    @ApiModelProperty(value = "用户问题", required = true)
    private String question;

    @ApiModelProperty("历史消息，首版可为空")
    private List<AssistantMessageDto> history;
}
