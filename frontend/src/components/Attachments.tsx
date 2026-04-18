import { useState } from "react";
import type { AttachmentDto } from "../lib/messageTypes";

export function Attachments({ list }: { list: AttachmentDto[] }) {
  if (!list || list.length === 0) return null;
  return (
    <div className="mt-1 space-y-1">
      {list.map((a) => (a.mimeType.startsWith("image/")
        ? <ImageAttachment key={a.id} a={a} />
        : <FileAttachment key={a.id} a={a} />))}
    </div>
  );
}

function ImageAttachment({ a }: { a: AttachmentDto }) {
  const [expanded, setExpanded] = useState(false);
  const url = `/api/attachments/${a.id}`;
  return (
    <div className="inline-block">
      <img src={url} alt={a.originalName}
           onClick={() => setExpanded(!expanded)}
           className={`rounded border cursor-zoom-in object-contain bg-slate-100 ${expanded ? "max-h-[70vh]" : "max-h-48"}`} />
      {a.comment && <div className="text-xs text-slate-500 mt-0.5">{a.comment}</div>}
    </div>
  );
}

function FileAttachment({ a }: { a: AttachmentDto }) {
  return (
    <div className="inline-block border rounded bg-slate-50 px-3 py-2 text-sm">
      <a href={`/api/attachments/${a.id}`} download={a.originalName}
         className="font-medium text-slate-800 hover:underline">{a.originalName}</a>
      <span className="ml-2 text-xs text-slate-500">{humanSize(a.sizeBytes)}</span>
      {a.comment && <div className="text-xs text-slate-500 mt-0.5">{a.comment}</div>}
    </div>
  );
}

function humanSize(bytes: number): string {
  if (bytes < 1024) return bytes + " B";
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB";
  return (bytes / (1024 * 1024)).toFixed(1) + " MB";
}
