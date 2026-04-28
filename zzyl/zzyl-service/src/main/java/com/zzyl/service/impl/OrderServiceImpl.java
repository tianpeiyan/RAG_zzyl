package com.zzyl.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zzyl.base.PageResponse;
import com.zzyl.dto.OrderDto;
import com.zzyl.entity.Bed;
import com.zzyl.entity.Elder;
import com.zzyl.entity.NursingTask;
import com.zzyl.entity.Order;
import com.zzyl.entity.RefundRecord;
import com.zzyl.enums.BasicEnum;
import com.zzyl.exception.BaseException;
import com.zzyl.mapper.BedMapper;
import com.zzyl.mapper.ElderMapper;
import com.zzyl.mapper.NursingElderMapper;
import com.zzyl.mapper.NursingTaskMapper;
import com.zzyl.mapper.OrderMapper;
import com.zzyl.mapper.RefundRecordMapper;
import com.zzyl.mapper.MemberMapper;
import com.zzyl.service.BedService;
import com.zzyl.service.ElderService;
import com.zzyl.service.NursingProjectService;
import com.zzyl.service.OrderService;
import com.zzyl.utils.SnowflakeIdWorker;
import com.zzyl.utils.UserThreadLocal;
import com.zzyl.vo.BedVo;
import com.zzyl.vo.NursingProjectVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.zzyl.vo.ElderVo;
import com.zzyl.entity.Member;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private ElderService elderService;

    @Autowired
    private NursingProjectService nursingProjectService;

    @Autowired
    private RefundRecordMapper refundRecordMapper;
    @Autowired
    private NursingTaskMapper nursingTaskMapper;
    @Autowired
    private NursingElderMapper nursingElderMapper;
    @Autowired
    private ElderMapper elderMapper;
    @Autowired
    private BedMapper bedMapper;

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private BedService bedService;

    private final SnowflakeIdWorker snowflakeIdWorker = new SnowflakeIdWorker(1, 1);

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order createOrder(OrderDto orderDto) {
        Order order = new Order();
        BeanUtils.copyProperties(orderDto, order);

        // 前端备注映射到订单备注字段
        order.setMark(orderDto.getRemark());
        order.setRemark(orderDto.getRemark());

        // 设置默认状态
        order.setStatus(0);
        order.setPaymentStatus(1);
        order.setIsRefund("NO");
        order.setViewStatus("0");
        order.setOCreateType(1);

        // 生成订单号
        long orderId = snowflakeIdWorker.nextId();
        order.setTradingOrderNo(orderId);
        order.setOrderNo(String.valueOf(orderId));

        // 设置用户与时间
        Long userId = UserThreadLocal.getUserId();
        order.setMemberId(userId);
        order.setCreateBy(userId);
        order.setUpdateBy(userId);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());

        order.setRefund(BigDecimal.ZERO);

        orderMapper.insert(order);
        return order;
    }

    @Override
    public void checkPlaceOrder(Long elderId) {
        ElderVo elderVo = elderService.selectByPrimaryKey(elderId);
        if (elderVo == null) {
            throw new BaseException(BasicEnum.ELDER_NOT_EXIST);
        }
        if (elderVo.getStatus() != null && elderVo.getStatus() == 5) {
            throw new BaseException(BasicEnum.CANNOT_PLACE_ORDER_DUE_ELDER_ALREADY_RETREATED);
        }
    }

    @Override
    public PageResponse<Order> getOrderPage(Integer pageNum, Integer pageSize, Integer status) {
        Integer currentPage = pageNum == null ? 1 : pageNum;
        Integer currentPageSize = pageSize == null ? 10 : pageSize;

        Long memberId = UserThreadLocal.getUserId();
        PageHelper.startPage(currentPage, currentPageSize);
        Page<Order> page = orderMapper.selectByPage(memberId, status);

        List<Order> orders = page.getResult();
        if (orders != null && !orders.isEmpty()) {
            for (Order order : orders) {
                enrichOrder(order);
            }
        }

        return PageResponse.of(orders, page.getPageNum(), page.getPageSize(), (long) page.getPages(), page.getTotal());
    }

    @Override
    public Order getOrderDetail(Long id) {
        Long memberId = UserThreadLocal.getUserId();
        Order order = orderMapper.selectByIdAndMemberId(id, memberId);
        if (order == null) {
            return null;
        }
        enrichOrder(order);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Long id) {
        Long memberId = UserThreadLocal.getUserId();
        Order order = orderMapper.selectByIdAndMemberId(id, memberId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getPaymentStatus() != null && order.getPaymentStatus() == 2) {
            return;
        }
        if (order.getStatus() != null && order.getStatus() != 0) {
            throw new BaseException(BasicEnum.ORDER_CLOSED);
        }

        int rows = orderMapper.paySuccessById(id, memberId, LocalDateTime.now());
        if (rows <= 0) {
            throw new RuntimeException("订单支付状态更新失败");
        }
        NursingTask task = buildTaskFromOrder(order, memberId);
        if (task != null) {
            nursingTaskMapper.insert(task);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long id, String reason) {
        Long memberId = UserThreadLocal.getUserId();
        String cancelReason = reason == null ? "" : reason;
        int rows = orderMapper.cancelById(id, memberId, cancelReason, LocalDateTime.now());
        if (rows <= 0) {
            throw new RuntimeException("订单无法取消");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long id) {
        Long memberId = UserThreadLocal.getUserId();
        int rows = orderMapper.hideById(id, memberId, LocalDateTime.now());
        if (rows <= 0) {
            throw new RuntimeException("订单无法删除");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getPaymentStatus() != 2) {
             throw new RuntimeException("订单未支付，无法退款");
        }
        if (order.getStatus() == 5) {
             throw new RuntimeException("订单已退款");
        }
        
        int rows = orderMapper.updateStatusToRefunded(id, LocalDateTime.now());
        if (rows <= 0) {
            throw new RuntimeException("订单退款状态更新失败");
        }
        
        RefundRecord record = new RefundRecord();
        record.setTradingOrderNo(order.getTradingOrderNo());
        record.setProductOrderNo(order.getId());
        record.setRefundNo(snowflakeIdWorker.nextId());
        record.setRefundStatus(2); // 成功
        record.setRefundAmount(order.getAmount());
        record.setCreateTime(LocalDateTime.now());
        record.setCreateBy(UserThreadLocal.getMgtUserId());
        record.setCreateType(2); // 后台
        refundRecordMapper.insert(record);
    }

    @Override
    public PageResponse<Order> getAdminOrderPage(Integer pageNum, Integer pageSize, String orderNo, Integer status) {
        PageHelper.startPage(pageNum, pageSize);
        Page<Order> page = (Page<Order>) orderMapper.selectByPageAdmin(orderNo, status);
        page.getResult().forEach(this::enrichOrder);
        return PageResponse.of(page.getResult(), page.getPageNum(), page.getPageSize(), (long)page.getPages(), page.getTotal());
    }

    @Override
    public Order getAdminOrderDetail(Long id) {
        Order order = orderMapper.selectById(id);
        if (order != null) {
            enrichOrder(order);
        }
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrderAdmin(Long id, String reason) {
        Long userId = UserThreadLocal.getUserId();
        String cancelReason = reason == null ? "" : reason;
        int rows = orderMapper.cancelByIdAdmin(id, cancelReason, userId, LocalDateTime.now());
        if (rows <= 0) {
            throw new RuntimeException("订单无法取消");
        }
    }

    @Override
    public PageResponse<RefundRecord> getRefundRecordPage(Integer pageNum, Integer pageSize, Integer refundStatus) {
        PageHelper.startPage(pageNum, pageSize);
        Page<RefundRecord> page = refundRecordMapper.selectByPage(refundStatus);
        return PageResponse.of(page.getResult(), page.getPageNum(), page.getPageSize(), (long)page.getPages(), page.getTotal());
    }


    private void enrichOrder(Order order) {
        if (order == null) {
            return;
        }

        if (order.getProjectId() != null) {
            NursingProjectVo nursingProjectVo = nursingProjectService.findById(order.getProjectId());
            order.setNursingProjectVo(nursingProjectVo);
        }
        if (order.getElderId() != null) {
            ElderVo elderVo = elderService.selectByPrimaryKey(order.getElderId());
            order.setElderVo(elderVo);
            if (elderVo != null && elderVo.getBedName() != null) {
                BedVo bedVo = new BedVo();
                bedVo.setBedNumber(elderVo.getBedName());
                order.setBedVo(bedVo);
            }
        }
        if (order.getMemberId() != null) {
            Member member = memberMapper.getById(order.getMemberId());
            order.setMemberVo(member);
        }

        Map<String, Object> refundRecordVo = new HashMap<>();
        RefundRecord refundRecord = refundRecordMapper.selectByProductOrderNo(order.getId());
        if (refundRecord != null) {
            refundRecordVo.put("refundStatus", refundRecord.getRefundStatus());
            refundRecordVo.put("refundMsg", refundRecord.getRefundMsg());
            refundRecordVo.put("updateTime", refundRecord.getUpdateTime());
            refundRecordVo.put("refundAmount", refundRecord.getRefundAmount());
        } else {
            refundRecordVo.put("refundStatus", null);
            refundRecordVo.put("refundMsg", null);
        }
        order.setRefundRecordVo(refundRecordVo);
    }

    private NursingTask buildTaskFromOrder(Order order, Long memberId) {
        if (order == null || order.getElderId() == null || order.getProjectId() == null) {
            return null;
        }
        NursingTask task = new NursingTask();
        task.setTaskType(1);
        task.setStatus(1);
        task.setElderId(order.getElderId());
        task.setProjectId(order.getProjectId());
        task.setNursingId(getPrimaryNursingId(order.getElderId()));
        task.setBedNumber(getBedNumber(order.getElderId()));
        task.setEstimatedServerTime(order.getEstimatedArrivalTime());
        task.setRelNo(order.getOrderNo());
        task.setRemark(order.getMark());
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        task.setCreateBy(memberId);
        task.setUpdateBy(memberId);
        return task;
    }

    private String getBedNumber(Long elderId) {
        if (elderId == null) {
            return null;
        }
        Elder elder = elderMapper.selectByPrimaryKey(elderId);
        if (elder == null) {
            return null;
        }
        if (elder.getBedNumber() != null && !elder.getBedNumber().isEmpty()) {
            return elder.getBedNumber();
        }
        if (elder.getBedId() == null) {
            return null;
        }
        Bed bed = bedMapper.getById(elder.getBedId());
        return bed == null ? null : bed.getBedNumber();
    }

    private Long getPrimaryNursingId(Long elderId) {
        if (elderId == null) {
            return null;
        }
        List<Long> nursingIds = nursingElderMapper.selectNursingIdsByElderId(elderId);
        if (nursingIds == null || nursingIds.isEmpty()) {
            return null;
        }
        return nursingIds.get(0);
    }
}
