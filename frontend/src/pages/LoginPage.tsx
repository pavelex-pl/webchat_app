import { FormEvent, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import FormError from "../components/FormError";
import { ApiError, api } from "../lib/api";
import { Me, useAuthStore } from "../stores/authStore";

export default function LoginPage() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [keep, setKeep] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const setMe = useAuthStore((s) => s.setMe);
  const nav = useNavigate();
  const location = useLocation() as { state?: { from?: { pathname?: string } } };

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const me = await api.post<Me>("/api/auth/login", { email, password, keepSignedIn: keep });
      setMe(me);
      nav(location.state?.from?.pathname ?? "/", { replace: true });
    } catch (e) {
      setError(e instanceof ApiError ? e.detail : String(e));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center p-6">
      <form onSubmit={submit} className="bg-white rounded-lg shadow p-6 w-full max-w-sm space-y-4">
        <h1 className="text-2xl font-semibold text-slate-800">Sign in</h1>
        <FormError error={error} />
        <label className="block text-sm">
          <span className="text-slate-700">Email</span>
          <input type="email" required value={email} onChange={(e) => setEmail(e.target.value)}
                 className="mt-1 w-full border rounded px-3 py-2" autoComplete="email" />
        </label>
        <label className="block text-sm">
          <span className="text-slate-700">Password</span>
          <input type="password" required value={password} onChange={(e) => setPassword(e.target.value)}
                 className="mt-1 w-full border rounded px-3 py-2" autoComplete="current-password" />
        </label>
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={keep} onChange={(e) => setKeep(e.target.checked)} />
          Keep me signed in
        </label>
        <button disabled={loading} className="w-full bg-slate-800 text-white py-2 rounded disabled:opacity-50">
          {loading ? "Signing in..." : "Sign in"}
        </button>
        <div className="flex justify-between text-sm">
          <Link to="/forgot-password" className="text-slate-600 hover:underline">Forgot password?</Link>
          <Link to="/register" className="text-slate-600 hover:underline">Create account</Link>
        </div>
      </form>
    </div>
  );
}
