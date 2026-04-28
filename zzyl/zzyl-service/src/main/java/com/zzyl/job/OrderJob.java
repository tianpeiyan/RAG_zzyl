package com.zzyl.job;

import com.zzyl.entity.Order;
import com.zzyl.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderJob {

    @Autowired
    private OrderMapper orderMapper;

    /**
     * 超时订单自动取消
     * 每分钟执行一次，检查并取消超过15分钟未支付的订单
     */
    @Scheduled(cron = "0 * * * * ?")
    public void processTimeoutOrders() {
        log.info("超时订单-自动取消-begin");
        LocalDateTime time = LocalDateTime.now().minusMinutes(15);
        List<Order> orders = orderMapper.selectTimeoutOrders(time);
        
        if (orders != null && !orders.isEmpty()) {
            for (Order order : orders) {
                try {
                    orderMapper.updateStatusToClosed(order.getId(), LocalDateTime.now());
                    log.info("订单超时自动取消: {}", order.getId());
                } catch (Exception e) {
                    log.error("订单超时自动取消失败: {}", order.getId(), e);
                }
            }
        }
        log.info("超时订单-自动取消-end");
    }
}
