import { useInfiniteQuery, useQuery } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { Link, NavLink, useParams } from "react-router-dom";
import { api } from "../lib/api";
import type { ChatSummary, ChatType, Friendship, Invitation, Page } from "../lib/types";
import { useInfiniteScrollSentinel } from "../lib/useInfiniteScrollSentinel";
import CreateRoomModal from "./CreateRoomModal";
import PresenceDot from "./PresenceDot";

const PAGE_SIZE = 50;

function useChatsPage(type: ChatType) {
  return useInfiniteQuery({
    queryKey: ["chats", type],
    queryFn: ({ pageParam = 0 }) =>
      api.get<Page<ChatSummary>>(
        `/api/chats?type=${type}&page=${pageParam}&size=${PAGE_SIZE}`,
      ),
    initialPageParam: 0,
    getNextPageParam: (last) =>
      (last.page + 1) * last.size < last.total ? last.page + 1 : undefined,
  });
}

export default function Sidebar() {
  const [creating, setCreating] = useState(false);
  const { id } = useParams();
  const activeId = id ? Number(id) : undefined;

  const publics = useChatsPage("PUBLIC_ROOM");
  const privates = useChatsPage("PRIVATE_ROOM");
  const directs = useChatsPage("DIRECT");

  const inv = useQuery({
    queryKey: ["invitations"],
    queryFn: () => api.get<Invitation[]>("/api/invitations"),
  });
  const incoming = useQuery({
    queryKey: ["friends", "incoming"],
    queryFn: () => api.get<Friendship[]>("/api/friends/requests/incoming"),
  });

  return (
    <aside className="w-72 shrink-0 bg-white border-r border-slate-200 flex flex-col">
      <div className="p-3 space-y-2">
        <button
          onClick={() => setCreating(true)}
          className="w-full bg-slate-800 text-white py-1.5 rounded text-sm">
          + Create room
        </button>
        <NavLink to="/rooms/public" className={({ isActive }) =>
          `block text-sm px-2 py-1 rounded ${isActive ? "bg-slate-100 text-slate-900" : "text-slate-600 hover:bg-slate-50"}`}>
          Public rooms catalog
        </NavLink>
        <NavLink to="/friends" className={({ isActive }) =>
          `block text-sm px-2 py-1 rounded ${isActive ? "bg-slate-100 text-slate-900" : "text-slate-600 hover:bg-slate-50"}`}>
          Friends
          {incoming.data && incoming.data.length > 0 && (
            <span className="ml-1 inline-flex items-center justify-center bg-red-600 text-white rounded-full px-1.5 text-xs">
              {incoming.data.length}
            </span>
          )}
        </NavLink>
        <NavLink to="/invitations" className={({ isActive }) =>
          `block text-sm px-2 py-1 rounded ${isActive ? "bg-slate-100 text-slate-900" : "text-slate-600 hover:bg-slate-50"}`}>
          Invitations
          {inv.data && inv.data.length > 0 && (
            <span className="ml-1 inline-flex items-center justify-center bg-red-600 text-white rounded-full px-1.5 text-xs">
              {inv.data.length}
            </span>
          )}
        </NavLink>
      </div>
      <div className="px-3 pb-3 flex-1 overflow-y-auto">
        <ChatSection title="Public rooms" q={publics} activeId={activeId} />
        <ChatSection title="Private rooms" q={privates} activeId={activeId} />
        <ChatSection title="Direct messages" q={directs} activeId={activeId} />
      </div>
      {creating && <CreateRoomModal onClose={() => setCreating(false)} />}
    </aside>
  );
}

type ChatsInfiniteQuery = ReturnType<typeof useChatsPage>;

function ChatSection({
  title,
  q,
  activeId,
}: {
  title: string;
  q: ChatsInfiniteQuery;
  activeId?: number;
}) {
  const items = useMemo(
    () => (q.data?.pages ?? []).flatMap((p) => p.items),
    [q.data],
  );
  const sentinelRef = useInfiniteScrollSentinel<HTMLDivElement>(
    q.hasNextPage,
    q.isFetchingNextPage,
    q.fetchNextPage,
  );

  return (
    <section className="mt-3">
      <h3 className="text-[11px] uppercase tracking-wide text-slate-500 px-2 mb-1">{title}</h3>
      <div className="space-y-0.5">
        {items.map((r) => (
          <ChatLink key={r.id} chat={r} active={r.id === activeId} />
        ))}
        {q.isSuccess && items.length === 0 && (
          <div className="text-xs text-slate-400 px-2 py-1">Empty</div>
        )}
        {q.hasNextPage && <div ref={sentinelRef} className="h-1" />}
        {q.isFetchingNextPage && (
          <div className="text-xs text-slate-400 px-2 py-1">Loading…</div>
        )}
      </div>
    </section>
  );
}

function ChatLink({ chat, active }: { chat: ChatSummary; active: boolean }) {
  const isDm = chat.type === "DIRECT";
  const unread = chat.unreadCount ?? 0;
  return (
    <Link
      to={`/chat/${chat.id}`}
      className={`flex items-center gap-1.5 text-sm px-2 py-1 rounded ${
        active ? "bg-slate-200 text-slate-900" : "text-slate-700 hover:bg-slate-50"
      }`}>
      {isDm && chat.peerUserId != null && <PresenceDot userId={chat.peerUserId} />}
      <span className={`truncate flex-1 ${unread > 0 ? "font-semibold text-slate-900" : ""}`}>
        {isDm ? `@ ${chat.peerUsername ?? "(unknown)"}` : `# ${chat.name}`}
      </span>
      {unread > 0 && (
        <span className="ml-1 inline-flex items-center justify-center bg-red-600 text-white rounded-full px-1.5 text-xs min-w-[18px]">
          {unread > 99 ? "99+" : unread}
        </span>
      )}
    </Link>
  );
}
