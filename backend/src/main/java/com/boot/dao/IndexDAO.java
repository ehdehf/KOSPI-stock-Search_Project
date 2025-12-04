package com.boot.dao;

import com.boot.dto.IndexDataDTO;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface IndexDAO {

    // 🌟 KOSPI 지수 데이터 삽입 또는 업데이트 (Merge Into)
    void insertOrUpdateIndexData(IndexDataDTO dto);
    
    // 🌟 차트 출력을 위한 전체 히스토리 조회
    List<IndexDataDTO> selectKospiHistory();
    
    // KOSPI 데이터 개수 카운트 (초기 로딩 스킵 여부 판단용)
    int countIndexData(String idxNm);
    
    // KOSDAQ
    void insertOrUpdateKosdaqIndexData(com.boot.dto.IndexDataDTO dto);
    List<com.boot.dto.IndexDataDTO> selectKosdaqHistory();
    int countKosdaqIndexData(String idxNm);

}