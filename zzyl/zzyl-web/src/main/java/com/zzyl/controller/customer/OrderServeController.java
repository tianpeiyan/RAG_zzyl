package com.zzyl.controller.customer;

import com.zzyl.base.PageResponse;
import com.zzyl.base.ResponseResult;
import com.zzyl.controller.BaseController;
import com.zzyl.dto.OrderCheckDto;
import com.zzyl.dto.OrderDto;
import com.zzyl.entity.Order;
import com.zzyl.service.NursingProjectService;
import com.zzyl.service.OrderService;
import com.zzyl.vo.NursingProjectVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/customer/orders")
@Api(tags = "用户端订单管理")
public class OrderServeController extends BaseController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private NursingProjectService nursingProjectService;

    @PostMapping
    @ApiOperation("用户下单")
    public ResponseResult createOrder(@RequestBody OrderDto orderDto) {
        Order order = orderService.createOrder(orderDto);
        return success(order);
    }

    @PostMapping("/check")
    @ApiOperation("下单前校验")
    public ResponseResult check(@RequestBody OrderCheckDto orderCheckDto) {
        orderService.checkPlaceOrder(orderCheckDto.getElderId());
        return success();
    }

    @GetMapping("/order/page")
    @ApiOperation("分页查询订单")
    public ResponseResult<PageResponse<Order>> getOrderPage(
            @ApiParam(value = "状态") @RequestParam(required = false) Integer status,
            @ApiParam(value = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize) {
        PageResponse<Order> pageResponse = orderService.getOrderPage(pageNum, pageSize, status);
        return success(pageResponse);
    }

    @GetMapping("/{id}")
    @ApiOperation("查询订单详情")
    public ResponseResult<Order> getOrderDetail(@PathVariable("id") Long id) {
        Order order = orderService.getOrderDetail(id);
        if (order == null) {
            return error("订单不存在");
        }
        return success(order);
    }

    @PostMapping("/{id}/pay")
    @ApiOperation("订单支付成功")
    public ResponseResult payOrder(@PathVariable("id") Long id) {
        orderService.payOrder(id);
        return success();
    }

    @PostMapping("/{id}/cancel")
    @ApiOperation("取消订单")
    public ResponseResult cancelOrder(@PathVariable("id") Long id, @RequestParam(required = false) String reason) {
        orderService.cancelOrder(id, reason);
        return success();
    }

    @DeleteMapping("/{id}")
    @ApiOperation("删除订单")
    public ResponseResult deleteOrder(@PathVariable("id") Long id) {
        orderService.deleteOrder(id);
        return success();
    }

    @PostMapping("/refund")
    @ApiOperation("订单退款")
    public ResponseResult refund(@RequestBody(required = false) Map<String, Object> params) {
        if (params == null) {
            return ResponseResult.error();
        }
        Long id = null;
        if (params.get("id") != null) {
            try {
                id = Long.valueOf(params.get("id").toString());
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        if (id == null && params.get("tradingOrderNo") != null) {
            try {
                id = Long.valueOf(params.get("tradingOrderNo").toString());
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        if (id == null && params.get("orderNo") != null) {
             try {
                id = Long.valueOf(params.get("orderNo").toString());
            } catch (NumberFormatException e) {
                // ignore
            }
        }

        if (id != null) {
            orderService.refund(id);
            return success();
        }
        return ResponseResult.error();
    }

    @GetMapping("/project/page")
    @ApiOperation("分页查询护理项目")
    public ResponseResult<PageResponse<NursingProjectVo>> selectProjectByPage(
            @ApiParam(value = "护理项目名称") @RequestParam(required = false) String name,
            @ApiParam(value = "状态") @RequestParam(required = false) Integer status,
            @ApiParam(value = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @ApiParam(value = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize) {
        // 用户端默认只展示启用的项目
        if (status == null) {
            status = 1;
        }
        PageResponse<NursingProjectVo> pageResponse = nursingProjectService.selectByPage(name, status, pageNum, pageSize);
        return success(pageResponse);
    }

    @GetMapping("/project/{id}")
    @ApiOperation("根据ID查询护理项目")
    public ResponseResult<NursingProjectVo> getProjectById(@PathVariable("id") Long id) {
        NursingProjectVo nursingProjectVo = nursingProjectService.findById(id);
        return success(nursingProjectVo);
    }
}
