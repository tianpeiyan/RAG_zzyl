package com.zzyl.assistant.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel("智能助手会话摘要")
public class AssistantSessionVo {

    @ApiModelProperty("会话 ID")
    private String sessionId;

    @ApiModelProperty("会话标题")
    private String title;

    @ApiModelProperty("消息数")
    private Integer messageCount;

    @ApiModelProperty("提问数")
    private Integer questionCount;

    @ApiModelProperty("最近更新时间")
    private LocalDateTime updatedAt;
}
