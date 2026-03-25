const BASE = "/api";

// ── GET request cache (30s TTL) + in-flight deduplication ────────────
const cache = new Map<string, { data: unknown; ts: number }>();
const inflight = new Map<string, Promise<unknown>>();
const CACHE_TTL = 30_000;

function getCacheKey(path: string, token: string | null) {
  return `${token ?? "anon"}:${path}`;
}

function invalidateCache(pathPrefix?: string) {
  if (!pathPrefix) {
    cache.clear();
    return;
  }
  const keys = Array.from(cache.keys());
  for (const key of keys) {
    if (key.includes(pathPrefix)) cache.delete(key);
  }
}

async function request<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const token =
    typeof window !== "undefined" ? localStorage.getItem("access_token") : null;
  const method = (options.method ?? "GET").toUpperCase();
  const isGet = method === "GET";

  // Check cache for GET requests
  if (isGet) {
    const cacheKey = getCacheKey(path, token);
    const cached = cache.get(cacheKey);
    if (cached && Date.now() - cached.ts < CACHE_TTL) {
      return cached.data as T;
    }
    // Deduplicate concurrent identical GET requests
    const existing = inflight.get(cacheKey);
    if (existing) return existing as Promise<T>;
  }

  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(options.headers as Record<string, string>),
  };
  if (token) headers["Authorization"] = `Bearer ${token}`;

  const doFetch = async (): Promise<T> => {
    const res = await fetch(`${BASE}${path}`, { ...options, headers });

    if (res.status === 401 && typeof window !== "undefined") {
      const refreshed = await tryRefresh();
      if (refreshed) return request<T>(path, options);
      localStorage.removeItem("access_token");
      localStorage.removeItem("refresh_token");
      window.location.href = "/login";
      throw new Error("Session expired");
    }

    if (!res.ok) {
      const body = await res.json().catch(() => ({}));
      const detail = body.detail;
      const message =
        typeof detail === "object" && detail !== null
          ? detail.detail || detail.error || JSON.stringify(detail)
          : typeof detail === "string"
          ? detail
          : res.statusText;
      throw new Error(message);
    }

    if (res.status === 204) return {} as T;
    const data = await res.json();

    // Cache successful GET responses
    if (isGet) {
      cache.set(getCacheKey(path, token), { data, ts: Date.now() });
    } else {
      // Mutations invalidate related cache entries
      invalidateCache(path.split("?")[0].split("/").slice(0, 2).join("/"));
    }

    return data;
  };

  if (isGet) {
    const cacheKey = getCacheKey(path, token);
    const promise = doFetch().finally(() => inflight.delete(cacheKey));
    inflight.set(cacheKey, promise);
    return promise;
  }

  return doFetch();
}

