
package com.zzyl.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zzyl.base.PageResponse;
import com.zzyl.entity.CheckInConfig;
import com.zzyl.entity.Contract;
import com.zzyl.entity.Elder;
import com.zzyl.entity.NursingElder;
import com.zzyl.dto.ElderDto;
import com.zzyl.dto.NursingElderDto;
import com.zzyl.mapper.CheckInConfigMapper;
import com.zzyl.mapper.ContractMapper;
import com.zzyl.mapper.ElderMapper;
import com.zzyl.mapper.NursingElderMapper;
import com.zzyl.service.ElderService;
import com.zzyl.utils.ObjectUtil;
import com.zzyl.utils.UserThreadLocal;
import com.zzyl.vo.ElderVo;
import com.zzyl.vo.RetreatVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ElderServiceImpl是ElderService的实现类
 */
@Service
public class ElderServiceImpl implements ElderService {

    @Autowired
    private ElderMapper elderMapper;

    @Autowired
    private ContractMapper contractMapper;

    @Autowired
    private CheckInConfigMapper checkInConfigMapper;

    @Autowired
    private NursingElderMapper nursingElderMapper;

    /**
     * 根据id删除老人信息
     */
    @Override
    public int deleteByPrimaryKey(Long id) {
        return elderMapper.deleteByPrimaryKey(id);
    }
        /**
         * 分页查询老人
         */
        @Override
    public PageResponse<ElderVo> pageQuery(Integer pageNum, Integer pageSize, String name, String idCardNo, Integer status) {
        PageHelper.startPage(pageNum, pageSize);
        Page<Elder> page = elderMapper.selectByPage(name, idCardNo, status);
        List<ElderVo> elderVos = new ArrayList<>();
        if (page != null && !page.isEmpty()) {
            for (Elder elder : page) {
                ElderVo elderVo = new ElderVo();
                BeanUtil.copyProperties(elder, elderVo);

                // Bed Name
                elderVo.setBedName(elder.getBedNumber());

                // Contract (Fee Term)
                Contract contract = contractMapper.selectByElderId(elder.getId());
                if (contract != null) {
                    elderVo.setCostStartTime(contract.getStartTime());
                    elderVo.setCostEndTime(contract.getEndTime());

                    // CheckIn Config (CheckIn Term & Nursing Level)
                    if (contract.getCheckInNo() != null) {
                        CheckInConfig config = checkInConfigMapper.selectByCheckInCode(contract.getCheckInNo());
                        if (config != null) {
                            elderVo.setCheckInStartTime(config.getCheckInStartTime());
                            elderVo.setCheckInEndTime(config.getCheckInEndTime());
                            elderVo.setNursingLevelName(config.getNursingLevelName());
                        }
                    }
                }

                // Nursing Assistant - Tentatively empty
                elderVo.setNursingAssistantName("");

                elderVos.add(elderVo);
            }
        }
        return PageResponse.<ElderVo>builder()
                .page(page.getPageNum())
                .pageSize(page.getPageSize())
                .total(page.getTotal())
                .pages((long) page.getPages())
                .records(elderVos)
                .build();
    }

    /**
     * 插入老人信息
     * @return
     */
    @Override
    public Elder insert(ElderDto elderDto) {
        Elder elder = BeanUtil.toBean(elderDto, Elder.class);
        elder.setStatus(4);
        elder.setRemark("0");
        ElderVo elderVo = selectByIdCardAndName(elderDto.getIdCardNo(), elderDto.getName());
        if (ObjectUtil.isNotEmpty(elderVo)) {
            int i = Integer.parseInt(elderVo.getRemark()) + 1;
            elder.setName(elder.getName() + i);
            elderVo.setRemark(i + "");
            Elder elder1 = BeanUtil.toBean(elderVo, Elder.class);
            elderMapper.updateByPrimaryKeySelective(elder1);
        }
        elderMapper.insert(elder);
        return elder;
    }

    /**
     * 选择性插入老人信息
     */
    @Override
    public int insertSelective(ElderDto elderDto) {
        Elder elder = BeanUtil.toBean(elderDto, Elder.class);
        return elderMapper.insertSelective(elder);
    }

    /**
     * 根据id选择老人信息
     */
    @Override
    public ElderVo selectByPrimaryKey(Long id) {
        Elder elder = elderMapper.selectByPrimaryKey(id);
        ElderVo elderVo = BeanUtil.toBean(elder, ElderVo.class);
        if (elderVo != null && elder != null) {
            elderVo.setBedName(elder.getBedNumber());
        }
        return elderVo;
    }

