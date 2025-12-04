package com.boot.dao;

import com.boot.dto.UserInfoDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserDAO {

    UserInfoDTO findByEmail(@Param("email") String email);

    int updateFailCount(@Param("email") String email,
                        @Param("count") int count);

    int lockUser(@Param("email") String email,
                 @Param("lockUntil") String lockUntil);

    int resetFailCount(@Param("email") String email);
   
    //회원가입
    int insertUser(
            @Param("email") String email,
            @Param("firstName") String firstName,
            @Param("lastName") String lastName,
            @Param("fullName") String fullName,
            @Param("password") String password,
            @Param("provider") String provider,
            @Param("role") String role,
            @Param("resetToken") String resetToken,
            @Param("tokenExpireAt") String tokenExpireAt
    );

    //이메일 인증
    UserInfoDTO findByToken(String token);

    void activateUser(String email);
    
    //비밀번호 재설정
    int updateResetToken(
            @Param("email") String email,
            @Param("resetToken") String resetToken,
            @Param("tokenExpireAt") String tokenExpireAt
    );

    int updatePasswordAndClearToken(
            @Param("email") String email,
            @Param("password") String newPassword
    );
    
    void updateRefreshToken(
    		@Param("email") String email,
            @Param("refreshToken") String refreshToken);

    UserInfoDTO findByRefreshToken(String refreshToken);
    void deleteRefreshToken(String email);
    
    //소셜로그인
    void insertSocialUser(String email, String fullName, String provider);
}