package com.zzyl.assistant.mapper;

import com.zzyl.assistant.entity.AssistantHistory;
import com.zzyl.assistant.vo.AssistantSessionVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AssistantHistoryMapper {

    int insert(AssistantHistory history);

    List<AssistantHistory> findBySession(@Param("userId") Long userId, @Param("sessionId") String sessionId);

    List<AssistantHistory> findRecentMessages(@Param("userId") Long userId,
                                              @Param("sessionId") String sessionId,
                                              @Param("limit") int limit);

    List<AssistantSessionVo> findSessions(@Param("userId") Long userId, @Param("limit") int limit);

    int deleteSession(@Param("userId") Long userId, @Param("sessionId") String sessionId);
}
