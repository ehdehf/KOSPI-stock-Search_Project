package com.boot.security;

import com.boot.dao.UserDAO;
import com.boot.dto.UserInfoDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserDAO userDAO;

    // JWT 인증 과정에서 email로 유저정보를 읽는 기능
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        UserInfoDTO user = userDAO.findByEmail(email);

        if (user == null) throw new UsernameNotFoundException("User not found");

        // 스프링 시큐리티가 이해할 수 있는 User 객체로 변환
        return User
                .withUsername(user.getEmail())
                .password(user.getPassword())  // password는 필요한 상황에만 사용됨
                .roles(user.getRole())         // USER / ADMIN
                .build();
    }
}
