import { create } from "zustand";

export type Me = {
  id: number;
  email: string;
  username: string;
  createdAt: string;
};

type AuthState = {
  me: Me | null;
  bootstrapped: boolean;
  setMe: (me: Me | null) => void;
  setBootstrapped: (v: boolean) => void;
};

export const useAuthStore = create<AuthState>((set) => ({
  me: null,
  bootstrapped: false,
  setMe: (me) => set({ me }),
  setBootstrapped: (v) => set({ bootstrapped: v }),
}));
