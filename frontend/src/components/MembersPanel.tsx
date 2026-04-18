import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { api } from "../lib/api";
import type { Member, RoomDetail } from "../lib/types";
import ManageRoomModal from "./ManageRoomModal";
import PresenceDot from "./PresenceDot";

export default function MembersPanel({ room }: { room: RoomDetail }) {
  const [managing, setManaging] = useState(false);
  const canManage = room.yourRole === "OWNER" || room.yourRole === "ADMIN";

  const q = useQuery({
    queryKey: ["room", room.id, "members"],
    queryFn: () => api.get<Member[]>(`/api/rooms/${room.id}/members`),
  });

  return (
    <aside className="w-64 shrink-0 bg-white border-l border-slate-200 p-4 overflow-y-auto">
      <h3 className="text-sm font-semibold text-slate-700">Room info</h3>
      <div className="text-xs text-slate-500 mt-1">{room.type === "PUBLIC_ROOM" ? "Public room" : "Private room"}</div>
      <div className="text-xs text-slate-500">Owner: <span className="font-mono">{room.ownerUsername ?? "—"}</span></div>

      <h3 className="text-sm font-semibold text-slate-700 mt-4 mb-1">
        Members <span className="text-slate-400 font-normal">({q.data?.length ?? 0})</span>
      </h3>
      <ul className="space-y-0.5">
        {q.data?.map((m) => (
          <li key={m.userId} className="text-sm flex items-center gap-2">
            <PresenceDot userId={m.userId} />
            <span className="font-mono truncate flex-1">@{m.username}</span>
            {m.role !== "MEMBER" && (
              <span className="text-[10px] uppercase bg-slate-100 text-slate-600 px-1.5 py-0.5 rounded">
                {m.role}
              </span>
            )}
          </li>
        ))}
      </ul>
      {canManage && (
        <button
          onClick={() => setManaging(true)}
          className="mt-4 w-full border rounded px-3 py-1.5 text-sm">
          Manage room
        </button>
      )}
      {managing && <ManageRoomModal room={room} onClose={() => setManaging(false)} />}
    </aside>
  );
}
