import axiosInstance from "./adminAxios";

const adminApi = {
  getDashboard(days = 7, newsLimit = 5) {
    return axiosInstance.get(`/admin/dashboard?days=${days}&newsLimit=${newsLimit}`);
  },

  getUsers: () => axiosInstance.get("/admin/users"),

  suspendUser: (data) => axiosInstance.put("/admin/user/suspend", data),

  unsuspendUser: (email) =>
    axiosInstance.put(`/admin/user/unsuspend?email=${email}`),

  changeUserRole: (data) =>
    axiosInstance.put("/admin/user/role", data),

  // ✅ 수정됨 — reset-login-fail
  resetLoginFail: (email) =>
    axiosInstance.put(`/admin/user/reset-fail?email=${email}`),

  forceLogout: (email) =>
    axiosInstance.put(`/admin/user/logout?email=${email}`),

  getTokens: () => axiosInstance.get("/admin/tokens"),

  // 🔥 특정 사용자 Refresh Token 삭제
  deleteUserToken: (email) =>
    axiosInstance.delete("/admin/tokens", {
      params: { email }
    }),

  // 🔥 전체 Refresh Token 초기화
  clearAllTokens: () =>
    axiosInstance.delete("/admin/tokens/all"),

  getLoginLog: () => axiosInstance.get("/admin/logs/login"),

  getAdminLog: () => axiosInstance.get("/admin/logs/admin"),

  getLockedUsers: () => axiosInstance.get("/admin/security/locked-users"),
  getRapidFailAccounts: () => axiosInstance.get("/admin/security/rapid-fail"),
  getRiskyIpAccounts: () => axiosInstance.get("/admin/security/risky-ip"),
  getNewsRefreshStatus: () => axiosInstance.get("/admin/news/refresh-status"),
};

export default adminApi;
