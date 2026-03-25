"use client";

import { useState, useEffect, useRef } from "react";
import { useParams, useRouter } from "next/navigation";
import {
  ArrowLeft,
  Bookmark,
  Share2,
  ExternalLink,
  Quote,
  TrendingUp,
  Clock,
  CheckCircle2,
  FileText,
  Lightbulb,
  FlaskConical,
  AlertTriangle,
  Target,
  Sparkles,
  BookOpen,
  Compass,
  Zap,
  Link2,
  Network,
} from "lucide-react";
import {
  papers as papersApi,
  citations as citationsApi,
  library as libraryApi,
  Paper,
  PaperSummary,
  PaperSuggestions,
  LiteratureReview,
  CitationNetwork,
  Folder,
} from "@/lib/api";
import { X, FolderOpen, Plus } from "lucide-react";
import clsx from "clsx";

const AI_TABS = ["Summary", "Citations", "Suggestions", "Lit Review"] as const;
type AITab = (typeof AI_TABS)[number];

export default function PaperDetailsPage() {
  const { paperId } = useParams<{ paperId: string }>();
  const router = useRouter();
  const [paper, setPaper] = useState<Paper | null>(null);
  const [saved, setSaved] = useState(false);
  const [loading, setLoading] = useState(true);
  const [showSaveModal, setShowSaveModal] = useState(false);

  // AI analysis state
  const [aiTriggered, setAiTriggered] = useState(false);
  const [aiLoading, setAiLoading] = useState(false);
  const [aiTab, setAiTab] = useState<AITab>("Summary");
  const [summary, setSummary] = useState<PaperSummary | null>(null);
  const [citationData, setCitationData] = useState<CitationNetwork | null>(null);
  const [suggestions, setSuggestions] = useState<PaperSuggestions | null>(null);
  const [litReview, setLitReview] = useState<LiteratureReview | null>(null);
  const [aiError, setAiError] = useState(false);

  const mountedRef = useRef(true);
  useEffect(() => {
    mountedRef.current = true;
    loadPaper();
    return () => { mountedRef.current = false; };
  }, [paperId]);

  async function loadPaper() {
    setLoading(true);
    try {
      const p = await papersApi.get(paperId);
      if (!mountedRef.current) return;
      setPaper(p);
    } catch {
      if (!mountedRef.current) return;
      setPaper(MOCK_PAPER);
    } finally {
      if (mountedRef.current) setLoading(false);
    }
  }

  async function handleAiAnalyze() {
    setAiTriggered(true);
    setAiLoading(true);
    setAiError(false);

    try {
      const results = await Promise.allSettled([
        papersApi.summary(paperId),
        citationsApi.get(paperId),
        papersApi.suggestions(paperId),
        papersApi.literatureReview(paperId),
      ]);

      const [summaryResult, citationResult, suggestionsResult, litReviewResult] = results;

      if (summaryResult.status === "fulfilled") {
        setSummary(summaryResult.value);
      } else {
        setSummary(MOCK_SUMMARY);
      }

      if (citationResult.status === "fulfilled") {
        setCitationData(citationResult.value);
      }

      if (suggestionsResult.status === "fulfilled") {
        setSuggestions(suggestionsResult.value);
      } else {
        setSuggestions(MOCK_SUGGESTIONS);
      }

      if (litReviewResult.status === "fulfilled") {
        setLitReview(litReviewResult.value);
      }
    } catch {
      setSummary(MOCK_SUMMARY);
      setSuggestions(MOCK_SUGGESTIONS);
      setAiError(true);
    } finally {
      setAiLoading(false);
    }
  }

  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    );
  }

  if (!paper) return null;

  const date = paper.published_date
    ? new Date(paper.published_date).toLocaleDateString("en-US", {
        month: "short",
        year: "numeric",
      })
    : "";

  return (
    <div className="min-h-screen bg-white">
      {/* Top bar */}
      <div className="sticky top-0 z-10 flex items-center justify-between border-b border-slate-100 bg-white/90 backdrop-blur-sm px-6 py-4">
        <button
          onClick={() => router.back()}
          className="flex items-center gap-2 text-sm font-medium text-slate-500 hover:text-primary transition-colors"
        >
          <ArrowLeft className="h-4 w-4" />
          Back to Feed
        </button>
        <div className="flex items-center gap-2">
          <button
            onClick={() => !saved && setShowSaveModal(true)}
            className={clsx(
              "rounded-lg p-2 transition-colors",
              saved
                ? "bg-primary/10 text-primary"
                : "text-slate-400 hover:text-primary hover:bg-primary/5"
            )}
          >
            <Bookmark className={clsx("h-5 w-5", saved && "fill-current")} />
          </button>
          <button className="rounded-lg p-2 text-slate-400 hover:text-primary hover:bg-primary/5 transition-colors">
            <Share2 className="h-5 w-5" />
          </button>
        </div>
      </div>

      <div className="mx-auto max-w-4xl px-8 py-8">
        {/* Category */}
        <span className="chip bg-primary/5 text-primary text-[11px] mb-4">
          COMPUTER SCIENCE &middot; {paper.source.toUpperCase()}
        </span>

        {/* Title */}
        <h1 className="text-3xl font-bold text-primary leading-tight mt-3">
          {paper.title}
        </h1>

        {/* Authors */}
        <p className="mt-3 text-sm text-slate-600">
          {paper.authors.join(", ")}
          {date && (
            <span className="text-slate-400"> &middot; {date}</span>
          )}
        </p>

        {/* Stats */}
        <div className="mt-6 grid grid-cols-3 gap-4">
          <StatCard
            icon={Quote}
            label="Citations"
            value={
              paper.citation_count > 1000
                ? `${(paper.citation_count / 1000).toFixed(1)}k`
                : String(paper.citation_count)
            }
          />
          <StatCard icon={TrendingUp} label="Impact" value="High" />
          <StatCard icon={Clock} label="Reading" value="12m" />
        </div>

        {/* Abstract */}
        {paper.abstract && (
          <div className="mt-8">
            <div className="flex items-center gap-2 mb-3">
              <div className="h-3 w-1 rounded-full bg-primary" />
              <span className="section-label">Abstract</span>
            </div>
            <p className="text-slate-600 leading-relaxed text-sm">
              {paper.abstract}
            </p>
          </div>
        )}

        {/* AI Loading State */}
        {aiLoading && (
          <div className="mt-10">
            <div className="card p-8">
              <div className="flex flex-col items-center gap-4">
                <div className="relative">
                  <div className="h-16 w-16 rounded-full bg-gradient-to-r from-primary to-accent-teal animate-spin" style={{ animationDuration: "3s" }}>
                    <div className="absolute inset-1 rounded-full bg-white" />
                  </div>
                  <Sparkles className="absolute inset-0 m-auto h-6 w-6 text-primary animate-pulse" />
                </div>
                <div className="text-center">
                  <p className="font-bold text-primary text-lg">Analyzing with AI</p>
                  <p className="text-sm text-slate-400 mt-1">
                    Generating summary, finding citations & creating suggestions...
                  </p>
                </div>
                <div className="flex gap-6 mt-2">
                  {["Summary", "Citations", "Suggestions"].map((step, i) => (
                    <div key={step} className="flex items-center gap-2 text-xs text-slate-400">
                      <div
                        className="h-2 w-2 rounded-full bg-primary animate-pulse"
                        style={{ animationDelay: `${i * 0.3}s` }}
                      />
                      {step}
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        )}

        {/* AI Results */}
        {aiTriggered && !aiLoading && (
          <div className="mt-10">
            {/* AI Results Header */}
            <div className="flex items-center gap-3 mb-6">
              <div className="flex items-center gap-2 px-3 py-1.5 rounded-full bg-gradient-to-r from-primary/10 to-accent-teal/10">
                <Sparkles className="h-4 w-4 text-primary" />
                <span className="text-xs font-bold text-primary">AI Analysis</span>
              </div>
              {aiError && (
                <span className="text-xs text-amber-500">Using cached results</span>
              )}
              <button
                onClick={handleAiAnalyze}
                className="ml-auto text-xs text-slate-400 hover:text-primary transition-colors"
              >
                Regenerate
              </button>
            </div>

            {/* AI Tabs */}
            <div className="border-b border-slate-100">
              <div className="flex">
                {AI_TABS.map((t) => {
                  const Icon =
                    t === "Summary"
                      ? FileText
                      : t === "Citations"
                      ? Network
                      : t === "Suggestions"
                      ? Lightbulb
                      : BookOpen;
                  return (
                    <button
                      key={t}
                      onClick={() => setAiTab(t)}
                      className={clsx(
                        "relative flex items-center gap-2 px-6 py-3 text-sm font-medium transition-colors",
                        aiTab === t
                          ? "text-primary"
                          : "text-slate-400 hover:text-slate-600"
                      )}
                    >
                      <Icon className="h-4 w-4" />
                      {t}
                      {aiTab === t && (
                        <div className="absolute bottom-0 left-1/2 -translate-x-1/2 h-0.5 w-12 bg-primary rounded-full" />
                      )}
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Tab Content */}
            <div className="py-8 space-y-8">
              {aiTab === "Summary" && summary && <SummaryPanel summary={summary} />}
              {aiTab === "Citations" && (
                <CitationsPanel citationData={citationData} paper={paper} />
              )}
              {aiTab === "Suggestions" && suggestions && (
                <SuggestionsPanel suggestions={suggestions} />
              )}
              {aiTab === "Lit Review" && (
                <LitReviewPanel litReview={litReview} />
              )}
            </div>
          </div>
        )}
      </div>

      {/* Bottom action bar */}
      <div className="sticky bottom-0 border-t border-slate-100 bg-white/90 backdrop-blur-sm px-8 py-4">
        <div className="mx-auto max-w-4xl flex gap-3">
          <button
            onClick={() => !saved && setShowSaveModal(true)}
            className={clsx(
              "flex-1 flex items-center justify-center gap-2",
              saved ? "btn-secondary opacity-60 cursor-default" : "btn-secondary"
            )}
          >
            <Bookmark className={clsx("h-4 w-4", saved && "fill-current")} />
            {saved ? "Saved" : "Save to Library"}
          </button>
          {!aiTriggered && (
            <button
              onClick={handleAiAnalyze}
              disabled={aiLoading}
              className="btn-primary flex-1 flex items-center justify-center gap-2"
            >
              <Sparkles className="h-4 w-4" />
              AI Analyze Paper
            </button>
          )}
          {paper.pdf_url && (
            <a
              href={paper.pdf_url}
              target="_blank"
              rel="noopener noreferrer"
              className="btn-primary flex-1 flex items-center justify-center gap-2"
            >
              <ExternalLink className="h-4 w-4" />
              Open PDF
            </a>
          )}
        </div>
      </div>

      {/* Save to Library Modal */}
      {showSaveModal && (
        <SaveToLibraryModal
          paperId={paperId}
          onClose={() => setShowSaveModal(false)}
          onSaved={() => {
            setSaved(true);
            setShowSaveModal(false);
          }}
        />
      )}
    </div>
  );
}

/* ── Summary Panel ─────────────────────────────────────────── */

function SummaryPanel({ summary }: { summary: PaperSummary }) {
  return (
    <>
      <Section icon={FileText} title="Executive Summary">
        <p className="text-slate-600 leading-relaxed">{summary.purpose}</p>
      </Section>

      <Section icon={FlaskConical} title="Methodology">
        <p className="text-slate-600 leading-relaxed">{summary.methodology}</p>
      </Section>

      <Section icon={CheckCircle2} title="Key Results">
        <div className="space-y-3">
          {(summary.key_results ?? "")
            .split(". ")
            .filter(Boolean)
            .map((r, i) => (
              <div key={i} className="flex items-start gap-3">
                <CheckCircle2 className="h-5 w-5 text-accent-teal mt-0.5 shrink-0" />
                <p className="text-slate-600 leading-relaxed">
                  {r.replace(/\.$/, "")}
                </p>
              </div>
            ))}
        </div>
      </Section>

      <Section icon={AlertTriangle} title="Limitations">
        <p className="text-slate-600 leading-relaxed">{summary.limitations}</p>
      </Section>

      <Section icon={Target} title="Relevance to Your Research">
        <div className="rounded-xl bg-primary/[0.03] border border-primary/10 p-4">
          <p className="text-slate-600 leading-relaxed">
            {summary.relevance_to_field}
          </p>
        </div>
      </Section>
    </>
  );
}

/* ── Citations Panel ───────────────────────────────────────── */

function CitationsPanel({
  citationData,
  paper,
}: {
  citationData: CitationNetwork | null;
  paper: Paper;
}) {
  const backward = citationData?.backward_citations ?? [];
  const forward = citationData?.forward_citations ?? [];
  const hasData = backward.length > 0 || forward.length > 0;

  return (
    <>
      {/* Citation Stats */}
      <div className="grid grid-cols-3 gap-4">
        <div className="card p-4 text-center">
          <Quote className="mx-auto h-5 w-5 text-primary mb-1" />
          <p className="text-2xl font-bold text-primary">{paper.citation_count}</p>
          <p className="text-[10px] font-bold tracking-wider text-slate-400 uppercase">
            Total Citations
          </p>
        </div>
        <div className="card p-4 text-center">
          <Link2 className="mx-auto h-5 w-5 text-accent-teal mb-1" />
          <p className="text-2xl font-bold text-accent-teal">{backward.length}</p>
          <p className="text-[10px] font-bold tracking-wider text-slate-400 uppercase">
            References
          </p>
        </div>
        <div className="card p-4 text-center">
          <TrendingUp className="mx-auto h-5 w-5 text-accent-gold mb-1" />
          <p className="text-2xl font-bold text-accent-gold">{forward.length}</p>
          <p className="text-[10px] font-bold tracking-wider text-slate-400 uppercase">
            Cited By
          </p>
        </div>
      </div>

      {hasData ? (
        <>
          {backward.length > 0 && (
            <Section icon={Link2} title="References (Papers This Cites)">
              <div className="space-y-3">
                {backward.map((p) => (
                  <CitationCard key={p.id} paper={p} />
                ))}
              </div>
            </Section>
          )}

          {forward.length > 0 && (
            <Section icon={TrendingUp} title="Cited By (Papers Citing This)">
              <div className="space-y-3">
                {forward.map((p) => (
                  <CitationCard key={p.id} paper={p} />
                ))}
              </div>
            </Section>
          )}
        </>
      ) : (
        <div className="card p-8 text-center">
          <Network className="mx-auto h-10 w-10 text-slate-300 mb-3" />
          <p className="font-semibold text-slate-500">
            Citation network not yet indexed
          </p>
          <p className="text-sm text-slate-400 mt-1">
            Citation data will be populated as more papers are ingested into the system.
          </p>
        </div>
      )}
    </>
  );
}

function CitationCard({ paper }: { paper: { id: string; title: string; authors: string[]; citation_count: number; venue: string | null } }) {
  return (
    <div className="rounded-xl border border-slate-100 p-4 hover:border-primary/20 hover:bg-primary/[0.02] transition-all">
      <p className="font-semibold text-sm text-primary leading-snug">
        {paper.title}
      </p>
      <p className="text-xs text-slate-400 mt-1">
        {paper.authors.slice(0, 3).join(", ")}
        {paper.authors.length > 3 && ` +${paper.authors.length - 3}`}
        {paper.venue && <span> &middot; {paper.venue}</span>}
      </p>
      <div className="flex items-center gap-1 mt-2 text-xs text-slate-400">
        <Quote className="h-3 w-3" />
        {paper.citation_count} citations
      </div>
    </div>
  );
}

/* ── Suggestions Panel ─────────────────────────────────────── */

function SuggestionsPanel({ suggestions }: { suggestions: PaperSuggestions }) {
  return (
    <>
      <Section icon={Zap} title="Key Takeaways">
        <div className="space-y-3">
          {suggestions.key_takeaways.map((t, i) => (
            <div key={i} className="flex items-start gap-3">
              <div className="mt-1 flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-primary/10 text-xs font-bold text-primary">
                {i + 1}
              </div>
              <p className="text-slate-600 leading-relaxed">{t}</p>
            </div>
          ))}
        </div>
      </Section>

      <Section icon={Compass} title="Research Directions">
        <div className="grid gap-3">
          {suggestions.research_directions.map((d, i) => (
            <div
              key={i}
              className="rounded-xl border border-slate-100 p-4 hover:border-primary/20 hover:bg-primary/[0.02] transition-all"
            >
              <div className="flex items-start gap-3">
                <Compass className="h-5 w-5 text-accent-teal mt-0.5 shrink-0" />
                <p className="text-slate-600 leading-relaxed text-sm">{d}</p>
              </div>
            </div>
          ))}
        </div>
      </Section>

      <Section icon={Lightbulb} title="Practical Applications">
        <div className="grid gap-3">
          {suggestions.practical_applications.map((a, i) => (
            <div
              key={i}
              className="rounded-xl bg-accent-gold/5 border border-accent-gold/15 p-4"
            >
              <div className="flex items-start gap-3">
                <Lightbulb className="h-5 w-5 text-accent-gold mt-0.5 shrink-0" />
                <p className="text-slate-600 leading-relaxed text-sm">{a}</p>
              </div>
            </div>
          ))}
        </div>
      </Section>

      <Section icon={BookOpen} title="Recommended Reading">
        <div className="space-y-3">
          {suggestions.recommended_reading.map((r, i) => (
            <div key={i} className="flex items-start gap-3">
              <BookOpen className="h-5 w-5 text-primary mt-0.5 shrink-0" />
              <p className="text-slate-600 leading-relaxed text-sm">{r}</p>
            </div>
          ))}
        </div>
      </Section>
    </>
  );
}

/* ── Literature Review Panel ───────────────────────────────── */

function LitReviewPanel({ litReview }: { litReview: LiteratureReview | null }) {
  if (!litReview || litReview.status === "failed" || !litReview.entries?.length) {
    return (
      <div className="card p-8 text-center">
        <BookOpen className="mx-auto h-10 w-10 text-slate-300 mb-3" />
        <p className="font-semibold text-slate-500">
          Literature review could not be generated
        </p>
        <p className="text-sm text-slate-400 mt-1">
          Try clicking Regenerate to try again.
        </p>
      </div>
    );
  }

  return (
    <Section icon={BookOpen} title="IEEE Literature Review Table">
      <div className="rounded-xl border border-slate-200 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="bg-primary text-white text-left">
                <th className="px-3 py-2.5 font-semibold text-xs whitespace-nowrap">#</th>
                <th className="px-3 py-2.5 font-semibold text-xs">Authors</th>
                <th className="px-3 py-2.5 font-semibold text-xs">Year</th>
                <th className="px-3 py-2.5 font-semibold text-xs">Title</th>
                <th className="px-3 py-2.5 font-semibold text-xs">Methodology</th>
                <th className="px-3 py-2.5 font-semibold text-xs">Key Findings</th>
                <th className="px-3 py-2.5 font-semibold text-xs">Limitations</th>
              </tr>
            </thead>
            <tbody>
              {litReview.entries.map((entry, i) => (
                <tr
                  key={i}
                  className={clsx(
                    "border-b border-slate-100 hover:bg-primary/[0.02] transition-colors",
                    i % 2 === 0 ? "bg-white" : "bg-slate-50/50"
                  )}
                >
                  <td className="px-3 py-2.5 text-xs font-bold text-primary whitespace-nowrap">
                    [{entry.ref_no}]
                  </td>
                  <td className="px-3 py-2.5 text-xs text-slate-700 whitespace-nowrap">
                    {entry.authors}
                  </td>
                  <td className="px-3 py-2.5 text-xs text-slate-500 whitespace-nowrap">
                    {entry.year}
                  </td>
                  <td className="px-3 py-2.5 text-xs font-medium text-slate-800 min-w-[180px]">
                    {entry.title}
                  </td>
                  <td className="px-3 py-2.5 text-xs text-slate-600 min-w-[160px]">
                    {entry.methodology}
                  </td>
                  <td className="px-3 py-2.5 text-xs text-slate-600 min-w-[160px]">
                    {entry.key_findings}
                  </td>
                  <td className="px-3 py-2.5 text-xs text-slate-600 min-w-[160px]">
                    {entry.limitations}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </Section>
  );
}

/* ── Save to Library Modal ─────────────────────────────────── */

function SaveToLibraryModal({
  paperId,
  onClose,
  onSaved,
}: {
  paperId: string;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [folders, setFolders] = useState<Folder[]>([]);
  const [selectedFolderId, setSelectedFolderId] = useState<string | null>(null);
  const [newFolderName, setNewFolderName] = useState("");
  const [showNewFolder, setShowNewFolder] = useState(false);
  const [note, setNote] = useState("");
  const [saving, setSaving] = useState(false);
  const [loadingFolders, setLoadingFolders] = useState(true);

  useEffect(() => {
    libraryApi.folders().then((f) => {
      setFolders(f);
      setLoadingFolders(false);
    }).catch(() => setLoadingFolders(false));
  }, []);

  async function handleSave() {
    setSaving(true);
    try {
      let folderId = selectedFolderId;

      if (showNewFolder && newFolderName.trim()) {
        const folder = await libraryApi.createFolder(newFolderName.trim());
        folderId = folder.id;
      }

      await libraryApi.save({
        paper_id: paperId,
        folder_id: folderId ?? undefined,
        personal_note: note.trim() || undefined,
      });
      onSaved();
    } catch {
      setSaving(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md mx-4 overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
          <h3 className="text-lg font-bold text-primary">Save to Library</h3>
          <button onClick={onClose} className="p-1 rounded-lg text-slate-400 hover:text-slate-600 hover:bg-slate-100 transition-colors">
            <X className="h-5 w-5" />
          </button>
        </div>

        <div className="px-6 py-5 space-y-5">
          {/* Folder selection */}
          <div>
            <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">
              Choose Folder
            </label>
            <div className="mt-2 space-y-2 max-h-48 overflow-y-auto">
              {/* No folder option */}
              <button
                onClick={() => { setSelectedFolderId(null); setShowNewFolder(false); }}
                className={clsx(
                  "w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm text-left transition-colors",
                  selectedFolderId === null && !showNewFolder
                    ? "bg-primary/10 text-primary font-semibold"
                    : "text-slate-600 hover:bg-slate-50"
                )}
              >
                <FolderOpen className="h-4 w-4 shrink-0" />
                No folder (root)
              </button>

              {loadingFolders ? (
                <p className="text-xs text-slate-400 px-3 py-2">Loading folders...</p>
              ) : (
                folders.map((folder) => (
                  <button
                    key={folder.id}
                    onClick={() => { setSelectedFolderId(folder.id); setShowNewFolder(false); }}
                    className={clsx(
                      "w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm text-left transition-colors",
                      selectedFolderId === folder.id
                        ? "bg-primary/10 text-primary font-semibold"
                        : "text-slate-600 hover:bg-slate-50"
                    )}
                  >
                    <FolderOpen className="h-4 w-4 shrink-0" />
                    {folder.name}
                  </button>
                ))
              )}

              {/* New folder option */}
              <button
                onClick={() => { setShowNewFolder(true); setSelectedFolderId(null); }}
                className={clsx(
                  "w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm text-left transition-colors",
                  showNewFolder
                    ? "bg-primary/10 text-primary font-semibold"
                    : "text-slate-600 hover:bg-slate-50"
                )}
              >
                <Plus className="h-4 w-4 shrink-0" />
                Create new folder
              </button>
            </div>

            {showNewFolder && (
              <input
                type="text"
                value={newFolderName}
                onChange={(e) => setNewFolderName(e.target.value)}
                placeholder="Folder name"
                autoFocus
                className="mt-2 w-full rounded-xl border border-slate-200 px-3 py-2 text-sm text-slate-700 placeholder:text-slate-400 focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary"
              />
            )}
          </div>

          {/* Note */}
          <div>
            <label className="text-xs font-bold text-slate-500 uppercase tracking-wider">
              Personal Note (optional)
            </label>
            <textarea
              value={note}
              onChange={(e) => setNote(e.target.value)}
              placeholder="Add a note about this paper..."
              rows={2}
              className="mt-2 w-full rounded-xl border border-slate-200 px-3 py-2 text-sm text-slate-700 placeholder:text-slate-400 focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary resize-none"
            />
          </div>
        </div>

        {/* Footer */}
        <div className="flex gap-3 px-6 py-4 border-t border-slate-100 bg-slate-50/50">
          <button onClick={onClose} className="btn-secondary flex-1">
            Cancel
          </button>
          <button
            onClick={handleSave}
            disabled={saving || (showNewFolder && !newFolderName.trim())}
            className="btn-primary flex-1 flex items-center justify-center gap-2 disabled:opacity-50"
          >
            {saving ? (
              <div className="h-4 w-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
            ) : (
              <Bookmark className="h-4 w-4" />
            )}
            {saving ? "Saving..." : "Save"}
          </button>
        </div>
      </div>
    </div>
  );
}

/* ── Shared Components ─────────────────────────────────────── */

function StatCard({
  icon: Icon,
  label,
  value,
}: {
  icon: any;
  label: string;
  value: string;
}) {
  return (
    <div className="rounded-2xl bg-primary/[0.03] p-4 text-center">
      <Icon className="mx-auto h-5 w-5 text-primary mb-1" />
      <p className="text-[10px] font-bold tracking-wider text-primary uppercase">
        {label}
      </p>
      <p className="text-sm font-bold text-slate-800">{value}</p>
    </div>
  );
}

function Section({
  icon: Icon,
  title,
  children,
}: {
  icon: any;
  title: string;
  children: React.ReactNode;
}) {
  return (
    <div>
      <div className="flex items-center gap-2 mb-3">
        <div className="h-3 w-1 rounded-full bg-primary" />
        <span className="section-label">{title}</span>
      </div>
      {children}
    </div>
  );
}

/* ── Mock Data ─────────────────────────────────────────────── */

const MOCK_PAPER: Paper = {
  id: "p1",
  external_id: "2403.1234",
  title: "Scalable Transformers for Long-Context Scientific Reasoning",
  authors: ["Zhang, W.", "Kumar, A.", "Liu, S."],
  abstract:
    "A novel architecture that enables processing of extremely long sequences without quadratic complexity overhead, achieving near-linear memory usage for scientific document understanding.",
  published_date: "2024-03-15T00:00:00Z",
  source: "arXiv",
  pdf_url: "https://arxiv.org/pdf/2403.1234",
  citation_count: 1200,
  venue: null,
  created_at: new Date().toISOString(),
};

const MOCK_SUMMARY: PaperSummary = {
  paper_id: "p1",
  purpose:
    "This paper proposes a novel architecture that allows Transformer models to process sequences of arbitrary length without the quadratic memory overhead. By implementing a sliding window attention mechanism coupled with dynamic key-value compression, the authors demonstrate 10x throughput improvements on scientific reasoning benchmarks.",
  methodology:
    "The authors introduce a hierarchical attention mechanism with three key innovations: (1) chunked sliding-window attention with overlapping segments, (2) dynamic KV-cache compression using learned importance scores, and (3) a cross-chunk propagation mechanism that maintains global context. The model is trained on a mixture of scientific papers, code, and mathematical proofs totaling 2.1T tokens.",
  key_results:
    "Achieves near-linear complexity in memory usage. Outperforms state-of-the-art on LongBench and ScienceQA benchmarks by 14.2%. Zero-shot capability demonstrated on 1M+ token contexts. 10x throughput improvement over standard Transformer at 128k context length.",
  limitations:
    "The approach requires pre-training from scratch and cannot be easily applied to existing models. Performance degrades on tasks requiring precise positional information beyond 500k tokens. Training compute requirements are 2.3x higher than standard approaches.",
  relevance_to_field:
    "Highly relevant to your research on efficient NLP models. The sliding window attention technique could be applied to your current project on scientific document understanding, and the KV-cache compression method addresses the exact memory bottleneck you identified in your proposal.",
  status: "ready",
  generated_at: new Date().toISOString(),
  model_version: "llama3.2:3b",
};

const MOCK_SUGGESTIONS: PaperSuggestions = {
  paper_id: "p1",
  research_directions: [
    "Investigate applying the sliding window attention to domain-specific scientific corpora (e.g., biomedical literature) where long documents are common and context windows are the primary bottleneck.",
    "Explore combining this architecture with retrieval-augmented generation (RAG) to handle documents exceeding 1M tokens while maintaining factual grounding.",
    "Develop a fine-tuning strategy that adapts pre-trained standard Transformers to use this architecture, eliminating the need to train from scratch.",
    "Study the interplay between KV-cache compression rates and downstream task performance to find optimal compression schedules for different scientific domains.",
  ],
  practical_applications: [
    "Build a scientific literature review assistant that can process and synthesize entire PhD theses or systematic reviews in a single pass, leveraging the near-linear memory scaling.",
    "Apply to real-time analysis of streaming scientific data (e.g., genomics pipelines) where low-latency long-context processing is critical.",
    "Integrate into academic search engines to enable whole-paper semantic understanding rather than abstract-only matching.",
  ],
  recommended_reading: [
    "Ring Attention (Liu et al., 2023) - A complementary approach to distributed long-context processing that could be combined with this work.",
    "Longformer (Beltagy et al., 2020) - The foundational work on sliding window attention patterns for long documents.",
    "GQA: Training Generalized Multi-Query Transformer Models (Ainslie et al., 2023) - Relevant KV-cache optimization techniques.",
    "SciBERT (Beltagy et al., 2019) - Domain-specific pre-training for scientific text that could benefit from longer context windows.",
    "Mamba (Gu & Dao, 2023) - Alternative state-space model approach to long sequences worth comparing against.",
  ],
  key_takeaways: [
    "Near-linear memory scaling is achievable without sacrificing benchmark performance — the 14.2% improvement over SOTA proves efficiency and quality aren't trade-offs.",
    "The KV-cache compression with learned importance scores is the most novel contribution and could be extracted as a standalone technique for existing models.",
    "Pre-training from scratch remains a significant barrier to adoption; this limits practical impact until transfer learning solutions are developed.",
    "The cross-chunk propagation mechanism is essential for maintaining coherence — ablation studies show 23% performance drop without it.",
  ],
  status: "ready",
};
