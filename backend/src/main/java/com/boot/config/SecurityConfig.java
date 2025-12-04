package com.boot.config;

import com.boot.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.csrf().disable()
            .cors().and()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // JWT 방식
            .and()
            .authorizeHttpRequests()
                .antMatchers("/auth/**").permitAll()        // 로그인/회원가입 API
                .antMatchers("/auth/kakao/**").permitAll() //카카오 로그인
                .antMatchers("/auth/naver/**").permitAll() //네이버 로그인
                .antMatchers("/auth/google/**").permitAll() //구글 로그인
                .antMatchers("/api/stocks/**").permitAll()  // 공개 API
                .antMatchers("/api/news/**").permitAll()    // 공개 API
                .antMatchers("/admin/**").hasRole("ADMIN")  // 추가
                .antMatchers("/api/chart/**").permitAll()
                .anyRequest().authenticated()                   // 나머지는 인증 필요
            .and()
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .formLogin().disable()
            .httpBasic().disable();

        return http.build();
    }
    
    //비밀번호 암호화
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
