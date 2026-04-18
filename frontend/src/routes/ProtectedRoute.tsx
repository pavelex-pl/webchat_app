import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuthStore } from "../stores/authStore";

export default function ProtectedRoute() {
  const me = useAuthStore((s) => s.me);
  const bootstrapped = useAuthStore((s) => s.bootstrapped);
  const location = useLocation();

  if (!bootstrapped) {
    return <div className="p-6 text-sm text-slate-500">Loading...</div>;
  }
  if (!me) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }
  return <Outlet />;
}
