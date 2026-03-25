"use client";

import Link from "next/link";
import Image from "next/image";
import { usePathname } from "next/navigation";
import {
  LayoutDashboard,
  Users,
  FileText,
  Bell,
  Settings,
  LogOut,
  ArrowLeft,
  Shield,
} from "lucide-react";
import { useAuth } from "@/hooks/useAuth";
import clsx from "clsx";

const NAV_ITEMS = [
  { href: "/admin", label: "Dashboard", icon: LayoutDashboard, exact: true },
  { href: "/admin/users", label: "Users", icon: Users },
  { href: "/admin/papers", label: "Papers", icon: FileText },
  { href: "/admin/alerts", label: "Alerts", icon: Bell },
  { href: "/admin/settings", label: "Settings", icon: Settings },
];

export default function AdminSidebar() {
  const pathname = usePathname();
  const { user, logout } = useAuth();

  return (
    <aside
      className="fixed left-0 top-0 z-40 flex h-screen w-64 flex-col border-r border-[#0a2444]"
      style={{ backgroundColor: "#0C2C55" }}
    >
      {/* Logo */}
      <div className="flex items-center gap-3 px-6 py-6 border-b border-white/10">
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-white/10">
          <Image src="/favicon.svg" alt="ScholarSync" width={24} height={24} className="invert" />
        </div>
        <div>
          <span className="text-lg font-bold text-white">ScholarSync</span>
          <p className="text-[10px] font-medium tracking-widest text-white/60 uppercase">
            Admin Panel
          </p>
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 space-y-1 px-3 py-4">
        {NAV_ITEMS.map(({ href, label, icon: Icon, exact }) => {
          const active = exact
            ? pathname === href
            : pathname.startsWith(href);
          return (
            <Link
              key={href}
              href={href}
              className={clsx(
                "flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-medium transition-all",
                active
                  ? "bg-white/20 text-white shadow-md"
                  : "text-white/70 hover:bg-white/10 hover:text-white"
              )}
            >
              <Icon className="h-5 w-5" />
              {label}
            </Link>
          );
        })}

        <div className="my-4 border-t border-white/10" />

        <Link
          href="/feed"
          className="flex items-center gap-3 rounded-xl px-4 py-3 text-sm font-medium text-white/70 hover:bg-white/10 hover:text-white transition-all"
        >
          <ArrowLeft className="h-5 w-5" />
          Back to App
        </Link>
      </nav>

      {/* User footer */}
      <div className="border-t border-white/10 p-4">
        <div className="flex items-center gap-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-full bg-white/10 text-sm font-bold text-white">
            {user?.full_name?.[0]?.toUpperCase() ?? "?"}
          </div>
          <div className="flex-1 min-w-0">
            <p className="truncate text-sm font-semibold text-white">
              {user?.full_name ?? "Loading..."}
            </p>
            <p className="truncate text-xs text-white/60">
              <Shield className="inline h-3 w-3 mr-1" />
              Admin
            </p>
          </div>
          <button
            onClick={logout}
            className="rounded-lg p-2 text-white/70 transition-colors hover:bg-white/10 hover:text-white"
            title="Sign out"
          >
            <LogOut className="h-4 w-4" />
          </button>
        </div>
      </div>
    </aside>
  );
}
