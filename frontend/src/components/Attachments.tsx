import { useState } from "react";
import type { AttachmentDto } from "../lib/messageTypes";

export function Attachments({ list, readOnly = false }: { list: AttachmentDto[]; readOnly?: boolean }) {
  if (!list || list.length === 0) return null;
  return (
    <div className="mt-1 space-y-1">
      {list.map((a) => (a.mimeType.startsWith("image/")
        ? <ImageAttachment key={a.id} a={a} readOnly={readOnly} />
        : <FileAttachment key={a.id} a={a} readOnly={readOnly} />))}
    </div>
  );
}

function ImageAttachment({ a, readOnly }: { a: AttachmentDto; readOnly: boolean }) {
  const [expanded, setExpanded] = useState(false);
  if (readOnly) {
    return (
      <div className="inline-block border rounded bg-slate-50 px-3 py-2 text-sm text-slate-500 italic">
        Image unavailable in read-only conversation
        {a.comment && <div className="text-xs text-slate-500 mt-0.5 not-italic">{a.comment}</div>}
      </div>
    );
  }
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

function FileAttachment({ a, readOnly }: { a: AttachmentDto; readOnly: boolean }) {
  if (readOnly) {
    return (
      <div className="inline-block border rounded bg-slate-50 px-3 py-2 text-sm">
        <span className="font-medium text-slate-500 italic">{a.originalName}</span>
        <span className="ml-2 text-xs text-slate-500">{humanSize(a.sizeBytes)}</span>
        <div className="text-xs text-slate-500 mt-0.5">Download unavailable in read-only conversation</div>
        {a.comment && <div className="text-xs text-slate-500 mt-0.5">{a.comment}</div>}
      </div>
    );
  }
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
