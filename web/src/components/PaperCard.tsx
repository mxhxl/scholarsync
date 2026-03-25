"use client";

import { memo } from "react";
import Link from "next/link";
import { Bookmark, ExternalLink, Clock } from "lucide-react";
import clsx from "clsx";
import type { Paper } from "@/lib/api";

interface Props {
  paper: Paper;
  relevanceScore?: number;
  priority?: string;
  saved?: boolean;
  onSave?: () => void;
  compact?: boolean;
}

const SOURCE_COLORS: Record<string, string> = {
  arxiv: "bg-accent-teal/10 text-accent-teal",
  pubmed: "bg-accent-gold/10 text-accent-gold",
  default: "bg-primary/10 text-primary",
};

function getSourceColor(source: string) {
  const key = source.toLowerCase();
  if (key.includes("arxiv")) return SOURCE_COLORS.arxiv;
  if (key.includes("pubmed")) return SOURCE_COLORS.pubmed;
  return SOURCE_COLORS.default;
}

export default memo(function PaperCard({
  paper,
  relevanceScore,
  priority,
  saved,
  onSave,
  compact,
}: Props) {
  const date = paper.published_date
    ? new Date(paper.published_date).toLocaleDateString("en-US", {
        month: "short",
        year: "numeric",
      })
    : null;

  return (
    <Link href={`/feed/${paper.id}`} className="block group">
      <div
        className={clsx(
          "card p-5 transition-all hover:shadow-md hover:border-primary/20",
          priority === "high" && "border-l-4 border-l-accent-teal"
        )}
      >
        <div className="flex items-start justify-between gap-4">
          <div className="flex-1 min-w-0">
            {/* Source badge */}
            <div className="flex items-center gap-2 mb-2">
              <span
                className={clsx(
                  "chip text-[11px]",
                  getSourceColor(paper.source)
                )}
              >
                {paper.source}
              </span>
              {priority === "high" && (
                <span className="chip bg-accent-teal/10 text-accent-teal text-[11px]">
                  High Priority
                </span>
              )}
              {relevanceScore != null && (
                <span className="text-[11px] text-slate-400 font-medium">
                  {Math.round(relevanceScore * 100)}% match
                </span>
              )}
            </div>

            {/* Title */}
            <h3
              className={clsx(
                "font-bold text-primary group-hover:text-primary-light transition-colors",
                compact ? "text-sm line-clamp-1" : "text-base line-clamp-2"
              )}
            >
              {paper.title}
            </h3>

            {/* Authors & date */}
            <p className="mt-1 text-xs text-slate-400">
              {paper.authors.slice(0, 3).join(", ")}
              {paper.authors.length > 3 && " et al."}
              {date && <> &middot; {date}</>}
            </p>

            {/* Abstract excerpt */}
            {!compact && (
              <p className="mt-2 text-sm text-slate-500 line-clamp-2 leading-relaxed">
                {paper.abstract}
              </p>
            )}

            {/* Bottom meta */}
            <div className="mt-3 flex items-center gap-4 text-xs text-slate-400">
              {paper.citation_count > 0 && (
                <span>{paper.citation_count} citations</span>
              )}
              {paper.pdf_url && (
                <span className="flex items-center gap-1">
                  <ExternalLink className="h-3 w-3" />
                  PDF
                </span>
              )}
            </div>
          </div>

          {/* Bookmark button */}
          {onSave && (
            <button
              onClick={(e) => {
                e.preventDefault();
                e.stopPropagation();
                onSave();
              }}
              className={clsx(
                "rounded-lg p-2 transition-colors",
                saved
                  ? "bg-primary/10 text-primary"
                  : "text-slate-300 hover:text-primary hover:bg-primary/5"
              )}
            >
              <Bookmark className={clsx("h-5 w-5", saved && "fill-current")} />
            </button>
          )}
        </div>
      </div>
    </Link>
  );
});

