// src/admin/pages/Dashboard.jsx
import React, { useEffect, useState } from "react";
import { Card, Row, Col } from "antd";
import AdminApi from "../api/adminApi";

import {
  Line,
  Pie,
  Bar
} from "@ant-design/plots";

export default function Dashboard() {
  const [data, setData] = useState(null);

  // 📌 API 호출
  useEffect(() => {
    AdminApi.getDashboard(7, 5)
      .then(res => setData(res.data))
      .catch(err => console.error(err));
  }, []);

  if (!data) return <div>Loading...</div>;

  const { summary, dailyJoins, loginStats, topNewsStocks } = data;

  return (
    <div style={{ padding: "24px" }}>
      <h2>관리자 대시보드</h2>

      {/* 🔹 1) Summary Cards */}
      <Row gutter={16} style={{ marginTop: 20 }}>
        <Col span={4}>
          <Card title="총 사용자">{summary.totalUsers}</Card>
        </Col>
        <Col span={4}>
          <Card title="활성 사용자">{summary.activeUsers}</Card>
        </Col>
        <Col span={4}>
          <Card title="정지 사용자">{summary.suspendedUsers}</Card>
        </Col>
        <Col span={4}>
          <Card title="미인증 사용자">{summary.waitingVerifyUsers}</Card>
        </Col>
        <Col span={4}>
          <Card title="위험 사용자">{summary.dangerUsers}</Card>
        </Col>
        <Col span={4}>
          <Card title="뉴스 수">{summary.totalNews}</Card>
        </Col>
      </Row>

      {/* 🔹 2) Line Chart: 최근 7일 가입자 */}
      <Card title="최근 7일 가입자 수" style={{ marginTop: 30 }}>
        <Line
          data={dailyJoins}
          xField="joinDate"
          yField="count"
          smooth
          height={250}
        />
      </Card>

      {/* 🔹 3) Pie Chart: 로그인 성공/실패 */}
      <Card title="로그인 성공/실패 비율" style={{ marginTop: 30 }}>
        <Pie
          data={loginStats}
          angleField="count"
          colorField="status"
          radius={0.8}
        />
      </Card>

      {/* 🔹 4) Bar Chart: 뉴스 많은 종목 TOP 5 */}
      <Card title="뉴스 많은 종목 Top 5" style={{ marginTop: 30 }}>
        <Bar
          data={topNewsStocks}
          xField="newsCount"
          yField="stockName"
          height={300}
        />
      </Card>
    </div>
  );
}
