package com.telegram.auth.dto.response;

import com.telegram.user.dto.response.UserProfileResponse;

public record AuthResponse(
        String accessToken,
        UserProfileResponse user
) {}
