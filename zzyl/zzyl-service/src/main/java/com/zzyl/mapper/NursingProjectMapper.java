package com.zzyl.mapper;

import com.github.pagehelper.Page;
import com.zzyl.entity.NursingProject;
import com.zzyl.vo.NursingProjectVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface NursingProjectMapper {
    int insert(NursingProject nursingProject);
    
    int update(NursingProject nursingProject);
    
    int deleteById(long id);
    
    NursingProject findById(long id);
    
    Page<NursingProjectVo> selectByPage(String name, Integer status);

    @Update("update nursing_project set status = #{status} where id = #{id}")
    void isEnable(Long id, Integer status);

    @Select("select * from nursing_project")
    List<NursingProjectVo> selectAll();
}