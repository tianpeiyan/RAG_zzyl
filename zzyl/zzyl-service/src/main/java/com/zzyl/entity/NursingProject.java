package com.zzyl.entity;

import com.zzyl.base.BaseEntity;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
* <p>
* nursing_project 实体类
* </p>
*
* @author yuhon
* @since 2024-01-02 11:10:46
*/
@Getter
@Setter
public class NursingProject extends BaseEntity {

    /**
    * 名称
    */
    private String name;

    /**
    * 排序号
    */
    private Integer orderNo;

    /**
    * 单位
    */
    private String unit;

    /**
    * 价格
    */
    private BigDecimal price;

    /**
    * 图片
    */
    private String image;

    /**
    * 护理要求
    */
    private String nursingRequirement;

    /**
    * 状态（0：禁用，1：启用）
    */
    private Integer status;

}
