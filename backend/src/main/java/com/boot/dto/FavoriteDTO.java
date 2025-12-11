package com.boot.dto;
import lombok.Data;

@Data
public class FavoriteDTO {
    private String email;
    private String stockCode;
    private Long newsId;
    
    // 조인을 통해 가져올 추가 정보들 (화면에 보여줄 것)
    private String stockName;
    private Double price;
    private Double changeRate;
    
    private String newsTitle;
    private String newsUrl;
    private String newsDate;
    
    private String isRead; // 'Y' 또는 'N' 값을 담을 필드 추가
    
    // 메모 필드
    private String memo;
    
//    업종 정보 필드
    private String industry;
}