package com.zzyl.assistant.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("知识库索引状态")
public class KbIndexStatusVo {

    @ApiModelProperty("索引状态")
    private String status;

    @ApiModelProperty("是否正在重建")
    private Boolean rebuilding;

    @ApiModelProperty("文档数量")
    private Integer documentCount;

    @ApiModelProperty("分片数量")
    private Integer chunkCount;

    @ApiModelProperty("最后索引时间")
    private String lastIndexedAt;

    @ApiModelProperty("状态说明")
    private String message;

    @ApiModelProperty("失败任务")
    private List<KbFailedTaskVo> failedTasks;
}
