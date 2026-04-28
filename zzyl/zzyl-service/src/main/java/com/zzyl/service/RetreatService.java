package com.zzyl.service;

import com.zzyl.base.PageResponse;
import com.zzyl.dto.RetreatApplyDto;
import com.zzyl.vo.RetreatVo;

public interface RetreatService {

    /**
     * 发起退住申请
     * @param retreatApplyDto
     */
    void apply(RetreatApplyDto retreatApplyDto);

    /**
     * 分页查询
     * @param pageNum
     * @param pageSize
     * @param name
     * @param idCard
     * @param startTime
     * @param endTime
     * @return
     */
    PageResponse<RetreatVo> pageQuery(Integer pageNum, Integer pageSize, String name, String idCard, String startTime, String endTime);

    /**
     * 获取详情
     * @param id
     * @return
     */
    RetreatVo detail(Long id);
}
