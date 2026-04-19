import { useMutation, useQueryClient } from "@tanstack/react-query";
import { FormEvent, useState } from "react";
import { ApiError, api } from "../lib/api";
import FormError from "./FormError";
import Modal from "./Modal";

export default function InviteUserModal({
  chatId,
  roomName,
  onClose,
}: {
  chatId: number;
  roomName: string;
  onClose: () => void;
}) {
  const qc = useQueryClient();
  const [username, setUsername] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [sent, setSent] = useState<string | null>(null);

  const invite = useMutation({
    mutationFn: (name: string) => api.post(`/api/rooms/${chatId}/invitations`, { username: name }),
    onSuccess: (_d, name) => {
      setSent(name);
      setUsername("");
      qc.invalidateQueries({ queryKey: ["room", chatId, "invitations"] });
    },
    onError: (e) => setError(e instanceof ApiError ? e.detail : String(e)),
  });

  function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    setSent(null);
    if (username.trim()) invite.mutate(username.trim());
  }

  return (
    <Modal title={`Invite to #${roomName}`} onClose={onClose}>
      <form onSubmit={submit} className="space-y-3">
        <p className="text-sm text-slate-600">
          Send a private-room invitation by username. The invitee can accept or decline from their
          Invitations page.
        </p>
        <FormError error={error} />
        {sent && (
          <div className="text-sm text-green-700 bg-green-50 p-2 rounded">
            Invited <span className="font-mono">@{sent}</span>.
          </div>
        )}
        <input
          autoFocus
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          placeholder="Username"
          className="w-full border rounded px-3 py-2 text-sm"
        />
        <div className="flex justify-end gap-2">
          <button type="button" onClick={onClose} className="px-3 py-1.5 border rounded text-sm text-slate-700">
            Close
          </button>
          <button
            type="submit"
            disabled={invite.isPending || !username.trim()}
            className="px-3 py-1.5 bg-slate-800 text-white rounded text-sm disabled:opacity-50">
            Send invite
          </button>
        </div>
      </form>
    </Modal>
  );
}
