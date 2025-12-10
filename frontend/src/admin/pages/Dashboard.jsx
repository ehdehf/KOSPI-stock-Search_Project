import React, { useEffect, useState } from "react";
import { Card, Row, Col, Statistic } from "antd";
import adminApi from "../api/adminApi";

export default function Dashboard() {
  const [data, setData] = useState(null);

  useEffect(() => {
    adminApi.getDashboard().then((res) => setData(res.data));
  }, []);

  if (!data) return <div>Loading...</div>;

  return (
    <Row gutter={20}>
      <Col span={6}>
        <Card bordered={false}>
          <Statistic title="총 사용자 수" value={data.summary.totalUsers} />
        </Card>
      </Col>

      <Col span={6}>
        <Card bordered={false}>
          <Statistic title="활성 사용자" value={data.summary.activeUsers} />
        </Card>
      </Col>

      <Col span={6}>
        <Card bordered={false}>
          <Statistic title="정지된 계정" value={data.summary.suspendedUsers} />
        </Card>
      </Col>

      <Col span={6}>
        <Card bordered={false}>
          <Statistic title="미인증 계정" value={data.summary.waitingVerifyUsers} />
        </Card>
      </Col>
    </Row>
  );
}
