package com.cloudzao.aivoice.agent.llm.dto;

/**
 * OpenAI 兼容协议中的单条对话消息。
 *
 * @param role    角色标识，取值 {@code system} / {@code user} / {@code assistant}
 * @param content 消息文本内容，UTF-8
 * @author cloudzao
 * @date 2026-06-11
 */
public record ChatMessage(String role, String content) {

    public static ChatMessage system(String content) {
        return new ChatMessage("system", content);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage("user", content);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage("assistant", content);
    }
}
