import { useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import { ws } from "../lib/ws";

/**
 * Subscribes to /user/queue/notifications and invalidates the sidebar "chats"
 * query so unread counts refresh for chats the user isn't currently viewing.
 */
export default function NotificationManager() {
  const qc = useQueryClient();
  useEffect(() => {
    const unsub = ws.subscribe("/user/queue/notifications", () => {
      qc.invalidateQueries({ queryKey: ["chats"] });
    });
    return unsub;
  }, [qc]);
  return null;
}
