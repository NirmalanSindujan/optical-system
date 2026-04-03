package com.optical.modules.users.dto;

import com.optical.modules.users.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCreateRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String password;

    @NotNull
    private Role role;

    private Long branchId;
}
