package com.zzyl.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import com.google.common.collect.Lists;
import com.zzyl.constant.SuperConstant;
import com.zzyl.dto.DeptDto;
import com.zzyl.entity.Dept;
import com.zzyl.enums.BasicEnum;
import com.zzyl.exception.BaseException;
import com.zzyl.mapper.DeptMapper;
import com.zzyl.mapper.PostMapper;
import com.zzyl.service.DeptService;
import com.zzyl.utils.EmptyUtil;
import com.zzyl.utils.NoProcessing;
import com.zzyl.utils.ObjectUtil;
import com.zzyl.utils.StringUtils;
import com.zzyl.vo.DeptVo;
import com.zzyl.vo.TreeItemVo;
import com.zzyl.vo.TreeVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 部门表服务实现类
 */
@Service
public class DeptServiceImpl implements DeptService {

    @Autowired
    DeptMapper deptMapper;

    @Resource
    private PostMapper postMapper;

    /**
     * @param deptDto 对象信息
     * @return DeptVo
     *  创建部门表
     */
    @Transactional
    @Override
    public Boolean createDept(DeptDto deptDto) {
        //转换deptDto为Dept
        Dept dept = BeanUtil.toBean(deptDto, Dept.class);

        //根据传递过来的父部门编号创建当前部门编号
        String deptNo = createDeptNo(dept.getParentDeptNo());
        dept.setDeptNo(deptNo);

        //保存
        int flag = deptMapper.insert(dept);
        if (flag != 1) {
            throw new RuntimeException("保存部门信息出错");
        }

        //如果当前leader也是其他其他部门的负责人，则清空其他部门的leader数据
        //一个人只能是一个部门的leader
        if (ObjectUtil.isNotEmpty(deptDto.getLeaderId())) {

            //根据leader查询，如果存在，则清空
            deptMapper.clearOtherDeptLeader(deptDto.getLeaderId(), deptNo);
        }

        return true;
    }

    /**
     * @param deptDto 对象信息
     * @return Boolean
     *  修改部门表
     */
    @Transactional
    @Override
    public Boolean updateDept(DeptDto deptDto) {
        //转换DeptVo为Dept
        Dept dept = BeanUtil.toBean(deptDto, Dept.class);

        //TODO 检验  部门存在用户,不允许禁用

        //修改
        int flag = deptMapper.updateByPrimaryKey(dept);
        if (flag == 0) {
            throw new RuntimeException("修改部门信息出错");
        }

        //如果当前leader也是其他其他部门的负责人，则清空其他部门的leader数据
        //一个人只能是一个部门的leader
        if (ObjectUtil.isNotEmpty(deptDto.getLeaderId())) {
            //根据leader查询，如果存在，则清空
            deptMapper.clearOtherDeptLeader(deptDto.getLeaderId(), deptDto.getDeptNo());
        }

        return true;
    }

    /**
     * @param deptDto 查询条件
     *  多条件查询部门表列表
     * @return: List<DeptVo>
     */
    @Override
    public List<DeptVo> findDeptList(DeptDto deptDto) {
        List<Dept> deptList = deptMapper.selectList(deptDto);
        List<DeptVo> deptVos = BeanUtil.copyToList(deptList, DeptVo.class);
        deptVos.forEach(v -> v.setCreateDay(LocalDateTimeUtil.format(v.getCreateTime(), "yyyy-MM-dd")));
        return deptVos;
    }


    @Override
    public List<DeptVo> findDeptInDeptNos(List<String> deptNos) {
        List<Dept> depts = deptMapper.findDeptInDeptNos(deptNos);
        return BeanUtil.copyToList(depts, DeptVo.class);
    }


    @Override
    public String createDeptNo(String parentDeptNo) {
        if (NoProcessing.processString(parentDeptNo).length() / 3 == 5) {
            throw new BaseException(BasicEnum.DEPT_DEPTH_UPPER_LIMIT);
        }
        DeptDto deptDto = DeptDto.builder().parentDeptNo(parentDeptNo).build();
        List<Dept> deptList = deptMapper.selectList(deptDto);
        //无下属节点则创建下属节点
        if (EmptyUtil.isNullOrEmpty(deptList)) {
            return NoProcessing.createNo(parentDeptNo, false);
            //有下属节点则累加下属节点
        } else {
            Long deptNo = deptList.stream()
                    .map(dept -> {
                        return Long.valueOf(dept.getDeptNo());
                    })
                    .max(Comparator.comparing(i -> i)).get();
            return NoProcessing.createNo(String.valueOf(deptNo), true);
        }
    }

