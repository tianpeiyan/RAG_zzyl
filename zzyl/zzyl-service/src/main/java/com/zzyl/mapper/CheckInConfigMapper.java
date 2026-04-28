package com.zzyl.mapper;

import com.zzyl.entity.CheckInConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CheckInConfigMapper {
    int insert(CheckInConfig checkInConfig);

    CheckInConfig selectByCheckInCode(@Param("checkInCode") String checkInCode);
}
