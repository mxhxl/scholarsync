"use client";

import { useAuth } from "@/hooks/useAuth";
import {
  Shield,
  Server,
  Database,
  Clock,
  Globe,
} from "lucide-react";

export default function AdminSettingsPage() {
  const { user } = useAuth();

  return (
    <div className="p-8 max-w-4xl">
      <div className="mb-8">
        <h1 className="text-2xl font-bold text-primary">System Settings</h1>
        <p className="text-sm text-slate-400 mt-1">
          Platform configuration and system information
        </p>
      </div>

      {/* Admin profile */}
      <div className="rounded-2xl border border-slate-200 bg-white p-6 mb-6 shadow-sm">
        <div className="flex items-center gap-4 mb-6">
          <div className="flex h-14 w-14 items-center justify-center rounded-xl bg-primary/10 text-xl font-bold text-primary">
            {user?.full_name?.[0]?.toUpperCase() ?? "?"}
          </div>
          <div>
            <h2 className="text-lg font-semibold text-slate-800">{user?.full_name}</h2>
            <p className="text-sm text-slate-400">{user?.email}</p>
            <span className="inline-flex items-center gap-1 mt-1 rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-medium text-primary">
              <Shield className="h-3 w-3" />
              Administrator
            </span>
          </div>
        </div>
      </div>

      {/* System info */}
      <div className="space-y-4">
        <h2 className="text-lg font-semibold text-slate-800">System Information</h2>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="flex items-center gap-3 mb-3">
              <Server className="h-5 w-5 text-blue-500" />
              <h3 className="text-sm font-semibold text-slate-800">Backend</h3>
            </div>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-slate-400">Framework</span>
                <span className="text-slate-700">FastAPI</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">API Version</span>
                <span className="text-slate-700">v1</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">AI Model</span>
                <span className="text-slate-700">Ollama (Local LLM)</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Embeddings</span>
                <span className="text-slate-700">SciBERT</span>
              </div>
            </div>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="flex items-center gap-3 mb-3">
              <Database className="h-5 w-5 text-purple-500" />
              <h3 className="text-sm font-semibold text-slate-800">Database</h3>
            </div>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-slate-400">Engine</span>
                <span className="text-slate-700">PostgreSQL</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Extension</span>
                <span className="text-slate-700">pgvector</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Cache</span>
                <span className="text-slate-700">Redis</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Queue</span>
                <span className="text-slate-700">Celery + RabbitMQ</span>
              </div>
            </div>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="flex items-center gap-3 mb-3">
              <Clock className="h-5 w-5 text-teal-500" />
              <h3 className="text-sm font-semibold text-slate-800">Scheduled Tasks</h3>
            </div>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-slate-400">Paper Fetch</span>
                <span className="text-slate-700">Every 6 hours</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Overlap Detection</span>
                <span className="text-slate-700">Daily 02:00</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Trend Analysis</span>
                <span className="text-slate-700">Weekly</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Email Digest</span>
                <span className="text-slate-700">Daily 09:00</span>
              </div>
            </div>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="flex items-center gap-3 mb-3">
              <Globe className="h-5 w-5 text-orange-500" />
              <h3 className="text-sm font-semibold text-slate-800">Data Sources</h3>
            </div>
            <div className="space-y-2 text-sm">
              <div className="flex justify-between">
                <span className="text-slate-400">arXiv</span>
                <span className="text-green-500 text-xs">Connected</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">PubMed</span>
                <span className="text-green-500 text-xs">Connected</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-400">Semantic Scholar</span>
                <span className="text-green-500 text-xs">Connected</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
