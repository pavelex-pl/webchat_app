import { FormEvent, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import FormError from "../components/FormError";
import { ApiError, api } from "../lib/api";
import { Me, useAuthStore } from "../stores/authStore";

export default function RegisterPage() {
  const [email, setEmail] = useState("");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const setMe = useAuthStore((s) => s.setMe);
  const nav = useNavigate();

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (password !== confirm) {
      setError("Passwords do not match");
      return;
    }
    setError(null);
    setLoading(true);
    try {
      await api.post("/api/auth/register", { email, username, password });
      const me = await api.post<Me>("/api/auth/login", { email, password, keepSignedIn: true });
      setMe(me);
      nav("/", { replace: true });
    } catch (e) {
      setError(e instanceof ApiError ? e.detail : String(e));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center p-6">
      <form onSubmit={submit} className="bg-white rounded-lg shadow p-6 w-full max-w-sm space-y-4">
        <h1 className="text-2xl font-semibold text-slate-800">Create account</h1>
        <FormError error={error} />
        <label className="block text-sm">
          <span className="text-slate-700">Email</span>
          <input type="email" required value={email} onChange={(e) => setEmail(e.target.value)}
                 className="mt-1 w-full border rounded px-3 py-2" autoComplete="email" />
        </label>
        <label className="block text-sm">
          <span className="text-slate-700">Username</span>
          <input required minLength={3} maxLength={32} pattern="[a-zA-Z0-9_.\-]+"
                 value={username} onChange={(e) => setUsername(e.target.value)}
                 className="mt-1 w-full border rounded px-3 py-2" autoComplete="username" />
          <span className="text-xs text-slate-500">3–32 chars; letters, digits, _ . -</span>
        </label>
        <label className="block text-sm">
          <span className="text-slate-700">Password</span>
          <input type="password" required minLength={12} value={password}
                 onChange={(e) => setPassword(e.target.value)}
                 className="mt-1 w-full border rounded px-3 py-2" autoComplete="new-password" />
          <span className="text-xs text-slate-500">Min 12 chars, incl. upper, lower, digit, special.</span>
        </label>
        <label className="block text-sm">
          <span className="text-slate-700">Confirm password</span>
          <input type="password" required value={confirm} onChange={(e) => setConfirm(e.target.value)}
                 className="mt-1 w-full border rounded px-3 py-2" autoComplete="new-password" />
        </label>
        <button disabled={loading} className="w-full bg-slate-800 text-white py-2 rounded disabled:opacity-50">
          {loading ? "Creating..." : "Create account"}
        </button>
        <div className="text-sm text-center">
          <Link to="/login" className="text-slate-600 hover:underline">Back to sign in</Link>
        </div>
      </form>
    </div>
  );
}
