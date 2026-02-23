package com.optical.security;

import com.optical.modules.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        return userRepository
                .findByUsernameAndDeletedAtIsNull(username)
                .map(CustomUserDetails::new)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found"));
    }
}