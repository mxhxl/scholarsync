"use client";

import { useState, useEffect, useMemo } from "react";
import {
  Bell,
  BookOpen,
  RefreshCw,
  AlertTriangle,
  CheckCheck,
} from "lucide-react";
import { alerts as alertsApi, Alert, AlertsResponse } from "@/lib/api";
import PageHeader from "@/components/PageHeader";
import EmptyState from "@/components/EmptyState";
import clsx from "clsx";

export default function AlertsPage() {
  const [data, setData] = useState<AlertsResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadAlerts();
  }, []);

  async function loadAlerts() {
    setLoading(true);
    try {
      const d = await alertsApi.list();
      setData(d);
    } catch {
      setData(MOCK_ALERTS);
    } finally {
      setLoading(false);
    }
  }

  async function markAllRead() {
    if (!data) return;
    const unread = data.items.filter((a) => !a.is_read);
    await Promise.all(unread.map((a) => alertsApi.acknowledge(a.id).catch(() => {})));
    setData({ ...data, items: data.items.map((a) => ({ ...a, is_read: true })) });
  }

  const { unreadCount, newPapers, overlaps, updates } = useMemo(() => {
    const items = data?.items ?? [];
    return {
      unreadCount: items.filter((a) => !a.is_read).length,
      newPapers: items.filter((a) => a.type === "new_paper" || a.type === "high_relevance"),
      overlaps: items.filter((a) => a.type === "overlap"),
      updates: items.filter((a) => a.type === "update" || a.type === "system"),
    };
  }, [data]);

  return (
    <div className="min-h-screen bg-slate-50">
      <PageHeader
        title="Alerts"
        subtitle={data ? `${unreadCount} unread` : undefined}
        actions={
          <div className="flex items-center gap-2">
            {data && unreadCount > 0 && (
              <button
                onClick={markAllRead}
                className="btn-ghost text-sm flex items-center gap-1"
              >
                <CheckCheck className="h-4 w-4" />
                Mark all read
              </button>
            )}
          </div>
        }
      />

      <div className="px-8 py-6 space-y-8">
        {loading ? (
          <div className="space-y-4">
            {[1, 2, 3].map((i) => (
              <div key={i} className="card p-5 animate-pulse">
                <div className="h-4 w-48 bg-slate-100 rounded mb-2" />
                <div className="h-4 w-full bg-slate-100 rounded" />
              </div>
            ))}
          </div>
        ) : !data || data.items.length === 0 ? (
          <EmptyState
            icon={Bell}
            title="No alerts yet"
            description="You'll be notified when new papers match your research profile or overlap with your projects."
          />
        ) : (
          <>
            {newPapers.length > 0 && (
              <AlertSection
                icon={BookOpen}
                title="New Papers"
                alerts={newPapers}
                onAcknowledge={async (id) => {
                  await alertsApi.acknowledge(id).catch(() => {});
                  setData({
                    ...data,
                    items: data.items.map((a) =>
                      a.id === id ? { ...a, is_read: true } : a
                    ),
                  });
                }}
              />
            )}

            {overlaps.length > 0 && (
              <AlertSection
                icon={AlertTriangle}
                title="Overlap Detection"
                alerts={overlaps}
                onAcknowledge={async (id) => {
                  await alertsApi.acknowledge(id).catch(() => {});
                  setData({
                    ...data,
                    items: data.items.map((a) =>
                      a.id === id ? { ...a, is_read: true } : a
                    ),
                  });
                }}
              />
            )}

            {updates.length > 0 && (
              <AlertSection
                icon={RefreshCw}
                title="App Updates"
                alerts={updates}
                onAcknowledge={async (id) => {
                  await alertsApi.acknowledge(id).catch(() => {});
                  setData({
                    ...data,
                    items: data.items.map((a) =>
                      a.id === id ? { ...a, is_read: true } : a
                    ),
                  });
                }}
              />
            )}
          </>
        )}
      </div>
    </div>
  );
}

