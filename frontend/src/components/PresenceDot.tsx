import { useEffect } from "react";
import { Status, usePresenceStore } from "../stores/presenceStore";

export default function PresenceDot({ userId, className = "" }: { userId: number; className?: string }) {
  const status = usePresenceStore((s) => s.statuses[userId]) ?? "OFFLINE";
  const addWatch = usePresenceStore((s) => s.addWatch);
  const removeWatch = usePresenceStore((s) => s.removeWatch);
  useEffect(() => {
    addWatch(userId);
    return () => removeWatch(userId);
  }, [userId, addWatch, removeWatch]);
  return (
    <span
      title={label(status)}
      className={`inline-block w-2 h-2 rounded-full ${color(status)} ${className}`}
    />
  );
}

function color(status: Status): string {
  if (status === "ONLINE") return "bg-green-500";
  if (status === "AFK") return "bg-yellow-500";
  return "bg-slate-300";
}

function label(status: Status): string {
  if (status === "ONLINE") return "online";
  if (status === "AFK") return "away";
  return "offline";
}
