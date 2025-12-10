package com.boot.controller;

import com.boot.dto.ChangeRoleDTO;
import com.boot.dto.SuspendRequestDTO;
import com.boot.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ✔ 사용자 목록
    @GetMapping("/users")
    public ResponseEntity<?> getUsers() {
        return adminService.getUsers();
    }

    // 1) 계정 정지
    @PutMapping("/user/suspend")
    public ResponseEntity<?> suspendUser(@RequestBody SuspendRequestDTO req) {
        return adminService.suspendUser(req);
    }

    // 2) 계정 정지 해제
    @PutMapping("/user/unsuspend")
    public ResponseEntity<?> unsuspendUser(@RequestParam String email) {
        return adminService.unsuspendUser(email);
    }

    // ✔ 권한 변경
    @PutMapping("/user/role")
    public ResponseEntity<?> changeUserRole(@RequestBody ChangeRoleDTO dto) {
        return adminService.changeUserRole(dto);
    }

    // ✔ 로그인 실패 횟수 초기화
    @PutMapping("/user/reset-fail")
    public ResponseEntity<?> resetFail(@RequestParam String email) {
        return adminService.resetLoginFail(email);
    }

    // ✔ 강제 로그아웃
    @PutMapping("/user/logout")
    public ResponseEntity<?> forceLogout(@RequestParam String email) {
        return adminService.forceLogout(email);
    }

    // ✔ Refresh Token 목록 조회
    @GetMapping("/tokens")
    public ResponseEntity<?> getTokens() {
        return adminService.getTokens();
    }
    
    // ✔ 특정 사용자 Refresh Token 삭제
    @DeleteMapping("/tokens")
    public ResponseEntity<?> deleteUserToken(@RequestParam String email) {
        return adminService.deleteUserToken(email);
    }
    
    // ✔ 전체 토큰 초기화
    @DeleteMapping("/tokens/all")
    public ResponseEntity<?> clearAllTokens() {
        return adminService.clearAllTokens();
    }
//
//    // ✔ 로그인 로그 조회
//    @GetMapping("/logs/login")
//    public ResponseEntity<?> getLoginLog() {
//        return adminService.getLoginLog();
//    }
//
//    // ✔ 관리자 활동 로그 조회
//    @GetMapping("/logs/admin")
//    public ResponseEntity<?> getAdminLog() {
//        return adminService.getAdminLog();
//    }
//
//    // ✔ 관리자 대시보드 통계
//    @GetMapping("/dashboard")
//    public ResponseEntity<?> dashboard() {
//        return adminService.dashboard();
//    }
}
