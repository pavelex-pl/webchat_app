import { useMutation, useQueryClient } from "@tanstack/react-query";
import { FormEvent, useState } from "react";
import { useNavigate } from "react-router-dom";
import { ApiError, api } from "../lib/api";
import type { ChatType, Room } from "../lib/types";
import FormError from "./FormError";
import Modal from "./Modal";

export default function CreateRoomModal({ onClose }: { onClose: () => void }) {
  const qc = useQueryClient();
  const nav = useNavigate();
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [type, setType] = useState<ChatType>("PUBLIC_ROOM");
  const [error, setError] = useState<string | null>(null);

  const m = useMutation({
    mutationFn: () => api.post<Room>("/api/rooms", { name, description, type }),
    onSuccess: (room) => {
      qc.invalidateQueries({ queryKey: ["chats"] });
      onClose();
      nav(`/chat/${room.id}`);
    },
    onError: (e) => setError(e instanceof ApiError ? e.detail : String(e)),
  });

  function submit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    m.mutate();
  }

  return (
    <Modal title="Create room" onClose={onClose}>
      <form onSubmit={submit} className="space-y-3">
        <FormError error={error} />
        <label className="block text-sm">
          <span className="text-slate-700">Name</span>
          <input required minLength={3} maxLength={64} value={name}
                 onChange={(e) => setName(e.target.value)}
                 className="mt-1 w-full border rounded px-3 py-2" />
        </label>
        <label className="block text-sm">
          <span className="text-slate-700">Description</span>
          <textarea value={description} onChange={(e) => setDescription(e.target.value)}
                    className="mt-1 w-full border rounded px-3 py-2" rows={3} />
        </label>
        <fieldset className="text-sm">
          <legend className="text-slate-700">Visibility</legend>
          <label className="flex items-center gap-2 mt-1">
            <input type="radio" checked={type === "PUBLIC_ROOM"} onChange={() => setType("PUBLIC_ROOM")} />
            Public — listed in catalog, anyone can join
          </label>
          <label className="flex items-center gap-2">
            <input type="radio" checked={type === "PRIVATE_ROOM"} onChange={() => setType("PRIVATE_ROOM")} />
            Private — invitation only
          </label>
        </fieldset>
        <div className="flex gap-2 justify-end">
          <button type="button" onClick={onClose} className="px-4 py-2 border rounded text-sm">Cancel</button>
          <button disabled={m.isPending} className="px-4 py-2 bg-slate-800 text-white rounded text-sm">
            {m.isPending ? "Creating..." : "Create"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
