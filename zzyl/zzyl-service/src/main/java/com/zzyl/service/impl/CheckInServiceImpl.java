package com.zzyl.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zzyl.base.PageResponse;
import com.zzyl.dto.CheckInConfigDto;
import com.zzyl.dto.CheckInContractDto;
import com.zzyl.dto.CheckInDto;
import com.zzyl.dto.CheckInElderDto;
import com.zzyl.entity.*;
import com.zzyl.mapper.*;
import com.zzyl.service.CheckInService;
import com.zzyl.utils.UserThreadLocal;
import com.zzyl.vo.CheckInConfigVo;
import com.zzyl.vo.CheckInContractVo;
import com.zzyl.vo.CheckInDetailVo;
import com.zzyl.vo.CheckInElderVo;
import com.zzyl.vo.CheckInVo;
import com.zzyl.vo.ElderFamilyVo;
import com.zzyl.vo.UserVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class CheckInServiceImpl implements CheckInService {

    @Resource
    private CheckInMapper checkInMapper;

    @Resource
    private CheckInConfigMapper checkInConfigMapper;

    @Resource
    private ElderMapper elderMapper;

    @Autowired
    private ContractMapper contractMapper;

    @Autowired
    private BedMapper bedMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void apply(CheckInDto checkInDto) {
        // 1. 处理老人信息
        CheckInElderDto elderDto = checkInDto.getCheckInElderDto();
        Elder elder;
        // 根据身份证查询老人是否存在
        Elder existElder = elderMapper.selectByIdCard(elderDto.getIdCardNo());
        if (existElder != null) {
            elder = existElder;
            BeanUtils.copyProperties(elderDto, elder);
            elder.setImage(elderDto.getOneInchPhoto());
            elder.setUpdateTime(LocalDateTime.now());
            // 如果有床位信息，更新床位
            if (checkInDto.getCheckInConfigDto() != null && checkInDto.getCheckInConfigDto().getBedId() != null) {
                elder.setBedId(checkInDto.getCheckInConfigDto().getBedId());
                // 这里假设前端传的code或者需要查询bedNumber。
                // 暂时用CheckInConfigDto里的bedNumber (if available) or update separately.
                // CheckInConfigDto has bedId and code (which might be room code)
                // We will rely on CheckInConfig logic below.
            }
            elderMapper.updateByPrimaryKeySelective(elder);
        } else {
            elder = new Elder();
            BeanUtils.copyProperties(elderDto, elder);
            elder.setImage(elderDto.getOneInchPhoto());
            elder.setCreateTime(LocalDateTime.now());
            elder.setStatus(1); // 启用
            if (checkInDto.getCheckInConfigDto() != null && checkInDto.getCheckInConfigDto().getBedId() != null) {
                elder.setBedId(checkInDto.getCheckInConfigDto().getBedId());
            }
            elderMapper.insert(elder);
        }

        // 2. 生成入住编码
        // 格式：RZ + yyyyMMddHHmmss + 随机数
        String checkInCode = "RZ" + IdUtil.getSnowflakeNextIdStr();

        // 3. 处理入住申请信息
        CheckIn checkIn = new CheckIn();
        checkIn.setCheckInCode(checkInCode);
        checkIn.setTitle(elder.getName() + "的入住申请");
        checkIn.setElderId(elder.getId());
        checkIn.setCheckInTime(LocalDateTime.now());
        checkIn.setStatus(0); // 入住中
        checkIn.setCreateTime(LocalDateTime.now());
        // 设置申请人
        String subject = UserThreadLocal.getSubject();
        if (ObjectUtil.isNotEmpty(subject)) {
            UserVo userVo = JSONObject.parseObject(subject, UserVo.class);
            String applicat = userVo.getRealName();
            if (ObjectUtil.isEmpty(applicat)) {
                applicat = userVo.getNickName();
            }
            if (ObjectUtil.isEmpty(applicat)) {
                applicat = userVo.getCreator();
            }
            checkIn.setApplicat(ObjectUtil.isNotEmpty(applicat) ? applicat : "系统管理员");
            checkIn.setApplicatId(userVo.getId());
            checkIn.setDeptNo(userVo.getDeptNo());
        } else {
             checkIn.setApplicat("系统管理员");
             checkIn.setApplicatId(1L);
        }
        
        // 家属信息存入 otherApplyInfo
        if (checkInDto.getElderFamilyDtoList() != null) {
            checkIn.setOtherApplyInfo(JSON.toJSONString(checkInDto.getElderFamilyDtoList()));
        }
        checkInMapper.insert(checkIn);

        // 4. 处理入住配置信息
        CheckInConfigDto configDto = checkInDto.getCheckInConfigDto();
        if (configDto != null) {
            CheckInConfig config = new CheckInConfig();
            BeanUtils.copyProperties(configDto, config);
            config.setElderId(elder.getId());
            config.setCheckInCode(checkInCode);
            config.setCreateTime(LocalDateTime.now());
            // Fetch bed number from bedMapper
            if (configDto.getBedId() != null) {
                Bed bed = bedMapper.getById(configDto.getBedId());
                if (bed != null) {
                    config.setBedNumber(bed.getBedNumber());
                    bed.setBedStatus(1);
                    bedMapper.updateBed(bed);
                }
            }
        
            checkInConfigMapper.insert(config);
            
            // Update Elder bed info
            elder.setBedId(configDto.getBedId());
            elder.setBedNumber(config.getBedNumber());
            elderMapper.updateByPrimaryKeySelective(elder);
        }

        // 5. 处理合同信息
        CheckInContractDto contractDto = checkInDto.getCheckInContractDto();
        if (contractDto != null) {
            Contract contract = new Contract();
            BeanUtils.copyProperties(contractDto, contract);
            contract.setCheckInNo(checkInCode);
            contract.setElderId(elder.getId());
            contract.setElderName(elder.getName());
            contract.setContractNo("HT" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + RandomUtil.randomNumbers(1));
            contract.setCreateTime(LocalDateTime.now());
            contract.setStatus(1); // 已生效? Or 0未生效
            // Set start/end time from config
            if (configDto != null) {
                contract.setStartTime(configDto.getCheckInStartTime());
                contract.setEndTime(configDto.getCheckInEndTime());
            } else {
                 contract.setStartTime(LocalDateTime.now());
                 contract.setEndTime(LocalDateTime.now().plusYears(1));
            }
            contractMapper.insert(contract);
        }
    }

    @Override
    public PageResponse<CheckInVo> pageQuery(Integer pageNum, Integer pageSize, String name, String idCard) {
        PageHelper.startPage(pageNum, pageSize);
        List<CheckInVo> list = checkInMapper.selectList(name, idCard);
        Page<CheckInVo> page = (Page<CheckInVo>) list;
        return PageResponse.of(page, CheckInVo.class);
    }

    @Override
    public CheckInDetailVo detail(Long id) {
        CheckIn checkIn = checkInMapper.selectByPrimaryKey(id);
        if (checkIn == null) {
            return null;
        }
        CheckInDetailVo dto = new CheckInDetailVo();
        
        // Elder
        Elder elder = elderMapper.selectByPrimaryKey(checkIn.getElderId());
        if (elder != null) {
            CheckInElderVo elderVo = new CheckInElderVo();
            BeanUtils.copyProperties(elder, elderVo);
            elderVo.setOneInchPhoto(elder.getImage());
            dto.setCheckInElderVo(elderVo);
        }

        // Config
        CheckInConfig config = checkInConfigMapper.selectByCheckInCode(checkIn.getCheckInCode());
        if (config != null) {
            CheckInConfigVo configVo = new CheckInConfigVo();
            BeanUtils.copyProperties(config, configVo);
            dto.setCheckInConfigVo(configVo);
        }

        // Contract
        if (elder != null) {
            Contract contract = contractMapper.selectByElderId(elder.getId());
            if (contract != null) {
                CheckInContractVo contractVo = new CheckInContractVo();
                BeanUtils.copyProperties(contract, contractVo);
                dto.setCheckInContractVo(contractVo);
            }
        }
        
        // Family
        if (checkIn.getOtherApplyInfo() != null) {
            dto.setElderFamilyVoList(JSON.parseArray(checkIn.getOtherApplyInfo(), ElderFamilyVo.class));
        }

        return dto;
    }
}
