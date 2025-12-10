import { Navigate } from "react-router-dom";

export default function AdminRoute({ children }) {
  const storedUser = localStorage.getItem("user");
  const user = storedUser ? JSON.parse(storedUser) : null;

  // 로그인 안 했으면 로그인 페이지로
  if (!user) return <Navigate to="/login" replace />;

  // 권한이 ADMIN이 아니면 접근 차단
  if (user.role !== "ADMIN") {
    alert("관리자만 접근할 수 있습니다.");
    return <Navigate to="/" replace />;
  }

  return children;
}
