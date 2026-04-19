import { useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import { ws } from "../lib/ws";
import { useAuthStore } from "../stores/authStore";

/**
 * Subscribes to the per-user notifications topic and invalidates the sidebar
 * "chats" query so unread counts refresh for chats the user isn't viewing.
 */
export default function NotificationManager() {
  const qc = useQueryClient();
  const meId = useAuthStore((s) => s.me?.id);
  useEffect(() => {
    if (!meId) return;
    const unsub = ws.subscribe(`/topic/user.${meId}.notifications`, () => {
      qc.invalidateQueries({ queryKey: ["chats"] });
    });
    return unsub;
  }, [meId, qc]);
  return null;
}
