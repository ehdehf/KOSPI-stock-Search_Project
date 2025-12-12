package com.boot.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.boot.dto.DailyUserJoinDTO;
import com.boot.dto.DashboardSummaryDTO;
import com.boot.dto.LoginStatusStatDTO;
import com.boot.dto.StockNewsTopDTO;
import com.boot.dto.UserInfoDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface AdminDAO {

	//계정 조회
    List<UserInfoDTO> getUsers();

    //계정 정지
    void suspendUser(@Param("email") String email,
		             @Param("until") String until,
		             @Param("reason") String reason);
    //계정 정지 해제
    void unsuspendUser(String email);
    
    //권한 변경
    UserInfoDTO findUserByEmail(String email);

    void updateUserRole(@Param("email") String email,
                        @Param("role") String role);

    //로그인 시도 횟수 초기화
    int resetLoginFail(String email);
    
    //계정 강제 로그아웃
    void forceLogout(String email);
    
    //Refresh Token 전체 조회
    List<Map<String,Object>> getTokens();
    
    // 특정 사용자 Refresh Token
    int deleteRefreshToken(String email);
    
    // 전체 Refresh Token 초기화
    void clearAllTokens();
    
    // 로그인 로그 조회
    List<Map<String,Object>> getLoginLog();

    // 관리자 로그 조회
    List<Map<String,Object>> getAdminLog();
    
    // 대시보드 상단 카드 요약
    DashboardSummaryDTO getDashboardSummary();

    // 최근 N일 일별 가입자 수
    List<DailyUserJoinDTO> getDailyJoins(@Param("days") int days);

    // 최근 N일 로그인 STATUS 통계
    List<LoginStatusStatDTO> getLoginStatusStats(@Param("days") int days);

    // 최근 N일 뉴스 많이 나온 종목 TOP N
    List<StockNewsTopDTO> getTopNewsStocks(@Param("days") int days,
                                           @Param("limit") int limit);
    // 보안 통계
    int countLockedUsers();
    int countRapidFailAttempts();
    int countRiskyIp();
    
    List<Map<String, Object>> listLockedUsers();
    List<Map<String, Object>> listRapidFailAccounts();
    List<Map<String, Object>> listRiskyIpAccounts();
    
    void insertAdminLog(@Param("adminEmail") String adminEmail,
			            @Param("targetEmail") String targetEmail,
			            @Param("action") String action,
			            @Param("detail") String detail);
}
