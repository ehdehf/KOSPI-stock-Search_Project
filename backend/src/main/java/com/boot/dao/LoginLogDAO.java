package com.boot.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface LoginLogDAO {

    // 로그인 로그 저장
    void insertLog(
            @Param("email") String email,
            @Param("status") String status,
            @Param("ip") String ip,
            @Param("userAgent") String userAgent
    );

    // 로그인 로그 전체 조회
    List<Map<String, Object>> getLoginLog();
}
