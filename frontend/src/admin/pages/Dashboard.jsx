// src/admin/pages/Dashboard.jsx

import React, { useEffect, useState } from "react";
import {
  Card,
  Row,
  Col,
  List,
  Tag,
  Space,
  Modal,   // ✅ 추가
  Table,   // ✅ 추가
} from "antd";

import { Line, Pie, Bar } from "@ant-design/plots";

import {
  UserOutlined,
  CheckCircleOutlined,
  StopOutlined,
  MailOutlined,
  FileTextOutlined,
} from "@ant-design/icons";

import adminApi from "../api/adminApi";

// ======================================================
// 메인 Dashboard 컴포넌트
// ======================================================
export default function Dashboard() {
  const [data, setData] = useState(null);
  const [adminLogs, setAdminLogs] = useState([]);

  // ✅ (추가) 보안 계정 조회 모달 상태
  const [securityModalOpen, setSecurityModalOpen] = useState(false);
  const [securityModalTitle, setSecurityModalTitle] = useState("");
  const [securityAccounts, setSecurityAccounts] = useState([]);

  // ---------------------------------------------------------
  // 데이터 로드
  // ---------------------------------------------------------
  useEffect(() => {
    const loadAll = async () => {
      try {
        const [dashRes, logRes] = await Promise.all([
          adminApi.getDashboard(7, 5),
          adminApi.getAdminLog(),
        ]);

        setData(dashRes.data || {});
        setAdminLogs((logRes.data || []).slice(0, 5));
      } catch (err) {
        console.error(err);
      }
    };
    loadAll();
  }, []);

  if (!data) return <div style={{ padding: 24 }}>Loading...</div>;

  const {
    summary = {},
    dailyJoins = [],
    loginStats = [],
    topNewsStocks = [],
    securityStats = {}, // 🔥 보안 통계 데이터
  } = data;

  // ✅ (추가) 보안 카드 클릭 → 계정 리스트 조회 후 모달 오픈
  const openSecurityAccounts = async (type) => {
    try {
      let res;

      if (type === "RISKY_IP") {
        setSecurityModalTitle("🚨 위험 IP 대상 계정");
        res = await adminApi.getRiskyIpAccounts();
      } else if (type === "RAPID_FAIL") {
        setSecurityModalTitle("⚠ Rapid Fail 의심 계정");
        res = await adminApi.getRapidFailAccounts();
      } else if (type === "LOCKED") {
        setSecurityModalTitle("🔒 잠금된 계정");
        res = await adminApi.getLockedUsers();
      }

      setSecurityAccounts(res?.data || []);
      setSecurityModalOpen(true);
    } catch (e) {
      console.error(e);
    }
  };

  // ✅ (추가) 모달 테이블 컬럼
  const securityColumns = [
    { title: "이메일", dataIndex: "EMAIL", key: "EMAIL" },
    { title: "IP", dataIndex: "IP_ADDRESS", key: "IP_ADDRESS", render: (v) => v || "-" },
    { title: "실패횟수", dataIndex: "FAIL_COUNT", key: "FAIL_COUNT", render: (v) => v ?? "-" },
    { title: "잠금해제", dataIndex: "LOCK_UNTIL", key: "LOCK_UNTIL", render: (v) => v || "-" },
  ];

  // ======================================================
  // Summary 카드 구성
  // ======================================================
  const summaryCards = [
    {
      title: "총 사용자",
      value: summary.totalUsers,
      color: "#2563eb",
      icon: <UserOutlined style={{ fontSize: 20 }} />,
    },
    {
      title: "활성 사용자",
      value: summary.activeUsers,
      color: "#16a34a",
      icon: <CheckCircleOutlined style={{ fontSize: 20 }} />,
    },
    {
      title: "정지 사용자",
      value: summary.suspendedUsers,
      color: "#dc2626",
      icon: <StopOutlined style={{ fontSize: 20 }} />,
    },
    {
      title: "미인증 사용자",
      value: summary.waitingVerifyUsers,
      color: "#f59e0b",
      icon: <MailOutlined style={{ fontSize: 20 }} />,
    },
    {
      title: "등록된 종목 수",
      value: summary.totalStocks,
      color: "#7c3aed",
      icon: <FileTextOutlined style={{ fontSize: 20 }} />,
    },
    {
      title: "전체 뉴스 수",
      value: summary.totalNews,
      color: "#6366f1",
      icon: <FileTextOutlined style={{ fontSize: 20 }} />,
    },
  ];

  // ======================================================
  // 차트 설정
  // ======================================================

  // 최근 가입자 그래프
  const lineConfig = {
    data: dailyJoins,
    xField: "joinDate",
    yField: "count",
    smooth: true,
    height: 260,
    point: { size: 4 },
    areaStyle: { fill: "rgba(37,99,235,0.25)" },
  };

  // 로그인 성공/실패 비율
  const pieConfig = {
    data: loginStats,
    angleField: "count",
    colorField: "status",
    radius: 0.8,
    innerRadius: 0.6,
    label: {
      type: "inner",
      content: "{count}",
      style: { fontSize: 14, fontWeight: "bold" },
    },
  };

  // 뉴스 많은 종목 TOP 5
  const barConfig = {
    data: topNewsStocks,
    xField: "newsCount",
    yField: "stockName",
    height: 300,
    label: { position: "right" },
    barStyle: { fill: "#6366f1" },
  };

  // 관리자 로그 Action 매핑
  const actionLabel = (action) => {
    switch (action) {
      case "CLEAR_TOKENS":
        return "전체 토큰 초기화";
      case "TOKEN_DELETE":
        return "개별 토큰 삭제";
      case "RESET_FAIL":
        return "로그인 실패 초기화";
      case "SUSPEND":
        return "계정 정지";
      case "UNSUSPEND":
        return "정지 해제";
      case "ROLE_CHANGE":
        return "권한 변경";
      case "FORCE_LOGOUT":
        return "강제 로그아웃";
      default:
        return action || "기타 작업";
    }
  };

  // ======================================================
  // UI 출력
  // ======================================================

  return (
    <div style={{ padding: 24 }}>
      <h2 style={{ marginBottom: 20, fontWeight: 700 }}>📊 관리자 대시보드</h2>

      {/* --------------------------------------- */}
      {/* 1. Summary 카드 */}
      {/* --------------------------------------- */}
      <Row gutter={[16, 16]}>
        {summaryCards.map((card, idx) => (
          <Col xs={24} sm={12} md={8} lg={8} xl={4} key={idx}>
            <Card
              style={{
                background: card.color,
                color: "white",
                borderRadius: 12,
                minHeight: 120,
                display: "flex",
                flexDirection: "column",
                justifyContent: "space-between",
                padding: 12,
              }}
            >
              <div style={{ fontSize: 16 }}>{card.title}</div>
              <div style={{ fontSize: 30, fontWeight: "bold" }}>{card.value}</div>
              <div>{card.icon}</div>
            </Card>
          </Col>
        ))}
      </Row>

      {/* --------------------------------------- */}
      {/* 🔐 2. 보안 통계(Security Overview) */}
      {/* --------------------------------------- */}
      <div style={{ marginTop: 40 }}>
        <h3 style={{ fontWeight: 700, marginBottom: 16 }}>
          🔐 보안 통계 (Security Overview)
        </h3>

        <Card style={{ borderRadius: 10 }}>
          <Row gutter={16}>
            <Col
              xs={24}
              md={8}
              style={{ cursor: "pointer" }}                         // ✅ 추가
              onClick={() => openSecurityAccounts("RISKY_IP")}       // ✅ 추가
            >
              <div style={{ padding: 12 }}>
                <h4 style={{ marginBottom: 4 }}>🚨 위험 IP 탐지</h4>
                <div style={{ fontSize: 28, fontWeight: "bold", color: "#dc2626" }}>
                  {securityStats.riskyIpCount ?? 0}
                </div>
                <div style={{ fontSize: 12, color: "#666" }}>최근 24시간 기준</div>
              </div>
            </Col>

            <Col
              xs={24}
              md={8}
              style={{ cursor: "pointer" }}                         // ✅ 추가
              onClick={() => openSecurityAccounts("RAPID_FAIL")}     // ✅ 추가
            >
              <div style={{ padding: 12 }}>
                <h4 style={{ marginBottom: 4 }}>⚠ Rapid Fail 탐지</h4>
                <div style={{ fontSize: 28, fontWeight: "bold", color: "#f59e0b" }}>
                  {securityStats.rapidFailAttempts ?? 0}
                </div>
                <div style={{ fontSize: 12, color: "#666" }}>짧은 시간 내 연속 실패</div>
              </div>
            </Col>

            <Col
              xs={24}
              md={8}
              style={{ cursor: "pointer" }}                         // ✅ 추가
              onClick={() => openSecurityAccounts("LOCKED")}         // ✅ 추가
            >
              <div style={{ padding: 12 }}>
                <h4 style={{ marginBottom: 4 }}>🔒 잠금된 계정</h4>
                <div style={{ fontSize: 28, fontWeight: "bold", color: "#2563eb" }}>
                  {securityStats.lockedUsers ?? 0}
                </div>
                <div style={{ fontSize: 12, color: "#666" }}>비밀번호 실패로 자동 잠금</div>
              </div>
            </Col>
          </Row>

          {/* 상세 보기 (기존 유지) */}
          <div style={{ marginTop: 20, textAlign: "right" }}>
            <a
              href="/admin/logs/login"
              style={{
                fontSize: 14,
                color: "#2563eb",
                textDecoration: "underline",
                cursor: "pointer",
              }}
            >
              🔎 보안 이벤트 상세 보기 →
            </a>
          </div>
        </Card>
      </div>

      {/* --------------------------------------- */}
      {/* 3. 가입자 / 로그인 비율 (기존 유지) */}
      {/* --------------------------------------- */}
      <Row gutter={16} style={{ marginTop: 30 }}>
        <Col xs={24} lg={12}>
          <Card title="📈 최근 7일 가입자 수">
            <Line {...lineConfig} />
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Card title="🔑 로그인 성공/실패 비율">
            <Pie {...pieConfig} />
          </Card>
        </Col>
      </Row>

      {/* --------------------------------------- */}
      {/* 4. 뉴스 TOP 5 + 관리자 로그 (기존 유지) */}
      {/* --------------------------------------- */}
      <Row gutter={16} style={{ marginTop: 30 }}>
        <Col xs={24} lg={12}>
          <Card title="📰 뉴스 많은 종목 Top 5">
            <Bar {...barConfig} />
          </Card>
        </Col>

        <Col xs={24} lg={12}>
          <Card title="🛠 최근 관리자 작업 로그 (5건)">
            <List
              dataSource={adminLogs}
              renderItem={(log) => (
                <List.Item>
                  <List.Item.Meta
                    title={
                      <Space>
                        <Tag color="blue">{actionLabel(log.action || log.ACTION)}</Tag>
                        <span>{log.adminEmail || log.ADMIN_EMAIL}</span>
                      </Space>
                    }
                    description={
                      <>
                        <div>시간: {log.createdAt || log.CREATED_AT}</div>
                        <div>대상: {log.targetEmail || log.TARGET_EMAIL}</div>
                        <div style={{ whiteSpace: "pre-line" }}>
                          {log.detail || log.DETAIL}
                        </div>
                      </>
                    }
                  />
                </List.Item>
              )}
            />
          </Card>
        </Col>
      </Row>

      {/* ✅ (추가) 보안 계정 조회 모달 */}
      <Modal
        open={securityModalOpen}
        title={securityModalTitle}
        footer={null}
        width={900}
        onCancel={() => setSecurityModalOpen(false)}
      >
        <Table
          rowKey={(r) => `${r.EMAIL}-${r.IP_ADDRESS || "NONE"}`}
          columns={securityColumns}
          dataSource={securityAccounts}
          pagination={{ pageSize: 6 }}
        />
      </Modal>
    </div>
  );
}
