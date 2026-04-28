package com.zzyl.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.zzyl.constant.CacheConstants;
import com.zzyl.constant.SuperConstant;
import com.zzyl.dto.ResourceDto;
import com.zzyl.entity.Resource;
import com.zzyl.enums.BasicEnum;
import com.zzyl.exception.BaseException;
import com.zzyl.mapper.ResourceMapper;
import com.zzyl.properties.SecurityConfigProperties;
import com.zzyl.service.ResourceService;
import com.zzyl.utils.EmptyUtil;
import com.zzyl.utils.NoProcessing;
import com.zzyl.utils.StringUtils;
import com.zzyl.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ResourceServiceImpl implements ResourceService {

    @Autowired
    private ResourceMapper resourceMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private SecurityConfigProperties securityConfigProperties;

    /**
     * 多条件查询资源列表
     * @param resourceDto
     * @return
     */
    @Override
    public List<ResourceVo> findResourceList(ResourceDto resourceDto) {
        List<Resource> resourceList = resourceMapper.selectList(resourceDto);
        return BeanUtil.copyToList(resourceList, ResourceVo.class);
    }

    /**
     * 树形结构
     * @param resourceDto
     * @return
     */
    @Override
    public TreeVo resourceTreeVo(ResourceDto resourceDto) {
        //构造查询条件
        ResourceDto dto = ResourceDto.builder()
                .dataState(SuperConstant.DATA_STATE_0)
                .parentResourceNo(NoProcessing.processString(SuperConstant.ROOT_PARENT_ID))
                .resourceType(SuperConstant.MENU).build();
        //查询所有资源数据
        List<Resource> resourceList = resourceMapper.selectList(dto);
        if(EmptyUtil.isNullOrEmpty(resourceList)){
            throw new RuntimeException("资源信息未定义");
        }
        //没有根节点，构建根节点
        Resource rootResource = new Resource();
        rootResource.setResourceNo(SuperConstant.ROOT_PARENT_ID);
        rootResource.setResourceName("颐家亲养老院");

        //返回的树形集合
        List<TreeItemVo> itemVos = new ArrayList<>();

        //使用递归构建树形结构
        recursionTreeItem(itemVos,rootResource,resourceList);

        return TreeVo.builder().items(itemVos).build();
    }



    /**
     * 使用递归构建树形结构
     * @param itemVos
     * @param rootResource
     * @param resourceList
     */
    private void recursionTreeItem(List<TreeItemVo> itemVos, Resource rootResource, List<Resource> resourceList) {
        //构建每个资源的属性
        TreeItemVo treeItemVo = TreeItemVo.builder()
                .id(rootResource.getResourceNo())
                .label(rootResource.getResourceName()).build();
        //获取当前资源下的子资源
        List<Resource> childrenResourceList = resourceList.stream()
                .filter(n -> n.getParentResourceNo().equals(rootResource.getResourceNo()))
                .collect(Collectors.toList());
        //判断子资源是否为空
        if(!EmptyUtil.isNullOrEmpty(childrenResourceList)){
            List<TreeItemVo> listChildren = new ArrayList<>();
            //构建子资源
            childrenResourceList.forEach(resource -> {
                recursionTreeItem(listChildren,resource,resourceList);
            });
            treeItemVo.setChildren(listChildren);
        }

        //添加到集合
        itemVos.add(treeItemVo);

    }

    /**
     * 添加资源
     * @param resourceDto
     */
    @Override
    public void createResource(ResourceDto resourceDto) {
        //属性拷贝
        Resource resource = BeanUtil.toBean(resourceDto, Resource.class);
        //查询父资源
        Resource parentResource = resourceMapper.selectByResourceNo(resourceDto.getParentResourceNo());
        resource.setDataState(parentResource.getDataState());
        boolean isIgnore = true;
        //判断是否是按钮，如果是按钮，则不限制层级
        if(StringUtils.isNotEmpty(resourceDto.getResourceType())
                && resourceDto.getResourceType().equals(SuperConstant.BUTTON)){
            isIgnore = false;
        }
        //创建当前资源的编号
        String resourceNo = createResourceNo(resourceDto.getParentResourceNo(),isIgnore);
        resource.setResourceNo(resourceNo);
        resourceMapper.insert(resource);
        
        log.info("Created resource: {}, Name: {}", resourceNo, resource.getResourceName());
        clearMenuCache();
    }

    @Override
    public void updateResource(ResourceDto resourceDto) {
        Resource resource = BeanUtil.toBean(resourceDto, Resource.class);
        int flag = resourceMapper.updateByResourceNo(resource);
        if (flag == 0) {
            throw new RuntimeException("修改资源失败");
        }
        log.info("Updated resource: {}", resource.getResourceNo());
        clearMenuCache();
    }

    @Override
    public void deleteResource(String resourceNo) {
        int count = resourceMapper.countChildrenByParentResourceNo(resourceNo);
        if (count > 0) {
            throw new RuntimeException("该资源下存在子资源，不允许删除");
        }
        resourceMapper.deleteByResourceNo(resourceNo);
        log.info("Deleted resource: {}", resourceNo);
        clearMenuCache();
    }

    @Override
    public void enable(ResourceDto resourceDto) {
        Resource resource = new Resource();
        resource.setResourceNo(resourceDto.getResourceNo());
        resource.setDataState(resourceDto.getDataState());
        resourceMapper.updateByResourceNo(resource);
        log.info("Enabled/Disabled resource: {}, State: {}", resourceDto.getResourceNo(), resourceDto.getDataState());
        clearMenuCache();
    }



    /**
     * @return
     *  动态菜单（根据用户id，实现不同用户可访问资源不同）
     */
    @Override
    public List<MenuVo> menus(Long userId) {
        // 1. 尝试从缓存获取
        String key = CacheConstants.USER_MENU + userId;
        String menuJson = redisTemplate.opsForValue().get(key);
        if (StringUtils.isNotEmpty(menuJson)) {
            return JSONUtil.toList(menuJson, MenuVo.class);
        }

        // 2. 缓存未命中，查询数据库
        // 获取用户已分配的菜单
        List<MenuVo> menuVoList = resourceMapper.findListByUserId(userId);
        
        // 3. 获取白名单中的公共资源并合并
        List<MenuVo> allMenus = resourceMapper.findAllMenu();
        List<String> whitelist = securityConfigProperties.getPublicAccessUrls();
        
        // 过滤出白名单中的菜单资源
        List<MenuVo> publicMenus = allMenus.stream()
                .filter(menu -> StringUtils.isNotEmpty(menu.getPath()) && whitelist.stream().anyMatch(url -> {
                    // 白名单格式可能是 "METHOD/path", 资源路径可能是 "/path"
                    String path = menu.getPath();
                    return url.contains(path);
                }))
                .collect(Collectors.toList());

        // 合并并去重
        Set<MenuVo> mergedSet = new HashSet<>(menuVoList);
        mergedSet.addAll(publicMenus);
        List<MenuVo> finalMenuList = new ArrayList<>(mergedSet);

        if(CollUtil.isEmpty(finalMenuList)){
            throw new BaseException(BasicEnum.USER_ROLE_AND_MENU_EMPTY);
        }

        // 4. 构建树形结构
        // 数据进行分组
        Map<String, List<MenuVo>> parentRNoMap = finalMenuList
                .stream()
                .collect(Collectors.groupingBy(MenuVo::getParentResourceNo));
        
        // 遍历所有数据设置元数据和子节点
        finalMenuList.forEach(menuVo -> {
            menuVo.setMeta(MenuMetaVo.builder().title(menuVo.getName()).build());
            menuVo.setRedirect("/" + menuVo.getName());
            
            List<MenuVo> children = parentRNoMap.get(menuVo.getResourceNo());
            if(!EmptyUtil.isNullOrEmpty(children)){
                // 排序子菜单
                children.sort(Comparator.comparing(MenuVo::getSortNo, Comparator.nullsLast(Integer::compareTo)));
                // 确保子节点也设置了Meta信息
                children.forEach(m -> {
                    m.setMeta(MenuMetaVo.builder().title(m.getName()).build());
                    m.setRedirect("/" + m.getName());
                });
                menuVo.setChildren(children);
            }
        });
        
        // 获取根节点列表并排序
        List<MenuVo> roots = parentRNoMap.get(SuperConstant.ROOT_PARENT_ID);
        if (CollUtil.isNotEmpty(roots)) {
            roots.sort(Comparator.comparing(MenuVo::getSortNo, Comparator.nullsLast(Integer::compareTo)));
        }
        
        // 5. 存入缓存
        if (CollUtil.isNotEmpty(roots)) {
            redisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(roots), 1, TimeUnit.HOURS);
        }

        return roots;
    }


    /**
     * 创建资源编号
     * @param parentResourceNo
     * @param isIgnore
     * @return
     */
    private String createResourceNo(String parentResourceNo, boolean isIgnore) {
        //100 001 000 000 000
        //100 001 001 000 000
        //100 001 001 001 000
        //100 001 001 001 001 001
        //判断资源编号是否大于三级
        if(isIgnore && NoProcessing.processString(parentResourceNo).length() / 3 >= 5){
            throw new BaseException(BasicEnum.RESOURCE_DEPTH_UPPER_LIMIT);
        }
        //根据父资源编号查询子资源
        ResourceDto dto = ResourceDto.builder().parentResourceNo(parentResourceNo).build();
        List<Resource> resourceList = resourceMapper.selectList(dto);
        if(EmptyUtil.isNullOrEmpty(resourceList)){
            //无下属节点，创建新的节点编号
            //100 001 001 001 000 -->100 001 001 001 001
            return NoProcessing.createNo(parentResourceNo,false);
        }else{
            //有下属节点，在已有节点上追加
            //100 001 001 001 000 -->100 001 001 001 003 -->100 001 001 001 004
            //先获取已有节点的最大值
            Long maxNo = resourceList.stream()
                    .map(resource -> {
                        return Long.valueOf(resource.getResourceNo());
                    }).max(Comparator.comparing(i -> i)).get();
            return NoProcessing.createNo(String.valueOf(maxNo),true);
        }

    }
    
    private void clearMenuCache() {
        // 简单实现：清除所有用户菜单缓存
        // 在实际生产中可能需要更精细的控制，例如只清除受影响用户的缓存，
        // 但由于资源变更频率低，且这里无法直接知道哪些用户受影响，全量清除是安全的策略。
        try {
            java.util.Set<String> keys = redisTemplate.keys(CacheConstants.USER_MENU + "*");
            if (CollUtil.isNotEmpty(keys)) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.error("Failed to clear menu cache", e);
        }
    }

}
