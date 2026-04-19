import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import { ApiError, api } from "../lib/api";
import type { Ban, ChatType, Invitation, Member, RoomDetail } from "../lib/types";
import { useAuthStore } from "../stores/authStore";
import ConfirmDialog from "./ConfirmDialog";
import FormError from "./FormError";
import Modal from "./Modal";

type Tab = "members" | "admins" | "bans" | "invitations" | "settings";

export default function ManageRoomModal({ room, onClose }: { room: RoomDetail; onClose: () => void }) {
  const [tab, setTab] = useState<Tab>("members");
  const isOwner = room.yourRole === "OWNER";
  const tabs: { id: Tab; label: string; visible: boolean }[] = [
    { id: "members", label: "Members", visible: true },
    { id: "admins", label: "Admins", visible: true },
    { id: "bans", label: "Banned", visible: true },
    { id: "invitations", label: "Invitations", visible: room.type === "PRIVATE_ROOM" },
    { id: "settings", label: "Settings", visible: isOwner },
  ];

  return (
    <Modal title={`Manage: #${room.name}`} onClose={onClose} wide>
      <div className="flex gap-1 border-b mb-4 text-sm">
        {tabs.filter(t => t.visible).map((t) => (
          <button key={t.id} onClick={() => setTab(t.id)}
                  className={`px-3 py-1.5 -mb-px border-b-2 ${tab === t.id ? "border-slate-800 text-slate-900" : "border-transparent text-slate-500 hover:text-slate-800"}`}>
            {t.label}
          </button>
        ))}
      </div>
      {tab === "members" && <MembersTab room={room} />}
      {tab === "admins" && <AdminsTab room={room} />}
      {tab === "bans" && <BansTab room={room} />}
      {tab === "invitations" && room.type === "PRIVATE_ROOM" && <InvitationsTab room={room} />}
      {tab === "settings" && isOwner && <SettingsTab room={room} onClose={onClose} />}
    </Modal>
  );
}

