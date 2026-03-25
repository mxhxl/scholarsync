"use client";

import { useState, useEffect } from "react";
import {
  TrendingUp,
  Sparkles,
  ArrowUpRight,
  ArrowDownRight,
  Minus,
  Download,
  BarChart3,
  Lightbulb,
  Shield,
} from "lucide-react";
import { insights as insightsApi, TrendResult } from "@/lib/api";
import PageHeader from "@/components/PageHeader";
import clsx from "clsx";

export default function InsightsPage() {
  const [trends, setTrends] = useState<TrendResult[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadTrends();
  }, []);

  async function loadTrends() {
    setLoading(true);
    try {
      const d = await insightsApi.trends();
      setTrends(d.trends);
    } catch {
      setTrends(MOCK_TRENDS);
    } finally {
      setLoading(false);
    }
  }

  const trendIcon = (trend: string) => {
    if (trend === "rising") return <ArrowUpRight className="h-4 w-4 text-emerald-500" />;
    if (trend === "declining") return <ArrowDownRight className="h-4 w-4 text-red-400" />;
    return <Minus className="h-4 w-4 text-slate-400" />;
  };

  const now = new Date();
  const monthRange = `${new Date(now.getFullYear(), now.getMonth() - 1).toLocaleDateString("en-US", { month: "short" })} – ${now.toLocaleDateString("en-US", { month: "short", year: "numeric" })}`;

  return (
    <div className="min-h-screen bg-slate-50">
      <PageHeader
        title="Insights"
        subtitle={monthRange}
        actions={
          <button className="btn-secondary text-sm flex items-center gap-2">
            <Download className="h-4 w-4" />
            Export Report
          </button>
        }
      />

      <div className="px-8 py-6 space-y-6">
        {/* Top stats */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="card p-6">
            <div className="flex items-start justify-between mb-4">
              <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/5">
                <Sparkles className="h-5 w-5 text-primary" />
              </div>
              <span className="chip bg-success-light text-success text-[11px]">
                +12% vs last month
              </span>
            </div>
            <p className="text-sm font-medium text-slate-500">Total Papers Processed</p>
            <p className="text-4xl font-bold text-primary mt-1">1,284</p>
            <p className="text-xs text-slate-400 mt-2">
              Your AI assistant saved you approx. 42 hours of manual reading
              this month.
            </p>
          </div>

          <div className="card p-6">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/5 mb-4">
              <Lightbulb className="h-5 w-5 text-primary" />
            </div>
            <p className="text-sm font-medium text-slate-500">Key Findings</p>
            <p className="text-4xl font-bold text-primary mt-1">86</p>
            <p className="text-xs text-slate-400 mt-2">
              Important findings extracted from your field this month.
            </p>
          </div>

          <div className="card p-6">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/5 mb-4">
              <Shield className="h-5 w-5 text-primary" />
            </div>
            <p className="text-sm font-medium text-slate-500">
              Duplications Prevented
            </p>
            <p className="text-4xl font-bold text-primary mt-1">14</p>
            <p className="text-xs text-slate-400 mt-2">
              Overlap alerts that helped you avoid redundant research directions.
            </p>
          </div>
        </div>

        {/* Research Activity chart */}
        <div className="card p-6">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h3 className="text-base font-bold text-slate-800">
                Research Activity
              </h3>
              <p className="text-xs text-slate-400 mt-1">
                Daily average: 42 papers
              </p>
            </div>
            <BarChart3 className="h-5 w-5 text-slate-300" />
          </div>

          <div className="flex items-end gap-2 h-32">
            {ACTIVITY_BARS.map((h, i) => (
              <div
                key={i}
                className="flex-1 rounded-t-sm bg-primary transition-all hover:opacity-80"
                style={{ height: `${h * 100}%`, opacity: 0.3 + h * 0.7 }}
              />
            ))}
          </div>
          <div className="flex justify-between mt-2 text-[10px] font-medium text-slate-400 tracking-wider">
            <span>Mon</span>
            <span>Tue</span>
            <span>Wed</span>
            <span>Thu</span>
            <span>Fri</span>
            <span>Sat</span>
            <span>Sun</span>
          </div>
        </div>

        {/* Trending Topics */}
        <div>
          <h3 className="section-label mb-4">Trending Topics</h3>

          {loading ? (
            <div className="space-y-3">
              {[1, 2, 3].map((i) => (
                <div key={i} className="card p-5 animate-pulse">
                  <div className="h-4 w-40 bg-slate-100 rounded mb-2" />
                  <div className="h-3 w-24 bg-slate-100 rounded" />
                </div>
              ))}
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {trends.map((topic, i) => (
                <div key={i} className="card p-5 hover:shadow-md transition-shadow">
                  <div className="flex items-start justify-between mb-3">
                    <h4 className="text-sm font-bold text-primary">
                      {topic.label}
                    </h4>
                    <div className="flex items-center gap-1">
                      {trendIcon(topic.trend)}
                      <span
                        className={clsx(
                          "text-xs font-semibold",
                          topic.trend === "rising" && "text-emerald-500",
                          topic.trend === "declining" && "text-red-400",
                          topic.trend === "stable" && "text-slate-400"
                        )}
                      >
                        {topic.trend}
                      </span>
                    </div>
                  </div>
                  <p className="text-xs text-slate-400 mb-3">
                    {topic.papers_count} papers this month
                  </p>
                  <div className="flex flex-wrap gap-1.5">
                    {topic.keywords.map((kw) => (
                      <span key={kw} className="chip chip-inactive text-[10px]">
                        {kw}
                      </span>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

const ACTIVITY_BARS = [0.4, 0.6, 0.35, 0.8, 1, 0.55, 0.45, 0.9, 0.25, 0.65, 0.75, 0.5, 0.85, 0.3];

const MOCK_TRENDS: TrendResult[] = [
  {
    topic_id: 1,
    label: "Large Language Models for Science",
    papers_count: 342,
    growth_rate: 0.24,
    keywords: ["LLM", "scientific reasoning", "few-shot", "chain-of-thought"],
    trend: "rising",
    monthly_counts: [],
  },
  {
    topic_id: 2,
    label: "Diffusion Models in Medical Imaging",
    papers_count: 189,
    growth_rate: 0.18,
    keywords: ["diffusion", "MRI synthesis", "radiology", "generative"],
    trend: "rising",
    monthly_counts: [],
  },
  {
    topic_id: 3,
    label: "Federated Learning for Healthcare",
    papers_count: 156,
    growth_rate: 0.02,
    keywords: ["privacy", "distributed", "hospital", "EHR"],
    trend: "stable",
    monthly_counts: [],
  },
  {
    topic_id: 4,
    label: "Graph Neural Networks for Drug Discovery",
    papers_count: 134,
    growth_rate: 0.15,
    keywords: ["GNN", "molecular", "drug-target", "knowledge graph"],
    trend: "rising",
    monthly_counts: [],
  },
  {
    topic_id: 5,
    label: "Reinforcement Learning from Human Feedback",
    papers_count: 127,
    growth_rate: 0.01,
    keywords: ["RLHF", "alignment", "reward model", "preference"],
    trend: "stable",
    monthly_counts: [],
  },
  {
    topic_id: 6,
    label: "Quantum Machine Learning",
    papers_count: 98,
    growth_rate: -0.08,
    keywords: ["QML", "variational", "quantum advantage", "NISQ"],
    trend: "declining",
    monthly_counts: [],
  },
];
