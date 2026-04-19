import { useInfiniteQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { ApiError, api } from "../lib/api";
import type { Page, Room } from "../lib/types";
import { useInfiniteScrollSentinel } from "../lib/useInfiniteScrollSentinel";

const PAGE_SIZE = 50;

export default function PublicRoomsPage() {
  const [q, setQ] = useState("");
  const qc = useQueryClient();
  const nav = useNavigate();

  const catalog = useInfiniteQuery({
    queryKey: ["publicRooms", q],
    queryFn: ({ pageParam = 0 }) =>
      api.get<Page<Room>>(
        `/api/rooms/public?page=${pageParam}&size=${PAGE_SIZE}&q=${encodeURIComponent(q)}`,
      ),
    initialPageParam: 0,
    getNextPageParam: (last) =>
      (last.page + 1) * last.size < last.total ? last.page + 1 : undefined,
  });

  const rooms = useMemo(
    () => (catalog.data?.pages ?? []).flatMap((p) => p.items),
    [catalog.data],
  );

  const sentinelRef = useInfiniteScrollSentinel<HTMLLIElement>(
    catalog.hasNextPage,
    catalog.isFetchingNextPage,
    catalog.fetchNextPage,
  );

  const join = useMutation({
    mutationFn: (id: number) => api.post(`/api/rooms/${id}/join`),
    onSuccess: (_d, id) => {
      qc.invalidateQueries({ queryKey: ["chats"] });
      qc.invalidateQueries({ queryKey: ["publicRooms"] });
      nav(`/chat/${id}`);
    },
  });

  return (
    <div className="p-6 max-w-6xl mx-auto">
      <h1 className="text-xl font-semibold text-slate-800 mb-3">Public rooms</h1>
      <input
        value={q}
        onChange={(e) => setQ(e.target.value)}
        placeholder="Search rooms..."
        className="w-full border rounded px-3 py-2 text-sm mb-4"
      />
      {catalog.isLoading && <p className="text-sm text-slate-500">Loading...</p>}
      {catalog.isError && <p className="text-sm text-red-600">Failed to load</p>}
      <ul className="bg-white rounded shadow divide-y">
        {rooms.map((r) => (
          <li key={r.id} className="px-4 py-2 flex items-center gap-4">
            <div className="font-medium text-slate-800 truncate w-48 shrink-0">#{r.name}</div>
            <div className="text-sm text-slate-500 truncate flex-1 min-w-0">{r.description ?? "—"}</div>
            <div className="text-xs text-slate-400 shrink-0 w-24 text-right">
              {r.memberCount} member{r.memberCount === 1 ? "" : "s"}
            </div>
            {r.bannedFromRoom && (
              <div className="text-xs text-red-600 shrink-0">banned</div>
            )}
            <div className="shrink-0">
              {r.youAreMember ? (
                <button
                  onClick={() => nav(`/chat/${r.id}`)}
                  className="px-3 py-1.5 border border-slate-300 text-slate-700 rounded text-sm">
                  Open
                </button>
              ) : r.bannedFromRoom ? (
                <button
                  disabled
                  title="You are banned from this room"
                  className="px-3 py-1.5 bg-slate-300 text-slate-500 rounded text-sm cursor-not-allowed">
                  Join
                </button>
              ) : (
                <button
                  disabled={join.isPending}
                  onClick={() => join.mutate(r.id)}
                  className="px-3 py-1.5 bg-slate-800 text-white rounded text-sm">
                  Join
                </button>
              )}
            </div>
          </li>
        ))}
        {catalog.isSuccess && rooms.length === 0 && (
          <li className="p-6 text-center text-sm text-slate-500">No rooms match.</li>
        )}
        {catalog.hasNextPage && <li ref={sentinelRef} className="h-1" />}
        {catalog.isFetchingNextPage && (
          <li className="p-3 text-center text-xs text-slate-400">Loading…</li>
        )}
      </ul>
      {join.isError && (
        <p className="text-sm text-red-600 mt-2">
          {join.error instanceof ApiError ? join.error.detail : String(join.error)}
        </p>
      )}
    </div>
  );
}