    @Override
    public List<DeptVo> findDeptVoListInRoleId(List<Long> roleIdSet) {
        return deptMapper.findDeptVoListInRoleId(roleIdSet);
    }

    @Transactional
    @Override
    public int deleteDeptById(String deptId) {
        if (hasChildByDeptId(deptId)) {
            throw new RuntimeException("存在下级部门,不允许删除");
        }
        //TODO  部门存在用户,不允许删除

        postMapper.deletePostByDeptNo(deptId);
        return deptMapper.deleteByDeptNo(deptId);
    }

    /**
     * 启用-禁用部门
     *
     * @param deptDto
     * @return
     */
    @Override
    public Boolean isEnable(DeptDto deptDto) {

        //1.启用部门
        if(deptDto.getDataState().equals("0")){
            //1.1 判断父级菜单是否是禁用，如果是禁用，不允许启用
            String parentDeptNo = deptDto.getParentDeptNo();
            Dept deptParent = deptMapper.selectByDeptNo(parentDeptNo);
            if(deptParent != null && deptParent.getDataState().equals("1")){
                throw new BaseException(BasicEnum.PARENT_DEPT_DISABLE);
            }
        }

        //TODO 如果部门下有用户，则不允许禁用

        //判断是否有子菜单，如果有子菜单，则一起启用或禁用
        String deptNo = deptDto.getDeptNo();
        deptMapper.updateByDeptNo(deptNo,deptDto.getDataState());
        deptMapper.updateByParentDeptNo(NoProcessing.processString(deptNo),deptDto.getDataState());
        return true;
    }

    /**
     * 是否存在子节点
     *
     * @param deptId 部门ID
     * @return 结果
     */
    public boolean hasChildByDeptId(String deptId) {
        int result = deptMapper.hasChildByDeptId(deptId);
        return result > 0 ? true : false;
    }

    /**
     * 组织部门树形
     * @return: deptDto
     */
    @Override
    public TreeVo deptTreeVo(DeptDto dto) {
        //根节点查询树形结构
        String parentDeptNo = SuperConstant.ROOT_DEPT_PARENT_ID;
        //指定节点查询树形结构
        DeptDto deptDto = new DeptDto();
        if(StringUtils.isEmpty(dto.getDataState()) || dto.getDataState().equals("0")){
            deptDto.setDataState("0");
        }
        if(StringUtils.isNotEmpty(dto.getDataState()) && !dto.getDataState().equals("0")){
            deptDto.setDataState(null);
        }
        deptDto.setParentDeptNo(NoProcessing.processString(parentDeptNo));
        //查询部门列表数据
        List<Dept> deptList =  deptMapper.selectList(deptDto);

        if (EmptyUtil.isNullOrEmpty(deptList)) {
            throw new RuntimeException("部门数据没有定义！");
        }
        //返回的部门列表
        List<TreeItemVo> treeItemVoList = new ArrayList<>();
        //找根节点
        Dept rootDept = deptList.stream()
                .filter(d -> SuperConstant.ROOT_DEPT_PARENT_ID.equals(d.getParentDeptNo()))
                .collect(Collectors.toList()).get(0);
        //递归调用
        recursionTreeItem(treeItemVoList, rootDept, deptList);
        //返回
        return TreeVo.builder()
                .items(treeItemVoList)
                .build();
    }

    /**
     * 构建树形结构，递归调用
     * @param treeItemVoList   封装返回的对象
     * @param deptRoot  当前部门
     * @param deptList  部门列表（全部数据）
     */
    private void recursionTreeItem(List<TreeItemVo> treeItemVoList, Dept deptRoot, List<Dept> deptList) {
        //构建item对象
        TreeItemVo treeItem = TreeItemVo.builder()
                .id(deptRoot.getDeptNo())
                .label(deptRoot.getDeptName())
                .build();
        //获得当前部门下子部门
        List<Dept> childrenDept = deptList.stream()
                .filter(n -> n.getParentDeptNo().equals(deptRoot.getDeptNo()))
                .collect(Collectors.toList());
        //如果子部门不为空，则继续递归查询
        if (!EmptyUtil.isNullOrEmpty(childrenDept)) {
            //子部门列表
            List<TreeItemVo> listChildren = Lists.newArrayList();
            //继续找子部门，找不到为止
            childrenDept.forEach(dept -> {
                this.recursionTreeItem(listChildren, dept, deptList);
            });
            treeItem.setChildren(listChildren);
        }
        treeItemVoList.add(treeItem);
    }

    @Override
    public boolean isLowestDept(String dept) {
        int count = deptMapper.isLowestDept(dept);
        return count > 0 ? true : false;
    }
}
