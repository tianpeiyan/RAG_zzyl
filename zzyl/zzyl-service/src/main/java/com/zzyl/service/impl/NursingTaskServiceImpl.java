package com.zzyl.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zzyl.base.PageResponse;
import com.zzyl.dto.NursingTaskDto;
import com.zzyl.entity.NursingTask;
import com.zzyl.mapper.ContractMapper;
import com.zzyl.mapper.NursingTaskMapper;
import com.zzyl.service.NursingTaskService;
import com.zzyl.vo.NursingTaskVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
public class NursingTaskServiceImpl implements NursingTaskService {

    @Autowired
    private NursingTaskMapper nursingTaskMapper;
    @Autowired
    private ContractMapper contractMapper;

    @Override
    public PageResponse<NursingTaskVo> getByPage(Integer page, Integer pageSize, String elderName, Long nurseId, Long projectId, String startTime, String endTime, Integer status) {
        contractMapper.updateExpiredStatus(LocalDateTime.now(), 2);
        nursingTaskMapper.deletePendingPlanTasksForInvalidElders();
        PageHelper.startPage(page, pageSize);
        DateTimeFormatter df = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime start = null;
        LocalDateTime end = null;
        if (startTime != null && !startTime.isEmpty()) {
             start = LocalDateTime.parse(startTime, df);
        }
        if (endTime != null && !endTime.isEmpty()) {
             end = LocalDateTime.parse(endTime, df);
        }

        // 仅展示当前月份数据：不删除历史任务，查询时限定在本月范围（与传入时间范围取交集）
        LocalDate nowDate = LocalDate.now();
        LocalDateTime monthStart = nowDate.withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = nowDate.withDayOfMonth(nowDate.lengthOfMonth()).atTime(LocalTime.MAX);
        if (start == null || start.isBefore(monthStart)) {
            start = monthStart;
        }
        if (end == null || end.isAfter(monthEnd)) {
            end = monthEnd;
        }

        List<NursingTaskVo> list = nursingTaskMapper.selectByPage(elderName, nurseId, projectId, start, end, status);
        return PageResponse.of((Page<NursingTaskVo>) list, NursingTaskVo.class);
    }

    @Override
    public void executeTask(NursingTaskDto nursingTaskDto) {
        NursingTask task = nursingTaskMapper.selectById(nursingTaskDto.getId());
        if (task != null) {
            task.setStatus(2); // Completed
            task.setRealServerTime(LocalDateTime.now());
            task.setTaskImage(nursingTaskDto.getTaskImage());
            task.setMark(nursingTaskDto.getMark());
            task.setUpdateTime(LocalDateTime.now());
            nursingTaskMapper.update(task);
        }
    }

    @Override
    public void cancelTask(NursingTaskDto nursingTaskDto) {
        NursingTask task = nursingTaskMapper.selectById(nursingTaskDto.getId());
        if (task != null) {
            task.setStatus(3); // Cancelled
            task.setCancelReason(nursingTaskDto.getReason());
            task.setUpdateTime(LocalDateTime.now());
            nursingTaskMapper.update(task);
        }
    }

    @Override
    public void updateTaskTime(NursingTaskDto nursingTaskDto) {
        NursingTask task = nursingTaskMapper.selectById(nursingTaskDto.getId());
        if (task != null) {
            task.setEstimatedServerTime(nursingTaskDto.getEstimatedServerTime());
            task.setUpdateTime(LocalDateTime.now());
            nursingTaskMapper.update(task);
        }
    }

    @Override
    public NursingTaskVo getById(Long id) {
        return nursingTaskMapper.selectVoById(id);
    }
}
