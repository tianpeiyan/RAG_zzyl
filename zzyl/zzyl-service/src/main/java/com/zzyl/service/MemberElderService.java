package com.zzyl.service;

import com.zzyl.dto.MemberElderDto;
import com.zzyl.vo.MemberElderVo;

import java.util.List;

public interface MemberElderService {
    void add(MemberElderDto memberElderDto);

    List<MemberElderVo> list();

    void deleteById(Long id);
}
