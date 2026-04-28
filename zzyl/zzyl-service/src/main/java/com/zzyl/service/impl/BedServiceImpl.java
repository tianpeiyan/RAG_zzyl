package com.zzyl.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.zzyl.dto.BedDto;
import com.zzyl.entity.Bed;
import com.zzyl.enums.BasicEnum;
import com.zzyl.exception.BaseException;
import com.zzyl.mapper.BedMapper;
import com.zzyl.service.BedService;
import com.zzyl.utils.ObjectUtil;
import com.zzyl.vo.BedVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BedServiceImpl implements BedService {

    @Autowired
    private BedMapper bedMapper;

    @Override
    public List<BedVo> getBedsByRoomId(Long roomId) {
        return bedMapper.getBedsByRoomId(roomId);
    }

    /**
     * 新增床位
     * @param bedDto
     */
    @Override
    public void addBed(BedDto bedDto) {

        //对象拷贝
        Bed bed = BeanUtil.toBean(bedDto, Bed.class);
        //创建人
        bed.setCreateBy(1L);
        //创建时间
        bed.setCreateTime(LocalDateTime.now());
        bed.setUpdateTime(LocalDateTime.now());
        bed.setBedStatus(0);

        try {
            bedMapper.addBed(bed);
        } catch (Exception e) {
            throw new BaseException(BasicEnum.BED_INSERT_FAIL);
        }
    }

    /**
     * 根据id查询
     *
     * @param id
     * @return
     */
    @Override
    public BedVo getById(Long id) {
        Bed bed = bedMapper.getById(id);
        return BeanUtil.toBean(bed,BedVo.class);
    }

    /**
     * 修改床位
     * @param bedDto
     */
    @Override
    public void updateBed(BedDto bedDto) {

        Bed bed = BeanUtil.toBean(bedDto, Bed.class);
        bed.setUpdateTime(LocalDateTime.now());
        bed.setUpdateBy(1L);
        if(ObjectUtil.isNotEmpty(bedDto.getBedStatus())){
            bed.setBedStatus(bedDto.getBedStatus());
        }

        bedMapper.updateBed(bed);
    }

    /**
     * 删除床位
     * @param id
     */
    @Override
    public void delBed(Long id) {
        bedMapper.deleteById(id);
    }
}

