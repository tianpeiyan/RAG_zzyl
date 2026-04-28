package com.zzyl.assistant.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("知识库失败任务")
public class KbFailedTaskVo {

    @ApiModelProperty("任务 ID")
    private String taskId;

    @ApiModelProperty("文件名")
    private String sourceFile;

    @ApiModelProperty("失败原因")
    private String reason;

    @ApiModelProperty("发生时间")
    private String occurredAt;
}
