package com.zzyl.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zzyl.base.PageResponse;
import com.zzyl.dto.RetreatApplyDto;
import com.zzyl.entity.CheckInConfig;
import com.zzyl.entity.Contract;
import com.zzyl.entity.Bed;
import com.zzyl.entity.Elder;
import com.zzyl.entity.Retreat;
import com.zzyl.enums.BasicEnum;
import com.zzyl.exception.BaseException;
import com.zzyl.mapper.BedMapper;
import com.zzyl.mapper.CheckInConfigMapper;
import com.zzyl.mapper.ContractMapper;
import com.zzyl.mapper.ElderMapper;
import com.zzyl.mapper.NursingElderMapper;
import com.zzyl.mapper.RetreatMapper;
import com.zzyl.service.RetreatService;
import com.zzyl.utils.UserThreadLocal;
import com.zzyl.vo.RetreatVo;
import com.zzyl.vo.UserVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RetreatServiceImpl implements RetreatService {

    @Resource
    private RetreatMapper retreatMapper;

    @Resource
    private ElderMapper elderMapper;

    @Resource
    private CheckInConfigMapper checkInConfigMapper;

    @Resource
    private ContractMapper contractMapper;

    @Resource
    private BedMapper bedMapper;

    @Resource
    private NursingElderMapper nursingElderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void apply(RetreatApplyDto retreatApplyDto) {
        Long elderId = retreatApplyDto.getElderId();
        Elder elder = elderMapper.selectByPrimaryKey(elderId);
        if (elder == null) {
            throw new BaseException(BasicEnum.ELDER_NOT_EXIST);
        }

        Long bedId = elder.getBedId();
        
        if (elder.getStatus() != null && elder.getStatus() == 5) {
            throw new BaseException(BasicEnum.ELDER_ALREADY_RETREATED);
        }
        if (elder.getStatus() != null && elder.getStatus() == 3) {
            throw new BaseException(BasicEnum.ELDER_RETREATING);
        }

        Retreat retreat = new Retreat();
        // 1. 生成退住编码 TZ + Snowflake
        String retreatCode = "TZ" + IdUtil.getSnowflakeNextIdStr();
        retreat.setRetreatCode(retreatCode);
        retreat.setTitle(elder.getName() + "的退住申请");
        retreat.setElderId(elderId);
        retreat.setName(elder.getName());
        retreat.setIdCardNo(elder.getIdCardNo());
        retreat.setPhone(elder.getPhone());
        retreat.setAddress(elder.getAddress());
        retreat.setCheckOutTime(retreatApplyDto.getCheckOutTime());
        retreat.setReason(retreatApplyDto.getReason());
        retreat.setRemark(retreatApplyDto.getRemark());
        retreat.setCreateTime(LocalDateTime.now());

        // 2. 填充入住相关信息 (Snapshot)
        Contract contract = contractMapper.selectByElderId(elderId);
        if (contract != null) {
            retreat.setContractName(contract.getName());
            retreat.setContractUrl(contract.getPdfUrl());
            retreat.setContractNo(contract.getContractNo());
            
            if (contract.getCheckInNo() != null) {
                CheckInConfig config = checkInConfigMapper.selectByCheckInCode(contract.getCheckInNo());
                if (config != null) {
                    retreat.setCheckInStartTime(config.getCheckInStartTime());
                    retreat.setCheckInEndTime(config.getCheckInEndTime());
                    retreat.setCostStartTime(config.getCostStartTime());
                    retreat.setCostEndTime(config.getCostEndTime());
                    retreat.setNursingLevelName(config.getNursingLevelName());
                    retreat.setBedNumber(config.getBedNumber());
                    retreat.setNursingName("无"); // 暂无字段
                }
            }
        }
        
        // 3. 设置申请人
        String subject = UserThreadLocal.getSubject();
        Long operatorId = 1L;
        if (ObjectUtil.isNotEmpty(subject)) {
            UserVo userVo = JSONObject.parseObject(subject, UserVo.class);
            String applicat = userVo.getRealName();
            if (ObjectUtil.isEmpty(applicat)) {
                applicat = userVo.getNickName();
            }
            if (ObjectUtil.isEmpty(applicat)) {
                applicat = userVo.getCreator();
            }
            retreat.setApplicat(ObjectUtil.isNotEmpty(applicat) ? applicat : "系统管理员");
            retreat.setApplicatId(userVo.getId());
            retreat.setDeptNo(userVo.getDeptNo());
            retreat.setCreateBy(userVo.getId());
            retreat.setUpdateBy(userVo.getId());
            operatorId = userVo.getId();
        } else {
             retreat.setApplicat("系统管理员");
             retreat.setApplicatId(1L);
             retreat.setCreateBy(1L);
             retreat.setUpdateBy(1L);
        }

        retreatMapper.insert(retreat);

      /* // 4. 更新老人状态为退住中
        elder.setStatus(3);*/


        //4. 更新老人状态为已退住
        elder.setStatus(5);
        elder.setUpdateBy(operatorId);
        elder.setUpdateTime(LocalDateTime.now());
        elderMapper.updateByPrimaryKeySelective(elder);

        nursingElderMapper.deleteByElderId(elderId);
        elderMapper.clearBedNum(elderId);
        if (bedId != null) {
            Bed bed = bedMapper.getById(bedId);
            if (bed != null) {
                bed.setBedStatus(0);
                bed.setUpdateBy(operatorId);
                bed.setUpdateTime(LocalDateTime.now());
                bedMapper.updateBed(bed);
            }
        }
    }

    @Override
    public PageResponse<RetreatVo> pageQuery(Integer pageNum, Integer pageSize, String name, String idCard, String startTime, String endTime) {
        PageHelper.startPage(pageNum, pageSize);
        List<RetreatVo> list = retreatMapper.selectList(name, idCard, startTime, endTime);
        Page<RetreatVo> page = (Page<RetreatVo>) list;
        return PageResponse.of(page, RetreatVo.class);
    }

    @Override
    public RetreatVo detail(Long id) {
        Retreat retreat = retreatMapper.selectByPrimaryKey(id);
        if (retreat == null) {
            return null;
        }
        RetreatVo vo = new RetreatVo();
        BeanUtils.copyProperties(retreat, vo);
        return vo;
    }
}
