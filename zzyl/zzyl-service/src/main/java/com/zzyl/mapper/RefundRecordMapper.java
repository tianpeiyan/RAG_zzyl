package com.zzyl.mapper;

import com.github.pagehelper.Page;
import com.zzyl.entity.RefundRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RefundRecordMapper {

    int insert(RefundRecord refundRecord);

    Page<RefundRecord> selectByPage(@Param("refundStatus") Integer refundStatus);

    RefundRecord selectByProductOrderNo(Long productOrderNo);
}