function MembersTab({ room }: { room: RoomDetail }) {
  const qc = useQueryClient();
  const meId = useAuthStore((s) => s.me?.id);
  const [kickTarget, setKickTarget] = useState<Member | null>(null);
  const [demoteTarget, setDemoteTarget] = useState<Member | null>(null);
  const q = useQuery({
    queryKey: ["room", room.id, "members"],
    queryFn: () => api.get<Member[]>(`/api/rooms/${room.id}/members`),
  });
  const kick = useMutation({
    mutationFn: (userId: number) => api.delete(`/api/rooms/${room.id}/members/${userId}`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["room", room.id, "members"] });
      qc.invalidateQueries({ queryKey: ["room", room.id, "bans"] });
      setKickTarget(null);
    },
  });
  const promote = useMutation({
    mutationFn: (userId: number) => api.post(`/api/rooms/${room.id}/admins/${userId}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["room", room.id, "members"] }),
  });
  const demote = useMutation({
    mutationFn: (userId: number) => api.delete(`/api/rooms/${room.id}/admins/${userId}`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["room", room.id, "members"] });
      setDemoteTarget(null);
    },
  });

  return (
    <>
      <table className="w-full text-sm">
        <thead className="text-left text-xs uppercase text-slate-500">
          <tr><th className="p-2">Username</th><th className="p-2">Role</th><th className="p-2">Actions</th></tr>
        </thead>
        <tbody>
          {q.data?.map((m) => (
            <tr key={m.userId} className="border-t">
              <td className="p-2 font-mono">@{m.username}</td>
              <td className="p-2">{m.role}</td>
              <td className="p-2 space-x-2">
                {room.yourRole === "OWNER" && m.role === "MEMBER" && m.userId !== meId && (
                  <button onClick={() => promote.mutate(m.userId)} className="text-slate-700 hover:underline">Make admin</button>
                )}
                {m.role === "ADMIN" && m.userId !== meId && (
                  <button onClick={() => setDemoteTarget(m)} className="text-slate-700 hover:underline">Remove admin</button>
                )}
                {m.role !== "OWNER" && m.userId !== meId && (
                  <button
                    onClick={() => setKickTarget(m)}
                    className="text-red-600 hover:underline">
                    Kick &amp; ban
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      {kickTarget && (
        <ConfirmDialog
          title="Kick and ban user"
          message={
            <>
              Kick and ban <span className="font-mono">@{kickTarget.username}</span> from{" "}
              <span className="font-mono">#{room.name}</span>? They will not be able to rejoin until unbanned.
            </>
          }
          confirmLabel="Kick & ban"
          danger
          onConfirm={() => kick.mutate(kickTarget.userId)}
          onCancel={() => setKickTarget(null)}
        />
      )}
      {demoteTarget && (
        <ConfirmDialog
          title="Remove admin role"
          message={
            <>
              Remove admin role from <span className="font-mono">@{demoteTarget.username}</span>? They will
              remain a member but lose moderation privileges.
            </>
          }
          confirmLabel="Remove admin"
          onConfirm={() => demote.mutate(demoteTarget.userId)}
          onCancel={() => setDemoteTarget(null)}
        />
      )}
    </>
  );
}

function AdminsTab({ room }: { room: RoomDetail }) {
  const q = useQuery({
    queryKey: ["room", room.id, "members"],
    queryFn: () => api.get<Member[]>(`/api/rooms/${room.id}/members`),
  });
  const admins = q.data?.filter(m => m.role === "OWNER" || m.role === "ADMIN") ?? [];
  return (
    <div className="text-sm space-y-1">
      {admins.map((a) => (
        <div key={a.userId} className="flex items-center justify-between py-1">
          <span className="font-mono">@{a.username}</span>
          <span className="text-xs text-slate-500">{a.role}</span>
        </div>
      ))}
    </div>
  );
}

function BansTab({ room }: { room: RoomDetail }) {
  const qc = useQueryClient();
  const q = useQuery({
    queryKey: ["room", room.id, "bans"],
    queryFn: () => api.get<Ban[]>(`/api/rooms/${room.id}/bans`),
  });
  const unban = useMutation({
    mutationFn: (userId: number) => api.delete(`/api/rooms/${room.id}/bans/${userId}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["room", room.id, "bans"] }),
  });
  return (
    <table className="w-full text-sm">
      <thead className="text-left text-xs uppercase text-slate-500">
        <tr><th className="p-2">Username</th><th className="p-2">Banned by</th><th className="p-2">When</th><th className="p-2"></th></tr>
      </thead>
      <tbody>
        {q.data?.map((b) => (
          <tr key={b.userId} className="border-t">
            <td className="p-2 font-mono">@{b.username}</td>
            <td className="p-2 font-mono">{b.bannedByUsername ?? "(deleted)"}</td>
            <td className="p-2">{new Date(b.bannedAt).toLocaleString()}</td>
            <td className="p-2 text-right">
              <button onClick={() => unban.mutate(b.userId)} className="text-slate-700 hover:underline">Unban</button>
            </td>
          </tr>
        ))}
        {q.data && q.data.length === 0 && (
          <tr><td colSpan={4} className="p-4 text-center text-slate-500">No bans.</td></tr>
        )}
      </tbody>
    </table>
  );
}

