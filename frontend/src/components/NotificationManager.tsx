import { useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import { ws } from "../lib/ws";
import { useAuthStore } from "../stores/authStore";

/**
 * Subscribes to the per-user notifications topic and invalidates the sidebar
 * queries so unread counts and invitation badges refresh in real time.
 */
export default function NotificationManager() {
  const qc = useQueryClient();
  const meId = useAuthStore((s) => s.me?.id);
  useEffect(() => {
    if (!meId) return;
    const unsub = ws.subscribe(`/topic/user.${meId}.notifications`, (payload) => {
      const type = (payload as { type?: string } | null)?.type;
      if (type === "invitation") {
        qc.invalidateQueries({ queryKey: ["invitations"] });
        // an accepted invitation also changes the chat list
        qc.invalidateQueries({ queryKey: ["chats"] });
      } else if (type === "friend_request") {
        // refresh whichever friend views depend on this event
        qc.invalidateQueries({ queryKey: ["friends"] });
      } else {
        qc.invalidateQueries({ queryKey: ["chats"] });
      }
    });
    return unsub;
  }, [meId, qc]);
  return null;
}
