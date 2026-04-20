import { useState } from "react";
import { Attachments } from "./Attachments";
import ConfirmDialog from "./ConfirmDialog";
import { ApiError, api } from "../lib/api";
import type { MessageDto } from "../lib/messageTypes";
import type { ChatRole } from "../lib/types";

export default function MessageItem({
  m,
  meId,
  yourRole,
  readOnly,
  grouped = false,
  parent,
  onReply,
}: {
  m: MessageDto;
  meId: number | undefined;
  yourRole: ChatRole | null;
  readOnly: boolean;
  grouped?: boolean;
  parent?: MessageDto;
  onReply: (m: MessageDto) => void;
}) {
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(m.body ?? "");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);

  const isAuthor = meId != null && meId === m.authorId;
  const canDelete = isAuthor || yourRole === "OWNER" || yourRole === "ADMIN";

  async function save() {
    setError(null);
    setBusy(true);
    try {
      await api.patch(`/api/messages/${m.id}`, { body: draft });
      setEditing(false);
    } catch (e) {
      setError(e instanceof ApiError ? e.detail : String(e));
    } finally {
      setBusy(false);
    }
  }

  async function del() {
    setConfirmDelete(false);
    try {
      await api.delete(`/api/messages/${m.id}`);
    } catch (e) {
      alert(e instanceof ApiError ? e.detail : String(e));
    }
  }

  const bubbleBase = "text-sm rounded-lg px-3 py-1.5 whitespace-pre-wrap break-words";
  const bubbleSelf = "bg-blue-600 text-white";
  const bubbleOther = "bg-slate-100 text-slate-800";

  const showHeader = !grouped;
  const rowPadding = grouped ? "py-0.5" : "py-1.5";

  return (
    <div className={`group px-4 ${rowPadding} hover:bg-slate-50`}>
      <div className={`flex ${isAuthor ? "justify-end" : "justify-start"}`}>
        <div className={`flex flex-col max-w-[70%] ${isAuthor ? "items-end" : "items-start"}`}>
          {parent && (
            <div className="text-xs text-slate-500 mb-0.5 truncate max-w-full border-l-2 border-slate-300 pl-2">
              ↪ <span className="font-mono">{parent.authorUsername ?? "?"}</span>{" "}
              {parent.deleted ? <em>message deleted</em> : parent.body}
            </div>
          )}
          {showHeader && (
            <div className="flex items-baseline gap-2 mb-0.5">
              {!isAuthor && (
                <span className="font-semibold text-slate-800 text-sm">
                  {m.authorUsername ?? "(deleted user)"}
                </span>
              )}
              <span className="text-xs text-slate-400">
                {new Date(m.createdAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
              </span>
              {m.editedAt && !m.deleted && <span className="text-xs text-slate-400">(edited)</span>}
              <div className="opacity-0 group-hover:opacity-100 flex gap-3 text-xs">
                {!m.deleted && !readOnly && (
                  <button onClick={() => onReply(m)} className="text-slate-500 hover:text-slate-800">Reply</button>
                )}
                {!m.deleted && !readOnly && isAuthor && (
                  <button onClick={() => { setDraft(m.body ?? ""); setEditing(true); }}
                          className="text-slate-500 hover:text-slate-800">Edit</button>
                )}
                {!m.deleted && !readOnly && canDelete && (
                  <button onClick={() => setConfirmDelete(true)} className="text-red-500 hover:text-red-700">Delete</button>
                )}
              </div>
            </div>
          )}
          {!showHeader && (
            <div className={`opacity-0 group-hover:opacity-100 flex gap-3 text-xs mb-0.5 ${isAuthor ? "justify-end" : ""}`}>
              <span className="text-slate-400">
                {new Date(m.createdAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
              </span>
              {m.editedAt && !m.deleted && <span className="text-slate-400">(edited)</span>}
              {!m.deleted && !readOnly && (
                <button onClick={() => onReply(m)} className="text-slate-500 hover:text-slate-800">Reply</button>
              )}
              {!m.deleted && !readOnly && isAuthor && (
                <button onClick={() => { setDraft(m.body ?? ""); setEditing(true); }}
                        className="text-slate-500 hover:text-slate-800">Edit</button>
              )}
              {!m.deleted && !readOnly && canDelete && (
                <button onClick={() => setConfirmDelete(true)} className="text-red-500 hover:text-red-700">Delete</button>
              )}
            </div>
          )}
          {m.deleted ? (
            <div className={`${bubbleBase} bg-slate-100 text-slate-400 italic`}>Message deleted</div>
          ) : editing ? (
            <div className="w-full space-y-1">
              <textarea value={draft} onChange={(e) => setDraft(e.target.value)}
                        className="w-full border rounded px-2 py-1 text-sm" rows={2} autoFocus />
              {error && <div className="text-xs text-red-600">{error}</div>}
              <div className={`flex gap-2 text-xs ${isAuthor ? "justify-end" : ""}`}>
                <button onClick={save} disabled={busy} className="px-2 py-1 bg-slate-800 text-white rounded">Save</button>
                <button onClick={() => setEditing(false)} className="px-2 py-1 border rounded">Cancel</button>
              </div>
            </div>
          ) : (
            <>
              {m.body && (
                <div className={`${bubbleBase} ${isAuthor ? bubbleSelf : bubbleOther}`}>
                  {m.body}
                </div>
              )}
              {m.attachments && m.attachments.length > 0 && (
                <div className={`mt-1 ${isAuthor ? "text-right" : ""}`}>
                  <Attachments list={m.attachments} readOnly={readOnly} />
                </div>
              )}
            </>
          )}
        </div>
      </div>
      {confirmDelete && (
        <ConfirmDialog
          title="Delete message"
          message="Delete this message? This cannot be undone."
          confirmLabel="Delete"
          danger
          onConfirm={del}
          onCancel={() => setConfirmDelete(false)}
        />
      )}
    </div>
  );
}
