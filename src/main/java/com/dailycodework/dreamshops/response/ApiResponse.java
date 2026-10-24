package com.dailycodework.dreamshops.response;

/** Every endpoint answers with a message and, when there is one, the data it produced. */
public record ApiResponse(String message, Object data) {
}
