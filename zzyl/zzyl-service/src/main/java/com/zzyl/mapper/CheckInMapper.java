package com.zzyl.mapper;

import com.zzyl.entity.CheckIn;
import com.zzyl.vo.CheckInVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CheckInMapper {
    int insert(CheckIn checkIn);

    CheckIn selectByPrimaryKey(Long id);

    List<CheckInVo> selectList(@Param("name") String name, @Param("idCard") String idCard);

    /**
     * 查询入住中的记录（status=0）
     */
    List<CheckIn> selectActiveList();
}
