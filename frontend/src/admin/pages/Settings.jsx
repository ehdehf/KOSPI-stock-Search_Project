import React, { useEffect, useState } from "react";
import { Card, Row, Col, Button, Tag, Statistic, Divider, message } from "antd";
import {
  ReloadOutlined,
  CheckCircleOutlined,
  WarningOutlined,
  CloseCircleOutlined,
} from "@ant-design/icons";

import adminApi from "../api/adminApi";

export default function SettingsPage() {
  const [status, setStatus] = useState(null);
  const [loading, setLoading] = useState(false);

  // 상태 조회
  const loadStatus = async () => {
    try {
      setLoading(true);
      const res = await adminApi.getNewsRefreshStatus();
      setStatus(res.data);
    } catch (e) {
      message.error("뉴스 상태 조회 실패");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadStatus();
  }, []);

  // 상태별 아이콘 / 색상
  const renderStatus = () => {
    if (!status) return null;

    switch (status.status) {
      case "NORMAL":
        return <Tag color="green" icon={<CheckCircleOutlined />}>NORMAL</Tag>;
      case "DELAY":
        return <Tag color="orange" icon={<WarningOutlined />}>DELAY</Tag>;
      default:
        return <Tag color="red" icon={<CloseCircleOutlined />}>FAIL</Tag>;
    }
  };

  return (
    <div style={{ padding: 24 }}>
      <h2 style={{ fontWeight: 700 }}>⚙ 관리자 설정</h2>
      <p style={{ color: "#666", marginBottom: 24 }}>
        관리자 전용 시스템 모니터링 화면입니다.
      </p>

      {/* ========================= */}
      {/* 1️⃣ 요약 통계 카드 */}
      {/* ========================= */}
      <Row gutter={16}>
        <Col span={8}>
          <Card>
            <Statistic
              title="총 뉴스 수"
              value={status?.totalNews ?? "-"}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic
              title="최근 1시간 뉴스"
              value={status?.recentHourCount ?? "-"}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic
              title="감성 분석 미완료"
              value={status?.noSentimentCount ?? "-"}
            />
          </Card>
        </Col>
      </Row>

      <Divider />

      {/* ========================= */}
      {/* 2️⃣ 뉴스 수집 상태 */}
      {/* ========================= */}
      <Card
        title="📰 뉴스 수집 상태"
        extra={
          <Button
            icon={<ReloadOutlined />}
            onClick={loadStatus}
            loading={loading}
          >
            상태 다시 확인
          </Button>
        }
        style={{ marginBottom: 24 }}
      >
        <Row gutter={16}>
          <Col span={12}>
            <p><b>상태</b> : {renderStatus()}</p>
            <p><b>마지막 수집 시각</b> : {status?.lastCreatedAt || "-"}</p>
            <p><b>지연 시간</b> : {status?.delayMinutes ?? "-"} 분</p>
          </Col>

          <Col span={12}>
            <Card
              size="small"
              style={{
                background: "#fafafa",
                borderLeft: "4px solid #faad14",
              }}
            >
              <b>관리자 안내</b>
              <p style={{ marginTop: 8 }}>
                {status?.message ||
                  "뉴스 수집 상태를 확인할 수 없습니다."}
              </p>
              <p style={{ fontSize: 12, color: "#888" }}>
                ※ 뉴스 수집은 Python 스케줄러에 의해 자동 수행됩니다.
              </p>
            </Card>
          </Col>
        </Row>
      </Card>

      {/* ========================= */}
      {/* 3️⃣ 시스템 상태 요약 */}
      {/* ========================= */}
      <Card title="🛠 시스템 상태 요약">
        <ul style={{ paddingLeft: 20 }}>
          <li>사용자 인증 시스템: 정상</li>
          <li>관리자 로그 수집: 정상</li>
          <li>
            뉴스 수집 상태:{" "}
            <b style={{ color: status?.status === "NORMAL" ? "green" : "red" }}>
              {status?.status ?? "UNKNOWN"}
            </b>
          </li>
        </ul>
      </Card>
    </div>
  );
}
