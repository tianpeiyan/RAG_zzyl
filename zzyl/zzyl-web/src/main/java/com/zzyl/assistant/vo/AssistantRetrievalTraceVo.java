package com.zzyl.assistant.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("智能助手检索过程")
public class AssistantRetrievalTraceVo {

    @ApiModelProperty("候选片段总数")
    private Integer totalCandidates;

    @ApiModelProperty("采用片段数")
    private Integer selectedCount;

    @ApiModelProperty("丢弃片段数")
    private Integer discardedCount;

    @ApiModelProperty("改写后的检索问题")
    private String rewrittenQuery;

    @ApiModelProperty("查询扩展词")
    private List<String> expandedQueries;

    @ApiModelProperty("检索关键词")
    private List<String> requiredTerms;
}
