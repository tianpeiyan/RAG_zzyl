package com.zzyl.assistant.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("助手会话消息")
public class AssistantMessageDto {

    @ApiModelProperty("角色：user/assistant")
    private String role;

    @ApiModelProperty("消息内容")
    private String content;
}
