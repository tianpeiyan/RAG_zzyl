package com.zzyl.mapper;

import com.zzyl.dto.ResourceDto;
import com.zzyl.entity.Resource;
import com.zzyl.vo.MenuVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ResourceMapper {

    int deleteByPrimaryKey(Long id);

    int insert(Resource record);

    int insertSelective(Resource record);

    Resource selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Resource record);

    int updateByPrimaryKey(Resource record);

    /**
     * 这是Mybatis Generator拓展插件生成的方法(请勿删除).
     * This method corresponds to the database table sys_resource
     *
     * @mbg.generated
     * @author hewei
     */
    int batchInsert(@Param("list") List<Resource> list);

    List<Resource> selectList(ResourceDto dto);

    @Select("select * from sys_resource where resource_no = #{resourceNo}")
    Resource selectByResourceNo(String resourceNo);

    int deleteByResourceNo(String resourceNo);

    int countChildrenByParentResourceNo(String resourceNo);

    int updateByResourceNo(Resource resource);

    List<MenuVo> findListByUserId(Long userId);

    List<MenuVo> findAllMenu();
}