package com.boot.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.boot.dto.UserInfoDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface AdminDAO {

    List<UserInfoDTO> getUsers();

    void suspendUser(@Param("email") String email,
                     @Param("until") LocalDateTime until);

    void unsuspendUser(String email);

    void changeRole(@Param("email") String email,
                    @Param("role") String role);

    void resetFail(String email);

    void forceLogout(String email);

    List<Map<String,Object>> getTokens();

    void clearTokens();

    List<Map<String,Object>> getLoginLog();

    List<Map<String,Object>> getAdminLog();

    Map<String,Object> getDashboard();

    void insertAdminLog(@Param("action") String action,
                        @Param("target") String target,
                        @Param("detail") String detail);
}
