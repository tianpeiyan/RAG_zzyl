package com.zzyl.mapper;

import com.zzyl.entity.Member;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface MemberMapper {

    void insertMember(Member member);

    void updateMember(Member member);

    @Select("select * from member where open_id = #{openid}")
    public Member getByOpenid(String openid);

    @Select("select * from member where id = #{id}")
    Member getById(Long id);
}
