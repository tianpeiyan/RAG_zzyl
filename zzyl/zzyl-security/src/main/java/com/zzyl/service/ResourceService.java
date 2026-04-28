package com.zzyl.service;

import com.zzyl.dto.ResourceDto;
import com.zzyl.vo.MenuVo;
import com.zzyl.vo.ResourceVo;
import com.zzyl.vo.TreeVo;

import java.util.List;

public interface ResourceService {
    /**
     * 多条件查询资源列表
     * @param resourceDto
     * @return
     */
    List<ResourceVo> findResourceList(ResourceDto resourceDto);

    /**
     * 资源树形
     * @param resourceDto
     * @return
     */
    TreeVo resourceTreeVo(ResourceDto resourceDto);

    /**
     * 添加资源菜单
     * @param resourceDto
     */
    void createResource(ResourceDto resourceDto);

    /**
     * 修改资源
     * @param resourceDto
     */
    void updateResource(ResourceDto resourceDto);

    /**
     * 删除资源
     * @param resourceNo
     */
    void deleteResource(String resourceNo);

    /**
     * 启用禁用资源
     * @param resourceDto
     */
    void enable(ResourceDto resourceDto);

    /**
     * @return
     *  动态菜单（根据用户id，实现不同用户可访问资源不同）
     */
    List<MenuVo> menus(Long userId);
}
