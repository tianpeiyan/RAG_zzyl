package com.zzyl.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 动态菜单VO对象
 */
@Data
@ApiModel("菜单VO")
public class MenuVo implements Serializable{

	// 设定路由的名字，一定要填写不然使用<keep-alive>时会出现各种问题
	@ApiModelProperty(value = "路由名字")
	private  String name;

	@ApiModelProperty(value = "资源编号")
	private String resourceNo;

	@ApiModelProperty(value = "父资源编号")
	private String parentResourceNo;

	@ApiModelProperty(value = "请求路径")
	private String path;

	@ApiModelProperty(value = "排序")
	private Integer sortNo;

	@ApiModelProperty(value = "层级名称展示")
	private String redirect;

	@ApiModelProperty(value = "子菜单")
	private List<MenuVo> children = new ArrayList<>();

	@ApiModelProperty(value = "meta属性")
	private MenuMetaVo meta;

}