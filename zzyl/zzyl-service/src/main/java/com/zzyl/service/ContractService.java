

package com.zzyl.service;

import com.zzyl.base.PageResponse;
import com.zzyl.dto.ContractDto;
import com.zzyl.entity.Contract;
import com.zzyl.vo.ContractVo;

import java.time.LocalDateTime;
import java.util.List;


/**
 * 合同 Service
 */
public interface ContractService {

    /**
     * 根据合同id查询合同信息
     *
     * @param id 合同id
     * @return 合同信息
     */
    ContractVo getById(Long id);

    /**
     * 更新合同信息
     *
     * @param contractDto 合同信息
     * @return 受影响的行数
     */
    int update(ContractDto contractDto);

    /**
     * 根据合同id删除合同信息
     * * @param id 合同id
     * * @return 受影响的行数
     */
    int deleteById(Long id);


    /**
     * 查询所有合同
     * @return
     */
    List<Contract> listAllContracts();

    /**
     * 批量修改合同
     * @param updateList
     */
    void updateBatchById(List<Contract> updateList);

    /**
     * 根据手机号查询合同列表
     * @param phone
     * @return
     */
    List<ContractVo> listByMemberPhone(String phone);

    /**
     * 分页查询合同信息
     * @param pageNum
     * @param pageSize
     * @param contractNo
     * @param elderName
     * @param status
     * @param localDateTime
     * @param localDateTime1
     * @return
     */
    PageResponse<ContractVo> selectByPage(Integer pageNum, Integer pageSize, String contractNo, String elderName, Integer status, LocalDateTime localDateTime, LocalDateTime localDateTime1);
}


