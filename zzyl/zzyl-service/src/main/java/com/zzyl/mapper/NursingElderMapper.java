package com.zzyl.mapper;

import com.zzyl.entity.NursingElder;
import com.zzyl.vo.UserVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NursingElderMapper {
    List<UserVo> selectUserByElderId(@Param("elderId") Long elderId);

    List<String> selectNursingNameByElderId(@Param("elderId") Long elderId);

    List<Long> selectNursingIdsByElderId(@Param("elderId") Long elderId);

    int deleteByElderId(@Param("elderId") Long elderId);

    int insertBatch(@Param("list") List<NursingElder> list);
}
