import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import FormError from "../components/FormError";
import PresenceDot from "../components/PresenceDot";
import { ApiError, api } from "../lib/api";
import type { ChatDetail, Friendship, UserSummary } from "../lib/types";

export default function FriendsPage() {
  return (
    <div className="p-6 max-w-3xl mx-auto space-y-6">
      <h1 className="text-xl font-semibold text-slate-800">Friends</h1>
      <AddFriend />
      <Incoming />
      <Outgoing />
      <CurrentFriends />
      <Blocks />
    </div>
  );
}

function AddFriend() {
  const qc = useQueryClient();
  const [username, setUsername] = useState("");
  const [text, setText] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [ok, setOk] = useState<string | null>(null);
  const m = useMutation({
    mutationFn: () => api.post("/api/friends/requests", { username, text }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["friends"] });
      qc.invalidateQueries({ queryKey: ["friends", "outgoing"] });
      setUsername(""); setText(""); setOk("Request sent");
    },
    onError: (e) => setError(e instanceof ApiError ? e.detail : String(e)),
  });
  function submit(e: FormEvent) { e.preventDefault(); setError(null); setOk(null); m.mutate(); }
  return (
    <section className="bg-white rounded shadow p-4 space-y-2">
      <h2 className="font-semibold text-slate-800">Add a friend</h2>
      <FormError error={error} />
      {ok && <div className="text-sm text-green-700 bg-green-50 p-2 rounded">{ok}</div>}
      <form onSubmit={submit} className="space-y-2">
        <input value={username} onChange={(e) => setUsername(e.target.value)} placeholder="Username"
               className="w-full border rounded px-3 py-2 text-sm" />
        <input value={text} onChange={(e) => setText(e.target.value)}
               placeholder="Optional message" maxLength={500}
               className="w-full border rounded px-3 py-2 text-sm" />
        <button disabled={!username || m.isPending} className="px-3 py-1.5 bg-slate-800 text-white rounded text-sm">
          {m.isPending ? "Sending..." : "Send request"}
        </button>
      </form>
    </section>
  );
}

function Incoming() {
  const qc = useQueryClient();
  const q = useQuery({ queryKey: ["friends", "incoming"], queryFn: () => api.get<Friendship[]>("/api/friends/requests/incoming") });
  const accept = useMutation({
    mutationFn: (id: number) => api.post(`/api/friends/requests/${id}/accept`),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["friends", "incoming"] }); qc.invalidateQueries({ queryKey: ["friends"] }); },
  });
  const decline = useMutation({
    mutationFn: (id: number) => api.post(`/api/friends/requests/${id}/decline`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["friends", "incoming"] }),
  });
  if (!q.data || q.data.length === 0) return null;
  return (
    <Section title="Incoming requests">
      {q.data.map((f) => (
        <Row key={f.userId} name={f.username} right={
          <>
            <span className="text-xs text-slate-500 italic">{f.requestText ?? ""}</span>
            <button onClick={() => accept.mutate(f.userId)} className="px-2 py-1 bg-slate-800 text-white rounded text-xs">Accept</button>
            <button onClick={() => decline.mutate(f.userId)} className="px-2 py-1 border rounded text-xs">Decline</button>
          </>
        } />
      ))}
    </Section>
  );
}

function Outgoing() {
  const qc = useQueryClient();
  const q = useQuery({ queryKey: ["friends", "outgoing"], queryFn: () => api.get<Friendship[]>("/api/friends/requests/outgoing") });
  const cancel = useMutation({
    mutationFn: (id: number) => api.delete(`/api/friends/requests/${id}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["friends", "outgoing"] }),
  });
  if (!q.data || q.data.length === 0) return null;
  return (
    <Section title="Outgoing requests">
      {q.data.map((f) => (
        <Row key={f.userId} name={f.username} right={
          <button onClick={() => cancel.mutate(f.userId)} className="px-2 py-1 border rounded text-xs">Cancel</button>
        } />
      ))}
    </Section>
  );
}

function CurrentFriends() {
  const qc = useQueryClient();
  const nav = useNavigate();
  const q = useQuery({ queryKey: ["friends"], queryFn: () => api.get<Friendship[]>("/api/friends") });

  const openDm = useMutation({
    mutationFn: (username: string) => api.post<ChatDetail>("/api/chats/direct", { username }),
    onSuccess: (chat) => { qc.invalidateQueries({ queryKey: ["chats"] }); nav(`/chat/${chat.id}`); },
  });
  const remove = useMutation({
    mutationFn: (id: number) => api.delete(`/api/friends/${id}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["friends"] }),
  });
  const block = useMutation({
    mutationFn: (id: number) => api.post(`/api/friends/block/${id}`),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ["friends"] });
      qc.invalidateQueries({ queryKey: ["friends", "blocks"] });
    },
  });

  return (
    <Section title={`Friends (${q.data?.length ?? 0})`}>
      {q.data && q.data.length === 0 && <p className="text-sm text-slate-500 px-2">No friends yet.</p>}
      {q.data?.map((f) => (
        <Row key={f.userId} name={f.username} userId={f.userId} right={
          <>
            <button onClick={() => openDm.mutate(f.username)} className="px-2 py-1 bg-slate-800 text-white rounded text-xs">Message</button>
            <button onClick={() => remove.mutate(f.userId)} className="px-2 py-1 border rounded text-xs">Remove</button>
            <button onClick={() => block.mutate(f.userId)} className="px-2 py-1 text-red-600 border border-red-300 rounded text-xs">Block</button>
          </>
        } />
      ))}
    </Section>
  );
}

function Blocks() {
  const qc = useQueryClient();
  const q = useQuery({ queryKey: ["friends", "blocks"], queryFn: () => api.get<UserSummary[]>("/api/friends/blocks") });
  const unblock = useMutation({
    mutationFn: (id: number) => api.delete(`/api/friends/block/${id}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["friends", "blocks"] }),
  });
  if (!q.data || q.data.length === 0) return null;
  return (
    <Section title="Blocked users">
      {q.data.map((u) => (
        <Row key={u.id} name={u.username} right={
          <button onClick={() => unblock.mutate(u.id)} className="px-2 py-1 border rounded text-xs">Unblock</button>
        } />
      ))}
    </Section>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="bg-white rounded shadow p-4 space-y-2">
      <h2 className="font-semibold text-slate-800">{title}</h2>
      <div className="divide-y">{children}</div>
    </section>
  );
}

function Row({ name, right, userId }: { name: string; right: React.ReactNode; userId?: number }) {
  return (
    <div className="py-2 flex items-center gap-2 justify-between">
      <div className="flex items-center gap-2 min-w-0">
        {userId != null && <PresenceDot userId={userId} />}
        <span className="font-mono text-sm truncate">@{name}</span>
      </div>
      <div className="flex items-center gap-2">{right}</div>
    </div>
  );
}
