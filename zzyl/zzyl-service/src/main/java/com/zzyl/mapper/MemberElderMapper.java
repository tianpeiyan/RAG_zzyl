package com.zzyl.mapper;

import com.zzyl.entity.MemberElder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MemberElderMapper {
    int insert(MemberElder memberElder);

    List<MemberElder> selectByMemberId(Long memberId);

    int deleteById(Long id);

    List<MemberElder> selectByMemberIdAndElderId(@Param("memberId") Long memberId, @Param("elderId") Long elderId);
}
