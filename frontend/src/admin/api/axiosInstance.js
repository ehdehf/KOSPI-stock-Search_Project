import axios from "axios";

const axiosInstance = axios.create({
  baseURL: "http://localhost:8484/admin",
  withCredentials: true
});

export default axiosInstance; // ⬅ default export 추가
