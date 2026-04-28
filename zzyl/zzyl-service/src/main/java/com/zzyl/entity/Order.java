package com.zzyl.entity;

import com.zzyl.base.BaseEntity;
import com.zzyl.vo.BedVo;
import com.zzyl.vo.ElderVo;
import com.zzyl.vo.NursingProjectVo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Order extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 交易系统订单号
     */
    private Long tradingOrderNo;

    /**
     * 付款状态,1.未付 2已付 3已关闭
     */
    private Integer paymentStatus;

    /**
     * 金额
     */
    private BigDecimal amount;

    /**
     * 退款金额
     */
    private BigDecimal refund;

    /**
     * 是否有退款：YES，NO
     */
    private String isRefund;

    /**
     * 客户id
     */
    private Long memberId;

    /**
     * 服务项目id
     */
    private Long projectId;

    /**
     * 服务对象ID
     */
    private Long elderId;

    /**
     * 预计服务时间
     */
    private LocalDateTime estimatedArrivalTime;

    /**
     * 用户备注
     */
    private String mark;

    /**
     * 取消原因
     */
    private String reason;

    /**
     * 订单状态 0待支付 1待执行 2已执行 3已完成 4已关闭 5已退款
     */
    private Integer status;

    /**
     * 是否可见 0可见 1不可见
     */
    private String viewStatus;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 取消人类型 1前台 2后台
     */
    private Integer oCreateType;

    private ElderVo elderVo;

    private NursingProjectVo nursingProjectVo;

    private Member memberVo;

    private BedVo bedVo;

    private Object refundRecordVo;
}
