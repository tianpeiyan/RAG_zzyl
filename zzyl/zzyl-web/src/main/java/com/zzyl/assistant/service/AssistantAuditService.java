package com.zzyl.assistant.service;

import com.zzyl.assistant.vo.AssistantChatVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AssistantAuditService {

    public void recordChat(Long userId, String username, String question, long costMillis,
                           String status, AssistantChatVo response, String failReason) {
        Integer hitCount = response == null ? 0 : response.getHitCount();
        Boolean refused = response == null ? null : response.getRefused();
        log.info(
                "assistant_audit userId={} username={} costMillis={} status={} hitCount={} refused={} question=\"{}\" failReason=\"{}\"",
                userId,
                username,
                costMillis,
                status,
                hitCount == null ? 0 : hitCount,
                refused,
                summarize(question),
                failReason == null ? "" : failReason
        );
    }

    private String summarize(String question) {
        if (question == null) {
            return "";
        }
        String normalized = question.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 80) {
            return normalized;
        }
        return normalized.substring(0, 80);
    }
}
