import { useEffect, useRef } from "react";
import { tabId } from "../lib/tabId";
import { ws } from "../lib/ws";
import { useAuthStore } from "../stores/authStore";
import { Status, usePresenceStore } from "../stores/presenceStore";

type Update = { userId: number; status: Status };

/**
 * Lives at the root of the authenticated shell. Subscribes to the per-user presence topic,
 * drives watch/unwatch traffic, and sends activity pings.
 */
// When the user resumes activity (mouse move, key press, etc.) push an
// out-of-band ping so AFK→ONLINE transitions don't have to wait for the
// 15s heartbeat. Throttled to avoid spamming the broker.
const ACTIVITY_PING_MIN_INTERVAL_MS = 2000;

export default function PresenceManager() {
  const lastActivityRef = useRef<number>(Date.now());
  const lastPingRef = useRef<number>(0);
  const setStatus = usePresenceStore((s) => s.setStatus);
  const meId = useAuthStore((s) => s.me?.id);

  useEffect(() => {
    const sendActivityPing = () => {
      if (!ws.connected()) return;
      const now = Date.now();
      if (now - lastPingRef.current < ACTIVITY_PING_MIN_INTERVAL_MS) return;
      lastPingRef.current = now;
      ws.publish("/app/presence/ping", {
        tabId: tabId(),
        lastActivityAt: lastActivityRef.current,
      });
    };
    const bump = () => {
      lastActivityRef.current = Date.now();
      sendActivityPing();
    };
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
    if (!meId) return;
    const topic = `/topic/user.${meId}.presence`;
    console.log("[presence] subscribing to", topic);
    const unsub = ws.subscribe(topic, (payload) => {
      console.log("[presence] payload received", payload);
      if (Array.isArray(payload)) (payload as Update[]).forEach((u) => setStatus(u.userId, u.status));
      else if (payload && typeof payload === "object") {
        const u = payload as Update;
        if (u.userId != null && u.status) setStatus(u.userId, u.status);
      }
    });
    return unsub;
  }, [meId, setStatus]);

  useEffect(() => {
    const id = tabId();
    const sendPing = () => {
      if (!ws.connected()) return;
      lastPingRef.current = Date.now();
      ws.publish("/app/presence/ping", {
        tabId: id,
        lastActivityAt: lastActivityRef.current,
      });
    };
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
