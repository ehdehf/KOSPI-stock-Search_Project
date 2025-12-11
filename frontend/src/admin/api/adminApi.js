import axiosInstance from "./adminAxios";

const adminApi = {
 getDashboard(days = 7, newsLimit = 5) {
    return axiosInstance.get(`/admin/dashboard?days=${days}&newsLimit=${newsLimit}`);
  },
  getUsers: () => axiosInstance.get("/admin/users"),
  getTokens: () => axiosInstance.get("/admin/tokens"),
  getLoginLog: () => axiosInstance.get("/admin/logs/login"),
  getAdminLog: () => axiosInstance.get("/admin/logs/admin"),
};

export default adminApi;
