import data from "@emoji-mart/data";
import Picker from "@emoji-mart/react";
import { ClipboardEvent, KeyboardEvent, useEffect, useRef, useState } from "react";
import { ApiError, api } from "../lib/api";
import type { AttachmentDto, MessageDto } from "../lib/messageTypes";

type Pending = AttachmentDto & { comment: string };

export default function Composer({
  chatId,
  replyTo,
  onClearReply,
}: {
  chatId: number;
  replyTo: MessageDto | null;
  onClearReply: () => void;
}) {
  const [body, setBody] = useState("");
  const [pending, setPending] = useState<Pending[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [sending, setSending] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [showEmoji, setShowEmoji] = useState(false);
  const fileInput = useRef<HTMLInputElement | null>(null);
  const textareaRef = useRef<HTMLTextAreaElement | null>(null);
  const emojiWrapper = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!showEmoji) return;
    function close(e: MouseEvent) {
      if (!emojiWrapper.current?.contains(e.target as Node)) setShowEmoji(false);
    }
    window.addEventListener("mousedown", close);
    return () => window.removeEventListener("mousedown", close);
  }, [showEmoji]);

  async function uploadFiles(files: FileList | File[]) {
    const list = Array.from(files);
    if (list.length === 0) return;
    setUploading(true);
    setError(null);
    try {
      for (const file of list) {
        const a = await api.upload<AttachmentDto>(`/api/chats/${chatId}/attachments`, file);
        setPending((prev) => [...prev, { ...a, comment: "" }]);
      }
    } catch (e) {
      setError(e instanceof ApiError ? e.detail : String(e));
    } finally {
      setUploading(false);
    }
  }

  function onPaste(e: ClipboardEvent<HTMLTextAreaElement>) {
    const items = e.clipboardData?.files;
    if (items && items.length > 0) {
      e.preventDefault();
      uploadFiles(items);
    }
  }

  async function send() {
    const trimmed = body.trim();
    if (!trimmed && pending.length === 0) return;
    setError(null);
    setSending(true);
    try {
      await api.post(`/api/chats/${chatId}/messages`, {
        body: trimmed || null,
        replyToId: replyTo?.id,
        attachments: pending.map((p) => ({ id: p.id, comment: p.comment || null })),
      });
      setBody("");
      setPending([]);
      onClearReply();
    } catch (e) {
      setError(e instanceof ApiError ? e.detail : String(e));
    } finally {
      setSending(false);
    }
  }

  function onKeyDown(e: KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      send();
    }
  }

  function insertEmoji(native: string) {
    const el = textareaRef.current;
    if (!el) {
      setBody((prev) => prev + native);
      return;
    }
    const start = el.selectionStart ?? body.length;
    const end = el.selectionEnd ?? body.length;
    const next = body.slice(0, start) + native + body.slice(end);
    setBody(next);
    requestAnimationFrame(() => {
      el.focus();
      const pos = start + native.length;
      el.setSelectionRange(pos, pos);
    });
  }

  function updateComment(id: number, comment: string) {
    setPending((prev) => prev.map((p) => (p.id === id ? { ...p, comment } : p)));
  }

  function removePending(id: number) {
    setPending((prev) => prev.filter((p) => p.id !== id));
  }

  const canSend = !sending && !uploading && (body.trim().length > 0 || pending.length > 0);

  return (
    <div className="border-t bg-white p-3 space-y-2">
      {replyTo && (
        <div className="text-xs flex items-center gap-2 bg-slate-100 rounded px-2 py-1">
          <span className="text-slate-600">Replying to</span>
          <span className="font-mono truncate flex-1">
            @{replyTo.authorUsername}: {replyTo.body}
          </span>
          <button onClick={onClearReply} className="text-slate-500 hover:text-slate-800">×</button>
        </div>
      )}
      {pending.length > 0 && (
        <div className="flex flex-wrap gap-2">
          {pending.map((p) => (
            <div key={p.id} className="flex items-center gap-2 border rounded bg-slate-50 px-2 py-1 text-xs max-w-xs">
              <span className="font-medium truncate">{p.originalName}</span>
              <input
                placeholder="Optional comment"
                value={p.comment}
                onChange={(e) => updateComment(p.id, e.target.value)}
                className="flex-1 border rounded px-1 py-0.5 min-w-0"
              />
              <button onClick={() => removePending(p.id)} className="text-slate-500 hover:text-slate-800">×</button>
            </div>
          ))}
        </div>
      )}
      {error && <div className="text-xs text-red-600">{error}</div>}
      <div className="relative flex items-end gap-2">
        <input
          ref={fileInput}
          type="file"
          multiple
          className="hidden"
          onChange={(e) => e.target.files && uploadFiles(e.target.files)}
        />
        <div ref={emojiWrapper} className="relative">
          <button
            type="button"
            onClick={() => setShowEmoji((s) => !s)}
            title="Emoji"
            className="px-3 py-2 border rounded text-sm text-slate-700 hover:bg-slate-50">
            😊
          </button>
          {showEmoji && (
            <div className="absolute bottom-12 left-0 z-40">
              <Picker
                data={data}
                onEmojiSelect={(e: { native: string }) => { insertEmoji(e.native); setShowEmoji(false); }}
                theme="light"
                previewPosition="none"
                skinTonePosition="none"
              />
            </div>
          )}
        </div>
        <button
          onClick={() => fileInput.current?.click()}
          disabled={uploading}
          title="Attach file"
          className="px-3 py-2 border rounded text-sm text-slate-700 hover:bg-slate-50 disabled:opacity-40">
          📎
        </button>
        <textarea
          ref={textareaRef}
          value={body}
          onChange={(e) => setBody(e.target.value)}
          onKeyDown={onKeyDown}
          onPaste={onPaste}
          rows={2}
          placeholder="Message... (Shift+Enter for newline, paste to attach)"
          className="flex-1 border rounded px-3 py-2 text-sm resize-none"
        />
        <button
          onClick={send}
          disabled={!canSend}
          className="px-4 py-2 bg-slate-800 text-white rounded text-sm disabled:opacity-40">
          {sending ? "Sending..." : "Send"}
        </button>
      </div>
    </div>
  );
}
