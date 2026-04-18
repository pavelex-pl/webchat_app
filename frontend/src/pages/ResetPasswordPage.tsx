import { FormEvent, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import FormError from "../components/FormError";
import { ApiError, api } from "../lib/api";

export default function ResetPasswordPage() {
  const [params] = useSearchParams();
  const [token, setToken] = useState(params.get("token") ?? "");
  const [password, setPassword] = useState("");
  const [confirm, setConfirm] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [done, setDone] = useState(false);
  const [loading, setLoading] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (password !== confirm) {
      setError("Passwords do not match");
      return;
    }
    setError(null);
    setLoading(true);
    try {
      await api.post("/api/auth/password-reset/confirm", { token, newPassword: password });
      setDone(true);
    } catch (e) {
      setError(e instanceof ApiError ? e.detail : String(e));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center p-6">
      <form onSubmit={submit} className="bg-white rounded-lg shadow p-6 w-full max-w-sm space-y-4">
        <h1 className="text-2xl font-semibold text-slate-800">Reset password</h1>
        {done ? (
          <p className="text-sm text-slate-600">Password updated. <Link to="/login" className="underline">Sign in</Link>.</p>
        ) : (
          <>
            <FormError error={error} />
            <label className="block text-sm">
              <span className="text-slate-700">Token</span>
              <input required value={token} onChange={(e) => setToken(e.target.value)}
                     className="mt-1 w-full border rounded px-3 py-2 font-mono text-xs" />
            </label>
            <label className="block text-sm">
              <span className="text-slate-700">New password</span>
              <input type="password" required minLength={12} value={password}
                     onChange={(e) => setPassword(e.target.value)}
                     className="mt-1 w-full border rounded px-3 py-2" />
            </label>
            <label className="block text-sm">
              <span className="text-slate-700">Confirm new password</span>
              <input type="password" required value={confirm} onChange={(e) => setConfirm(e.target.value)}
                     className="mt-1 w-full border rounded px-3 py-2" />
            </label>
            <button disabled={loading} className="w-full bg-slate-800 text-white py-2 rounded disabled:opacity-50">
              {loading ? "Saving..." : "Save"}
            </button>
          </>
        )}
      </form>
    </div>
  );
}
