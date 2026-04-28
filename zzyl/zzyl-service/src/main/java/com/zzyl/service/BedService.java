package com.zzyl.service;

import com.zzyl.dto.BedDto;
import com.zzyl.vo.BedVo;

import java.util.List;

/**
 * 服务接口：BedService（床位管理服务）
 */
public interface BedService {


    /**
     * 通过房间ID检索床位
     * @param roomId 房间ID
     * @return 床位视图对象列表
     */
    List<BedVo> getBedsByRoomId(Long roomId);

    /**
     * 新增床位
     * @param bedDto
     */
    public void addBed(BedDto bedDto);

    /**
     * 根据id查询
     * @param id
     * @return
     */
    public BedVo getById(Long id);

    /**
     * 修改床位
     * @param bedDto
     */
    public void updateBed(BedDto bedDto);

    /**
     * 删除床位
     * @param id
     */
    public void delBed(Long id);

}
