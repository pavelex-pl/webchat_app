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
  parent,
  onReply,
}: {
  m: MessageDto;
  meId: number | undefined;
  yourRole: ChatRole | null;
  readOnly: boolean;
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

  return (
    <div className="group px-4 py-1.5 hover:bg-slate-50">
      {parent && (
        <div className="ml-6 border-l-2 border-slate-300 pl-2 text-xs text-slate-500 mb-0.5 truncate">
          ↪ <span className="font-mono">{parent.authorUsername ?? "?"}</span>{" "}
          {parent.deleted ? <em>message deleted</em> : parent.body}
        </div>
      )}
      <div className="flex items-baseline gap-2">
        <span className="font-semibold text-slate-800 text-sm">
          {m.authorUsername ?? "(deleted user)"}
        </span>
        <span className="text-xs text-slate-400">
          {new Date(m.createdAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
        </span>
        {m.editedAt && !m.deleted && <span className="text-xs text-slate-400">(edited)</span>}
        <div className="ml-auto opacity-0 group-hover:opacity-100 flex gap-3 text-xs">
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
      {m.deleted ? (
        <div className="text-sm text-slate-400 italic">Message deleted</div>
      ) : editing ? (
        <div className="mt-1 space-y-1">
          <textarea value={draft} onChange={(e) => setDraft(e.target.value)}
                    className="w-full border rounded px-2 py-1 text-sm" rows={2} autoFocus />
          {error && <div className="text-xs text-red-600">{error}</div>}
          <div className="flex gap-2 text-xs">
            <button onClick={save} disabled={busy} className="px-2 py-1 bg-slate-800 text-white rounded">Save</button>
            <button onClick={() => setEditing(false)} className="px-2 py-1 border rounded">Cancel</button>
          </div>
        </div>
      ) : (
        <>
          {m.body && (
            <div className="text-sm text-slate-800 whitespace-pre-wrap break-words">{m.body}</div>
          )}
          <Attachments list={m.attachments ?? []} readOnly={readOnly} />
        </>
      )}
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
