package com.optical.modules.users.repository;

import com.optical.modules.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsernameAndDeletedAtIsNull(String username);
    Optional<User> findByIdAndDeletedAtIsNull(Long id);
    boolean existsByUsernameAndDeletedAtIsNull(String username);
    boolean existsByUsernameAndDeletedAtIsNullAndIdNot(String username, Long id);
    List<User> findAllByDeletedAtIsNullOrderByIdDesc();

}
