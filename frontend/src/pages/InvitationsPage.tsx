import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { api } from "../lib/api";
import type { Invitation } from "../lib/types";

export default function InvitationsPage() {
  const qc = useQueryClient();
  const nav = useNavigate();

  const q = useQuery({
    queryKey: ["invitations"],
    queryFn: () => api.get<Invitation[]>("/api/invitations"),
  });

  const accept = useMutation({
    mutationFn: (id: number) => api.post(`/api/invitations/${id}/accept`),
    onSuccess: (_d, id) => {
      const chatId = q.data?.find((i) => i.id === id)?.chatId;
      qc.invalidateQueries({ queryKey: ["invitations"] });
      qc.invalidateQueries({ queryKey: ["chats"] });
      if (chatId) nav(`/chat/${chatId}`);
    },
  });

  const decline = useMutation({
    mutationFn: (id: number) => api.post(`/api/invitations/${id}/decline`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["invitations"] }),
  });

  return (
    <div className="p-6 max-w-2xl mx-auto">
      <h1 className="text-xl font-semibold text-slate-800 mb-3">Invitations</h1>
      {q.isLoading && <p className="text-sm text-slate-500">Loading...</p>}
      {q.data && q.data.length === 0 && (
        <p className="text-sm text-slate-500 bg-white rounded shadow p-6 text-center">No pending invitations.</p>
      )}
      <ul className="space-y-2">
        {q.data?.map((inv) => (
          <li key={inv.id} className="bg-white rounded shadow p-4 flex items-center justify-between gap-4">
            <div className="min-w-0">
              <div className="text-sm text-slate-800">
                <span className="font-medium">#{inv.chatName}</span>
                <span className="text-slate-500"> invited by </span>
                <span className="font-mono">{inv.invitedByUsername ?? "(deleted user)"}</span>
              </div>
              <div className="text-xs text-slate-400 mt-0.5">{new Date(inv.createdAt).toLocaleString()}</div>
            </div>
            <div className="flex gap-2">
              <button onClick={() => accept.mutate(inv.id)}
                      disabled={accept.isPending}
                      className="px-3 py-1.5 bg-slate-800 text-white rounded text-sm">Accept</button>
              <button onClick={() => decline.mutate(inv.id)}
                      disabled={decline.isPending}
                      className="px-3 py-1.5 border rounded text-sm">Decline</button>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}
