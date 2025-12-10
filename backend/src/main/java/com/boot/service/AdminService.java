package com.boot.service;

import com.boot.dao.AdminDAO;
import com.boot.dto.ChangeRoleDTO;
import com.boot.dto.SuspendRequestDTO;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminDAO adminDAO;
    private final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    //계정 조회
    public ResponseEntity<?> getUsers() {
        return ResponseEntity.ok(adminDAO.getUsers());
    }

 // 1) 계정 정지
    public ResponseEntity<?> suspendUser(SuspendRequestDTO req) {

        LocalDateTime until = LocalDateTime.now().plusDays(req.getDays());

        adminDAO.suspendUser(
                req.getEmail(),
                until.format(formatter),
                req.getReason()
        );

        adminDAO.insertAdminLog(
                "ADMIN",
                req.getEmail(),
                "SUSPEND",
                "정지 " + req.getDays() + "일, 사유: " + req.getReason()
        );

        return ResponseEntity.ok(
                "계정 정지 완료\n정지 해제: " + until.format(formatter)
                + "\n사유: " + req.getReason()
        );
    }

    // 2) 계정 정지 해제
    public ResponseEntity<?> unsuspendUser(String email) {

        adminDAO.unsuspendUser(email);

        adminDAO.insertAdminLog(
                "ADMIN",
                email,
                "UNSUSPEND",
                "정지 해제"
        );

        return ResponseEntity.ok("정지 해제 완료");
    }

    public ResponseEntity<?> changeUserRole(ChangeRoleDTO dto) {
    	
    	String email = dto.getEmail();
        String newRole = dto.getNewRole();
        
        // 1) 권한 유효성 체크
        if (!"USER".equals(newRole) && !"ADMIN".equals(newRole)) {
            return ResponseEntity.status(400)
                    .body("role 값은 USER 또는 ADMIN만 가능합니다.");
        }

        // 2) 대상 유저 조회
        var user = adminDAO.findUserByEmail(email);
        if (user == null) {
            return ResponseEntity.status(404).body("해당 사용자를 찾을 수 없습니다.");
        }

        // 🚫 3) 정지된 계정이면 권한 변경 금지
        if ("Y".equals(user.getIsSuspended())) {
            return ResponseEntity.status(403)
                    .body("🚫 정지된 계정의 권한은 변경할 수 없습니다.");
        }

        // 🚫 4) 이메일 인증 되지 않은 계정의 권한 변경 금지
        if (!"ACTIVE".equals(user.getAccountStatus())) {
            return ResponseEntity.status(403)
                    .body("🚫 이메일 인증이 완료되지 않은 계정은 권한을 변경할 수 없습니다.");
        }

        // ✔ 이미 같은 권한이면 변경 불필요
        if (newRole.equals(user.getRole())) {
            return ResponseEntity.ok("이미 '" + newRole + "' 권한입니다.");
        }

        String oldRole = user.getRole();

        // 5) DB 업데이트
        adminDAO.updateUserRole(email, newRole);

        // 6) 관리자 로그 기록
        adminDAO.insertAdminLog(
                "ADMIN",
                email,
                "ROLE_CHANGE",
                "권한 변경: " + oldRole + " → " + newRole
        );

        return ResponseEntity.ok("권한이 성공적으로 " + newRole + "로 변경되었습니다.");
    }

    //로그인 횟수 초기화
    public ResponseEntity<?> resetLoginFail(String email) {

        // 1) 사용자 존재 확인
        var user = adminDAO.findUserByEmail(email);
        if (user == null) {
            return ResponseEntity.status(404)
                    .body("해당 사용자를 찾을 수 없습니다.");
        }

        // 2) 실패 횟수와 잠금 해제
        adminDAO.resetLoginFail(email);

        // 3) 관리자 로그 기록
        adminDAO.insertAdminLog(
                "ADMIN",
                email,
                "RESET_FAIL",
                "로그인 실패 횟수 초기화 + 계정 잠금 해제"
        );

        return ResponseEntity.ok("로그인 실패 횟수가 초기화되었습니다.");
    }
    
    //계정 강제 로그아웃
    public ResponseEntity<?> forceLogout(String email) {

        // 1) 사용자 존재 확인
        var user = adminDAO.findUserByEmail(email);
        if (user == null) {
            return ResponseEntity.status(404)
                    .body("해당 사용자를 찾을 수 없습니다.");
        }

        // 2) refresh token 제거 → 강제 로그아웃
        adminDAO.forceLogout(email);

        // 3) 관리자 로그 기록
        adminDAO.insertAdminLog(
                "ADMIN",
                email,
                "FORCE_LOGOUT",
                "강제 로그아웃 수행"
        );

        return ResponseEntity.ok("해당 사용자가 강제 로그아웃되었습니다.");
    }
    
    // Refresh Token 전체 조회
    public ResponseEntity<?> getTokens() {
        return ResponseEntity.ok(adminDAO.getTokens());
    }
    
    // 특정 사용자 Refresh Token
    public ResponseEntity<?> deleteUserToken(String email) {

        // 존재하는 유저인지 먼저 확인
        var user = adminDAO.findUserByEmail(email);
        if (user == null) {
            return ResponseEntity.status(404).body("해당 사용자를 찾을 수 없습니다.");
        }

        // DB 업데이트
        adminDAO.deleteRefreshToken(email);

        // 관리자 로그 기록
        adminDAO.insertAdminLog(
                "ADMIN",
                email,
                "TOKEN_DELETE",
                "사용자의 Refresh Token 삭제(강제 로그아웃)"
        );

        return ResponseEntity.ok("해당 사용자의 Refresh Token이 삭제되었습니다.");
    }

    // 전체 Refresh Token 초기화
    public ResponseEntity<?> clearAllTokens() {

        adminDAO.clearAllTokens();

        adminDAO.insertAdminLog(
                "ADMIN",
                null,
                "CLEAR_TOKENS",
                "전체 Refresh Token 초기화 (전체 사용자 즉시 로그아웃)"
        );

        return ResponseEntity.ok("전체 Refresh Token이 초기화되었습니다. 모든 사용자가 로그아웃됩니다.");
    }
//
//    public ResponseEntity<?> getLoginLog() {
//        return ResponseEntity.ok(adminDAO.getLoginLog());
//    }
//
//    public ResponseEntity<?> getAdminLog() {
//        return ResponseEntity.ok(adminDAO.getAdminLog());
//    }
//
//    public ResponseEntity<?> dashboard() {
//        return ResponseEntity.ok(adminDAO.getDashboard());
//    }
}
