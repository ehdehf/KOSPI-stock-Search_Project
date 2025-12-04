package com.boot.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IndexDataDTO {
    
    // XML 태그와 DB 컬럼에 매핑 (idxNm -> INDEX_NAME)
    private String idxNm;        // <idxNm>
    private String basDt;        // <basDt> (기준일자 YYYYMMDD)
    private Double clpr;         // <clpr> (종가)
    private Double vs;           // <vs> (전일 대비)
    private Double fltRt;        // <fltRt> (등락률 %)
    private Double mkp;          // <mkp> (시가)
    private Double hipr;         // <hipr> (고가)
    private Double lopr;         // <lopr> (저가)
    private Long trqu;           // <trqu> (거래량)
    private Long trPrc;          // <trPrc> (거래대금)
    private Long lstgMrktTotAmt; // <lstgMrktTotAmt> (시총 합계)
    
    // DB 저장/조회용
    private Date updatedAt;
    
    // API 응답에서 totalCount를 일시적으로 담기 위한 필드
    private Integer totalCount; 
}