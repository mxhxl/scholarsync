"use client";

import { useEffect, useState } from "react";
import { admin, AdminDashboard } from "@/lib/api";
import {
  Users,
  FileText,
  Bell,
  BookMarked,
  UserPlus,
  FilePlus,
  TrendingUp,
} from "lucide-react";

export default function AdminDashboardPage() {
  const [data, setData] = useState<AdminDashboard | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    admin
      .dashboard()
      .then(setData)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    );
  }

  if (!data) {
    return (
      <div className="p-8 text-slate-500">Failed to load dashboard data.</div>
    );
  }

  const stats = [
    {
      label: "Total Users",
      value: data.total_users,
      icon: Users,
      color: "bg-blue-100 text-blue-600",
    },
    {
      label: "Active Users",
      value: data.active_users,
      icon: TrendingUp,
      color: "bg-green-100 text-green-600",
    },
    {
      label: "Total Papers",
      value: data.total_papers,
      icon: FileText,
      color: "bg-purple-100 text-purple-600",
    },
    {
      label: "Total Alerts",
      value: data.total_alerts,
      icon: Bell,
      color: "bg-yellow-100 text-yellow-600",
    },
    {
      label: "Saved Papers",
      value: data.total_saved_papers,
      icon: BookMarked,
      color: "bg-teal-100 text-teal-600",
    },
    {
      label: "New Users (7d)",
      value: data.new_users_last_7_days,
      icon: UserPlus,
      color: "bg-pink-100 text-pink-600",
    },
    {
      label: "New Papers (7d)",
      value: data.new_papers_last_7_days,
      icon: FilePlus,
      color: "bg-orange-100 text-orange-600",
    },
  ];

  return (
    <div className="p-8">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-primary">Dashboard</h1>
        <p className="text-sm text-slate-400 mt-1">
          Overview of your ScholarSync platform
        </p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
        {stats.map((s) => (
          <div
            key={s.label}
            className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm"
          >
            <div className="flex items-center gap-4">
              <div
                className={`flex h-12 w-12 items-center justify-center rounded-xl ${s.color}`}
              >
                <s.icon className="h-6 w-6" />
              </div>
              <div>
                <p className="text-2xl font-bold text-slate-800">
                  {s.value.toLocaleString()}
                </p>
                <p className="text-xs text-slate-400">{s.label}</p>
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Quick links */}
      <div className="mt-10">
        <h2 className="text-lg font-semibold text-slate-800 mb-4">Quick Actions</h2>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <a
            href="/admin/users"
            className="rounded-2xl border border-slate-200 bg-white p-5 hover:border-primary/30 hover:shadow-md transition-all group"
          >
            <Users className="h-8 w-8 text-blue-500 mb-3" />
            <h3 className="text-sm font-semibold text-slate-800 group-hover:text-primary transition-colors">
              Manage Users
            </h3>
            <p className="text-xs text-slate-400 mt-1">
              View, edit, activate/deactivate user accounts
            </p>
          </a>
          <a
            href="/admin/papers"
            className="rounded-2xl border border-slate-200 bg-white p-5 hover:border-primary/30 hover:shadow-md transition-all group"
          >
            <FileText className="h-8 w-8 text-purple-500 mb-3" />
            <h3 className="text-sm font-semibold text-slate-800 group-hover:text-primary transition-colors">
              Manage Papers
            </h3>
            <p className="text-xs text-slate-400 mt-1">
              Browse, search, and remove papers from the database
            </p>
          </a>
          <a
            href="/admin/alerts"
            className="rounded-2xl border border-slate-200 bg-white p-5 hover:border-primary/30 hover:shadow-md transition-all group"
          >
            <Bell className="h-8 w-8 text-yellow-500 mb-3" />
            <h3 className="text-sm font-semibold text-slate-800 group-hover:text-primary transition-colors">
              Manage Alerts
            </h3>
            <p className="text-xs text-slate-400 mt-1">
              Monitor and manage system-generated alerts
            </p>
          </a>
        </div>
      </div>
    </div>
  );
}
