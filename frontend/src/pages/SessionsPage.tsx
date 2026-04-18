import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import TopBar from "../components/TopBar";
import { ApiError, api } from "../lib/api";
import { useAuthStore } from "../stores/authStore";

type SessionRow = {
  id: string;
  userAgent: string | null;
  ip: string | null;
  createdAt: string;
  lastSeenAt: string;
  expiresAt: string;
  current: boolean;
};

export default function SessionsPage() {
  const qc = useQueryClient();
  const nav = useNavigate();
  const setMe = useAuthStore((s) => s.setMe);

  const q = useQuery({
    queryKey: ["sessions"],
    queryFn: () => api.get<SessionRow[]>("/api/auth/sessions"),
  });

  const revoke = useMutation({
    mutationFn: (id: string) => api.delete(`/api/auth/sessions/${id}`),
    onSuccess: (_data, id) => {
      const cur = q.data?.find((s) => s.id === id && s.current);
      if (cur) {
        setMe(null);
        nav("/login", { replace: true });
      } else {
        qc.invalidateQueries({ queryKey: ["sessions"] });
      }
    },
  });

  return (
    <div className="min-h-screen bg-slate-50">
      <TopBar />
      <main className="max-w-3xl mx-auto p-6">
        <h1 className="text-2xl font-semibold text-slate-800 mb-4">Active sessions</h1>
        {q.isLoading && <p className="text-sm text-slate-500">Loading...</p>}
        {q.isError && <p className="text-sm text-red-600">{(q.error as ApiError).detail}</p>}
        {q.data && (
          <table className="w-full text-sm bg-white rounded shadow">
            <thead className="text-left text-slate-500 text-xs uppercase">
              <tr>
                <th className="p-3">Session</th>
                <th className="p-3">User agent</th>
                <th className="p-3">IP</th>
                <th className="p-3">Last seen</th>
                <th className="p-3"></th>
              </tr>
            </thead>
            <tbody>
              {q.data.map((s) => (
                <tr key={s.id} className="border-t">
                  <td className="p-3 font-mono text-xs">
                    {s.id.slice(0, 8)}{s.current && <span className="ml-2 text-green-700 text-xs">(current)</span>}
                  </td>
                  <td className="p-3 truncate max-w-xs" title={s.userAgent ?? ""}>{s.userAgent ?? "-"}</td>
                  <td className="p-3">{s.ip ?? "-"}</td>
                  <td className="p-3">{new Date(s.lastSeenAt).toLocaleString()}</td>
                  <td className="p-3 text-right">
                    <button
                      disabled={revoke.isPending}
                      onClick={() => revoke.mutate(s.id)}
                      className="text-red-600 hover:underline">
                      {s.current ? "Sign out" : "Revoke"}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </main>
    </div>
  );
}
