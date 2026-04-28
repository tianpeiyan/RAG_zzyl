package com.zzyl.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zzyl.base.PageResponse;
import com.zzyl.constant.SuperConstant;
import com.zzyl.dto.DeptDto;
import com.zzyl.dto.PostDto;
import com.zzyl.entity.Dept;
import com.zzyl.entity.Post;
import com.zzyl.enums.BasicEnum;
import com.zzyl.exception.BaseException;
import com.zzyl.mapper.DeptMapper;
import com.zzyl.mapper.PostMapper;
import com.zzyl.service.DeptService;
import com.zzyl.service.PostService;
import com.zzyl.utils.EmptyUtil;
import com.zzyl.utils.NoProcessing;
import com.zzyl.vo.DeptVo;
import com.zzyl.vo.PostVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 岗位表服务实现类
 */
@Service
public class PostServiceImpl implements PostService {

    @Autowired
    PostMapper postMapper;

    @Autowired
    DeptService deptService;

    @Autowired
    DeptMapper deptMapper;

    /**
     * 多条件查询岗位表分页列表
     *
     * @param postDto  查询条件
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return Page<PostVo>
     */
    @Override
    public PageResponse<PostVo> findPostPage(PostDto postDto, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        if (EmptyUtil.isNullOrEmpty(postDto.getDeptNo())) {
            throw new BaseException(BasicEnum.DEPT_NULL_EXCEPTION);
        }

        //处理部门编号
        postDto.setDeptNo(NoProcessing.processString(postDto.getDeptNo()));

        Page<List<Post>> page = postMapper.selectPage(postDto);
        PageResponse<PostVo> pageResponse = PageResponse.of(page, PostVo.class);
        if (!EmptyUtil.isNullOrEmpty(pageResponse.getRecords())) {
            //对应部门
            List<String> deptNos = pageResponse.getRecords().stream().map(PostVo::getDeptNo).collect(Collectors.toList());
            List<DeptVo> deptVoList = deptService.findDeptInDeptNos(deptNos);
            pageResponse.getRecords().forEach(n -> {
                n.setCreateDay(LocalDateTimeUtil.format(n.getCreateTime(), "yyyy-MM-dd"));
                //装配部门
                deptVoList.forEach(d -> {
                    if (n.getDeptNo().equals(d.getDeptNo())) {
                        n.setDeptVo(BeanUtil.toBean(d, DeptVo.class));
                    }
                });
            });
        }
        return pageResponse;
    }


    /**
     * 创建岗位表
     *
     * @param postDto 对象信息
     * @return PostDto
     */
    @Override
    public PostVo createPost(PostDto postDto) {
        //转换PostVo为Post

        //判断是否是最底层部门，如果不是，则提示：请选择最底层部门
        String deptNo = postDto.getDeptNo();
        if (deptService.isLowestDept(deptNo)) {
            throw new RuntimeException("请选择最底层部门");
        }

        Post post = BeanUtil.toBean(postDto, Post.class);
        String postNo = createPostNo(post.getDeptNo());
        post.setPostNo(postNo);
        int flag = postMapper.insert(post);
        if (flag == 0) {
            throw new RuntimeException("保存职位信息出错");
        }
        PostVo postVoResult = BeanUtil.toBean(post, PostVo.class);
        //装配部门
        DeptDto deptDto = DeptDto.builder()
                .dataState(SuperConstant.DATA_STATE_0)
                .parentDeptNo(postDto.getDeptNo()).build();
        List<Dept> deptList = deptMapper.selectList(deptDto);
        if (!EmptyUtil.isNullOrEmpty(deptList)) {
            postVoResult.setDeptVo(BeanUtil.toBean(deptList.get(0), DeptVo.class));
        }
        return postVoResult;
    }

    /**
     * 修改岗位表
     *
     * @param postDto 对象信息
     * @return Boolean
     */
    @Override
    public Boolean updatePost(PostDto postDto) {

        //判断是否是最底层部门，如果不是，则提示：请选择最底层部门
        String deptNo = postDto.getDeptNo();
        if (deptService.isLowestDept(deptNo)) {
            throw new RuntimeException("请选择最底层部门");
        }

        //转换PostVo为Post
        Post post = BeanUtil.toBean(postDto, Post.class);
        int flag = postMapper.updateByPrimaryKey(post);
        if (flag == 0) {
            throw new RuntimeException("修改职位信息出错");
        }
        return true;
    }

    /**
     * 职位启用和禁用
     *
     * @param postDto
     * @return
     */
    @Override
    public void isEnable(PostDto postDto) {

        //TODO 如果是禁用，则需要判断是否已经被用户引用

        Post post = BeanUtil.toBean(postDto, Post.class);
        postMapper.updateByPrimaryKeySelective(post);
    }

    @Override
    public List<PostVo> findPostList(PostDto postDto) {
        if (!EmptyUtil.isNullOrEmpty(postDto.getDeptNo())) {
            postDto.setDeptNo(NoProcessing.processString(postDto.getDeptNo()));
        }
        postDto.setDataState("0");
        List<Post> postList = postMapper.selectList(postDto);
        if (EmptyUtil.isNullOrEmpty(postList)) {
            return new ArrayList<>();
        }
        List<PostVo> postVoList = BeanUtil.copyToList(postList, PostVo.class);
        return postVoList;
    }

    @Override
    public List<PostVo> findPostVoListByUserId(Long userId) {
        return postMapper.findPostVoListByUserId(userId);
    }

    /**
     * 创建编号
     *
     * @param deptNo
     * @return
     */
    private String createPostNo(String deptNo) {
        PostDto postDto = PostDto.builder().deptNo(deptNo).build();
        List<Post> postList = postMapper.selectList(postDto);
        //无下属节点则创建下属节点
        if (EmptyUtil.isNullOrEmpty(postList)) {
            return NoProcessing.createNo(deptNo, false);
            //有下属节点则累加下属节点
        } else {
            Long postNo = postList.stream()
                    .map(post -> {
                        return Long.valueOf(post.getPostNo());
                    })
                    .max(Comparator.comparing(i -> i)).get();
            return NoProcessing.createNo(String.valueOf(postNo), true);
        }
    }

    @Override
    public int deletePostById(String postIds) {
        //TODO 验证当前岗位是否被用户关联
        return postMapper.deletePostById(postIds);
    }
}
