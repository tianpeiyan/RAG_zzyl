package com.zzyl.entity;

import com.zzyl.base.BaseEntity;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class Bed extends BaseEntity {

    /**
     * 床位编号
     */
    private String bedNumber;
    /**
     * 床位状态
     */
    private Integer bedStatus;
    /**
     * 房间ID
     */
    private Long roomId;

    /**
     * 排序号
     */
    private Integer sort;
}
