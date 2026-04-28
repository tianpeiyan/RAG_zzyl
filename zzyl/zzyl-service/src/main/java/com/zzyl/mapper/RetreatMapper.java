package com.zzyl.mapper;

import com.zzyl.entity.Retreat;
import com.zzyl.vo.RetreatVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RetreatMapper {
    int insert(Retreat retreat);

    Retreat selectByPrimaryKey(Long id);

    List<RetreatVo> selectList(@Param("name") String name, @Param("idCard") String idCard, @Param("startTime") String startTime, @Param("endTime") String endTime);

    Retreat selectByElderId(@Param("elderId") Long elderId);
}
