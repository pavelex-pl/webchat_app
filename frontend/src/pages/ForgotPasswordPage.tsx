import { FormEvent, useState } from "react";
import { Link } from "react-router-dom";
import FormError from "../components/FormError";
import { ApiError, api } from "../lib/api";

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [done, setDone] = useState(false);
  const [token, setToken] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const res = await api.post<{ token: string | null }>("/api/auth/password-reset/request", { email });
      setToken(res.token ?? null);
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
        <h1 className="text-2xl font-semibold text-slate-800">Forgot password</h1>
        {done ? (
          <div className="space-y-2 text-sm text-slate-600">
            <p>This build does not send emails, so the reset link is shown here directly.</p>
            {token ? (
              <>
                <p>Open this URL to set a new password:</p>
                <Link
                  to={`/reset-password?token=${encodeURIComponent(token)}`}
                  className="block font-mono text-xs text-blue-700 break-all hover:underline">
                  {`${window.location.origin}/reset-password?token=${token}`}
                </Link>
              </>
            ) : (
              <p>If that email is registered, a reset link was issued.</p>
            )}
          </div>
        ) : (
          <>
            <FormError error={error} />
            <label className="block text-sm">
              <span className="text-slate-700">Email</span>
              <input type="email" required value={email} onChange={(e) => setEmail(e.target.value)}
                     className="mt-1 w-full border rounded px-3 py-2" />
            </label>
            <button disabled={loading} className="w-full bg-slate-800 text-white py-2 rounded disabled:opacity-50">
              {loading ? "Sending..." : "Send reset link"}
            </button>
          </>
        )}
        <div className="text-sm text-center">
          <Link to="/login" className="text-slate-600 hover:underline">Back to sign in</Link>
        </div>
      </form>
    </div>
  );
}
