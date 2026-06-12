package com.cloudzao.aivoice.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 * <p>
 * 将各类异常统一转换为 RFC 7807 {@link ProblemDetail}（{@code application/problem+json}），
 * 避免向客户端泄漏 stack trace 与内部实现细节，符合架构文档 6.2 节安全策略。
 * </p>
 *
 * @author cloudzao
 * @date 2026-06-11
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OpenClawException.class)
    public ProblemDetail handleOpenClaw(OpenClawException ex, HttpServletRequest request) {
        HttpStatus mapped = mapUpstreamStatus(ex.getStatus());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(mapped,
                "OpenClaw upstream returned status " + ex.getStatus());
        problem.setTitle("OpenClaw Upstream Error");
        problem.setType(URI.create("https://aivoice.cloudzao.com/errors/openclaw-upstream"));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("upstreamStatus", ex.getStatus());
        problem.setProperty("upstreamBody", ex.getUpstreamBody());
        return problem;
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ProblemDetail handleRestClient(RestClientResponseException ex, HttpServletRequest request) {
        HttpStatusCode upstream = ex.getStatusCode();
        HttpStatus mapped = mapUpstreamStatus(upstream.value());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(mapped,
                "Upstream HTTP error: " + upstream.value());
        problem.setTitle("Upstream HTTP Error");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("upstreamStatus", upstream.value());
        problem.setProperty("upstreamBody", ex.getResponseBodyAsString());
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fe -> fe.getField(),
                        fe -> fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage(),
                        (a, b) -> a,
                        LinkedHashMap::new));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Request validation failed");
        problem.setTitle("Validation Failed");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("fieldErrors", fieldErrors);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Malformed request body: " + rootCauseMessage(ex));
        problem.setTitle("Malformed Request");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                ex.getMessage() == null ? "Bad request" : ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnknown(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {}", request.getRequestURI(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error");
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    private String rootCauseMessage(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getMessage() == null ? "invalid JSON" : cause.getMessage();
    }

    private HttpStatus mapUpstreamStatus(int upstream) {
        if (upstream == 401 || upstream == 403) {
            return HttpStatus.BAD_GATEWAY;
        }
        if (upstream == 404) {
            return HttpStatus.BAD_GATEWAY;
        }
        if (upstream == 503 || upstream == 504) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        if (upstream >= 500) {
            return HttpStatus.BAD_GATEWAY;
        }
        if (upstream >= 400) {
            return HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
