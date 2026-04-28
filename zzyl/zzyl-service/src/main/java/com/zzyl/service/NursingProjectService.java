package com.zzyl.service;

import com.zzyl.base.PageResponse;
import com.zzyl.dto.NursingProjectDto;
import com.zzyl.vo.NursingProjectVo;

import java.util.List;

public interface NursingProjectService {

    /**
     * 分页条件查询护理项目
     * @param name
     * @param status
     * @param pageNum
     * @param pageSize
     * @return
     */
    public PageResponse selectByPage(String name, Integer status, Integer pageNum, Integer pageSize);

    /**
     * 新增护理项目
     * @param nursingProjectDto
     */
    public void add(NursingProjectDto nursingProjectDto);

    /**
     * 根据id查询
     * @param id
     * @return
     */
    public NursingProjectVo findById(Long id);

    /**
     * 修改护理项目
     * @param nursingProjectDto
     */
    public void update(NursingProjectDto nursingProjectDto);

    /**
     * 启用或禁用
     * @param id
     * @param status
     */
    public void isEnable(Long id,Integer status);

    /**
     * 删除护理项目
     * @param id
     */
    void deleteById(Long id);

    List<NursingProjectVo> selectAll();
}
