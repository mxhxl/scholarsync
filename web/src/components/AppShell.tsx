"use client";

import { ReactNode, useEffect } from "react";
import dynamic from "next/dynamic";
import { usePathname, useRouter } from "next/navigation";
import { useAuth } from "@/hooks/useAuth";
import Sidebar from "./Sidebar";

const AdminSidebar = dynamic(() => import("./AdminSidebar"), {
  loading: () => <div className="w-64 shrink-0" />,
});

const PUBLIC_ROUTES = ["/login", "/register", "/profile-setup", "/privacy-policy"];

export default function AppShell({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth();
  const pathname = usePathname();
  const router = useRouter();
  const isPublic = PUBLIC_ROUTES.some((r) => pathname.startsWith(r));
  const isAdmin = pathname.startsWith("/admin");
  const needsRedirect = !loading && !user && !isPublic;
  const needsAdminRedirect = !loading && user && isAdmin && user.role !== "admin";

  useEffect(() => {
    if (needsRedirect) {
      router.replace("/login");
    }
    if (needsAdminRedirect) {
      router.replace("/feed");
    }
  }, [needsRedirect, needsAdminRedirect, router]);

  if (loading || needsRedirect || needsAdminRedirect) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    );
  }

  if (isPublic) {
    return <>{children}</>;
  }

  if (isAdmin) {
    return (
      <div className="flex min-h-screen bg-slate-50">
        <AdminSidebar />
        <main className="flex-1 ml-64">{children}</main>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen">
      <Sidebar />
      <main className="flex-1 ml-64">{children}</main>
    </div>
  );
}