function AlertSection({
  icon: Icon,
  title,
  alerts,
  onAcknowledge,
}: {
  icon: any;
  title: string;
  alerts: Alert[];
  onAcknowledge: (id: string) => void;
}) {
  return (
    <div>
      <div className="flex items-center gap-2 mb-4">
        <Icon className="h-4 w-4 text-primary" />
        <span className="section-label">{title}</span>
      </div>
      <div className="space-y-3">
        {alerts.map((alert) => (
          <div
            key={alert.id}
            className={clsx(
              "card overflow-hidden transition-all",
              !alert.is_read && "border-l-4 border-l-primary shadow-md"
            )}
          >
            <div className="p-5">
              <div className="flex items-start justify-between gap-4">
                <div className="flex-1 min-w-0">
                  <h4
                    className={clsx(
                      "text-sm font-bold",
                      alert.is_read ? "text-slate-600" : "text-primary"
                    )}
                  >
                    {alert.title}
                  </h4>
                  <p className="mt-1 text-sm text-slate-500 line-clamp-2 leading-relaxed">
                    {alert.description}
                  </p>
                  {alert.similarity_score != null && (
                    <span className="mt-2 inline-block chip bg-accent-teal/10 text-accent-teal text-[11px]">
                      {Math.round(alert.similarity_score * 100)}% similarity
                    </span>
                  )}
                </div>
                <div className="flex flex-col items-end gap-2">
                  <span className="text-[11px] text-slate-400 whitespace-nowrap">
                    {formatRelativeTime(alert.created_at)}
                  </span>
                  {!alert.is_read && (
                    <button
                      onClick={() => onAcknowledge(alert.id)}
                      className="text-[11px] font-semibold text-primary hover:underline"
                    >
                      Mark read
                    </button>
                  )}
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function formatRelativeTime(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime();
  const min = Math.floor(diff / 60000);
  if (min < 1) return "Just now";
  if (min < 60) return `${min}m ago`;
  const hrs = Math.floor(min / 60);
  if (hrs < 24) return `${hrs}h ago`;
  const days = Math.floor(hrs / 24);
  if (days === 1) return "Yesterday";
  return `${days}d ago`;
}

const MOCK_ALERTS: AlertsResponse = {
  total: 4,
  items: [
    {
      id: "a1",
      user_id: "u1",
      paper_id: "p1",
      paper: null as any,
      type: "high_relevance",
      title: "Large Language Models in Quantum Physics",
      description:
        "A new breakthrough paper matching your \"Quantum AI\" watch-list has been published. This paper introduces a novel hybrid approach combining LLMs with quantum circuit simulation.",
      similarity_score: 0.93,
      comparison_report: null,
      is_read: false,
      is_acknowledged: false,
      created_at: new Date(Date.now() - 2 * 60000).toISOString(),
    },
    {
      id: "a2",
      user_id: "u1",
      paper_id: "p2",
      paper: null as any,
      type: "new_paper",
      title: "Neural Architecture Search Efficiency",
      description:
        "New findings on reducing compute costs for NAS by up to 40%, directly relevant to your current project on efficient model design.",
      similarity_score: 0.85,
      comparison_report: null,
      is_read: false,
      is_acknowledged: false,
      created_at: new Date(Date.now() - 3600000).toISOString(),
    },
    {
      id: "a3",
      user_id: "u1",
      paper_id: "p3",
      paper: null as any,
      type: "overlap",
      title: "Potential Research Overlap Detected",
      description:
        "A recently published paper shares 78% thematic overlap with your current project description. Review the comparison report to assess.",
      similarity_score: 0.78,
      comparison_report: null,
      is_read: false,
      is_acknowledged: false,
      created_at: new Date(Date.now() - 7200000).toISOString(),
    },
    {
      id: "a4",
      user_id: "u1",
      paper_id: "p4",
      paper: null as any,
      type: "system",
      title: "New AI Model Integrated",
      description:
        "ScholarSync now uses updated models for even more accurate research summaries and improved overlap detection.",
      similarity_score: 0,
      comparison_report: null,
      is_read: true,
      is_acknowledged: true,
      created_at: new Date(Date.now() - 86400000).toISOString(),
    },
  ],
};
