package com.optical.modules.branch.entity;

import com.optical.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "branch")
@Getter
@Setter
public class Branch extends BaseEntity {

    @Column(unique = true, nullable = false, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "is_main")
    private Boolean isMain = false;
}