import { FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import TopBar from "../components/TopBar";
import FormError from "../components/FormError";
import { ApiError, api } from "../lib/api";
import { useAuthStore } from "../stores/authStore";

export default function ProfilePage() {
  const me = useAuthStore((s) => s.me);
  const setMe = useAuthStore((s) => s.setMe);
  const nav = useNavigate();

  const [cur, setCur] = useState("");
  const [next, setNext] = useState("");
  const [confirm, setConfirm] = useState("");
  const [pwdMsg, setPwdMsg] = useState<string | null>(null);
  const [pwdErr, setPwdErr] = useState<string | null>(null);

  async function changePassword(e: FormEvent) {
    e.preventDefault();
    setPwdMsg(null);
    setPwdErr(null);
    if (next !== confirm) {
      setPwdErr("Passwords do not match");
      return;
    }
    try {
      await api.post("/api/auth/password-change", { currentPassword: cur, newPassword: next });
      setCur(""); setNext(""); setConfirm("");
      setPwdMsg("Password updated");
    } catch (e) {
      setPwdErr(e instanceof ApiError ? e.detail : String(e));
    }
  }

  const [delPwd, setDelPwd] = useState("");
  const [delErr, setDelErr] = useState<string | null>(null);
  const [confirming, setConfirming] = useState(false);

  async function deleteAccount() {
    setDelErr(null);
    try {
      await api.delete("/api/auth/account", { password: delPwd });
      setMe(null);
      nav("/login", { replace: true });
    } catch (e) {
      setDelErr(e instanceof ApiError ? e.detail : String(e));
    }
  }

  return (
    <div className="min-h-screen bg-slate-50">
      <TopBar />
      <main className="max-w-2xl mx-auto p-6 space-y-6">
        <section className="bg-white rounded shadow p-6 space-y-2">
          <h2 className="text-lg font-semibold text-slate-800">Profile</h2>
          <div className="text-sm">Username: <span className="font-mono">{me?.username}</span></div>
          <div className="text-sm">Email: <span className="font-mono">{me?.email}</span></div>
        </section>

        <section className="bg-white rounded shadow p-6 space-y-3">
          <h2 className="text-lg font-semibold text-slate-800">Change password</h2>
          <FormError error={pwdErr} />
          {pwdMsg && <div className="text-sm text-green-700 bg-green-50 p-2 rounded">{pwdMsg}</div>}
          <form onSubmit={changePassword} className="space-y-3">
            <input type="password" required value={cur} onChange={(e) => setCur(e.target.value)}
                   placeholder="Current password" className="w-full border rounded px-3 py-2 text-sm" />
            <input type="password" required minLength={12} value={next} onChange={(e) => setNext(e.target.value)}
                   placeholder="New password (min 12 chars)" className="w-full border rounded px-3 py-2 text-sm" />
            <input type="password" required value={confirm} onChange={(e) => setConfirm(e.target.value)}
                   placeholder="Confirm new password" className="w-full border rounded px-3 py-2 text-sm" />
            <button className="bg-slate-800 text-white px-4 py-2 rounded text-sm">Update password</button>
          </form>
        </section>

        <section className="bg-white rounded shadow p-6 space-y-3 border border-red-100">
          <h2 className="text-lg font-semibold text-red-700">Danger zone</h2>
          <p className="text-sm text-slate-600">
            Deleting your account removes your rooms, their messages and files, and your membership everywhere.
          </p>
          {!confirming ? (
            <button onClick={() => setConfirming(true)} className="bg-red-600 text-white px-4 py-2 rounded text-sm">
              Delete account
            </button>
          ) : (
            <div className="space-y-2">
              <FormError error={delErr} />
              <input type="password" value={delPwd} onChange={(e) => setDelPwd(e.target.value)}
                     placeholder="Confirm with password" className="w-full border rounded px-3 py-2 text-sm" />
              <div className="flex gap-2">
                <button onClick={deleteAccount} className="bg-red-600 text-white px-4 py-2 rounded text-sm">
                  Permanently delete
                </button>
                <button onClick={() => { setConfirming(false); setDelPwd(""); setDelErr(null); }}
                        className="px-4 py-2 rounded text-sm border">Cancel</button>
              </div>
            </div>
          )}
        </section>
      </main>
    </div>
  );
}
