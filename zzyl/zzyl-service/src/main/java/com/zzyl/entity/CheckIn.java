package com.zzyl.entity;

import com.zzyl.base.BaseEntity;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 入住申请实体类
 */
@Data
public class CheckIn extends BaseEntity {

    /**
     * 编号
     */
    private String checkInCode;

    /**
     * 标题
     */
    private String title;

    /**
     * 老人id
     */
    private Long elderId;

    /**
     * 入住时间
     */
    private LocalDateTime checkInTime;

    /**
     * 备注
     */
    private String remark;

    /**
     * 申请人
     */
    private String applicat;

    /**
     * 部门编号
     */
    private String deptNo;

    /**
     * 申请人id
     */
    private Long applicatId;

    /**
     * 入住状态，0：入住中，1：已退住
     */
    private Integer status;

    /**
     * 其他申请信息
     */
    private String otherApplyInfo;

}
