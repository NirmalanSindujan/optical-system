package com.optical.modules.users.dto;

import com.optical.modules.users.entity.Role;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserResponse {

    private Long id;
    private String username;
    private Role role;
    private Long branchId;
    private String branchCode;
    private String branchName;
}
