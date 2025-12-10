// src/App.jsx
import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
// 🔴 경로: src 폴더 내의 layouts 폴더
import MainLayout from './layouts/MainLayout'; 
import FavoritesPage from './pages/FavoritesPage';
// 🔴 경로: src 폴더 내의 pages 폴더
import HomePage from './pages/HomePage';
import SearchResultPage from './pages/SearchResultPage';
import StockDetailPage from './pages/StockDetailPage';
import DashboardPage from './pages/DashboardPage';
import KeywordTrendPage from './pages/KeywordTrendPage';
// 🔴 경로: src 폴더 내의 styles 폴더
import GlobalStyles from './styles/GlobalStyles';
import MarketCapPage from './pages/MarketCapPage'; // ⬅️ 임포트 추가
import { AuthProvider } from './context/AuthContext';

import LoginPage from './pages/Login';
import Signup from './pages/Signup';
import FindPw from './pages/find_pw';
import VerifyPage from './pages/VerifyPage';

import TestStockDetailPage from './pages/TestStockDetailPage';

import FindPasswordPage from './pages/FindPasswordPage';
import ResetPasswordPage from './pages/ResetPasswordPage';

import SocialLoginCallback from './pages/SocialLoginCallback';
import MyPage from './pages/MyPage';
import MobileApprovePage from './pages/MobileApprovePage';

import StockTest from "./pages/StockTest";

//관리자 부분 추가
import AdminLayout from "./admin/layout/AdminLayout";
import AdminRoute from "./routes/AdminRoute";
import Dashboard from "./admin/pages/Dashboard";
import Users from "./admin/pages/Users";
import Tokens from "./admin/pages/Tokens";
import LogsLogin from "./admin/pages/LogsLogin";
import LogsAdmin from "./admin/pages/LogsAdmin";
import Settings from "./admin/pages/Settings";

function App() {
  return (
    <BrowserRouter>
      <GlobalStyles />
      <AuthProvider>
      <Routes>
          <Route path="/" element={<MainLayout />}>
            <Route index element={<HomePage />} />
            <Route path="search/:keyword" element={<SearchResultPage />} />
            <Route path="stock/:stockCode" element={<StockDetailPage />} />
            <Route path="dashboard" element={<DashboardPage />} />
            <Route path="trend" element={<KeywordTrendPage />} />
            <Route path="marketcap" element={<MarketCapPage />} />
            <Route path="favorites" element={<FavoritesPage />} /> {/* ⬅️ FavoritesPage 라우트 */}
            <Route path="mypage" element={<MyPage />} />
            <Route path="login" element={<LoginPage />} />
            <Route path="signup" element={<Signup />} />
            <Route path="findpw" element={<FindPw />} />
            <Route path="search" element={<SearchResultPage />} />
            <Route path="verify" element={<VerifyPage />} />
            <Route path="/chart/:code" element={<TestStockDetailPage />} />
            <Route path="find-pw" element={<FindPasswordPage />} />
            <Route path="reset-password" element={<ResetPasswordPage />} />
            <Route path="oauth/callback/kakao" element={<SocialLoginCallback provider="kakao" />} />
            <Route path="oauth/callback/naver" element={<SocialLoginCallback provider="naver" />} />
            <Route path="oauth/callback/google" element={<SocialLoginCallback provider="google" />} />
            <Route path="mobile-approve" element={<MobileApprovePage />} />
            <Route path="/stock/test/:code" element={<StockTest />} />
          </Route>
          {/* 관리자 부분 추가 */}
          <Route path="/admin" element={ <AdminRoute> <AdminLayout /> </AdminRoute> }>
            <Route path="dashboard" element={<Dashboard />} />
            <Route path="users" element={<Users />} />
            <Route path="tokens" element={<Tokens />} />
            <Route path="logs/login" element={<LogsLogin />} />
            <Route path="logs/admin" element={<LogsAdmin />} />
            <Route path="settings" element={<Settings />} />
        </Route>
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;