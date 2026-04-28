package com.zzyl.controller;

import com.zzyl.assistant.dto.AssistantChatRequest;
import com.zzyl.assistant.service.AssistantService;
import com.zzyl.assistant.vo.AssistantChatVo;
import com.zzyl.assistant.vo.AssistantHistoryMessageVo;
import com.zzyl.assistant.vo.AssistantSessionVo;
import com.zzyl.assistant.vo.KbDocumentVo;
import com.zzyl.assistant.vo.KbIndexStatusVo;
import com.zzyl.assistant.vo.KbRebuildVo;
import com.zzyl.base.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/assistant")
@Api(tags = "智能助手")
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/chat")
    @ApiOperation("智能助手聊天问答")
    public ResponseResult<AssistantChatVo> chat(@RequestBody AssistantChatRequest request) {
        return ResponseResult.success(assistantService.chat(request));
    }

    @PostMapping(value = "/chat", params = "stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperation("智能助手聊天问答流式输出")
    public SseEmitter chatStream(@RequestBody AssistantChatRequest request) {
        return assistantService.chatStream(request);
    }

    @GetMapping("/history/sessions")
    @ApiOperation("查询智能助手会话列表")
    public ResponseResult<List<AssistantSessionVo>> sessions() {
        return ResponseResult.success(assistantService.sessions());
    }

    @GetMapping("/history/sessions/{sessionId}/messages")
    @ApiOperation("查询智能助手会话消息")
    public ResponseResult<List<AssistantHistoryMessageVo>> messages(@PathVariable String sessionId) {
        return ResponseResult.success(assistantService.messages(sessionId));
    }

    @DeleteMapping("/history/sessions/{sessionId}")
    @ApiOperation("删除智能助手会话")
    public ResponseResult<Object> deleteSession(@PathVariable String sessionId) {
        assistantService.deleteSession(sessionId);
        return ResponseResult.success();
    }

    @GetMapping("/kb/documents")
    @ApiOperation("查询知识库文档列表")
    public ResponseResult<List<KbDocumentVo>> documents() {
        return ResponseResult.success(assistantService.documents());
    }

    @GetMapping("/kb/index/status")
    @ApiOperation("查询知识库索引状态")
    public ResponseResult<KbIndexStatusVo> status() {
        return ResponseResult.success(assistantService.status());
    }

    @PostMapping("/kb/index/rebuild")
    @ApiOperation("手动重建知识库索引")
    public ResponseResult<KbRebuildVo> rebuild() {
        return ResponseResult.success(assistantService.rebuild());
    }
}
