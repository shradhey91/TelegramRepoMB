package com.telegram.call.dto.response;

import com.telegram.common.enums.ParticipantRole;
import com.telegram.common.enums.ParticipantStatus;

public record ParticipantResponse(
        Long userId,
        String name,
        String avatarUrl,
        ParticipantRole role,
        ParticipantStatus status,
        boolean muted,
        boolean cameraEnabled,
        boolean screenSharing
) {

}
