import { useEffect } from "react";
import { BrowserRouter, Route, Routes } from "react-router-dom";
import AppLayout from "../components/AppLayout";
import { api } from "../lib/api";
import { ws } from "../lib/ws";
import { Me, useAuthStore } from "../stores/authStore";
import ChatPage from "../pages/ChatPage";
import ForgotPasswordPage from "../pages/ForgotPasswordPage";
import FriendsPage from "../pages/FriendsPage";
import HomePage from "../pages/HomePage";
import InvitationsPage from "../pages/InvitationsPage";
import LoginPage from "../pages/LoginPage";
import ProfilePage from "../pages/ProfilePage";
import PublicRoomsPage from "../pages/PublicRoomsPage";
import RegisterPage from "../pages/RegisterPage";
import ResetPasswordPage from "../pages/ResetPasswordPage";
import SessionsPage from "../pages/SessionsPage";
import ProtectedRoute from "./ProtectedRoute";

export default function AppRouter() {
  const me = useAuthStore((s) => s.me);
  const setMe = useAuthStore((s) => s.setMe);
  const setBootstrapped = useAuthStore((s) => s.setBootstrapped);

  useEffect(() => {
    api.get<Me>("/api/auth/me")
      .then(setMe)
      .catch(() => setMe(null))
      .finally(() => setBootstrapped(true));
  }, [setMe, setBootstrapped]);

  useEffect(() => {
    if (me) ws.connect();
    else ws.disconnect();
    return () => ws.disconnect();
  }, [me]);

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/forgot-password" element={<ForgotPasswordPage />} />
        <Route path="/reset-password" element={<ResetPasswordPage />} />
        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>
            <Route path="/" element={<HomePage />} />
            <Route path="/rooms/public" element={<PublicRoomsPage />} />
            <Route path="/invitations" element={<InvitationsPage />} />
            <Route path="/friends" element={<FriendsPage />} />
            <Route path="/chat/:id" element={<ChatPage />} />
          </Route>
          <Route path="/sessions" element={<SessionsPage />} />
          <Route path="/profile" element={<ProfilePage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
