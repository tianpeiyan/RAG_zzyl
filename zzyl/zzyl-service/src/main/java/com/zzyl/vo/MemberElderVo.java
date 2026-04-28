package com.zzyl.vo;

import com.zzyl.base.BaseVo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("客户老人关联Vo")
public class MemberElderVo extends BaseVo {

    @ApiModelProperty("老人ID")
    private Long elderId;

    @ApiModelProperty("备注（关系）")
    private String remark;

    @ApiModelProperty("老人信息")
    private ElderVo elderVo;

    @ApiModelProperty("房间信息")
    private RoomVo roomVo;

    @ApiModelProperty("床位信息")
    private BedVo bedVo;

    @ApiModelProperty("设备信息")
    private List<?> deviceVos;
}
