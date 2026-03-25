"use client";

import { useState, useEffect, useMemo } from "react";
import {
  RefreshCw,
  Search,
  Newspaper,
} from "lucide-react";
import { feed as feedApi, FeedItem } from "@/lib/api";
import { useAuth } from "@/hooks/useAuth";
import PaperCard from "@/components/PaperCard";
import PageHeader from "@/components/PageHeader";
import EmptyState from "@/components/EmptyState";
import clsx from "clsx";

const CATEGORIES = ["All Topics", "High Priority", "Unread"];

export default function FeedPage() {
  const { user } = useAuth();
  const [items, setItems] = useState<FeedItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [filter, setFilter] = useState("All Topics");
  const [search, setSearch] = useState("");

  useEffect(() => {
    loadFeed();
  }, []);

  async function loadFeed() {
    setLoading(true);
    try {
      const data = await feedApi.get();
      setItems(data.items);
    } catch {
      setItems(MOCK_FEED);
    } finally {
      setLoading(false);
    }
  }

  async function handleRefresh() {
    setRefreshing(true);
    try {
      await loadFeed();
    } finally {
      setRefreshing(false);
    }
  }

  const filtered = useMemo(
    () =>
      items.filter((item) => {
        if (filter === "High Priority" && item.priority !== "high") return false;
        if (filter === "Unread" && item.is_read) return false;
        if (search) {
          const q = search.toLowerCase();
          return (
            item.paper.title.toLowerCase().includes(q) ||
            item.paper.authors.some((a) => a.toLowerCase().includes(q))
          );
        }
        return true;
      }),
    [items, filter, search]
  );

  const today = useMemo(
    () =>
      new Date().toLocaleDateString("en-US", {
        weekday: "long",
        month: "long",
        day: "numeric",
        year: "numeric",
      }),
    []
  );

  return (
    <div className="min-h-screen bg-slate-50">
      <PageHeader
        title={`Welcome back, ${user?.full_name?.split(" ")[0] ?? "Researcher"}`}
        subtitle={today}
        actions={
          <button
            onClick={handleRefresh}
            disabled={refreshing}
            className="btn-secondary flex items-center gap-2 text-sm"
          >
            <RefreshCw
              className={clsx("h-4 w-4", refreshing && "animate-spin")}
            />
            Refresh Feed
          </button>
        }
      />

      <div className="px-8 py-6">
        {/* Filters bar */}
        <div className="flex items-center justify-between gap-4 mb-6">
          <div className="flex gap-2">
            {CATEGORIES.map((cat) => (
              <button
                key={cat}
                onClick={() => setFilter(cat)}
                className={clsx(
                  "chip cursor-pointer",
                  filter === cat ? "chip-active" : "chip-inactive"
                )}
              >
                {cat}
              </button>
            ))}
          </div>

          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search papers..."
              className="input-field pl-9 w-64 py-2 text-sm"
            />
          </div>
        </div>

        {/* Today's Papers heading */}
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-bold text-primary">
            Today&apos;s Papers
          </h2>
          <span className="text-sm text-slate-400">
            {filtered.length} papers
          </span>
        </div>

        {/* Feed list */}
        {loading ? (
          <div className="space-y-4">
            {[1, 2, 3].map((i) => (
              <div key={i} className="card p-5 animate-pulse">
                <div className="h-4 w-24 bg-slate-100 rounded mb-3" />
                <div className="h-5 w-3/4 bg-slate-100 rounded mb-2" />
                <div className="h-4 w-1/2 bg-slate-100 rounded mb-3" />
                <div className="h-4 w-full bg-slate-100 rounded" />
              </div>
            ))}
          </div>
        ) : filtered.length === 0 ? (
          <EmptyState
            icon={Newspaper}
            title="No papers found"
            description={
              search
                ? "Try a different search term."
                : "Your personalized feed is empty. Try refreshing or adjust your research profile."
            }
            action={{ label: "Refresh Feed", onClick: handleRefresh }}
          />
        ) : (
          <div className="space-y-4">
            {filtered.map((item) => (
              <PaperCard
                key={item.id}
                paper={item.paper}
                relevanceScore={item.relevance_score}
                priority={item.priority}
                saved={item.is_saved}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

const MOCK_FEED: FeedItem[] = [
  {
    id: "1",
    paper_id: "p1",
    paper: {
      id: "p1",
      external_id: "2403.1234",
      title: "Scalable Transformers for Long-Context Scientific Reasoning",
      authors: ["Zhang, W.", "Kumar, A.", "Liu, S."],
      abstract:
        "A novel architecture that enables processing of extremely long sequences without quadratic complexity overhead, achieving near-linear memory usage for scientific document understanding.",
      published_date: new Date().toISOString(),
      source: "arXiv:2403.1234",
      pdf_url: "https://arxiv.org/pdf/2403.1234",
      citation_count: 12,
      venue: null,
      created_at: new Date().toISOString(),
    },
    relevance_score: 0.94,
    priority: "high",
    is_read: false,
    is_saved: false,
    created_at: new Date().toISOString(),
  },
  {
    id: "2",
    paper_id: "p2",
    paper: {
      id: "p2",
      external_id: "ni-2024-001",
      title: "Neural Signal Processing for Real-time Brain-Computer Interfaces",
      authors: ["Patel, R.", "Chen, Y.", "Williams, D."],
      abstract:
        "Investigating the latency-accuracy trade-offs in modern BCI systems using sparse coding techniques and real-time neural decoding frameworks.",
      published_date: new Date().toISOString(),
      source: "Nature Intelligence",
      pdf_url: null,
      citation_count: 45,
      venue: "Nature Intelligence",
      created_at: new Date().toISOString(),
    },
    relevance_score: 0.87,
    priority: "medium",
    is_read: false,
    is_saved: true,
    created_at: new Date().toISOString(),
  },
  {
    id: "3",
    paper_id: "p3",
    paper: {
      id: "p3",
      external_id: "neurips-2024-789",
      title: "Diffusion Models for Synthetic Medical Imaging Generation",
      authors: ["Garcia, M.", "Thompson, K.", "Lee, J.", "Anderson, P."],
      abstract:
        "Synthesizing high-fidelity MRIs to augment rare disease datasets while preserving anatomical consistency and clinical relevance across multiple imaging modalities.",
      published_date: new Date().toISOString(),
      source: "NeurIPS 2024",
      pdf_url: "https://arxiv.org/pdf/2024.789",
      citation_count: 89,
      venue: "NeurIPS 2024",
      created_at: new Date().toISOString(),
    },
    relevance_score: 0.82,
    priority: "medium",
    is_read: true,
    is_saved: false,
    created_at: new Date().toISOString(),
  },
  {
    id: "4",
    paper_id: "p4",
    paper: {
      id: "p4",
      external_id: "pubmed-38291045",
      title:
        "Graph Neural Networks for Drug-Target Interaction Prediction in Oncology",
      authors: ["Nakamura, T.", "Fernandez, L."],
      abstract:
        "Applying heterogeneous graph attention networks to predict novel drug-target interactions from biomedical knowledge graphs with improved specificity for oncological targets.",
      published_date: new Date().toISOString(),
      source: "PubMed",
      pdf_url: null,
      citation_count: 23,
      venue: null,
      created_at: new Date().toISOString(),
    },
    relevance_score: 0.78,
    priority: "high",
    is_read: false,
    is_saved: false,
    created_at: new Date().toISOString(),
  },
];
