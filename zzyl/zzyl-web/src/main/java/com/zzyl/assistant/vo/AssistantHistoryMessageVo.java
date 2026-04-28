package com.zzyl.assistant.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@ApiModel("智能助手历史消息")
public class AssistantHistoryMessageVo {

    @ApiModelProperty("消息 ID")
    private Long id;

    @ApiModelProperty("会话 ID")
    private String sessionId;

    @ApiModelProperty("角色：user/assistant")
    private String role;

    @ApiModelProperty("消息内容")
    private String content;

    @ApiModelProperty("链路追踪 ID")
    private String traceId;

    @ApiModelProperty("命中文档数")
    private Integer hitCount;

    @ApiModelProperty("是否拒答")
    private Boolean refused;

    @ApiModelProperty("回答状态")
    private String status;

    @ApiModelProperty("命中来源")
    private List<AssistantSourceVo> sources;

    @ApiModelProperty("问题类型")
    private String questionType;

    @ApiModelProperty("答案置信度")
    private Double confidence;

    @ApiModelProperty("检索过程")
    private AssistantRetrievalTraceVo retrievalTrace;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
}
