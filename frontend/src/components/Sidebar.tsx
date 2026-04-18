import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { Link, NavLink, useParams } from "react-router-dom";
import { api } from "../lib/api";
import type { ChatSummary, Invitation } from "../lib/types";
import CreateRoomModal from "./CreateRoomModal";
import PresenceDot from "./PresenceDot";

export default function Sidebar() {
  const [creating, setCreating] = useState(false);
  const { id } = useParams();
  const activeId = id ? Number(id) : undefined;

  const chats = useQuery({
    queryKey: ["chats"],
    queryFn: () => api.get<ChatSummary[]>("/api/chats"),
  });
  const inv = useQuery({
    queryKey: ["invitations"],
    queryFn: () => api.get<Invitation[]>("/api/invitations"),
  });

  const publics = chats.data?.filter((r) => r.type === "PUBLIC_ROOM") ?? [];
  const privates = chats.data?.filter((r) => r.type === "PRIVATE_ROOM") ?? [];
  const directs = chats.data?.filter((r) => r.type === "DIRECT") ?? [];

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
        <Section title="Public rooms">
          {publics.map((r) => <ChatLink key={r.id} chat={r} active={r.id === activeId} />)}
          {publics.length === 0 && <Empty />}
        </Section>
        <Section title="Private rooms">
          {privates.map((r) => <ChatLink key={r.id} chat={r} active={r.id === activeId} />)}
          {privates.length === 0 && <Empty />}
        </Section>
        <Section title="Direct messages">
          {directs.map((r) => <ChatLink key={r.id} chat={r} active={r.id === activeId} />)}
          {directs.length === 0 && <Empty />}
        </Section>
      </div>
      {creating && <CreateRoomModal onClose={() => setCreating(false)} />}
    </aside>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="mt-3">
      <h3 className="text-[11px] uppercase tracking-wide text-slate-500 px-2 mb-1">{title}</h3>
      <div className="space-y-0.5">{children}</div>
    </section>
  );
}

function Empty() {
  return <div className="text-xs text-slate-400 px-2 py-1">Empty</div>;
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
      <span className={`truncate ${unread > 0 && !active ? "font-semibold text-slate-900" : ""}`}>
        {isDm ? `@ ${chat.peerUsername ?? "(unknown)"}` : `# ${chat.name}`}
      </span>
      {!isDm && (
        <span className="ml-auto text-slate-400 text-xs">({chat.memberCount})</span>
      )}
      {unread > 0 && !active && (
        <span className="ml-1 inline-flex items-center justify-center bg-red-600 text-white rounded-full px-1.5 text-xs min-w-[18px]">
          {unread > 99 ? "99+" : unread}
        </span>
      )}
    </Link>
  );
}
