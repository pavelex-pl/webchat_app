import { useAuthStore } from "../stores/authStore";

export default function HomePage() {
  const me = useAuthStore((s) => s.me);
  return (
    <div className="p-10 text-center text-slate-500">
      <h1 className="text-2xl text-slate-800 font-semibold mb-2">Welcome, {me?.username}</h1>
      <p className="text-sm">Select a room from the left, or browse the public catalog to get started.</p>
    </div>
  );
}
