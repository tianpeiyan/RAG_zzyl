package com.zzyl.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.zzyl.base.PageResponse;
import com.zzyl.dto.ContractDto;
import com.zzyl.entity.Contract;
import com.zzyl.mapper.ContractMapper;
import com.zzyl.service.ContractService;
import com.zzyl.utils.UserThreadLocal;
import com.zzyl.vo.ContractVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 合同 Service 实现类
 */
@Service
public class ContractServiceImpl implements ContractService {

    private static final int CONTRACT_STATUS_EXPIRED = 2;

    @Autowired
    private ContractMapper contractMapper;

    /**
     * 根据id查询合同
     * @param id 合同id
     * @return 合同实体类
     */
    @Override
    public ContractVo getById(Long id) {
        Contract contract = contractMapper.selectByPrimaryKey(id);
        if (contract != null
                && contract.getEndTime() != null
                && LocalDateTime.now().isAfter(contract.getEndTime())
                && (ObjectUtil.equal(contract.getStatus(), 0) || ObjectUtil.equal(contract.getStatus(), 1))) {
            Contract update = new Contract();
            update.setId(contract.getId());
            update.setStatus(CONTRACT_STATUS_EXPIRED);
            contractMapper.updateByPrimaryKeySelective(update);
            contract.setStatus(CONTRACT_STATUS_EXPIRED);
        }
        return BeanUtil.toBean(contract, ContractVo.class);
    }

    /**
     * 更新合同
     * @param contractDto 合同dto
     * @return 更新结果
     */
    @Override
    public int update(ContractDto contractDto) {
        Contract contract = new Contract();
        BeanUtils.copyProperties(contractDto, contract);
        return contractMapper.updateByPrimaryKey(contract);
    }

    /**
     * 根据id删除合同
     * @param id 合同id
     * @return 删除结果
     */
    @Override
    public int deleteById(Long id) {
        return contractMapper.deleteByPrimaryKey(id);
    }


    @Override
    public List<Contract> listAllContracts() {
        return contractMapper.listAllContracts();
    }

    @Override
    public void updateBatchById(List<Contract> updateList) {
        List<Long> collect = updateList.stream().map(Contract::getId).collect(Collectors.toList());
        contractMapper.batchUpdateByPrimaryKeySelective(collect, updateList.get(0).getStatus());
    }

    @Override
    public List<ContractVo> listByMemberPhone(String phone) {
        contractMapper.updateExpiredStatus(LocalDateTime.now(), CONTRACT_STATUS_EXPIRED);
        return contractMapper.listByMemberPhone(phone);
    }

    /**
     * 分页查询合同信息
     *
     * @param pageNum
     * @param pageSize
     * @param contractNo
     * @param elderName
     * @param status
     * @param startTime
     * @param endTime
     * @return
     */
    @Override
    public PageResponse<ContractVo> selectByPage(Integer pageNum, Integer pageSize, String contractNo, String elderName, Integer status, LocalDateTime startTime, LocalDateTime endTime) {
        contractMapper.updateExpiredStatus(LocalDateTime.now(), CONTRACT_STATUS_EXPIRED);
        PageHelper.startPage(pageNum, pageSize);
        Page<List<Contract>> page = contractMapper.selectByPage(null, contractNo, elderName, status, startTime, endTime);
        return PageResponse.of(page, ContractVo.class);
    }
}


