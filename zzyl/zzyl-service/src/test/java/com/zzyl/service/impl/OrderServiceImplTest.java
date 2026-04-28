package com.zzyl.service.impl;

import com.zzyl.entity.Order;
import com.zzyl.enums.BasicEnum;
import com.zzyl.exception.BaseException;
import com.zzyl.mapper.BedMapper;
import com.zzyl.mapper.ElderMapper;
import com.zzyl.mapper.NursingElderMapper;
import com.zzyl.mapper.NursingTaskMapper;
import com.zzyl.mapper.OrderMapper;
import com.zzyl.utils.UserThreadLocal;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

public class OrderServiceImplTest {

    private OrderServiceImpl orderService;
    private OrderMapper orderMapper;
    private NursingTaskMapper nursingTaskMapper;
    private NursingElderMapper nursingElderMapper;
    private ElderMapper elderMapper;
    private BedMapper bedMapper;

    @Before
    public void setUp() {
        orderService = new OrderServiceImpl();
        orderMapper = Mockito.mock(OrderMapper.class);
        nursingTaskMapper = Mockito.mock(NursingTaskMapper.class);
        nursingElderMapper = Mockito.mock(NursingElderMapper.class);
        elderMapper = Mockito.mock(ElderMapper.class);
        bedMapper = Mockito.mock(BedMapper.class);
        ReflectionTestUtils.setField(orderService, "orderMapper", orderMapper);
        ReflectionTestUtils.setField(orderService, "nursingTaskMapper", nursingTaskMapper);
        ReflectionTestUtils.setField(orderService, "nursingElderMapper", nursingElderMapper);
        ReflectionTestUtils.setField(orderService, "elderMapper", elderMapper);
        ReflectionTestUtils.setField(orderService, "bedMapper", bedMapper);
        UserThreadLocal.set(1L);
    }

    @After
    public void tearDown() {
        UserThreadLocal.remove();
    }

    @Test
    public void payOrder_shouldReturn_whenAlreadyPaid() {
        Order order = new Order();
        order.setId(10L);
        order.setMemberId(1L);
        order.setStatus(1);
        order.setPaymentStatus(2);
        Mockito.when(orderMapper.selectByIdAndMemberId(10L, 1L)).thenReturn(order);

        orderService.payOrder(10L);

        Mockito.verify(orderMapper, Mockito.never()).paySuccessById(Mockito.anyLong(), Mockito.anyLong(), Mockito.any(LocalDateTime.class));
    }

    @Test
    public void payOrder_shouldThrow_whenStatusNotPendingPay() {
        Order order = new Order();
        order.setId(10L);
        order.setMemberId(1L);
        order.setStatus(4);
        order.setPaymentStatus(1);
        Mockito.when(orderMapper.selectByIdAndMemberId(10L, 1L)).thenReturn(order);

        try {
            orderService.payOrder(10L);
            Assert.fail("Expected BaseException");
        } catch (BaseException e) {
            Assert.assertEquals(BasicEnum.ORDER_CLOSED.getCode(), e.getBasicEnum().getCode());
        }
    }

    @Test
    public void payOrder_shouldThrow_whenUpdateFailed() {
        Order order = new Order();
        order.setId(10L);
        order.setMemberId(1L);
        order.setStatus(0);
        order.setPaymentStatus(1);
        Mockito.when(orderMapper.selectByIdAndMemberId(10L, 1L)).thenReturn(order);
        Mockito.when(orderMapper.paySuccessById(Mockito.eq(10L), Mockito.eq(1L), Mockito.any(LocalDateTime.class))).thenReturn(0);

        try {
            orderService.payOrder(10L);
            Assert.fail("Expected RuntimeException");
        } catch (RuntimeException e) {
            Assert.assertEquals("订单支付状态更新失败", e.getMessage());
        }
    }

    @Test
    public void refund_success() {
        Order order = new Order();
        order.setId(1L);
        order.setStatus(1); // Paid
        order.setPaymentStatus(2); // Paid
        Mockito.when(orderMapper.selectById(1L)).thenReturn(order);
        Mockito.when(orderMapper.updateStatusToRefunded(Mockito.eq(1L), Mockito.any(LocalDateTime.class))).thenReturn(1);

        orderService.refund(1L);
        Mockito.verify(orderMapper, Mockito.times(1)).updateStatusToRefunded(Mockito.eq(1L), Mockito.any(LocalDateTime.class));
    }

    @Test(expected = RuntimeException.class)
    public void refund_fail_not_paid() {
        Order order = new Order();
        order.setId(1L);
        order.setStatus(0); // Unpaid
        order.setPaymentStatus(1); // Unpaid
        Mockito.when(orderMapper.selectById(1L)).thenReturn(order);

        orderService.refund(1L);
    }
}
