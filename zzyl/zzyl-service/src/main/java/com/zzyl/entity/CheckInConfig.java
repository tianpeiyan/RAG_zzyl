package com.zzyl.entity;

import com.zzyl.base.BaseEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 入住配置实体类
 */
@Data
public class CheckInConfig extends BaseEntity {

    /**
     * 老人ID
     */
    private Long elderId;

    /**
     * 入住编码
     */
    private String checkInCode;

    /**
     * 入住开始时间
     */
    private LocalDateTime checkInStartTime;

    /**
     * 入住结束时间
     */
    private LocalDateTime checkInEndTime;

    /**
     * 护理等级ID
     */
    private Long nursingLevelId;

    /**
     * 护理等级名称
     */
    private String nursingLevelName;

    /**
     * 床位号
     */
    private String bedNumber;

    /**
     * 费用开始时间
     */
    private LocalDateTime costStartTime;

    /**
     * 费用结束时间
     */
    private LocalDateTime costEndTime;

    /**
     * 押金（元）
     */
    private BigDecimal depositAmount;

    /**
     * 护理费用（元/月）
     */
    private BigDecimal nursingCost;

    /**
     * 床位费用（元/月）
     */
    private BigDecimal bedCost;

    /**
     * 其他费用（元/月）
     */
    private BigDecimal otherCost;

    /**
     * 医保支付（元/月）
     */
    private BigDecimal medicalInsurancePayment;

    /**
     * 政府补贴（元/月）
     */
    private BigDecimal governmentSubsidy;
    
    /**
     * 备注
     */
    private String remark;

}
