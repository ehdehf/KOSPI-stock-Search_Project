import React from "react";
import { Layout } from "antd";
import { Outlet } from "react-router-dom";   // ✔ 추가
import SidebarMenu from "./SidebarMenu";
import HeaderBar from "./HeaderBar";

const { Sider, Header, Content } = Layout;

export default function AdminLayout() {
  return (
    <Layout style={{ minHeight: "100vh" }}>
      
      {/* 사이드바 */}
      <Sider
        width={240}
        style={{
          background: "#0E1621",
          borderRight: "1px solid #1f2a3a",
          boxShadow: "2px 0 8px rgba(0,0,0,0.15)"
        }}
      >
        <SidebarMenu />
      </Sider>

      {/* 오른쪽 전체 영역 */}
      <Layout>
        {/* 헤더 */}
        <Header
          style={{
            background: "#fff",
            padding: "0 24px",
            height: "64px",
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            borderBottom: "1px solid #f0f0f0",
            boxShadow: "0 1px 4px rgba(0,0,0,0.1)",
          }}
        >
          <h2 style={{ margin: 0, fontSize: "20px", fontWeight: 600 }}>
            관리자 페이지
          </h2>

          <HeaderBar />
        </Header>

        {/* 컨텐츠 영역 */}
        <Content style={{ padding: "24px", background: "#f5f6fa" }}>
          <div
            style={{
              background: "#fff",
              padding: "24px",
              minHeight: "calc(100vh - 120px)",
              borderRadius: "10px",
              boxShadow: "0 2px 8px rgba(0,0,0,0.05)"
            }}
          >
            <Outlet />  {/* ← 이것이 핵심!! */}
          </div>
        </Content>
      </Layout>
    </Layout>
  );
}
