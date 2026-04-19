import { useInfiniteQuery, useQueryClient } from "@tanstack/react-query";
import { Fragment, useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
import { api } from "../lib/api";
import type { ChatEvent, MessageDto } from "../lib/messageTypes";
import type { ChatDetail, ChatRole } from "../lib/types";
import { ws } from "../lib/ws";
import { useAuthStore } from "../stores/authStore";
import MessageItem from "./MessageItem";

const PAGE = 50;
// Show the "New messages" divider for this long (while conditions are met)
// before advancing the read marker past the latest message.
const READ_DWELL_MS = 2500;

export default function MessageList({
  chatId,
  yourRole,
  initialLastReadMessageId,
  onReply,
}: {
  chatId: number;
  yourRole: ChatRole | null;
  initialLastReadMessageId: number | null;
  onReply: (m: MessageDto) => void;
}) {
  const meId = useAuthStore((s) => s.me?.id);
  const qc = useQueryClient();
  const scrollRef = useRef<HTMLDivElement | null>(null);
  const stickToBottom = useRef(true);
  const prevHeight = useRef(0);

  // Advances as the user catches up (tab visible + scrolled to bottom).
  // Stays put while they're scrolled up or the tab is hidden, so the
  // "New messages" divider moves to cover every fresh incoming message.
  const [readUpTo, setReadUpTo] = useState<number>(initialLastReadMessageId ?? 0);
  useEffect(() => {
    setReadUpTo(initialLastReadMessageId ?? 0);
  }, [chatId, initialLastReadMessageId]);

  const queryKey = ["messages", chatId];
  const q = useInfiniteQuery({
    queryKey,
    initialPageParam: null as number | null,
    queryFn: ({ pageParam }) =>
      api.get<MessageDto[]>(`/api/chats/${chatId}/messages?limit=${PAGE}${pageParam ? `&before=${pageParam}` : ""}`),
    getNextPageParam: (lastPage) =>
      lastPage.length < PAGE ? undefined : lastPage[lastPage.length - 1].id,
  });

  const all = useMemo(() => {
    const flat = (q.data?.pages.flat() ?? []).slice().sort((a, b) => a.id - b.id);
    const byId = new Map<number, MessageDto>();
    for (const m of flat) byId.set(m.id, m);
    return Array.from(byId.values()).sort((a, b) => a.id - b.id);
  }, [q.data]);

  // Live updates via STOMP
  useEffect(() => {
    const unsub = ws.subscribe(`/topic/chat.${chatId}`, (payload) => {
      const ev = payload as ChatEvent;
      qc.setQueryData(queryKey, (data: { pages: MessageDto[][]; pageParams: unknown[] } | undefined) => {
        if (!data) return data;
        const pages = data.pages.map((page) => page.slice());
        if (ev.kind === "created") {
          if (!pages[0]?.some((m) => m.id === ev.message.id)) {
            pages[0] = [ev.message, ...(pages[0] ?? [])];
          }
        } else {
          for (let i = 0; i < pages.length; i++) {
            const idx = pages[i].findIndex((m) => m.id === ev.message.id);
            if (idx >= 0) { pages[i][idx] = ev.message; break; }
          }
        }
        return { ...data, pages };
      });
    });
    return unsub;
  }, [chatId, qc]);

  const latestId = all.length > 0 ? all[all.length - 1].id : 0;

  // Keep the "New messages" divider visible until the user has been reading
  // continuously for READ_DWELL_MS (tab visible + scrolled to bottom). Any
  // new incoming message resets the dwell so the divider has time to be
  // noticed. Scrolling up or hiding the tab pauses the dwell, so unread
  // stays marked as unread.
  useEffect(() => {
    if (latestId <= readUpTo) return;
    let readyStart: number | null = null;
    const handle = window.setInterval(() => {
      const ready = !document.hidden && stickToBottom.current;
      if (!ready) { readyStart = null; return; }
      if (readyStart === null) { readyStart = Date.now(); return; }
      if (Date.now() - readyStart < READ_DWELL_MS) return;
      window.clearInterval(handle);
      setReadUpTo(latestId);
      api.post(`/api/chats/${chatId}/read`, { lastReadMessageId: latestId })
        .then(() => {
          qc.invalidateQueries({ queryKey: ["chats"] });
          qc.setQueryData(["chat", chatId], (old: ChatDetail | undefined) =>
            old ? { ...old, lastReadMessageId: latestId } : old);
        })
        .catch(() => undefined);
    }, 500);
    return () => window.clearInterval(handle);
  }, [latestId, readUpTo, chatId, qc]);

  // Stick-to-bottom behavior
  useLayoutEffect(() => {
    const el = scrollRef.current;
    if (!el) return;
    if (stickToBottom.current) {
      el.scrollTop = el.scrollHeight;
    } else {
      el.scrollTop = el.scrollHeight - prevHeight.current;
    }
    prevHeight.current = el.scrollHeight;
  }, [all]);

  function onScroll() {
    const el = scrollRef.current;
    if (!el) return;
    const nearBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 50;
    stickToBottom.current = nearBottom;
    if (el.scrollTop < 80 && q.hasNextPage && !q.isFetchingNextPage) {
      prevHeight.current = el.scrollHeight;
      stickToBottom.current = false;
      q.fetchNextPage();
    }
  }

  const byId = useMemo(() => new Map(all.map((m) => [m.id, m])), [all]);

  // Show divider above the first unread message from someone else. It moves
  // forward as new messages arrive (if the user isn't catching up) and
  // disappears once the read marker advances past the latest message.
  const dividerBeforeId = useMemo(() => {
    const firstUnread = all.find((m) => m.id > readUpTo && m.authorId !== meId);
    return firstUnread ? firstUnread.id : null;
  }, [readUpTo, all, meId]);

  return (
    <div ref={scrollRef} onScroll={onScroll} className="flex-1 overflow-y-auto bg-white">
      {q.isFetchingNextPage && (
        <div className="text-center text-xs text-slate-400 py-2">Loading older...</div>
      )}
      {!q.hasNextPage && all.length > 0 && (
        <div className="text-center text-xs text-slate-400 py-2">— start of history —</div>
      )}
      {all.length === 0 && !q.isLoading && (
        <div className="h-full flex items-center justify-center text-sm text-slate-400">
          No messages yet.
        </div>
      )}
      <div className="py-2">
        {all.map((m) => (
          <Fragment key={m.id}>
            {dividerBeforeId === m.id && <NewMessagesDivider />}
            <MessageItem
              m={m}
              meId={meId}
              yourRole={yourRole}
              parent={m.replyToId ? byId.get(m.replyToId) : undefined}
              onReply={onReply}
            />
          </Fragment>
        ))}
      </div>
    </div>
  );
}

function NewMessagesDivider() {
  return (
    <div className="flex items-center gap-2 px-4 py-2 my-1">
      <div className="flex-1 h-px bg-red-400" />
      <span className="text-xs font-medium text-red-500 uppercase tracking-wide">New messages</span>
      <div className="flex-1 h-px bg-red-400" />
    </div>
  );
}
