package com.zzyl.mapper;

import com.zzyl.entity.NursingTask;
import com.zzyl.vo.NursingTaskVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface NursingTaskMapper {

    int insert(NursingTask nursingTask);

    int update(NursingTask nursingTask);

    NursingTask selectById(Long id);

    List<NursingTaskVo> selectByPage(@Param("elderName") String elderName,
                                     @Param("nurseId") Long nurseId,
                                     @Param("projectId") Long projectId,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime,
                                     @Param("status") Integer status);

    NursingTaskVo selectVoById(Long id);

    int deletePendingPlanTasksForInvalidElders();

    /**
     * 检查是否已存在相同老人、项目、预计服务时间的计划内任务（用于定时生成时防重复）
     */
    int countByElderIdAndProjectIdAndEstimatedTime(@Param("elderId") Long elderId,
                                                    @Param("projectId") Long projectId,
                                                    @Param("estimatedServerTime") LocalDateTime estimatedServerTime);
}
