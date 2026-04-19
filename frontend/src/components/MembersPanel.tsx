import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useMemo, useState } from "react";
import { ApiError, api } from "../lib/api";
import type { Friendship, Member, RoomDetail } from "../lib/types";
import { useAuthStore } from "../stores/authStore";
import { ws } from "../lib/ws";
import ConfirmDialog from "./ConfirmDialog";
import InviteUserModal from "./InviteUserModal";
import ManageRoomModal from "./ManageRoomModal";
import PresenceDot from "./PresenceDot";

type FriendshipState = "self" | "friend" | "outgoing" | "incoming" | "none";

export default function MembersPanel({ room }: { room: RoomDetail }) {
  const [managing, setManaging] = useState(false);
  const [inviting, setInviting] = useState(false);
  const meId = useAuthStore((s) => s.me?.id);
  const qc = useQueryClient();
  const canManage = room.yourRole === "OWNER" || room.yourRole === "ADMIN";
  const canInvite = room.type === "PRIVATE_ROOM" && room.yourRole !== null;

  const membersQ = useQuery({
    queryKey: ["room", room.id, "members"],
    queryFn: () => api.get<Member[]>(`/api/rooms/${room.id}/members`),
  });
  // Live membership updates: refresh the list when anyone joins/leaves/gets
  // banned, so observers don't have to reload the page.
  useEffect(() => {
    const unsub = ws.subscribe(`/topic/chat.${room.id}.members`, () => {
      qc.invalidateQueries({ queryKey: ["room", room.id, "members"] });
      qc.invalidateQueries({ queryKey: ["room", room.id, "bans"] });
      qc.invalidateQueries({ queryKey: ["chats"] });
    });
    return unsub;
  }, [room.id, qc]);
  const friendsQ = useQuery({
    queryKey: ["friends"],
    queryFn: () => api.get<Friendship[]>("/api/friends"),
  });
  const incomingQ = useQuery({
    queryKey: ["friends", "incoming"],
    queryFn: () => api.get<Friendship[]>("/api/friends/requests/incoming"),
  });
  const outgoingQ = useQuery({
    queryKey: ["friends", "outgoing"],
    queryFn: () => api.get<Friendship[]>("/api/friends/requests/outgoing"),
  });

  const friendIds = useMemo(() => new Set((friendsQ.data ?? []).map((f) => f.userId)), [friendsQ.data]);
  const incomingIds = useMemo(() => new Set((incomingQ.data ?? []).map((f) => f.userId)), [incomingQ.data]);
  const outgoingIds = useMemo(() => new Set((outgoingQ.data ?? []).map((f) => f.userId)), [outgoingQ.data]);

  function stateFor(userId: number): FriendshipState {
    if (meId != null && userId === meId) return "self";
    if (friendIds.has(userId)) return "friend";
    if (outgoingIds.has(userId)) return "outgoing";
    if (incomingIds.has(userId)) return "incoming";
    return "none";
  }

  const sendRequest = useMutation({
    mutationFn: (username: string) => api.post("/api/friends/requests", { username, text: "" }),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["friends"] }),
  });
  const accept = useMutation({
    mutationFn: (userId: number) => api.post(`/api/friends/requests/${userId}/accept`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["friends"] }),
  });
  const cancel = useMutation({
    mutationFn: (userId: number) => api.delete(`/api/friends/requests/${userId}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["friends"] }),
  });
  const [removeTarget, setRemoveTarget] = useState<Member | null>(null);
  const removeFriend = useMutation({
    mutationFn: (userId: number) => api.delete(`/api/friends/${userId}`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["friends"] });
      setRemoveTarget(null);
    },
  });

  const [actionError, setActionError] = useState<string | null>(null);
  const handle = (fn: () => Promise<unknown>) => {
    setActionError(null);
    fn().catch((e) => setActionError(e instanceof ApiError ? e.detail : String(e)));
  };

  return (
    <aside className="w-64 shrink-0 bg-white border-l border-slate-200 p-4 overflow-y-auto">
      <h3 className="text-sm font-semibold text-slate-700">Room info</h3>
      <div className="text-xs text-slate-500 mt-1">{room.type === "PUBLIC_ROOM" ? "Public room" : "Private room"}</div>
      <div className="text-xs text-slate-500">Owner: <span className="font-mono">{room.ownerUsername ?? "—"}</span></div>

      <h3 className="text-sm font-semibold text-slate-700 mt-4 mb-1">
        Members <span className="text-slate-400 font-normal">({membersQ.data?.length ?? 0})</span>
      </h3>
      {actionError && <div className="text-xs text-red-600 mb-2">{actionError}</div>}
      <ul className="space-y-1">
        {membersQ.data?.map((m) => {
          const st = stateFor(m.userId);
          return (
            <li key={m.userId} className="text-sm flex items-center gap-2">
              <PresenceDot userId={m.userId} />
              <span className="font-mono truncate flex-1">@{m.username}</span>
              {m.role !== "MEMBER" && (
                <span className="text-[10px] uppercase bg-slate-100 text-slate-600 px-1.5 py-0.5 rounded">
                  {m.role}
                </span>
              )}
              {st === "friend" && (
                <button
                  onClick={() => setRemoveTarget(m)}
                  title="Remove from friends"
                  className="text-[10px] uppercase bg-green-50 text-green-700 px-1.5 py-0.5 rounded hover:bg-red-50 hover:text-red-700">
                  friend
                </button>
              )}
              {st === "outgoing" && (
                <button
                  disabled={cancel.isPending}
                  onClick={() => handle(() => cancel.mutateAsync(m.userId))}
                  className="text-[10px] uppercase text-slate-500 hover:text-red-600"
                  title="Cancel friend request">
                  requested
                </button>
              )}
              {st === "incoming" && (
                <button
                  disabled={accept.isPending}
                  onClick={() => handle(() => accept.mutateAsync(m.userId))}
                  className="text-[10px] uppercase bg-slate-800 text-white px-1.5 py-0.5 rounded hover:bg-slate-900">
                  accept
                </button>
              )}
              {st === "none" && (
                <button
                  disabled={sendRequest.isPending}
                  onClick={() => handle(() => sendRequest.mutateAsync(m.username))}
                  className="text-[10px] uppercase border border-slate-300 text-slate-700 px-1.5 py-0.5 rounded hover:bg-slate-50">
                  add
                </button>
              )}
            </li>
          );
        })}
      </ul>
      {canInvite && (
        <button
          onClick={() => setInviting(true)}
          className="mt-4 w-full border rounded px-3 py-1.5 text-sm">
          Invite user
        </button>
      )}
      {canManage && (
        <button
          onClick={() => setManaging(true)}
          className={`${canInvite ? "mt-2" : "mt-4"} w-full border rounded px-3 py-1.5 text-sm`}>
          Manage room
        </button>
      )}
      {managing && <ManageRoomModal room={room} onClose={() => setManaging(false)} />}
      {inviting && (
        <InviteUserModal chatId={room.id} roomName={room.name} onClose={() => setInviting(false)} />
      )}
      {removeTarget && (
        <ConfirmDialog
          title="Remove friend"
          message={
            <>
              Remove <span className="font-mono">@{removeTarget.username}</span> from your friends? You
              will need to send a new friend request to message them again.
            </>
          }
          confirmLabel="Remove"
          onConfirm={() => removeFriend.mutate(removeTarget.userId)}
          onCancel={() => setRemoveTarget(null)}
        />
      )}
    </aside>
  );
}
