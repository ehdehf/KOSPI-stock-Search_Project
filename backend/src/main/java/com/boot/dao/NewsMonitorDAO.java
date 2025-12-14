package com.boot.dao;

import org.apache.ibatis.annotations.Mapper;
import java.util.Map;

@Mapper
public interface NewsMonitorDAO {
    Map<String, Object> getLastNewsCreatedAt();
}