async function tryRefresh(): Promise<boolean> {
  const refreshToken = localStorage.getItem("refresh_token");
  if (!refreshToken) return false;
  try {
    const res = await fetch(`${BASE}/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refresh_token: refreshToken }),
    });
    if (!res.ok) return false;
    const data = await res.json();
    localStorage.setItem("access_token", data.access_token);
    localStorage.setItem("refresh_token", data.refresh_token);
    return true;
  } catch {
    return false;
  }
}

// ── Auth ──────────────────────────────────────────────────────────────

export const auth = {
  register(data: {
    email: string;
    password: string;
    name: string;
    institution?: string;
  }) {
    // Backend field is full_name, not name
    return request<{ access_token: string; refresh_token: string; user: User }>(
      "/auth/register",
      {
        method: "POST",
        body: JSON.stringify({
          email: data.email,
          password: data.password,
          full_name: data.name,
          institution: data.institution,
        }),
      }
    );
  },

  login(email: string, password: string) {
    return request<{ access_token: string; refresh_token: string; user: User }>(
      "/auth/login",
      { method: "POST", body: JSON.stringify({ email, password }) }
    );
  },

  me() {
    return request<User>("/auth/me");
  },

  refresh(refreshToken: string) {
    return request<{ access_token: string; refresh_token: string }>(
      "/auth/refresh",
      { method: "POST", body: JSON.stringify({ refresh_token: refreshToken }) }
    );
  },
};

// ── Profile ───────────────────────────────────────────────────────────

export const profile = {
  setup(data: ProfileSetup) {
    return request<ResearchProfile>("/profile/setup", {
      method: "POST",
      body: JSON.stringify(data),
    });
  },

  get() {
    return request<ResearchProfile>("/profile/");
  },

  update(data: Partial<ProfileSetup>) {
    return request<ResearchProfile>("/profile/", {
      method: "PUT",
      body: JSON.stringify(data),
    });
  },

  getPreferences() {
    return request<NotificationPreferences>("/profile/preferences");
  },

  updatePreferences(data: Partial<NotificationPreferences>) {
    return request<NotificationPreferences>("/profile/preferences", {
      method: "PUT",
      body: JSON.stringify(data),
    });
  },
};

// ── Feed ──────────────────────────────────────────────────────────────

export const feed = {
  get(params?: { filter?: string; limit?: number; offset?: number }) {
    const q = new URLSearchParams();
    if (params?.filter) q.set("filter", params.filter);
    if (params?.limit) q.set("limit", String(params.limit));
    if (params?.offset) q.set("offset", String(params.offset));
    const qs = q.toString() ? `?${q}` : "";
    return request<FeedResponse>(`/feed/${qs}`);
  },

  dismiss(paperId: string) {
    return request<void>(`/feed/${paperId}/dismiss`, { method: "POST" });
  },

  markRead(paperId: string) {
    return request<void>(`/feed/${paperId}/mark-read`, { method: "POST" });
  },
};

// ── Papers ────────────────────────────────────────────────────────────

export const papers = {
  get(id: string) {
    return request<Paper>(`/papers/${id}`);
  },

  summary(id: string) {
    return request<PaperSummary>(`/papers/${id}/summary`);
  },

  suggestions(id: string) {
    return request<PaperSuggestions>(`/papers/${id}/suggestions`);
  },

  literatureReview(id: string) {
    return request<LiteratureReview>(`/papers/${id}/literature-review`);
  },
};

// ── Library ───────────────────────────────────────────────────────────

export const library = {
  save(data: {
    paper_id: string;
    folder_id?: string;
    tags?: string[];
    personal_note?: string;
  }) {
    return request<LibraryItem>("/library/save", {
      method: "POST",
      body: JSON.stringify(data),
    });
  },

  list(params?: { folder_id?: string; tag?: string; limit?: number; offset?: number }) {
    const q = new URLSearchParams();
    if (params?.folder_id) q.set("folder_id", params.folder_id);
    if (params?.tag) q.set("tag", params.tag);
    if (params?.limit) q.set("limit", String(params.limit));
    if (params?.offset) q.set("offset", String(params.offset));
    const qs = q.toString() ? `?${q}` : "";
    return request<LibraryResponse>(`/library/${qs}`);
  },

  update(
    paperId: string,
    data: Partial<{
      folder_id: string;
      tags: string[];
      personal_note: string;
      is_read: boolean;
    }>
  ) {
    return request<LibraryItem>(`/library/${paperId}`, {
      method: "PUT",
      body: JSON.stringify(data),
    });
  },

  remove(paperId: string) {
    return request<void>(`/library/${paperId}`, { method: "DELETE" });
  },

  search(q: string) {
    return request<LibraryResponse>(
      `/library/search?q=${encodeURIComponent(q)}`
    );
  },

  folders() {
    return request<Folder[]>("/library/folders");
  },

  createFolder(name: string) {
    return request<Folder>("/library/folders", {
      method: "POST",
      body: JSON.stringify({ name }),
    });
  },
};

// ── Alerts ────────────────────────────────────────────────────────────

export const alerts = {
  list(params?: { type?: string; is_read?: boolean; limit?: number }) {
    const q = new URLSearchParams();
    if (params?.type) q.set("type", params.type);
    if (params?.is_read !== undefined) q.set("is_read", String(params.is_read));
    if (params?.limit) q.set("limit", String(params.limit));
    const qs = q.toString() ? `?${q}` : "";
    return request<AlertsResponse>(`/alerts/${qs}`);
  },

  unreadCount() {
    return request<{ count: number }>("/alerts/unread-count");
  },

  get(id: string) {
    return request<Alert>(`/alerts/${id}`);
  },

  markRead(id: string) {
    return request<void>(`/alerts/${id}/read`, { method: "POST" });
  },

  acknowledge(id: string) {
    return request<void>(`/alerts/${id}/acknowledge`, { method: "POST" });
  },
};

// ── Projects ──────────────────────────────────────────────────────────

export const projects = {
  setCurrent(data: { title: string; description: string }) {
    return request<Project>("/projects/current", {
      method: "POST",
      body: JSON.stringify(data),
    });
  },

  getCurrent() {
    return request<Project>("/projects/current");
  },

  deleteCurrent() {
    return request<void>("/projects/current", { method: "DELETE" });
  },
};

// ── Citations ─────────────────────────────────────────────────────────

export const citations = {
  get(paperId: string) {
    return request<CitationNetwork>(`/citations/${paperId}`);
  },

  mustCite() {
    return request<MustCiteResponse>("/citations/must-cite");
  },
};

// ── Insights ──────────────────────────────────────────────────────────

export const insights = {
  trends() {
    return request<TrendsResponse>("/insights/trends");
  },
};

// ── Stats ────────────────────────────────────────────────────────────

export const stats = {
  me() {
    return request<UserStats>("/stats/me");
  },
};

// ── Notifications ─────────────────────────────────────────────────────

export const notifications = {
  registerDevice(token: string, platform: "android" | "ios" | "web") {
    return request<void>("/notifications/device-token", {
      method: "POST",
      body: JSON.stringify({ token, platform }),
    });
  },
};

// ── Types ─────────────────────────────────────────────────────────────

export interface User {
  id: string;
  email: string;
  full_name: string;
  institution: string | null;
  role: string;
}

export interface ProfileSetup {
  research_field: string;
  topics: string[];
  keywords: string[];
  authors_following?: string[];
}

export interface ResearchProfile {
  id: string;
  user_id: string;
  research_field: string;
  topics: string[];
  keywords: string[];
  authors_following: string[];
  created_at: string;
  updated_at: string;
}

export interface NotificationPreferences {
  id: string;
  user_id: string;
  digest_time: string;
  overlap_sensitivity: string;
  enable_high_priority: boolean;
  enable_overlap_alerts: boolean;
  enable_email: boolean;
}

export interface Paper {
  id: string;
  external_id: string;
  source: string;
  title: string;
  authors: string[];
  abstract: string | null;
  published_date: string | null;
  pdf_url: string | null;
  citation_count: number;
  venue: string | null;
  created_at: string;
}

export interface PaperSummary {
  paper_id: string;
  purpose: string | null;
  methodology: string | null;
  key_results: string | null;
  limitations: string | null;
  relevance_to_field: string | null;
  status: string;
  model_version: string | null;
  generated_at: string | null;
}

export interface PaperSuggestions {
  paper_id: string;
  research_directions: string[];
  practical_applications: string[];
  recommended_reading: string[];
  key_takeaways: string[];
  status: string;
}

export interface LitReviewEntry {
  ref_no: number;
  authors: string;
  year: string;
  title: string;
  methodology: string;
  key_findings: string;
  limitations: string;
}

export interface LiteratureReview {
  paper_id: string;
  entries: LitReviewEntry[];
  status: string;
}

export interface FeedItem {
  id: string;
  paper_id: string;
  paper: Paper;
  relevance_score: number;
  priority: string;
  is_read: boolean;
  is_saved: boolean;
  created_at: string;
}

export interface FeedResponse {
  items: FeedItem[];
  total: number;
  has_more: boolean;
}

export interface LibraryItem {
  id: string;
  user_id: string;
  paper_id: string;
  paper: Paper;
  folder_id: string | null;
  tags: string[];
  personal_note: string | null;
  is_read: boolean;
  saved_at: string;
}

export interface LibraryResponse {
  items: LibraryItem[];
  total: number;
}

export interface Folder {
  id: string;
  user_id: string;
  name: string;
  created_at: string;
}

export interface Alert {
  id: string;
  user_id: string;
  paper_id: string;
  paper: Paper;
  type: string;
  title: string;
  description: string;
  similarity_score: number;
  comparison_report: Record<string, unknown> | null;
  is_read: boolean;
  is_acknowledged: boolean;
  created_at: string;
}

export interface AlertsResponse {
  items: Alert[];
  total: number;
}

export interface Project {
  id: string;
  user_id: string;
  title: string;
  description: string;
  created_at: string;
  updated_at: string;
}

export interface CitationNetwork {
  paper: Paper;
  backward_citations: Paper[];
  forward_citations: Paper[];
}

export interface MustCiteResponse {
  papers: Array<Paper & { pagerank_score: number }>;
}

export interface TrendResult {
  topic_id: number;
  keywords: string[];
  label: string;
  papers_count: number;
  growth_rate: number;
  trend: "rising" | "stable" | "declining";
  monthly_counts: number[];
}

export interface TrendsResponse {
  trends: TrendResult[];
  computed_at: string;
}

export interface UserStats {
  papers_read: number;
  saved_papers: number;
  summaries: number;
  unread_alerts: number;
}

// ── Admin Types ──────────────────────────────────────────────────────

export interface AdminDashboard {
  total_users: number;
  active_users: number;
  total_papers: number;
  total_alerts: number;
  total_saved_papers: number;
  new_users_last_7_days: number;
  new_papers_last_7_days: number;
}

export interface AdminUser {
  id: string;
  email: string;
  full_name: string;
  institution: string | null;
  role: string;
  is_active: boolean;
  is_verified: boolean;
  created_at: string;
  papers_read: number;
  saved_papers: number;
}

export interface AdminUserListResponse {
  items: AdminUser[];
  total: number;
}

export interface AdminPaper {
  id: string;
  external_id: string;
  source: string;
  title: string;
  authors: string[];
  published_date: string | null;
  citation_count: number;
  venue: string | null;
  has_summary: boolean;
  feed_count: number;
  saved_count: number;
  created_at: string;
}

export interface AdminPaperListResponse {
  items: AdminPaper[];
  total: number;
}

export interface AdminAlert {
  id: string;
  user_id: string;
  user_email: string;
  paper_id: string;
  paper_title: string;
  type: string;
  title: string;
  is_read: boolean;
  is_acknowledged: boolean;
  created_at: string;
}

export interface AdminAlertListResponse {
  items: AdminAlert[];
  total: number;
}

// ── Admin API ────────────────────────────────────────────────────────

export const admin = {
  dashboard() {
    return request<AdminDashboard>("/admin/dashboard");
  },

  createUser(data: {
    email: string;
    password: string;
    full_name: string;
    institution?: string;
    role: string;
  }) {
    return request<AdminUser>("/admin/users", {
      method: "POST",
      body: JSON.stringify(data),
    });
  },

  users(params?: {
    search?: string;
    role?: string;
    is_active?: boolean;
    limit?: number;
    offset?: number;
  }) {
    const q = new URLSearchParams();
    if (params?.search) q.set("search", params.search);
    if (params?.role) q.set("role", params.role);
    if (params?.is_active !== undefined) q.set("is_active", String(params.is_active));
    if (params?.limit) q.set("limit", String(params.limit));
    if (params?.offset) q.set("offset", String(params.offset));
    const qs = q.toString() ? `?${q}` : "";
    return request<AdminUserListResponse>(`/admin/users${qs}`);
  },

  getUser(id: string) {
    return request<AdminUser>(`/admin/users/${id}`);
  },

  updateUser(id: string, data: { is_active?: boolean; is_verified?: boolean; role?: string }) {
    return request<AdminUser>(`/admin/users/${id}`, {
      method: "PATCH",
      body: JSON.stringify(data),
    });
  },

  deleteUser(id: string) {
    return request<void>(`/admin/users/${id}`, { method: "DELETE" });
  },

  papers(params?: { search?: string; source?: string; limit?: number; offset?: number }) {
    const q = new URLSearchParams();
    if (params?.search) q.set("search", params.search);
    if (params?.source) q.set("source", params.source);
    if (params?.limit) q.set("limit", String(params.limit));
    if (params?.offset) q.set("offset", String(params.offset));
    const qs = q.toString() ? `?${q}` : "";
    return request<AdminPaperListResponse>(`/admin/papers${qs}`);
  },

  deletePaper(id: string) {
    return request<void>(`/admin/papers/${id}`, { method: "DELETE" });
  },

  alerts(params?: { type?: string; is_read?: boolean; limit?: number; offset?: number }) {
    const q = new URLSearchParams();
    if (params?.type) q.set("type", params.type);
    if (params?.is_read !== undefined) q.set("is_read", String(params.is_read));
    if (params?.limit) q.set("limit", String(params.limit));
    if (params?.offset) q.set("offset", String(params.offset));
    const qs = q.toString() ? `?${q}` : "";
    return request<AdminAlertListResponse>(`/admin/alerts${qs}`);
  },

  deleteAlert(id: string) {
    return request<void>(`/admin/alerts/${id}`, { method: "DELETE" });
  },
};
