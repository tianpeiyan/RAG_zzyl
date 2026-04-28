package com.zzyl.assistant.client;

import com.zzyl.assistant.config.AssistantProperties;
import com.zzyl.assistant.constant.AssistantErrorCode;
import com.zzyl.assistant.dto.InternalAssistantChatRequest;
import com.zzyl.assistant.exception.AssistantServiceException;
import com.zzyl.assistant.vo.AssistantChatVo;
import com.zzyl.assistant.vo.KbDocumentVo;
import com.zzyl.assistant.vo.KbIndexStatusVo;
import com.zzyl.assistant.vo.KbRebuildVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class AssistantPythonClient {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USERNAME_HEADER = "X-Username";

    private final RestTemplate restTemplate;
    private final AssistantProperties properties;

    public AssistantPythonClient(RestTemplate assistantRestTemplate, AssistantProperties properties) {
        this.restTemplate = assistantRestTemplate;
        this.properties = properties;
    }

    public AssistantChatVo chat(InternalAssistantChatRequest request) {
        return exchangeWithRetry(
                "/internal/chat",
                HttpMethod.POST,
                request,
                headers(request.getUserId(), request.getUsername()),
                new ParameterizedTypeReference<AssistantChatVo>() {
                }
        );
    }

    public List<KbDocumentVo> documents(Long userId, String username) {
        List<KbDocumentVo> documents = exchangeWithRetry(
                "/internal/kb/documents",
                HttpMethod.GET,
                null,
                headers(userId, username),
                new ParameterizedTypeReference<List<KbDocumentVo>>() {
                }
        );
        return documents == null ? Collections.emptyList() : documents;
    }

    public KbIndexStatusVo status(Long userId, String username) {
        return exchangeWithRetry(
                "/internal/kb/status",
                HttpMethod.GET,
                null,
                headers(userId, username),
                new ParameterizedTypeReference<KbIndexStatusVo>() {
                }
        );
    }

    public KbRebuildVo rebuild(Long userId, String username) {
        return exchangeWithRetry(
                "/internal/kb/reindex",
                HttpMethod.POST,
                Collections.emptyMap(),
                headers(userId, username),
                new ParameterizedTypeReference<KbRebuildVo>() {
                }
        );
    }

    private <T> T exchangeWithRetry(String path, HttpMethod method, Object body, HttpHeaders headers,
                                    ParameterizedTypeReference<T> responseType) {
        int maxAttempts = Math.max(1, properties.getMaxRetries() + 1);
        RuntimeException lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                ResponseEntity<T> response = restTemplate.exchange(
                        buildUrl(path),
                        method,
                        new HttpEntity<>(body, headers),
                        responseType
                );
                return response.getBody();
            } catch (ResourceAccessException exception) {
                lastException = exception;
                if (isTimeout(exception)) {
                    throw new AssistantServiceException(
                            AssistantErrorCode.AI_SERVICE_TIMEOUT,
                            "AI 服务响应超时，请稍后重试",
                            exception
                    );
                }
                if (attempt == maxAttempts) {
                    throw new AssistantServiceException(
                            AssistantErrorCode.AI_SERVICE_UNAVAILABLE,
                            unavailableMessage(exception),
                            exception
                    );
                }
            } catch (HttpStatusCodeException exception) {
                throw mapHttpException(exception);
            } catch (RestClientException exception) {
                lastException = exception;
                if (attempt == maxAttempts) {
                    throw new AssistantServiceException(
                            AssistantErrorCode.AI_SERVICE_UNAVAILABLE,
                            "AI 服务暂不可用，请稍后重试",
                            exception
                    );
                }
            }
        }
        throw new AssistantServiceException(
                AssistantErrorCode.AI_SERVICE_UNAVAILABLE,
                "AI 服务暂不可用，请稍后重试",
                lastException
        );
    }

    private String buildUrl(String path) {
        String baseUrl = properties.getBaseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl + path;
    }

    private HttpHeaders headers(Long userId, String username) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set(INTERNAL_API_KEY_HEADER, properties.getInternalApiKey());
        if (userId != null) {
            headers.set(USER_ID_HEADER, String.valueOf(userId));
        }
        if (username != null) {
            headers.set(USERNAME_HEADER, username);
        }
        return headers;
    }

    private boolean isTimeout(ResourceAccessException exception) {
        Throwable cause = exception.getCause();
        while (cause != null) {
            if (cause instanceof SocketTimeoutException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private boolean isConnectFailed(ResourceAccessException exception) {
        Throwable cause = exception.getCause();
        while (cause != null) {
            if (cause instanceof ConnectException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private String unavailableMessage(ResourceAccessException exception) {
        if (isConnectFailed(exception)) {
            return "AI 服务未启动或地址不可达，请检查 Python 服务";
        }
        return "AI 服务暂不可用，请稍后重试";
    }

    private AssistantServiceException mapHttpException(HttpStatusCodeException exception) {
        int statusCode = exception.getRawStatusCode();
        log.warn("AI service returned {}: {}", statusCode, exception.getResponseBodyAsString());
        if (statusCode == 404) {
            return new AssistantServiceException(AssistantErrorCode.AI_INDEX_NOT_READY, "知识库索引尚未就绪，请先重建索引", exception);
        }
        if (statusCode == 409) {
            return new AssistantServiceException(AssistantErrorCode.AI_REBUILDING, "知识库正在重建，请稍后再试", exception);
        }
        if (statusCode >= 500) {
            return new AssistantServiceException(AssistantErrorCode.AI_MODEL_ERROR, "AI 模型或检索服务异常，请稍后重试", exception);
        }
        return new AssistantServiceException(AssistantErrorCode.AI_SERVICE_UNAVAILABLE, "AI 服务请求失败，请检查知识库状态", exception);
    }
}
