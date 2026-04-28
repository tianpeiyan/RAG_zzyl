package com.zzyl.entity;

import com.zzyl.base.BaseEntity;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 退住申请实体类
 */
@Data
public class Retreat extends BaseEntity {

    /**
     * 退住编号
     */
    private String retreatCode;

    /**
     * 标题
     */
    private String title;

    /**
     * 老人id
     */
    private Long elderId;

    /**
     * 老人姓名
     */
    private String name;

    /**
     * 身份证号
     */
    private String idCardNo;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 家庭住址
     */
    private String address;

    /**
     * 入住开始时间
     */
    private LocalDateTime checkInStartTime;

    /**
     * 入住结束时间
     */
    private LocalDateTime checkInEndTime;

    /**
     * 退住时间
     */
    private LocalDateTime checkOutTime;

    /**
     * 费用开始时间
     */
    private LocalDateTime costStartTime;

    /**
     * 费用结束时间
     */
    private LocalDateTime costEndTime;

    /**
     * 护理等级名称
     */
    private String nursingLevelName;

    /**
     * 入住床位
     */
    private String bedNumber;

    /**
     * 合同名称
     */
    private String contractName;

    /**
     * 合同URL
     */
    private String contractUrl;

    /**
     * 合同编号
     */
    private String contractNo;

    /**
     * 备注
     */
    private String remark;

    /**
     * 退住原因
     */
    private String reason;

    /**
     * 护理员姓名
     */
    private String nursingName;

    /**
     * 申请人
     */
    private String applicat;

    /**
     * 申请人ID
     */
    private Long applicatId;

    /**
     * 部门编号
     */
    private String deptNo;
}
