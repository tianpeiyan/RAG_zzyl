package com.zzyl.mapper;

import com.zzyl.dto.DeptDto;
import com.zzyl.entity.Dept;
import com.zzyl.vo.DeptVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface DeptMapper {

    int insert(Dept record);

    Dept selectByPrimaryKey(Long id);

    int updateByPrimaryKey(Dept record);

    List<Dept> selectList(@Param("deptDto") DeptDto deptDto);

    List<Dept> findDeptInDeptNos(@Param("deptNos")List<String> deptNos);

    List<DeptVo> findDeptVoListInRoleId(@Param("roleIds")List<Long> roleIds);

    Dept selectByDeptNo(@Param("deptNo") String deptNo);

    /**
     * 是否存在子节点
     *
     * @param deptId 部门ID
     * @return 结果
     */
    public int hasChildByDeptId(@Param("deptId") String deptId);

    /**
     * 删除部门管理信息
     *
     * @param deptId 部门ID
     * @return 结果
     */
    public int deleteByDeptNo(@Param("deptId") String deptId);

    @Update("update sys_dept set leader_id = null where leader_id = #{leaderId} and dept_no != #{deptNo} ")
    void clearOtherDeptLeader(@Param("leaderId") Long leaderId,@Param("deptNo")String deptNo);

    @Select("select count(*) from sys_dept where parent_dept_no = #{deptNo}")
    int isLowestDept(String deptNo);

    @Update("update sys_dept set data_state = #{dataState} where dept_no = #{deptNo}")
    void updateByDeptNo(@Param("deptNo")String deptNo, @Param("dataState")String dataState);

    @Update("update sys_dept set data_state = #{dataState} where parent_dept_no like concat(#{deptNo},'%') ")
    void updateByParentDeptNo(@Param("deptNo")String deptNo, @Param("dataState")String dataState);

}
