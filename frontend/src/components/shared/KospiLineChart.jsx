// src/components/shared/KospiLineChart.jsx
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

function KospiLineChart() {
  const [data, setData] = useState([]);

  useEffect(() => {
    axios.get("http://localhost:8484/api/chart/kospi-history")
      .then((res) => {
        const mapped = res.data.map(item => ({
          date: item.basDt,
          value: item.clpr
        }));
        setData(mapped);
      });
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
          stroke="#3f51b5"
          strokeWidth={2}
          dot={false}
        />
      </LineChart>
    </ResponsiveContainer>
  );
}

export default KospiLineChart;
