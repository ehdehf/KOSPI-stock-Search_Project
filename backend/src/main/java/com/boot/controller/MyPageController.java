package com.boot.controller;

import com.boot.dto.FavoriteDTO;
import com.boot.dto.UserInfoDTO;
import com.boot.service.AuthService; // 기존 서비스 활용
import com.boot.dao.UserDAO;         // DAO 직접 호출 (간단한 예시)
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final UserDAO userDAO;
    private final PasswordEncoder passwordEncoder;

    // 1. 마이페이지 진입 시 모든 정보(내정보 + 찜목록) 가져오기
    @GetMapping("/info")
    public ResponseEntity<?> getMyPageInfo(@AuthenticationPrincipal UserDetails user) {
        if (user == null) return ResponseEntity.status(401).body("로그인 필요");
        String email = user.getUsername();

        UserInfoDTO userInfo = userDAO.findByEmail(email);
        List<FavoriteDTO> stocks = userDAO.getFavoriteStocks(email); // DAO 메서드 필요
        List<FavoriteDTO> news = userDAO.getFavoriteNews(email);     // DAO 메서드 필요

        // 민감 정보 제거
        userInfo.setPassword(null);

        Map<String, Object> response = new HashMap<>();
        response.put("user", userInfo);
        response.put("stocks", stocks);
        response.put("news", news);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateInfo(@RequestBody Map<String, String> req, 
                                      @AuthenticationPrincipal UserDetails user) {
        
        String email = user.getUsername();
        String fullName = req.get("fullName");
        String password = req.get("password"); // 프론트에서 빈 값("")으로 올 수 있음

        // [디버깅 로그] 콘솔에서 이 값이 찍히는지 확인하세요!
        System.out.println(">>> 회원 수정 요청: " + email);
        System.out.println(">>> 변경할 이름: " + fullName);

        String encodedPw = null;
        // 비밀번호가 입력되었을 때만 암호화 (입력 안 했으면 null 유지)
        if (password != null && !password.trim().isEmpty()) {
            encodedPw = passwordEncoder.encode(password);
            System.out.println(">>> 비밀번호도 변경함");
        }

        // DB 업데이트 호출
        userDAO.updateUserInfo(email, fullName, encodedPw);
        
        return ResponseEntity.ok("정보가 수정되었습니다.");
    }

    // 3. 탈퇴
    @DeleteMapping("/withdraw")
    public ResponseEntity<?> withdraw(@AuthenticationPrincipal UserDetails user) {
        userDAO.deleteUser(user.getUsername());
        return ResponseEntity.ok("탈퇴 완료");
    }
    
    

    // --- 관심 종목 관리 ---
    
    @PostMapping("/favorites/stock")
    public ResponseEntity<?> addStock(@RequestBody Map<String, String> req, 
                                      @AuthenticationPrincipal UserDetails user) {
        userDAO.addFavoriteStock(user.getUsername(), req.get("stockCode"));
        return ResponseEntity.ok("관심 종목에 추가되었습니다.");
    }

    @DeleteMapping("/favorites/stock/{stockCode}")
    public ResponseEntity<?> removeStock(@PathVariable String stockCode, 
                                         @AuthenticationPrincipal UserDetails user) {
        userDAO.removeFavoriteStock(user.getUsername(), stockCode);
        return ResponseEntity.ok("관심 종목에서 삭제되었습니다.");
    }

    // --- 뉴스 스크랩 관리 ---

 // 1. 목록 조회 (상세페이지 & 마이페이지 공용 사용)
    @GetMapping("/favorites/news")
    public ResponseEntity<List<FavoriteDTO>> getFavoriteNews(@AuthenticationPrincipal UserDetails user) {
        // 원래 있던(마이페이지용) 메서드를 그대로 재사용합니다.
        // 반환 타입: List<Long>이 아니라 List<FavoriteDTO>가 됩니다.
        return ResponseEntity.ok(userDAO.getFavoriteNews(user.getUsername()));
    }

    // 2. 추가
    @PostMapping("/favorites/news")
    public ResponseEntity<?> addNews(@RequestBody Map<String, Long> req, 
                                     @AuthenticationPrincipal UserDetails user) {
        try {
            userDAO.addFavoriteNews(user.getUsername(), req.get("newsId"));
            return ResponseEntity.ok("뉴스를 스크랩했습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("이미 스크랩했거나 오류 발생");
        }
    }

    // 3. 삭제
    @DeleteMapping("/favorites/news/{newsId}")
    public ResponseEntity<?> removeNews(@PathVariable Long newsId, 
                                        @AuthenticationPrincipal UserDetails user) {
        userDAO.removeFavoriteNews(user.getUsername(), newsId);
        return ResponseEntity.ok("스크랩을 취소했습니다.");
    }
    
    // 뉴스 읽음 처리
    @PostMapping("/favorites/news/read")
    public ResponseEntity<?> markNewsAsRead(@RequestBody Map<String, Long> req, 
                                            @AuthenticationPrincipal UserDetails user) {
        try {
            // DAO 메서드 호출 (newsId를 받아서 처리)
            userDAO.markNewsAsRead(user.getUsername(), req.get("newsId"));
            return ResponseEntity.ok("읽음 처리되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("오류 발생");
        }
    }
    
    @PostMapping("/favorites/stock/memo")
    public ResponseEntity<?> updateStockMemo(@RequestBody Map<String, String> req, 
                                             @AuthenticationPrincipal UserDetails user) {
        try {
            userDAO.updateStockMemo(
                user.getUsername(), 
                req.get("stockCode"), 
                req.get("memo")
            );
            return ResponseEntity.ok("종목 메모가 저장되었습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("메모 저장 실패");
        }
    }

    @PostMapping("/favorites/news/memo")
    public ResponseEntity<?> updateNewsMemo(@RequestBody Map<String, Object> req, 
                                            @AuthenticationPrincipal UserDetails user) {
        try {
            // newsId는 JSON에서 숫자로 넘어오므로 형변환에 주의해야 합니다.
            Long newsId = Long.valueOf(String.valueOf(req.get("newsId")));
            String memo = (String) req.get("memo");

            userDAO.updateNewsMemo(user.getUsername(), newsId, memo);
            return ResponseEntity.ok("뉴스 메모가 저장되었습니다.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("메모 저장 실패");
        }
    }
}