package com.zzyl.assistant.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("智能助手回答")
public class AssistantChatVo {

    @ApiModelProperty("会话 ID")
    private String sessionId;

    @ApiModelProperty("回答内容")
    private String answer;

    @ApiModelProperty("回答状态")
    private String status;

    @ApiModelProperty("是否拒答")
    private Boolean refused;

    @ApiModelProperty("命中文档数量")
    private Integer hitCount;

    @ApiModelProperty("链路追踪 ID")
    private String traceId;

    @ApiModelProperty("命中来源，前端首版可不展示")
    private List<AssistantSourceVo> sources;

    @ApiModelProperty("问题类型")
    private String questionType;

    @ApiModelProperty("答案置信度")
    private Double confidence;

    @ApiModelProperty("检索过程")
    private AssistantRetrievalTraceVo retrievalTrace;
}
