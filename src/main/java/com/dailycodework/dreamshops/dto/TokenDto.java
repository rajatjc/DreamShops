package com.dailycodework.dreamshops.dto;

import java.time.Instant;

/** Send the token as "Authorization: Bearer {token}" until it expires. */
public record TokenDto(String token, String tokenType, Instant expiresAt) {
}
