package com.carsai.back.message.dto;

public record SendMessageResponse(
        MessageDto userMessage,
        MessageDto assistantMessage
) {}
