package com.boot.dao;

import com.boot.dto.FavoriteDTO;
import com.boot.dto.UserInfoDTO;

import java.util.List;

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
    
    //소셜로그인 신규 유저용
    void insertSocialUser(
    		@Param("email") String email,
            @Param("fullName") String fullName,
            @Param("provider") String provider);
    

    // 찜목록 관련
    void addFavoriteStock(
    		@Param("email") String email,
    		@Param("stockCode") String stockCode);
    
    void removeFavoriteStock(
    		@Param("email") String email,
    		@Param("stockCode") String stockCode);
    
    void addFavoriteNews(
    		@Param("email") String email,
    		@Param("newsId") Long newsId);
    
    void removeFavoriteNews(
    		@Param("email") String email,
    		@Param("newsId") Long newsId);
    
    // 1. 회원 탈퇴
    void deleteUser(@Param("email") String email);

    // 2. 회원 정보 수정
    void updateUserInfo(
            @Param("email") String email,
            @Param("fullName") String fullName,
            @Param("password") String password
    );

    // 3. 관심 종목 목록 가져오기 (List 반환)
    List<FavoriteDTO> getFavoriteStocks(@Param("email") String email);

    // 4. 관심 뉴스 목록 가져오기 (List 반환)
    List<FavoriteDTO> getFavoriteNews(@Param("email") String email);
    
    void clearSuspend(String email);
    
    void markNewsAsRead(@Param("email") String email, @Param("newsId") Long newsId);
    
    // ⭐ [추가] 메모 업데이트 메서드
    void updateStockMemo(@Param("email") String email, 
                         @Param("stockCode") String stockCode, 
                         @Param("memo") String memo);

    void updateNewsMemo(@Param("email") String email, 
                        @Param("newsId") Long newsId, 
                        @Param("memo") String memo);
}