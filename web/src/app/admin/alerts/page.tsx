"use client";

import { useEffect, useState, useCallback } from "react";
import { admin, AdminAlert } from "@/lib/api";
import {
  Search,
  ChevronLeft,
  ChevronRight,
  Trash2,
  Mail,
  MailOpen,
  CheckCircle,
} from "lucide-react";
import clsx from "clsx";

const PAGE_SIZE = 20;

const ALERT_TYPE_COLORS: Record<string, string> = {
  new_paper: "bg-blue-100 text-blue-600",
  followed_author: "bg-purple-100 text-purple-600",
  overlap_critical: "bg-red-100 text-red-600",
  overlap_high: "bg-orange-100 text-orange-600",
  overlap_moderate: "bg-yellow-100 text-yellow-600",
  must_cite: "bg-teal-100 text-teal-600",
};

export default function AdminAlertsPage() {
  const [alerts, setAlerts] = useState<AdminAlert[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [typeFilter, setTypeFilter] = useState("");
  const [readFilter, setReadFilter] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await admin.alerts({
        type: typeFilter || undefined,
        is_read: readFilter === "" ? undefined : readFilter === "true",
        limit: PAGE_SIZE,
        offset: page * PAGE_SIZE,
      });
      setAlerts(res.items);
      setTotal(res.total);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to load alerts");
    }
    setLoading(false);
  }, [typeFilter, readFilter, page]);

  useEffect(() => {
    load();
  }, [load]);

  const totalPages = Math.ceil(total / PAGE_SIZE);

  const handleDelete = async (id: string) => {
    if (!confirm("Delete this alert?")) return;
    try {
      await admin.deleteAlert(id);
      setAlerts((prev) => prev.filter((a) => a.id !== id));
      setTotal((t) => t - 1);
    } catch (e) {
      alert(e instanceof Error ? e.message : "Delete failed");
    }
  };

  return (
    <div className="p-8">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-primary">Alert Management</h1>
        <p className="text-sm text-slate-400 mt-1">
          {total} total alert{total !== 1 ? "s" : ""}
        </p>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap items-center gap-3 mb-6">
        <select
          value={typeFilter}
          onChange={(e) => {
            setTypeFilter(e.target.value);
            setPage(0);
          }}
          className="rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-800 focus:border-primary focus:outline-none"
        >
          <option value="">All Types</option>
          <option value="new_paper">New Paper</option>
          <option value="followed_author">Followed Author</option>
          <option value="overlap_critical">Overlap Critical</option>
          <option value="overlap_high">Overlap High</option>
          <option value="overlap_moderate">Overlap Moderate</option>
          <option value="must_cite">Must Cite</option>
        </select>
        <select
          value={readFilter}
          onChange={(e) => {
            setReadFilter(e.target.value);
            setPage(0);
          }}
          className="rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-800 focus:border-primary focus:outline-none"
        >
          <option value="">All</option>
          <option value="false">Unread</option>
          <option value="true">Read</option>
        </select>
      </div>

      {/* Table */}
      <div className="rounded-2xl border border-slate-200 bg-white overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50">
                <th className="px-5 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  Alert
                </th>
                <th className="px-5 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  Type
                </th>
                <th className="px-5 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  User
                </th>
                <th className="px-5 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  Paper
                </th>
                <th className="px-5 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  Status
                </th>
                <th className="px-5 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  Created
                </th>
                <th className="px-5 py-3 text-right text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={7} className="px-5 py-12 text-center text-slate-400">
                    <div className="flex justify-center">
                      <div className="h-6 w-6 animate-spin rounded-full border-2 border-primary border-t-transparent" />
                    </div>
                  </td>
                </tr>
              ) : error ? (
                <tr>
                  <td colSpan={7} className="px-5 py-12 text-center text-red-400">
                    {error}
                    <button onClick={load} className="ml-2 underline hover:text-red-600">Retry</button>
                  </td>
                </tr>
              ) : alerts.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-5 py-12 text-center text-slate-400">
                    No alerts found
                  </td>
                </tr>
              ) : (
                alerts.map((a) => (
                  <tr
                    key={a.id}
                    className="border-b border-slate-50 hover:bg-slate-50 transition-colors"
                  >
                    <td className="px-5 py-4">
                      <p className="font-medium text-slate-800 truncate max-w-[200px]">
                        {a.title}
                      </p>
                    </td>
                    <td className="px-5 py-4">
                      <span
                        className={clsx(
                          "inline-flex rounded-full px-2.5 py-1 text-xs font-medium",
                          ALERT_TYPE_COLORS[a.type] || "bg-slate-100 text-slate-600"
                        )}
                      >
                        {a.type.replace(/_/g, " ")}
                      </span>
                    </td>
                    <td className="px-5 py-4 text-slate-500 text-xs">{a.user_email}</td>
                    <td className="px-5 py-4">
                      <p className="text-slate-500 text-xs truncate max-w-[180px]" title={a.paper_title}>
                        {a.paper_title}
                      </p>
                    </td>
                    <td className="px-5 py-4">
                      <div className="flex items-center gap-2">
                        {a.is_read ? (
                          <MailOpen className="h-4 w-4 text-slate-300" />
                        ) : (
                          <Mail className="h-4 w-4 text-blue-500" />
                        )}
                        {a.is_acknowledged && (
                          <CheckCircle className="h-4 w-4 text-green-500" />
                        )}
                      </div>
                    </td>
                    <td className="px-5 py-4 text-slate-400 text-xs">
                      {new Date(a.created_at).toLocaleDateString()}
                    </td>
                    <td className="px-5 py-4 text-right">
                      <button
                        onClick={() => handleDelete(a.id)}
                        className="rounded-lg p-2 text-slate-400 hover:bg-red-50 hover:text-red-500 transition-colors"
                        title="Delete alert"
                      >
                        <Trash2 className="h-4 w-4" />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between mt-4">
          <p className="text-sm text-slate-400">
            Showing {page * PAGE_SIZE + 1}–
            {Math.min((page + 1) * PAGE_SIZE, total)} of {total}
          </p>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="rounded-lg border border-slate-200 p-2 text-slate-500 hover:bg-slate-100 disabled:opacity-30 disabled:cursor-not-allowed"
            >
              <ChevronLeft className="h-4 w-4" />
            </button>
            <span className="text-sm text-slate-500">
              {page + 1} / {totalPages}
            </span>
            <button
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1}
              className="rounded-lg border border-slate-200 p-2 text-slate-500 hover:bg-slate-100 disabled:opacity-30 disabled:cursor-not-allowed"
            >
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
