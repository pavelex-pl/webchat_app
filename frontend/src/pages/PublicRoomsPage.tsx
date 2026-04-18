import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { ApiError, api } from "../lib/api";
import type { Page, Room } from "../lib/types";

export default function PublicRoomsPage() {
  const [q, setQ] = useState("");
  const [page, setPage] = useState(0);
  const qc = useQueryClient();
  const nav = useNavigate();

  const catalog = useQuery({
    queryKey: ["publicRooms", q, page],
    queryFn: () => api.get<Page<Room>>(`/api/rooms/public?page=${page}&size=20&q=${encodeURIComponent(q)}`),
  });

  const join = useMutation({
    mutationFn: (id: number) => api.post(`/api/rooms/${id}/join`),
    onSuccess: (_d, id) => {
      qc.invalidateQueries({ queryKey: ["chats"] });
      nav(`/chat/${id}`);
    },
  });

  return (
    <div className="p-6 max-w-3xl mx-auto">
      <h1 className="text-xl font-semibold text-slate-800 mb-3">Public rooms</h1>
      <input
        value={q}
        onChange={(e) => { setQ(e.target.value); setPage(0); }}
        placeholder="Search rooms..."
        className="w-full border rounded px-3 py-2 text-sm mb-4"
      />
      {catalog.isLoading && <p className="text-sm text-slate-500">Loading...</p>}
      {catalog.isError && <p className="text-sm text-red-600">Failed to load</p>}
      <ul className="bg-white rounded shadow divide-y">
        {catalog.data?.items.map((r) => (
          <li key={r.id} className="p-4 flex items-start justify-between gap-4">
            <div className="min-w-0">
              <div className="font-medium text-slate-800">#{r.name}</div>
              <div className="text-sm text-slate-500 truncate">{r.description ?? "—"}</div>
              <div className="text-xs text-slate-400 mt-1">{r.memberCount} member{r.memberCount === 1 ? "" : "s"}</div>
            </div>
            <button
              disabled={join.isPending}
              onClick={() => join.mutate(r.id)}
              className="px-3 py-1.5 bg-slate-800 text-white rounded text-sm">
              Join
            </button>
          </li>
        ))}
        {catalog.data && catalog.data.items.length === 0 && (
          <li className="p-6 text-center text-sm text-slate-500">No rooms match.</li>
        )}
      </ul>
      {join.isError && (
        <p className="text-sm text-red-600 mt-2">
          {join.error instanceof ApiError ? join.error.detail : String(join.error)}
        </p>
      )}
      {catalog.data && catalog.data.total > catalog.data.size && (
        <div className="mt-3 flex gap-2 items-center justify-end text-sm">
          <button disabled={page === 0} onClick={() => setPage(p => p - 1)}
                  className="px-2 py-1 border rounded disabled:opacity-30">Prev</button>
          <span>Page {page + 1}</span>
          <button disabled={(page + 1) * catalog.data.size >= catalog.data.total}
                  onClick={() => setPage(p => p + 1)}
                  className="px-2 py-1 border rounded disabled:opacity-30">Next</button>
        </div>
      )}
    </div>
  );
}
