import { Client, StompSubscription } from "@stomp/stompjs";
import SockJS from "sockjs-client";

type Listener = (payload: unknown) => void;

type Entry = {
  sub?: StompSubscription;
  listeners: Set<Listener>;
};

class WebchatSocket {
  private client: Client | null = null;
  private subs = new Map<string, Entry>();
  private connectListeners = new Set<() => void>();

  connect() {
    if (this.client) return;
    this.client = new Client({
      webSocketFactory: () => new SockJS("/ws"),
      reconnectDelay: 2000,
      debug: () => {},
      onConnect: () => {
        this.resubscribeAll();
        this.connectListeners.forEach((l) => l());
      },
    });
    this.client.activate();
  }

  disconnect() {
    if (!this.client) return;
    for (const entry of this.subs.values()) entry.sub?.unsubscribe();
    this.subs.clear();
    this.client.deactivate();
    this.client = null;
  }

  connected(): boolean {
    return this.client?.connected ?? false;
  }

  onConnect(listener: () => void): () => void {
    this.connectListeners.add(listener);
    if (this.connected()) listener();
    return () => this.connectListeners.delete(listener);
  }

  publish(destination: string, body: unknown) {
    if (!this.client?.connected) return;
    this.client.publish({ destination, body: JSON.stringify(body ?? {}) });
  }

  subscribe(destination: string, listener: Listener): () => void {
    let entry = this.subs.get(destination);
    if (!entry) {
      entry = { listeners: new Set() };
      this.subs.set(destination, entry);
      this.attach(destination, entry);
    }
    entry.listeners.add(listener);
    return () => {
      entry!.listeners.delete(listener);
      if (entry!.listeners.size === 0) {
        entry!.sub?.unsubscribe();
        this.subs.delete(destination);
      }
    };
  }

  private attach(destination: string, entry: Entry) {
    if (!this.client?.connected) return;
    entry.sub = this.client.subscribe(destination, (message) => {
      let payload: unknown;
      try {
        payload = JSON.parse(message.body);
      } catch {
        payload = message.body;
      }
      entry.listeners.forEach((l) => l(payload));
    });
  }

  private resubscribeAll() {
    for (const [destination, entry] of this.subs) {
      this.attach(destination, entry);
    }
  }
}

export const ws = new WebchatSocket();
