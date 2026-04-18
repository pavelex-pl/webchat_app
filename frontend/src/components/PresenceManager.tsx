import { useEffect, useRef } from "react";
import { tabId } from "../lib/tabId";
import { ws } from "../lib/ws";
import { Status, usePresenceStore } from "../stores/presenceStore";

type Update = { userId: number; status: Status };

/**
 * Lives at the root of the authenticated shell. Subscribes to the per-user presence queue,
 * drives watch/unwatch traffic, and sends activity pings.
 */
export default function PresenceManager() {
  const lastActivityRef = useRef<number>(Date.now());
  const setStatus = usePresenceStore((s) => s.setStatus);

  useEffect(() => {
    const bump = () => { lastActivityRef.current = Date.now(); };
    const onVisibility = () => { if (!document.hidden) bump(); };
    window.addEventListener("mousemove", bump, { passive: true });
    window.addEventListener("keydown", bump);
    window.addEventListener("scroll", bump, { passive: true });
    window.addEventListener("touchstart", bump, { passive: true });
    document.addEventListener("visibilitychange", onVisibility);
    return () => {
      window.removeEventListener("mousemove", bump);
      window.removeEventListener("keydown", bump);
      window.removeEventListener("scroll", bump);
      window.removeEventListener("touchstart", bump);
      document.removeEventListener("visibilitychange", onVisibility);
    };
  }, []);

  useEffect(() => {
    const unsub = ws.subscribe("/user/queue/presence", (payload) => {
      if (Array.isArray(payload)) (payload as Update[]).forEach((u) => setStatus(u.userId, u.status));
      else if (payload && typeof payload === "object") {
        const u = payload as Update;
        if (u.userId != null && u.status) setStatus(u.userId, u.status);
      }
    });
    return unsub;
  }, [setStatus]);

  useEffect(() => {
    const id = tabId();
    const sendPing = () => ws.publish("/app/presence/ping", {
      tabId: id,
      lastActivityAt: lastActivityRef.current,
    });
    const unsubConnect = ws.onConnect(() => { lastActivityRef.current = Date.now(); sendPing(); });
    const handle = window.setInterval(sendPing, 15_000);
    return () => { window.clearInterval(handle); unsubConnect(); };
  }, []);

  useEffect(() => {
    const flush = () => {
      const { adds, removes } = usePresenceStore.getState().flushPending();
      if (adds.length > 0) ws.publish("/app/watch", { userIds: adds });
      if (removes.length > 0) ws.publish("/app/unwatch", { userIds: removes });
    };
    const handle = window.setInterval(flush, 500);
    const unsubStore = usePresenceStore.subscribe(flush);
    const unsubConnect = ws.onConnect(flush);
    return () => { window.clearInterval(handle); unsubStore(); unsubConnect(); };
  }, []);

  return null;
}
