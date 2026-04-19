import { FormEvent, useState } from "react";
import { Link } from "react-router-dom";
import FormError from "../components/FormError";
import { ApiError, api } from "../lib/api";

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [done, setDone] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await api.post("/api/auth/password-reset/request", { email });
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
            <p>
              Reset link issued. This build does not send emails — check the backend logs for the reset URL.
            </p>
            <p className="text-xs text-slate-500 font-mono">
              docker compose logs backend | grep "PASSWORD RESET LINK"
            </p>
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
