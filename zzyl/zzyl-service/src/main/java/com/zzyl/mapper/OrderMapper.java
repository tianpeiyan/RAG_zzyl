package com.zzyl.mapper;

import com.github.pagehelper.Page;
import com.zzyl.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {
    /**
     * 新增订单
     * @param order 订单信息
     */
    void insert(Order order);

    Page<Order> selectByPage(@Param("memberId") Long memberId, @Param("status") Integer status);

    Order selectByIdAndMemberId(@Param("id") Long id, @Param("memberId") Long memberId);

    int paySuccessById(@Param("id") Long id, @Param("memberId") Long memberId, @Param("now") LocalDateTime now);

    int cancelById(@Param("id") Long id,
                   @Param("memberId") Long memberId,
                   @Param("reason") String reason,
                   @Param("now") LocalDateTime now);

    int hideById(@Param("id") Long id, @Param("memberId") Long memberId, @Param("now") LocalDateTime now);

    List<Order> selectTimeoutOrders(@Param("time") LocalDateTime time);

    int updateStatusToClosed(@Param("id") Long id, @Param("now") LocalDateTime now);

    int updateStatusToRefunded(@Param("id") Long id, @Param("now") LocalDateTime now);

    Order selectById(@Param("id") Long id);
    
    int updatePayStatus(@Param("id") Long id, @Param("memberId") Long memberId, @Param("now") LocalDateTime now);

    Page<Order> selectByPageAdmin(@Param("orderNo") String orderNo, @Param("status") Integer status);

    int cancelByIdAdmin(@Param("id") Long id, @Param("reason") String reason, @Param("updateBy") Long updateBy, @Param("now") LocalDateTime now);
}
