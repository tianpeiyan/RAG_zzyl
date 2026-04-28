package com.zzyl.assistant.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("知识库文档")
public class KbDocumentVo {

    @ApiModelProperty("文档标题")
    private String title;

    @ApiModelProperty("文档分类")
    private String category;

    @ApiModelProperty("来源文件名")
    private String sourceFile;

    @ApiModelProperty("更新时间")
    private String updatedAt;

    @ApiModelProperty("索引状态")
    private String status;

    @ApiModelProperty("切片数量")
    private Integer chunkCount;

    @ApiModelProperty("失败原因")
    private String errorMessage;
}
