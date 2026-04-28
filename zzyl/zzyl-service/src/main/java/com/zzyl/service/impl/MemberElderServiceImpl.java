package com.zzyl.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.zzyl.dto.MemberElderDto;
import com.zzyl.entity.Elder;
import com.zzyl.entity.MemberElder;
import com.zzyl.mapper.MemberElderMapper;
import com.zzyl.service.BedService;
import com.zzyl.service.ElderService;
import com.zzyl.service.MemberElderService;
import com.zzyl.service.RoomService;
import com.zzyl.utils.UserThreadLocal;
import com.zzyl.vo.BedVo;
import com.zzyl.vo.ElderVo;
import com.zzyl.vo.MemberElderVo;
import com.zzyl.vo.RoomVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 客户老人关联服务实现类
 * 处理小程序会员与养老院老人的绑定、查询及解绑逻辑
 */
@Service
public class MemberElderServiceImpl implements MemberElderService {

    @Autowired
    private MemberElderMapper memberElderMapper;

    @Autowired
    private ElderService elderService;

    @Autowired
    private BedService bedService;

    @Autowired
    private RoomService roomService;

    /**
     * 绑定老人
     * 逻辑：
     * 1. 根据姓名和身份证号验证老人是否在系统（养老院）中存在
     * 2. 检查当前会员是否已经绑定过该老人，避免重复绑定
     * 3. 创建绑定关系记录
     *
     * @param memberElderDto 包含老人姓名、身份证号及称呼（备注）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(MemberElderDto memberElderDto) {
        // 1. 校验老人是否存在：通过姓名和身份证号精确匹配
        ElderVo elderVo = elderService.selectByIdCardAndName(memberElderDto.getIdCard(), memberElderDto.getName());
        if (elderVo == null) {
            throw new RuntimeException("老人不存在，请检查姓名和身份证号是否输入正确");
        }

        // 2. 查重校验：获取当前登录会员ID，检查是否已存在绑定记录
        Long memberId = UserThreadLocal.getUserId();
        List<MemberElder> existList = memberElderMapper.selectByMemberIdAndElderId(memberId, elderVo.getId());
        if (CollUtil.isNotEmpty(existList)) {
            throw new RuntimeException("您已经绑定过该老人，请勿重复操作");
        }

        // 3. 执行绑定：创建关联实体并保存
        MemberElder memberElder = new MemberElder();
        memberElder.setMemberId(memberId);
        memberElder.setElderId(elderVo.getId());
        // 设置称呼（如：父亲、母亲等），对应前端输入的“对家人的称呼”
        memberElder.setRemark(memberElderDto.getRemark()); 
        memberElder.setCreateTime(LocalDateTime.now());
        memberElder.setUpdateTime(LocalDateTime.now());
        memberElder.setCreateBy(memberId);
        memberElder.setUpdateBy(memberId);

        memberElderMapper.insert(memberElder);
    }

    /**
     * 查询当前会员绑定的老人列表
     * 逻辑：
     * 1. 获取当前会员所有的绑定记录
     * 2. 批量获取对应老人的详细信息（姓名、床位、头像等）
     * 3. 组装成前端展示所需的 VO 对象
     *
     * @return 绑定老人信息列表
     */
    @Override
    public List<MemberElderVo> list() {
        // 1. 获取当前登录会员ID并查询所有绑定记录
        Long memberId = UserThreadLocal.getUserId();
        List<MemberElder> memberElders = memberElderMapper.selectByMemberId(memberId);
        if (CollUtil.isEmpty(memberElders)) {
            return new ArrayList<>();
        }

        // 2. 批量查询老人详细信息
        List<Long> elderIds = memberElders.stream().map(MemberElder::getElderId).collect(Collectors.toList());
        List<Elder> elders = elderService.selectByIds(elderIds);
        Map<Long, Elder> elderMap = elders.stream().collect(Collectors.toMap(Elder::getId, Function.identity()));

        // 3. 组装 VO 列表
        List<MemberElderVo> result = new ArrayList<>();
        for (MemberElder memberElder : memberElders) {
            Elder elder = elderMap.get(memberElder.getElderId());
            if (elder != null) {
                MemberElderVo vo = new MemberElderVo();
                vo.setId(memberElder.getId()); // 绑定记录ID
                vo.setElderId(elder.getId());
                vo.setRemark(memberElder.getRemark()); // 关系/称呼

                // 嵌套老人信息
                ElderVo elderVo = new ElderVo();
                BeanUtils.copyProperties(elder, elderVo);
                vo.setElderVo(elderVo);

                // 嵌套床位和房间信息
                if (elder.getBedId() != null) {
                    BedVo bedVo = bedService.getById(elder.getBedId());
                    if (bedVo != null) {
                        vo.setBedVo(bedVo);
                        if (bedVo.getRoomId() != null) {
                            RoomVo roomVo = roomService.getRoom(bedVo.getRoomId());
                            vo.setRoomVo(roomVo);
                        }
                    }
                }
                
                // 设备信息暂设为空列表（若后续有相关业务再补充）
                vo.setDeviceVos(new ArrayList<>());
                
                result.add(vo);
            }
        }
        return result;
    }

    /**
     * 解除绑定
     * @param id 绑定记录的 ID
     */
    @Override
    public void deleteById(Long id) {
        memberElderMapper.deleteById(id);
    }
}
