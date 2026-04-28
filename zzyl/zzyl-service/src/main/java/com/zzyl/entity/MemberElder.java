package com.zzyl.entity;

import com.zzyl.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 客户老人关联实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MemberElder extends BaseEntity {

    /**
     * 会员ID
     */
    private Long memberId;

    /**
     * 老人ID
     */
    private Long elderId;

}
