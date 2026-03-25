"use client";

import { useEffect, useState, useCallback } from "react";
import { admin, AdminPaper } from "@/lib/api";
import {
  Search,
  ChevronLeft,
  ChevronRight,
  Trash2,
  CheckCircle,
  XCircle,
  Users,
  BookMarked,
} from "lucide-react";

const PAGE_SIZE = 20;

export default function AdminPapersPage() {
  const [papers, setPapers] = useState<AdminPaper[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [sourceFilter, setSourceFilter] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await admin.papers({
        search: search || undefined,
        source: sourceFilter || undefined,
        limit: PAGE_SIZE,
        offset: page * PAGE_SIZE,
      });
      setPapers(res.items);
      setTotal(res.total);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to load papers");
    }
    setLoading(false);
  }, [search, sourceFilter, page]);

  useEffect(() => {
    load();
  }, [load]);

  const totalPages = Math.ceil(total / PAGE_SIZE);

  const handleDelete = async (id: string, title: string) => {
    if (!confirm(`Delete paper "${title}"? This will also remove it from all user feeds and libraries.`))
      return;
    try {
      await admin.deletePaper(id);
      setPapers((prev) => prev.filter((p) => p.id !== id));
      setTotal((t) => t - 1);
    } catch (e) {
      alert(e instanceof Error ? e.message : "Delete failed");
    }
  };

  return (
    <div className="p-8">
      <div className="mb-6">
        <h1 className="text-2xl font-bold text-primary">Paper Management</h1>
        <p className="text-sm text-slate-400 mt-1">
          {total} total paper{total !== 1 ? "s" : ""}
        </p>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap items-center gap-3 mb-6">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
          <input
            type="text"
            placeholder="Search by title..."
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
            className="w-full rounded-xl border border-slate-200 bg-white pl-10 pr-4 py-2.5 text-sm text-slate-800 placeholder-slate-400 focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
          />
        </div>
        <select
          value={sourceFilter}
          onChange={(e) => {
            setSourceFilter(e.target.value);
            setPage(0);
          }}
          className="rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-800 focus:border-primary focus:outline-none"
        >
          <option value="">All Sources</option>
          <option value="arxiv">arXiv</option>
          <option value="pubmed">PubMed</option>
          <option value="semantic_scholar">Semantic Scholar</option>
        </select>
      </div>

      {/* Table */}
      <div className="rounded-2xl border border-slate-200 bg-white overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50">
                <th className="px-5 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  Paper
                </th>
                <th className="px-5 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  Source
                </th>
                <th className="px-5 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  Summary
                </th>
                <th className="px-5 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  Citations
                </th>
                <th className="px-5 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  In Feeds
                </th>
                <th className="px-5 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  Saved
                </th>
                <th className="px-5 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  Published
                </th>
                <th className="px-5 py-3 text-right text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={8} className="px-5 py-12 text-center text-slate-400">
                    <div className="flex justify-center">
                      <div className="h-6 w-6 animate-spin rounded-full border-2 border-primary border-t-transparent" />
                    </div>
                  </td>
                </tr>
              ) : error ? (
                <tr>
                  <td colSpan={8} className="px-5 py-12 text-center text-red-400">
                    {error}
                    <button onClick={load} className="ml-2 underline hover:text-red-600">Retry</button>
                  </td>
                </tr>
              ) : papers.length === 0 ? (
                <tr>
                  <td colSpan={8} className="px-5 py-12 text-center text-slate-400">
                    No papers found
                  </td>
                </tr>
              ) : (
                papers.map((p) => (
                  <tr
                    key={p.id}
                    className="border-b border-slate-50 hover:bg-slate-50 transition-colors"
                  >
                    <td className="px-5 py-4 max-w-xs">
                      <p className="font-medium text-slate-800 truncate" title={p.title}>
                        {p.title}
                      </p>
                      <p className="text-xs text-slate-400 truncate">
                        {p.authors.slice(0, 3).join(", ")}
                        {p.authors.length > 3 ? ` +${p.authors.length - 3}` : ""}
                      </p>
                      {p.venue && (
                        <p className="text-xs text-slate-300 truncate">{p.venue}</p>
                      )}
                    </td>
                    <td className="px-5 py-4">
                      <span className="inline-flex rounded-full bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-600">
                        {p.source}
                      </span>
                    </td>
                    <td className="px-5 py-4">
                      {p.has_summary ? (
                        <CheckCircle className="h-4 w-4 text-green-500" />
                      ) : (
                        <XCircle className="h-4 w-4 text-slate-300" />
                      )}
                    </td>
                    <td className="px-5 py-4 text-slate-600">
                      {p.citation_count.toLocaleString()}
                    </td>
                    <td className="px-5 py-4">
                      <span className="inline-flex items-center gap-1 text-slate-600">
                        <Users className="h-3 w-3" />
                        {p.feed_count}
                      </span>
                    </td>
                    <td className="px-5 py-4">
                      <span className="inline-flex items-center gap-1 text-slate-600">
                        <BookMarked className="h-3 w-3" />
                        {p.saved_count}
                      </span>
                    </td>
                    <td className="px-5 py-4 text-slate-400 text-xs">
                      {p.published_date
                        ? new Date(p.published_date).toLocaleDateString()
                        : "—"}
                    </td>
                    <td className="px-5 py-4 text-right">
                      <button
                        onClick={() => handleDelete(p.id, p.title)}
                        className="rounded-lg p-2 text-slate-400 hover:bg-red-50 hover:text-red-500 transition-colors"
                        title="Delete paper"
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
