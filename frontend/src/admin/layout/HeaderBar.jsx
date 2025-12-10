import React from "react";
import { Dropdown, Menu, Button } from "antd";
import { UserOutlined, LogoutOutlined } from "@ant-design/icons";
import { useAuth } from "../../context/AuthContext";

export default function HeaderBar() {
  const { user, logout } = useAuth();

  const menu = (
    <Menu>
      <Menu.Item key="logout" icon={<LogoutOutlined />} onClick={logout}>
        로그아웃
      </Menu.Item>
    </Menu>
  );

  return (
    <Dropdown overlay={menu} trigger={["click"]}>
      <Button type="text" icon={<UserOutlined />}>
        {user?.fullName || "관리자"} 님
      </Button>
    </Dropdown>
  );
}