    /**
     * 选择性更新老人信息
     */
    @Override
    public Elder updateByPrimaryKeySelective(ElderDto elderDto, boolean b) {
        Elder elder = BeanUtil.toBean(elderDto, Elder.class);
        if (b) {
            elder.setRemark("0");
            ElderVo elderVo = selectByIdCardAndName(elderDto.getIdCardNo(), elderDto.getName());
            if (ObjectUtil.isNotEmpty(elderVo)) {
                int i = Integer.parseInt(elderVo.getRemark()) + 1;
                elder.setName(elder.getName() + i);
                elderVo.setRemark(i + "");
                Elder elder1 = BeanUtil.toBean(elderVo, Elder.class);
                elderMapper.updateByPrimaryKeySelective(elder1);
                return elder1;
            }
        }
        elderMapper.updateByPrimaryKeySelective(elder);
        return elder;
    }

    /**
     * 更新老人信息
     */
    @Override
    public int updateByPrimaryKey(ElderDto elderDto) {
        Elder elder = BeanUtil.toBean(elderDto, Elder.class);
        return elderMapper.updateByPrimaryKey(elder);
    }


    /**
     * 根据身份证号和姓名查询老人信息
     */
    @Override
    public ElderVo selectByIdCardAndName(String idCard, String name) {
        Elder elder = elderMapper.selectByIdCardAndName(idCard, name);
        return BeanUtil.toBean(elder, ElderVo.class);
    }


    /**
     * 根据身份证号和姓名查询老人信息
     */
    @Override
    public List<ElderVo> selectList() {
        List<Elder> elder = elderMapper.selectList();
        return BeanUtil.copyToList(elder, ElderVo.class);
    }

    @Override
    public List<Elder> selectByIds(List<Long> ids) {
        return elderMapper.selectByIds(ids);
    }


    @Override
    public ElderVo selectByIdCard(String idCard) {
        Elder elder = elderMapper.selectByIdCard(idCard);
        return BeanUtil.toBean(elder, ElderVo.class);
    }

    /**
     * 清除老人床位编号
     * @param elderId
     */
    @Override
    public void clearBedNum(Long elderId) {
        elderMapper.clearBedNum(elderId);
    }

    @Override
    public RetreatVo getCheckInInfo(Long elderId) {
        Elder elder = elderMapper.selectByPrimaryKey(elderId);
        if (elder == null) {
            throw new RuntimeException("老人不存在");
        }
        RetreatVo vo = new RetreatVo();
        BeanUtil.copyProperties(elder, vo);
        vo.setBedNo(elder.getBedNumber());
        vo.setBedNumber(elder.getBedNumber());

        Contract contract = contractMapper.selectByElderId(elderId);
        if (contract != null) {
            if (contract.getCheckInNo() != null) {
                CheckInConfig config = checkInConfigMapper.selectByCheckInCode(contract.getCheckInNo());
                if (config != null) {
                    vo.setCheckInStartTime(config.getCheckInStartTime());
                    vo.setCheckInEndTime(config.getCheckInEndTime());
                    vo.setCostStartTime(config.getCostStartTime());
                    vo.setCostEndTime(config.getCostEndTime());
                    vo.setNursingLevelName(config.getNursingLevelName());
                    vo.setNursingName("无");
                }
            }
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setNursing(List<NursingElderDto> nursingElderDtos) {
        if (ObjectUtil.isEmpty(nursingElderDtos)) {
            return;
        }
        Long userId = UserThreadLocal.getMgtUserId();
        LocalDateTime now = LocalDateTime.now();
        Map<Long, List<Long>> elderMap = new HashMap<>();
        for (NursingElderDto dto : nursingElderDtos) {
            if (dto == null || dto.getElderId() == null) {
                continue;
            }
            elderMap.put(dto.getElderId(), dto.getNursingIds());
        }
        List<NursingElder> insertList = new ArrayList<>();
        for (Map.Entry<Long, List<Long>> entry : elderMap.entrySet()) {
            Long elderId = entry.getKey();
            nursingElderMapper.deleteByElderId(elderId);
            List<Long> nursingIds = entry.getValue();
            if (ObjectUtil.isEmpty(nursingIds)) {
                continue;
            }
            for (Long nursingId : nursingIds) {
                if (nursingId == null) {
                    continue;
                }
                NursingElder nursingElder = new NursingElder();
                nursingElder.setElderId(elderId);
                nursingElder.setNursingId(nursingId);
                nursingElder.setCreateTime(now);
                nursingElder.setUpdateTime(now);
                nursingElder.setCreateBy(userId);
                nursingElder.setUpdateBy(userId);
                insertList.add(nursingElder);
            }
        }
        if (!insertList.isEmpty()) {
            nursingElderMapper.insertBatch(insertList);
        }
    }
}


