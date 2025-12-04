// src/components/shared/KosdaqLineChart.jsx
import React, { useEffect, useState } from "react";
import axios from "axios";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
} from "recharts";

// -----------------------------------------------------
// 🌟 커스텀 포맷터 함수들 정의
// -----------------------------------------------------

// 1. 날짜 레이블 포맷터: YYYYMMDD -> YYYY-MM-DD
const dateLabelFormatter = (label) => {
    if (typeof label === 'string' && label.length === 8) {
        const year = label.substring(0, 4);
        const month = label.substring(4, 6);
        const day = label.substring(6, 8);
        return `${year}-${month}-${day}`; // yyyy-MM-dd 형식
    }
    return label;
};

// 2. 값 포맷터: value -> "종가 : [포맷된 값]"
const valueFormatter = (value) => {
    if (value === undefined || value === null) {
        // Tooltip에 표시될 [값, 이름] 배열을 반환
        return ["-", "종가"]; 
    }
    
    // 값에 천 단위 구분 기호와 소수점 자릿수를 적용
    const formattedValue = Number(value).toLocaleString('ko-KR', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
    
    // [값, 이름] 형태로 반환하며, 이름은 "종가"로 표시
    return [formattedValue, "종가"]; 
};


function KosdaqLineChart() {
  const [data, setData] = useState([]);

  useEffect(() => {
    axios.get("http://localhost:8484/api/chart/kosdaq-history")
      .then((res) => {
        const mapped = res.data.map(item => ({
          date: item.basDt,   // 날짜
          value: item.clpr    // 종가
        }));
        setData(mapped);
      })
      .catch(err => console.error("KOSDAQ 데이터 로드 실패:", err));
  }, []);

  return (
    <ResponsiveContainer width="100%" height={180}>
      <LineChart data={data}>
        <XAxis dataKey="date" hide />
        <YAxis hide />
        
        {/* 🌟 Tooltip 수정: labelFormatter와 formatter 적용 */}
        <Tooltip 
            labelFormatter={dateLabelFormatter} 
            formatter={valueFormatter}
        />
        
        <Line
          type="monotone"
          dataKey="value"
          stroke="#1e88e5"   // 코스닥은 파란 계열
          strokeWidth={2}
          dot={false}
        />
      </LineChart>
    </ResponsiveContainer>
  );
}

export default KosdaqLineChart;