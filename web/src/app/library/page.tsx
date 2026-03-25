"use client";

import { useState, useEffect, useMemo } from "react";
import {
  Search,
  FolderOpen,
  Plus,
  Filter,
  Library as LibraryIcon,
  X,
} from "lucide-react";
import {
  library as libraryApi,
  LibraryItem,
  Folder,
} from "@/lib/api";
import PaperCard from "@/components/PaperCard";
import PageHeader from "@/components/PageHeader";
import EmptyState from "@/components/EmptyState";
import clsx from "clsx";

export default function LibraryPage() {
  const [items, setItems] = useState<LibraryItem[]>([]);
  const [folders, setFolders] = useState<Folder[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [activeFolder, setActiveFolder] = useState<string | null>(null);
  const [showNewFolder, setShowNewFolder] = useState(false);
  const [newFolderName, setNewFolderName] = useState("");

  useEffect(() => {
    loadLibrary();
  }, []);

  async function loadLibrary() {
    setLoading(true);
    try {
      const [lib, fld] = await Promise.all([
        libraryApi.list(),
        libraryApi.folders(),
      ]);
      setItems(lib.items);
      setFolders(fld);
    } catch {
      setItems(MOCK_LIBRARY);
      setFolders(MOCK_FOLDERS);
    } finally {
      setLoading(false);
    }
  }

  async function handleCreateFolder() {
    if (!newFolderName.trim()) return;
    try {
      const f = await libraryApi.createFolder(newFolderName.trim());
      setFolders((prev) => [...prev, f]);
    } catch {
      setFolders((prev) => [
        ...prev,
        {
          id: crypto.randomUUID(),
          user_id: "",
          name: newFolderName.trim(),
          created_at: new Date().toISOString(),
        },
      ]);
    }
    setNewFolderName("");
    setShowNewFolder(false);
  }

  const filtered = useMemo(
    () =>
      items.filter((item) => {
        if (activeFolder && item.folder_id !== activeFolder) return false;
        if (search) {
          const q = search.toLowerCase();
          return (
            item.paper.title.toLowerCase().includes(q) ||
            item.tags.some((t) => t.toLowerCase().includes(q))
          );
        }
        return true;
      }),
    [items, activeFolder, search]
  );

  const folderCounts = useMemo(() => {
    const counts: Record<string, number> = {};
    for (const item of items) {
      if (item.folder_id) {
        counts[item.folder_id] = (counts[item.folder_id] ?? 0) + 1;
      }
    }
    return counts;
  }, [items]);

  return (
    <div className="min-h-screen bg-slate-50">
      <PageHeader
        title="Library"
        subtitle={`${items.length} saved papers`}
        actions={
          <div className="relative">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search library..."
              className="input-field pl-9 w-64 py-2 text-sm"
            />
          </div>
        }
      />

      <div className="px-8 py-6">
        {/* Folders section */}
        <div className="mb-8">
          <div className="flex items-center justify-between mb-4">
            <h3 className="section-label">Folders</h3>
            <button
              onClick={() => setShowNewFolder(true)}
              className="btn-ghost text-xs flex items-center gap-1"
            >
              <Plus className="h-3 w-3" />
              New Folder
            </button>
          </div>

          {showNewFolder && (
            <div className="flex items-center gap-2 mb-4">
              <input
                type="text"
                value={newFolderName}
                onChange={(e) => setNewFolderName(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter") handleCreateFolder();
                  if (e.key === "Escape") setShowNewFolder(false);
                }}
                placeholder="Folder name"
                className="input-field flex-1 py-2 text-sm"
                autoFocus
              />
              <button onClick={handleCreateFolder} className="btn-primary text-sm py-2">
                Create
              </button>
              <button
                onClick={() => setShowNewFolder(false)}
                className="p-2 text-slate-400 hover:text-slate-600"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
          )}

          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
            <button
              onClick={() => setActiveFolder(null)}
              className={clsx(
                "card p-4 text-left transition-all hover:shadow-md",
                !activeFolder && "ring-2 ring-primary"
              )}
            >
              <FolderOpen className="h-8 w-8 text-primary mb-2" />
              <p className="text-sm font-bold text-primary">All Papers</p>
              <p className="text-xs text-slate-400 mt-1">{items.length} papers</p>
            </button>

            {folders.map((folder) => {
              const count = folderCounts[folder.id] ?? 0;
              return (
                <button
                  key={folder.id}
                  onClick={() => setActiveFolder(folder.id)}
                  className={clsx(
                    "card p-4 text-left transition-all hover:shadow-md",
                    activeFolder === folder.id && "ring-2 ring-primary"
                  )}
                >
                  <FolderOpen className="h-8 w-8 text-accent-teal mb-2" />
                  <p className="text-sm font-bold text-primary">{folder.name}</p>
                  <p className="text-xs text-slate-400 mt-1">
                    {count} paper{count !== 1 ? "s" : ""}
                  </p>
                </button>
              );
            })}
          </div>
        </div>

        {/* Papers list */}
        <div className="flex items-center justify-between mb-4">
          <h3 className="section-label">
            {activeFolder
              ? folders.find((f) => f.id === activeFolder)?.name ?? "Papers"
              : "Recent Papers"}
          </h3>
          <button className="p-2 text-slate-400 hover:text-primary transition-colors">
            <Filter className="h-4 w-4" />
          </button>
        </div>

        {loading ? (
          <div className="space-y-4">
            {[1, 2, 3].map((i) => (
              <div key={i} className="card p-5 animate-pulse">
                <div className="h-4 w-24 bg-slate-100 rounded mb-3" />
                <div className="h-5 w-3/4 bg-slate-100 rounded mb-2" />
                <div className="h-4 w-1/2 bg-slate-100 rounded" />
              </div>
            ))}
          </div>
        ) : filtered.length === 0 ? (
          <EmptyState
            icon={LibraryIcon}
            title="No papers here yet"
            description="Save papers from your feed to build your research library."
          />
        ) : (
          <div className="space-y-3">
            {filtered.map((item) => (
              <div key={item.id} className="relative">
                <PaperCard paper={item.paper} compact />
                {item.tags.length > 0 && (
                  <div className="flex gap-1 mt-1 ml-5">
                    {item.tags.map((tag) => (
                      <span
                        key={tag}
                        className="chip chip-inactive text-[10px]"
                      >
                        {tag}
                      </span>
                    ))}
                  </div>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

const MOCK_FOLDERS: Folder[] = [
  { id: "f1", user_id: "u1", name: "Deep Learning", created_at: new Date().toISOString() },
  { id: "f2", user_id: "u1", name: "NLP Projects", created_at: new Date().toISOString() },
];

const MOCK_LIBRARY: LibraryItem[] = [
  {
    id: "l1",
    user_id: "u1",
    paper_id: "p10",
    paper: {
      id: "p10",
      external_id: "1706.03762",
      title: "Attention is All You Need",
      authors: ["Vaswani, A.", "Shazeer, N.", "Parmar, N."],
      abstract: "The dominant sequence transduction models are based on complex recurrent or convolutional neural networks.",
      published_date: "2017-06-12T00:00:00Z",
      source: "arXiv",
      pdf_url: "https://arxiv.org/pdf/1706.03762",
      citation_count: 98000,
      venue: null,
      created_at: new Date().toISOString(),
    },
    folder_id: "f1",
    tags: ["transformers", "attention"],
    personal_note: null,
    is_read: true,
    saved_at: new Date(Date.now() - 2 * 86400000).toISOString(),
  },
  {
    id: "l2",
    user_id: "u1",
    paper_id: "p11",
    paper: {
      id: "p11",
      external_id: "1810.04805",
      title: "BERT: Pre-training of Deep Bidirectional Transformers for Language Understanding",
      authors: ["Devlin, J.", "Chang, M.", "Lee, K.", "Toutanova, K."],
      abstract: "We introduce a new language representation model called BERT.",
      published_date: "2018-10-11T00:00:00Z",
      source: "arXiv",
      pdf_url: "https://arxiv.org/pdf/1810.04805",
      citation_count: 75000,
      venue: null,
      created_at: new Date().toISOString(),
    },
    folder_id: "f2",
    tags: ["NLP", "pre-training"],
    personal_note: null,
    is_read: true,
    saved_at: new Date(Date.now() - 5 * 86400000).toISOString(),
  },
  {
    id: "l3",
    user_id: "u1",
    paper_id: "p12",
    paper: {
      id: "p12",
      external_id: "1905.11946",
      title: "EfficientNet: Rethinking Model Scaling for Convolutional Neural Networks",
      authors: ["Tan, M.", "Le, Q."],
      abstract: "Convolutional Neural Networks are commonly developed at a fixed resource budget.",
      published_date: "2019-05-28T00:00:00Z",
      source: "arXiv",
      pdf_url: "https://arxiv.org/pdf/1905.11946",
      citation_count: 12000,
      venue: null,
      created_at: new Date().toISOString(),
    },
    folder_id: "f1",
    tags: ["efficiency", "scaling"],
    personal_note: null,
    is_read: false,
    saved_at: new Date(Date.now() - 7 * 86400000).toISOString(),
  },
];
