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

function KosdaqLineChart() {
  const [data, setData] = useState([]);

  useEffect(() => {
    axios.get("http://localhost:8484/api/chart/kosdaq-history")
      .then((res) => {
        const mapped = res.data.map(item => ({
          date: item.basDt,   // 날짜
          value: item.clpr    // 종가
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
        <Tooltip />
        <Line
          type="monotone"
          dataKey="value"
          stroke="#1e88e5"   // 코스닥은 파란 계열
          strokeWidth={2}
          dot={false}
        />
      </LineChart>
    </ResponsiveContainer>
  );
}

export default KosdaqLineChart;
