package com.telegram.chat.dto.request;

import com.telegram.common.enums.MemberRole;

public record ChangeRoleRequest(
        MemberRole newRole
) {}