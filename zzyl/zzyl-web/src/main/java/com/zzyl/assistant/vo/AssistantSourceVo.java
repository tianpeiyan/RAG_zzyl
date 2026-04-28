package com.zzyl.assistant.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@ApiModel("知识库命中来源")
public class AssistantSourceVo {

    @ApiModelProperty("文档标题")
    private String documentTitle;

    @ApiModelProperty("文档分类")
    private String category;

    @ApiModelProperty("来源文件名")
    private String sourceFile;

    @ApiModelProperty("分片 ID")
    private String chunkId;

    @ApiModelProperty("召回分数")
    private Double score;

    @ApiModelProperty("是否被本次回答采用")
    private Boolean selected;

    @ApiModelProperty("采用或丢弃原因")
    private String reason;

    @ApiModelProperty("命中的关键词")
    private List<String> matchedTerms;

    @ApiModelProperty("分数拆解")
    private Map<String, Double> scoreBreakdown;

    @ApiModelProperty("片段摘要")
    private String snippet;

    @ApiModelProperty("片段完整内容")
    private String content;
}
