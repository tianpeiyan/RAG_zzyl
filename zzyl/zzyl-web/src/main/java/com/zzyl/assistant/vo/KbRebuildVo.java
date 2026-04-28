package com.zzyl.assistant.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("知识库重建结果")
public class KbRebuildVo {

    @ApiModelProperty("任务 ID")
    private String taskId;

    @ApiModelProperty("任务状态")
    private String status;

    @ApiModelProperty("提示消息")
    private String message;
}
