import { create } from "zustand";

export type Status = "ONLINE" | "AFK" | "OFFLINE";

type State = {
  statuses: Record<number, Status>;
  watchCount: Record<number, number>;
  pendingAdd: number[];
  pendingRemove: number[];
  setStatus: (id: number, status: Status) => void;
  addWatch: (id: number) => void;
  removeWatch: (id: number) => void;
  flushPending: () => { adds: number[]; removes: number[] };
};

export const usePresenceStore = create<State>((set, get) => ({
  statuses: {},
  watchCount: {},
  pendingAdd: [],
  pendingRemove: [],
  setStatus: (id, status) => set((s) => ({ statuses: { ...s.statuses, [id]: status } })),
  addWatch: (id) => {
    const prev = get().watchCount[id] ?? 0;
    const next = prev + 1;
    set((s) => ({
      watchCount: { ...s.watchCount, [id]: next },
      pendingAdd: prev === 0 ? [...s.pendingAdd, id] : s.pendingAdd,
    }));
  },
  removeWatch: (id) => {
    const prev = get().watchCount[id] ?? 0;
    if (prev <= 0) return;
    const next = prev - 1;
    set((s) => {
      const wc = { ...s.watchCount };
      if (next <= 0) delete wc[id]; else wc[id] = next;
      return {
        watchCount: wc,
        pendingRemove: next === 0 ? [...s.pendingRemove, id] : s.pendingRemove,
      };
    });
  },
  flushPending: () => {
    const { pendingAdd, pendingRemove } = get();
    if (pendingAdd.length === 0 && pendingRemove.length === 0) {
      return { adds: [], removes: [] };
    }
    set({ pendingAdd: [], pendingRemove: [] });
    return { adds: pendingAdd, removes: pendingRemove };
  },
}));
