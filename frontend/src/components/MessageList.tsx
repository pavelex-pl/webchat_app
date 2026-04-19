import { useInfiniteQuery, useQueryClient } from "@tanstack/react-query";
import { Fragment, useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
import { api } from "../lib/api";
import type { ChatEvent, MessageDto } from "../lib/messageTypes";
import type { ChatDetail, ChatRole } from "../lib/types";
import { ws } from "../lib/ws";
import { useAuthStore } from "../stores/authStore";
import MessageItem from "./MessageItem";

const PAGE = 50;
const READ_GRACE_MS = 3000;

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

  // Frozen on mount so new incoming messages don't shift the divider.
  const [frozenLastRead] = useState<number | null>(initialLastReadMessageId);
  const [graceEnded, setGraceEnded] = useState(false);

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

  // 3-second grace per chat. After it elapses we hide the divider and mark as read.
  useEffect(() => {
    setGraceEnded(false);
    const handle = window.setTimeout(() => setGraceEnded(true), READ_GRACE_MS);
    return () => window.clearTimeout(handle);
  }, [chatId]);

  // Mark as read once grace ends, and again whenever a newer message lands.
  useEffect(() => {
    if (!graceEnded || all.length === 0) return;
    const lastId = all[all.length - 1].id;
    api.post(`/api/chats/${chatId}/read`, { lastReadMessageId: lastId })
      .then(() => {
        qc.invalidateQueries({ queryKey: ["chats"] });
        // keep the chat-detail cache in sync so a future re-visit freezes the
        // up-to-date lastReadMessageId and no stale divider appears.
        qc.setQueryData(["chat", chatId], (old: ChatDetail | undefined) =>
          old ? { ...old, lastReadMessageId: lastId } : old);
      })
      .catch(() => undefined);
  }, [graceEnded, all.length, chatId, qc]);

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

  // Show divider before the first message we haven't read yet (authored by
  // someone else). A null read marker is treated as 0 so first visits still
  // get the separator. Hidden once the 3-second grace window elapses.
  const dividerBeforeId = useMemo(() => {
    if (graceEnded) return null;
    const reference = frozenLastRead ?? 0;
    const firstUnread = all.find((m) => m.id > reference && m.authorId !== meId);
    return firstUnread ? firstUnread.id : null;
  }, [graceEnded, frozenLastRead, all, meId]);

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
