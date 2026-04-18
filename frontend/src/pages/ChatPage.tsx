import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import Composer from "../components/Composer";
import MembersPanel from "../components/MembersPanel";
import MessageList from "../components/MessageList";
import { ApiError, api } from "../lib/api";
import type { MessageDto } from "../lib/messageTypes";
import type { ChatDetail, RoomDetail } from "../lib/types";

export default function ChatPage() {
  const { id } = useParams();
  const chatId = Number(id);
  const qc = useQueryClient();
  const nav = useNavigate();
  const [replyTo, setReplyTo] = useState<MessageDto | null>(null);

  const q = useQuery({
    queryKey: ["chat", chatId],
    queryFn: () => api.get<ChatDetail>(`/api/chats/${chatId}`),
    enabled: !Number.isNaN(chatId),
  });

  const leave = useMutation({
    mutationFn: () => api.post(`/api/rooms/${chatId}/leave`),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["chats"] }); nav("/"); },
  });

  if (q.isLoading) return <div className="p-6 text-sm text-slate-500">Loading...</div>;
  if (q.isError) return <div className="p-6 text-sm text-red-600">{(q.error as ApiError).detail}</div>;
  if (!q.data) return null;
  const chat = q.data;
  const isDirect = chat.type === "DIRECT";
  const title = isDirect ? `@ ${chat.peerUsername ?? "(unknown)"}` : `# ${chat.name ?? ""}`;
  const canMessage = isDirect ? !!chat.canMessage : true;

  // For rooms, MembersPanel expects the RoomDetail shape
  const roomForPanel: RoomDetail | null = !isDirect && chat.name
    ? {
        id: chat.id,
        type: chat.type,
        name: chat.name,
        description: chat.description,
        ownerId: chat.ownerId,
        ownerUsername: chat.ownerUsername,
        memberCount: chat.memberCount,
        yourRole: chat.yourRole,
      }
    : null;

  return (
    <div className="flex flex-1 min-h-0">
      <section className="flex-1 flex flex-col min-w-0">
        <header className="bg-white border-b px-4 py-3 flex items-center justify-between shrink-0">
          <div className="min-w-0">
            <h1 className="font-semibold text-slate-800 truncate">{title}</h1>
            <div className="text-xs text-slate-500 truncate">
              {isDirect
                ? (canMessage ? "Direct message" : "Read-only (not friends or blocked)")
                : (chat.description ?? "")}
            </div>
          </div>
          <div className="flex items-center gap-3 text-sm">
            {!isDirect && chat.yourRole && chat.yourRole !== "OWNER" && (
              <button onClick={() => leave.mutate()} className="text-red-600 hover:underline">Leave</button>
            )}
          </div>
        </header>
        <MessageList chatId={chatId} yourRole={chat.yourRole} onReply={setReplyTo} />
        {canMessage ? (
          <Composer chatId={chatId} replyTo={replyTo} onClearReply={() => setReplyTo(null)} />
        ) : (
          <div className="border-t bg-slate-50 px-4 py-3 text-sm text-slate-500 italic text-center">
            This conversation is read-only.
          </div>
        )}
      </section>
      {roomForPanel && <MembersPanel room={roomForPanel} />}
    </div>
  );
}
