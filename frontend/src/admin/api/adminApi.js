import axiosInstance from "./axiosInstance";

const adminApi = {
  getDashboard: () => axiosInstance.get("/admin/dashboard"),
  getUsers: () => axiosInstance.get("/admin/users"),
  getTokens: () => axiosInstance.get("/admin/tokens"),
  getLoginLog: () => axiosInstance.get("/admin/logs/login"),
  getAdminLog: () => axiosInstance.get("/admin/logs/admin"),
};

export default adminApi;
