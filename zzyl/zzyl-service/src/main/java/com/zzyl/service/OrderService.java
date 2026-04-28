package com.zzyl.service;

import com.zzyl.base.PageResponse;
import com.zzyl.dto.OrderDto;
import com.zzyl.entity.Order;
import com.zzyl.entity.RefundRecord;

/**
 * 订单服务接口
 */
public interface OrderService {

    /**
     * 创建订单
     * @param orderDto 订单参数
     * @return 订单信息
     */
    Order createOrder(OrderDto orderDto);

    /**
     * 下单前校验（例如：老人是否已退住）
     * @param elderId 老人ID
     */
    void checkPlaceOrder(Long elderId);

    PageResponse<Order> getOrderPage(Integer pageNum, Integer pageSize, Integer status);

    Order getOrderDetail(Long id);

    void payOrder(Long id);

    void cancelOrder(Long id, String reason);

    void deleteOrder(Long id);

    void refund(Long id);

    PageResponse<Order> getAdminOrderPage(Integer pageNum, Integer pageSize, String orderNo, Integer status);

    Order getAdminOrderDetail(Long id);

    void cancelOrderAdmin(Long id, String reason);

    PageResponse<RefundRecord> getRefundRecordPage(Integer pageNum, Integer pageSize, Integer refundStatus);
}
