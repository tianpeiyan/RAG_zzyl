package com.zzyl.mapper;

import com.zzyl.entity.Bed;
import com.zzyl.vo.BedVo;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BedMapper {

    List<BedVo> getBedsByRoomId(Long roomId);

    public void addBed(Bed bed);

    public Bed getById(Long id);

    public void updateBed(Bed bed);


    public void deleteById(Long id);
}

