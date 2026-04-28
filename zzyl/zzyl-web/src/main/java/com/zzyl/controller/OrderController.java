package com.zzyl.controller;

import com.zzyl.base.PageResponse;
import com.zzyl.base.ResponseResult;
import com.zzyl.entity.Order;
import com.zzyl.entity.RefundRecord;
import com.zzyl.service.OrderService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Api(tags = "后台订单管理")
public class OrderController extends BaseController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/orders/search")
    @ApiOperation("分页查询订单")
    public ResponseResult<PageResponse<Order>> getOrderPage(
            @ApiParam(value = "订单号") @RequestParam(required = false) String orderNo,
            @ApiParam(value = "状态") @RequestParam(required = false) Integer status,
            @ApiParam(value = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResponse<Order> pageResponse = orderService.getAdminOrderPage(pageNum, pageSize, orderNo, status);
        return success(pageResponse);
    }

    @GetMapping("/orders")
    @ApiOperation("查询订单详情")
    public ResponseResult<Order> getOrderDetail(@RequestParam("orderId") Long orderId) {
        Order order = orderService.getAdminOrderDetail(orderId);
        if (order == null) {
            return error("订单不存在");
        }
        return success(order);
    }

    @PostMapping("/orders/{id}/cancel")
    @ApiOperation("取消订单")
    public ResponseResult cancelOrder(@PathVariable("id") Long id, @RequestParam(required = false) String reason) {
        orderService.cancelOrderAdmin(id, reason);
        return success();
    }

    @PostMapping("/trade-common-feign/query-refund-record")
    @ApiOperation("分页查询退款记录")
    public ResponseResult<PageResponse<RefundRecord>> getRefundRecordPage(
            @RequestBody Map<String, Object> params) {
        Integer pageNum = 1;
        Integer pageSize = 10;
        Integer refundStatus = null;

        if (params.get("pageNum") != null) {
            pageNum = (Integer) params.get("pageNum");
        }
        if (params.get("pageSize") != null) {
            pageSize = (Integer) params.get("pageSize");
        }
        if (params.get("refundStatus") != null) {
            refundStatus = (Integer) params.get("refundStatus");
        }
        
        PageResponse<RefundRecord> pageResponse = orderService.getRefundRecordPage(pageNum, pageSize, refundStatus);
        return success(pageResponse);
    }

    @PostMapping("/trade-common-feign/refund")
    @ApiOperation("订单退款")
    public ResponseResult refund(@RequestBody Map<String, Object> params) {
        Long id = null;
        if (params != null) {
            if (params.get("id") != null) {
                id = Long.valueOf(params.get("id").toString());
            } else if (params.get("productOrderNo") != null) {
                id = Long.valueOf(params.get("productOrderNo").toString());
            } else if (params.get("tradingOrderNo") != null) {
                 try {
                     id = Long.valueOf(params.get("tradingOrderNo").toString());
                 } catch (Exception e) {}
            }
        }
        
        if (id == null) {
             return error("订单ID不能为空");
        }

        orderService.refund(id);
        return success();
    }
}
