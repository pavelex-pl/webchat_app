import { Outlet } from "react-router-dom";
import NotificationManager from "./NotificationManager";
import PresenceManager from "./PresenceManager";
import Sidebar from "./Sidebar";
import TopBar from "./TopBar";

export default function AppLayout() {
  return (
    <div className="h-screen bg-slate-100 flex flex-col">
      <PresenceManager />
      <NotificationManager />
      <TopBar />
      <div className="flex-1 min-h-0 flex">
        <Sidebar />
        <main className="flex-1 min-w-0 flex flex-col overflow-hidden">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
