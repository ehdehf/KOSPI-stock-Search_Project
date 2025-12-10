// src/admin/layout/SidebarMenu.jsx

import React from "react";
import { Menu } from "antd";
import {
  DashboardOutlined,
  UserOutlined,
  LockOutlined,
  DatabaseOutlined,
  SettingOutlined,
} from "@ant-design/icons";
import { useNavigate } from "react-router-dom";
import "./SidebarMenu.css"; // CSS 추가할 예정

export default function SidebarMenu() {
  const navigate = useNavigate();

  const menuItems = [
    {
      key: "dashboard",
      icon: <DashboardOutlined />,
      label: "대시보드",
      onClick: () => navigate("/admin/dashboard"),
    },
    {
      key: "users",
      icon: <UserOutlined />,
      label: "사용자 관리",
      onClick: () => navigate("/admin/users"),
    },
    {
      key: "tokens",
      icon: <LockOutlined />,
      label: "토큰 관리",
      onClick: () => navigate("/admin/tokens"),
    },
    {
      type: "group",
      label: "로그 조회",
      children: [
        {
          key: "logs-login",
          icon: <DatabaseOutlined />,
          label: "로그인 로그",
          onClick: () => navigate("/admin/logs/login"),
        },
        {
          key: "logs-admin",
          icon: <DatabaseOutlined />,
          label: "관리자 작업 로그",
          onClick: () => navigate("/admin/logs/admin"),
        },
      ],
    },
    {
      key: "settings",
      icon: <SettingOutlined />,
      label: "설정",
      onClick: () => navigate("/admin/settings"),
    },
  ];

  return (
    <div className="sidebar-container">
      {/* 🔷 로고 영역 */}
      <div className="admin-logo">
        <span>K-Stock Admin</span>
      </div>

      <Menu theme="dark" mode="inline" items={menuItems} />
    </div>
  );
}
