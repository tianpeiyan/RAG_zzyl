/**
 * 老人服务接口
 */
package com.zzyl.service;

import com.zzyl.dto.ElderDto;
import com.zzyl.dto.NursingElderDto;
import com.zzyl.entity.Elder;
import com.zzyl.vo.ElderVo;
import com.zzyl.vo.RetreatVo;

import java.util.List;

public interface ElderService {
    /**
     * 根据id删除老人信息
     * @param id
     * @return
     */
    int deleteByPrimaryKey(Long id);

    /**
     * 插入老人信息
     * @param record
     * @return
     */
    Elder insert(ElderDto record);

    /**
     * 选择性插入老人信息
     * @param record
     * @return
     */
    int insertSelective(ElderDto record);

    /**
     * 根据id选择老人信息
     * @param id
     * @return
     */
    ElderVo selectByPrimaryKey(Long id);

    /**
     * 选择性更新老人信息
     * @param record
     * @param b
     * @return
     */
    Elder updateByPrimaryKeySelective(ElderDto record, boolean b);

    /**
     * 更新老人信息
     * @param record
     * @return
     */
    int updateByPrimaryKey(ElderDto record);

    /**
     * 根据身份证号和姓名查询老人信息
     * @param idCard
     * @param name
     * @return
     */
    ElderVo selectByIdCardAndName(String idCard, String name);

    /**
     * 查询所有老人
     * @return
     */
    List<ElderVo> selectList();

    /**
     * 根据id集合查询老人列表
     * @param ids
     * @return
     */
    List<Elder> selectByIds(List<Long> ids);

    /**
     * 根据身份证号查询老人
     * @param idCard
     * @return
     */
    ElderVo selectByIdCard(String idCard);


    /**
     * 清除老人床位编号
     * @param elderId
     */
    void clearBedNum(Long elderId);

    /**
     * 分页查询老人信息
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param name 姓名
     * @param idCardNo 身份证号
     * @param status 状态
     * @return 分页结果
     */
    com.zzyl.base.PageResponse<ElderVo> pageQuery(Integer pageNum, Integer pageSize, String name, String idCardNo, Integer status);
    /**
     * 获取老人入住信息(用于退住申请)
     * @param elderId
     * @return
     */
    RetreatVo getCheckInInfo(Long elderId);

    void setNursing(List<NursingElderDto> nursingElderDtos);
}

