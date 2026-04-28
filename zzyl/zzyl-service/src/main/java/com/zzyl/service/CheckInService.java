package com.zzyl.service;

import com.zzyl.base.PageResponse;
import com.zzyl.dto.CheckInDto;
import com.zzyl.vo.CheckInDetailVo;
import com.zzyl.vo.CheckInVo;

public interface CheckInService {
    /**
     * 入住申请
     * @param checkInDto
     */
    void apply(CheckInDto checkInDto);

    /**
     * 分页查询
     * @param pageNum
     * @param pageSize
     * @param name
     * @param idCard
     * @return
     */
    PageResponse<CheckInVo> pageQuery(Integer pageNum, Integer pageSize, String name, String idCard);

    /**
     * 获取详情
     * @param id
     * @return
     */
    CheckInDetailVo detail(Long id);
}
