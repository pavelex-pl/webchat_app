import { Link, useNavigate } from "react-router-dom";
import { api } from "../lib/api";
import { useAuthStore } from "../stores/authStore";

export default function TopBar() {
  const me = useAuthStore((s) => s.me);
  const setMe = useAuthStore((s) => s.setMe);
  const nav = useNavigate();

  async function logout() {
    try {
      await api.post("/api/auth/logout");
    } finally {
      setMe(null);
      nav("/login");
    }
  }

  return (
    <header className="bg-slate-800 text-slate-100 px-4 py-2 flex items-center justify-between">
      <Link to="/" className="font-semibold">Webchat</Link>
      <nav className="flex items-center gap-4 text-sm">
        {me && (
          <>
            <Link to="/sessions" className="hover:text-white">Sessions</Link>
            <Link to="/profile" className="hover:text-white">Profile</Link>
            <span className="text-slate-400">@{me.username}</span>
            <button onClick={logout} className="hover:text-white">Sign out</button>
          </>
        )}
      </nav>
    </header>
  );
}
