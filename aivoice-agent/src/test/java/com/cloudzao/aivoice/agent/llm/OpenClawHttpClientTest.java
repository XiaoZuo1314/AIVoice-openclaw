package com.cloudzao.aivoice.agent.llm;

import com.cloudzao.aivoice.agent.llm.dto.ChatCompletionRequest;
import com.cloudzao.aivoice.agent.llm.dto.ChatCompletionResponse;
import com.cloudzao.aivoice.agent.llm.dto.ChatMessage;
import com.cloudzao.aivoice.common.config.OpenClawProperties;
import com.cloudzao.aivoice.common.exception.OpenClawException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * {@link OpenClawHttpClient} 的单元测试。
 *
 * @author cloudzao
 * @date 2026-06-11
 */
class OpenClawHttpClientTest {

    private static final String BASE_URL = "http://127.0.0.1:18789";
    private static final String CHAT_PATH = "/v1/chat/completions";
    private static final String MODELS_PATH = "/v1/models";

    private OpenClawHttpClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        OpenClawProperties props = new OpenClawProperties(
                BASE_URL,
                "test-token",
                "openclaw",
                new OpenClawProperties.Endpoint(CHAT_PATH, MODELS_PATH),
                new OpenClawProperties.Timeout(Duration.ofSeconds(1), Duration.ofSeconds(5)));

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer test-token");
        server = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        client = new OpenClawHttpClient(restClient, props);
    }

    @Test
    void chatCompletion_returnsParsedResponse_on200() {
        String body = """
                {
                  "id": "chatcmpl-1",
                  "model": "openclaw",
                  "choices": [
                    {
                      "index": 0,
                      "message": {"role": "assistant", "content": "hello world"},
                      "finish_reason": "stop"
                    }
                  ],
                  "usage": {"prompt_tokens": 10, "completion_tokens": 5, "total_tokens": 15}
                }
                """;

        server.expect(requestTo(BASE_URL + CHAT_PATH))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        ChatCompletionRequest req = new ChatCompletionRequest(
                "openclaw",
                List.of(ChatMessage.user("hi")),
                false,
                null);

        ChatCompletionResponse res = client.chatCompletion(req);

        server.verify();
        assertThat(res).isNotNull();
        assertThat(res.firstContent()).contains("hello world");
        assertThat(res.usage()).isNotNull();
        assertThat(res.usage().totalTokens()).isEqualTo(15);
        assertThat(res.usage().promptTokens()).isEqualTo(10);
        assertThat(res.usage().completionTokens()).isEqualTo(5);
    }

    @Test
    void chatCompletion_throwsOpenClawException_on401() {
        server.expect(requestTo(BASE_URL + CHAT_PATH))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"invalid token\"}"));

        ChatCompletionRequest req = new ChatCompletionRequest(
                "openclaw",
                List.of(ChatMessage.user("hi")),
                false,
                null);

        assertThatThrownBy(() -> client.chatCompletion(req))
                .isInstanceOf(OpenClawException.class)
                .satisfies(ex -> {
                    OpenClawException oce = (OpenClawException) ex;
                    assertThat(oce.getStatus()).isEqualTo(401);
                    assertThat(oce.getUpstreamBody()).contains("invalid token");
                });
        server.verify();
    }

    @Test
    void chatCompletion_throws503_whenGatewayUnreachable() {
        server.expect(requestTo(BASE_URL + CHAT_PATH))
                .andRespond(withException(new IOException("Connection refused")));

        ChatCompletionRequest req = new ChatCompletionRequest(
                "openclaw",
                List.of(ChatMessage.user("hi")),
                false,
                null);

        assertThatThrownBy(() -> client.chatCompletion(req))
                .isInstanceOfAny(OpenClawException.class, ResourceAccessException.class)
                .satisfies(ex -> {
                    if (ex instanceof OpenClawException oce) {
                        assertThat(oce.getStatus()).isEqualTo(503);
                    }
                });
        server.verify();
    }

    @Test
    void chatCompletionStream_emitsChunks_andCallsCompleteOnDone() {
        String sseBody = """
                data: {"id":"1","model":"openclaw","choices":[{"index":0,"delta":{"role":"assistant","content":"你"},"finish_reason":null}]}

                data: {"id":"1","model":"openclaw","choices":[{"index":0,"delta":{"content":"好"},"finish_reason":null}]}

                data: {"id":"1","model":"openclaw","choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}

                data: [DONE]

                """;

        server.expect(requestTo(BASE_URL + CHAT_PATH))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andRespond(withSuccess(sseBody, MediaType.TEXT_EVENT_STREAM));

        ChatCompletionRequest req = new ChatCompletionRequest(
                "openclaw",
                List.of(ChatMessage.user("hi")),
                false,
                null);

        List<String> contents = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        AtomicReference<Throwable> error = new AtomicReference<>();

        client.chatCompletionStream(
                req,
                contents::add,
                () -> completed.set(true),
                error::set);

        server.verify();
        assertThat(contents).containsExactly("你", "好");
        assertThat(completed).isTrue();
        assertThat(error.get()).isNull();
    }

    @Test
    void listModelsRaw_returnsRawJson_on200() {
        String body = "{\"object\":\"list\",\"data\":[{\"id\":\"openclaw\"}]}";

        server.expect(requestTo(BASE_URL + MODELS_PATH))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        String result = client.listModelsRaw();

        server.verify();
        assertThat(result).contains("openclaw");
    }
}