function InvitationsTab({ room }: { room: RoomDetail }) {
  const qc = useQueryClient();
  const [username, setUsername] = useState("");
  const [error, setError] = useState<string | null>(null);

  const q = useQuery({
    queryKey: ["room", room.id, "invitations"],
    queryFn: () => api.get<Invitation[]>(`/api/rooms/${room.id}/invitations`),
  });
  const invite = useMutation({
    mutationFn: () => api.post(`/api/rooms/${room.id}/invitations`, { username }),
    onSuccess: () => {
      setUsername("");
      qc.invalidateQueries({ queryKey: ["room", room.id, "invitations"] });
    },
    onError: (e) => setError(e instanceof ApiError ? e.detail : String(e)),
  });

  function submit(e: FormEvent) { e.preventDefault(); setError(null); invite.mutate(); }

  return (
    <div className="space-y-4">
      <form onSubmit={submit} className="flex gap-2">
        <input value={username} onChange={(e) => setUsername(e.target.value)}
               placeholder="Invite by username"
               className="flex-1 border rounded px-3 py-1.5 text-sm" />
        <button disabled={invite.isPending || !username} className="px-3 py-1.5 bg-slate-800 text-white rounded text-sm">Send</button>
      </form>
      <FormError error={error} />
      <table className="w-full text-sm">
        <thead className="text-left text-xs uppercase text-slate-500">
          <tr><th className="p-2">Invitee</th><th className="p-2">Invited by</th><th className="p-2">When</th></tr>
        </thead>
        <tbody>
          {q.data?.map((i) => (
            <tr key={i.id} className="border-t">
              <td className="p-2 font-mono">@{i.inviteeUsername}</td>
              <td className="p-2 font-mono">{i.invitedByUsername ?? "(deleted)"}</td>
              <td className="p-2">{new Date(i.createdAt).toLocaleString()}</td>
            </tr>
          ))}
          {q.data && q.data.length === 0 && (
            <tr><td colSpan={3} className="p-4 text-center text-slate-500">No pending invitations.</td></tr>
          )}
        </tbody>
      </table>
    </div>
  );
}

function SettingsTab({ room, onClose }: { room: RoomDetail; onClose: () => void }) {
  const qc = useQueryClient();
  const nav = useNavigate();
  const [name, setName] = useState(room.name);
  const [description, setDescription] = useState(room.description ?? "");
  const [type, setType] = useState<ChatType>(room.type);
  const [error, setError] = useState<string | null>(null);
  const [confirmDelete, setConfirmDelete] = useState(false);

  const save = useMutation({
    mutationFn: () => api.patch<RoomDetail>(`/api/rooms/${room.id}`, { name, description, type }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["chats"] });
      qc.invalidateQueries({ queryKey: ["room", room.id] });
      onClose();
    },
    onError: (e) => setError(e instanceof ApiError ? e.detail : String(e)),
  });
  const del = useMutation({
    mutationFn: () => api.delete(`/api/rooms/${room.id}`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["chats"] });
      onClose();
      nav("/");
    },
    onError: (e) => setError(e instanceof ApiError ? e.detail : String(e)),
  });

  return (
    <div className="space-y-3">
      <FormError error={error} />
      <label className="block text-sm">
        <span className="text-slate-700">Room name</span>
        <input value={name} onChange={(e) => setName(e.target.value)}
               className="mt-1 w-full border rounded px-3 py-2" />
      </label>
      <label className="block text-sm">
        <span className="text-slate-700">Description</span>
        <textarea value={description} onChange={(e) => setDescription(e.target.value)}
                  className="mt-1 w-full border rounded px-3 py-2" rows={3} />
      </label>
      <fieldset className="text-sm">
        <legend className="text-slate-700">Visibility</legend>
        <label className="flex items-center gap-2 mt-1">
          <input type="radio" checked={type === "PUBLIC_ROOM"} onChange={() => setType("PUBLIC_ROOM")} /> Public
        </label>
        <label className="flex items-center gap-2">
          <input type="radio" checked={type === "PRIVATE_ROOM"} onChange={() => setType("PRIVATE_ROOM")} /> Private
        </label>
      </fieldset>
      <div className="flex gap-2 justify-between pt-3 border-t">
        <button onClick={() => setConfirmDelete(true)}
                className="px-3 py-1.5 bg-red-600 text-white rounded text-sm">
          Delete room
        </button>
        <button onClick={() => save.mutate()} disabled={save.isPending}
                className="px-3 py-1.5 bg-slate-800 text-white rounded text-sm">
          Save changes
        </button>
      </div>
      {confirmDelete && (
        <ConfirmDialog
          title="Delete room"
          message={
            <>
              Permanently delete <span className="font-mono">#{room.name}</span>? All messages, files,
              and images in this room will be deleted. This cannot be undone.
            </>
          }
          confirmLabel="Delete room"
          danger
          onConfirm={() => { setConfirmDelete(false); del.mutate(); }}
          onCancel={() => setConfirmDelete(false)}
        />
      )}
    </div>
  );
}
