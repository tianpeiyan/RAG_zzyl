package com.zzyl.service;

import com.zzyl.base.PageResponse;
import com.zzyl.dto.NursingTaskDto;
import com.zzyl.entity.NursingTask;
import com.zzyl.vo.NursingTaskVo;

public interface NursingTaskService {
    PageResponse<NursingTaskVo> getByPage(Integer page, Integer pageSize, String elderName, Long nurseId, Long projectId, String startTime, String endTime, Integer status);

    void executeTask(NursingTaskDto nursingTaskDto);

    void cancelTask(NursingTaskDto nursingTaskDto);

    void updateTaskTime(NursingTaskDto nursingTaskDto);

    NursingTaskVo getById(Long id);
}
