"use client";

import { useState, useEffect, useRef } from "react";
import {
  User,
  Bell,
  Shield,
  Palette,
  Save,
  CheckCircle2,
  FlaskConical,
  Tags,
  Users,
  X,
  Plus,
} from "lucide-react";
import {
  profile as profileApi,
  ResearchProfile,
  NotificationPreferences,
} from "@/lib/api";
import { useAuth } from "@/hooks/useAuth";
import PageHeader from "@/components/PageHeader";
import clsx from "clsx";

type Tab = "profile" | "notifications" | "account";

export default function SettingsPage() {
  const { user } = useAuth();
  const [tab, setTab] = useState<Tab>("profile");
  const [researchProfile, setResearchProfile] = useState<ResearchProfile | null>(null);
  const [prefs, setPrefs] = useState<NotificationPreferences | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);

  const savedTimerRef = useRef<ReturnType<typeof setTimeout>>();
  useEffect(() => () => clearTimeout(savedTimerRef.current), []);

  // Editable fields
  const [researchField, setResearchField] = useState("");
  const [topics, setTopics] = useState<string[]>([]);
  const [keywords, setKeywords] = useState<string[]>([]);
  const [authors, setAuthors] = useState<string[]>([]);
  const [topicInput, setTopicInput] = useState("");
  const [keywordInput, setKeywordInput] = useState("");
  const [authorInput, setAuthorInput] = useState("");
  const [digestTime, setDigestTime] = useState("06:00");
  const [sensitivity, setSensitivity] = useState("medium");
  const [highPriority, setHighPriority] = useState(true);
  const [overlapAlerts, setOverlapAlerts] = useState(true);

  useEffect(() => {
    loadSettings();
  }, []);

  async function loadSettings() {
    setLoading(true);
    try {
      const [prof, notif] = await Promise.all([
        profileApi.get(),
        profileApi.getPreferences(),
      ]);
      setResearchProfile(prof);
      setPrefs(notif);
      setResearchField(prof.research_field);
      setTopics(prof.topics);
      setKeywords(prof.keywords);
      setAuthors(prof.authors_following);
      setDigestTime(notif.digest_time);
      setSensitivity(notif.overlap_sensitivity);
      setHighPriority(notif.enable_high_priority);
      setOverlapAlerts(notif.enable_overlap_alerts);
    } catch {
      // Use defaults
    } finally {
      setLoading(false);
    }
  }

  async function handleSaveProfile() {
    setSaving(true);
    try {
      await profileApi.update({
        research_field: researchField,
        topics,
        keywords,
        authors_following: authors,
      });
      setSaved(true);
      savedTimerRef.current = setTimeout(() => setSaved(false), 2000);
    } catch {
      // handle error
    } finally {
      setSaving(false);
    }
  }

  async function handleSaveNotifications() {
    setSaving(true);
    try {
      await profileApi.updatePreferences({
        digest_time: digestTime,
        overlap_sensitivity: sensitivity,
        enable_high_priority: highPriority,
        enable_overlap_alerts: overlapAlerts,
      });
      setSaved(true);
      savedTimerRef.current = setTimeout(() => setSaved(false), 2000);
    } catch {
      // handle error
    } finally {
      setSaving(false);
    }
  }

  function addChip(
    value: string,
    setter: (v: string) => void,
    list: string[],
    setList: (v: string[]) => void
  ) {
    const v = value.trim();
    if (v && !list.includes(v)) setList([...list, v]);
    setter("");
  }

  const TABS: { key: Tab; label: string; icon: any }[] = [
    { key: "profile", label: "Research Profile", icon: FlaskConical },
    { key: "notifications", label: "Notifications", icon: Bell },
    { key: "account", label: "Account", icon: User },
  ];

  return (
    <div className="min-h-screen bg-slate-50">
      <PageHeader title="Settings" />

      <div className="flex px-8 py-6 gap-8">
        {/* Sidebar tabs */}
        <div className="w-56 shrink-0">
          <div className="space-y-1">
            {TABS.map(({ key, label, icon: Icon }) => (
              <button
                key={key}
                onClick={() => setTab(key)}
                className={clsx(
                  "flex w-full items-center gap-3 rounded-xl px-4 py-3 text-sm font-medium transition-all",
                  tab === key
                    ? "bg-primary text-white"
                    : "text-slate-500 hover:bg-slate-100"
                )}
              >
                <Icon className="h-4 w-4" />
                {label}
              </button>
            ))}
          </div>
        </div>

        {/* Content */}
        <div className="flex-1 max-w-2xl">
          {loading ? (
            <div className="card p-8 animate-pulse space-y-4">
              {[1, 2, 3].map((i) => (
                <div key={i} className="h-10 bg-slate-100 rounded-xl" />
              ))}
            </div>
          ) : tab === "profile" ? (
            <div className="card p-8 space-y-6">
              <div>
                <h3 className="text-lg font-bold text-primary mb-1">
                  Research Profile
                </h3>
                <p className="text-sm text-slate-500">
                  Update your research interests to improve paper
                  recommendations.
                </p>
              </div>

              <div>
                <label className="mb-1.5 block text-sm font-medium text-slate-700">
                  Research Field
                </label>
                <input
                  type="text"
                  value={researchField}
                  onChange={(e) => setResearchField(e.target.value)}
                  className="input-field"
                />
              </div>

              <ChipField
                label="Topics"
                items={topics}
                setItems={setTopics}
                input={topicInput}
                setInput={setTopicInput}
                placeholder="Add a topic"
              />

              <ChipField
                label="Keywords"
                items={keywords}
                setItems={setKeywords}
                input={keywordInput}
                setInput={setKeywordInput}
                placeholder="Add a keyword"
              />

              <ChipField
                label="Following Authors"
                items={authors}
                setItems={setAuthors}
                input={authorInput}
                setInput={setAuthorInput}
                placeholder="Add an author"
              />

              <div className="flex items-center gap-3 pt-2">
                <button
                  onClick={handleSaveProfile}
                  disabled={saving}
                  className="btn-primary flex items-center gap-2 text-sm"
                >
                  {saving ? (
                    <div className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                  ) : saved ? (
                    <CheckCircle2 className="h-4 w-4" />
                  ) : (
                    <Save className="h-4 w-4" />
                  )}
                  {saved ? "Saved!" : "Save Changes"}
                </button>
              </div>
            </div>
          ) : tab === "notifications" ? (
            <div className="card p-8 space-y-6">
              <div>
                <h3 className="text-lg font-bold text-primary mb-1">
                  Notification Preferences
                </h3>
                <p className="text-sm text-slate-500">
                  Control how and when you receive alerts.
                </p>
              </div>

              <div>
                <label className="mb-1.5 block text-sm font-medium text-slate-700">
                  Daily Digest Time
                </label>
                <input
                  type="time"
                  value={digestTime}
                  onChange={(e) => setDigestTime(e.target.value)}
                  className="input-field w-40"
                />
              </div>

              <div>
                <label className="mb-1.5 block text-sm font-medium text-slate-700">
                  Overlap Sensitivity
                </label>
                <div className="flex gap-2">
                  {["low", "medium", "high"].map((s) => (
                    <button
                      key={s}
                      onClick={() => setSensitivity(s)}
                      className={clsx(
                        "chip cursor-pointer capitalize",
                        sensitivity === s ? "chip-active" : "chip-inactive"
                      )}
                    >
                      {s}
                    </button>
                  ))}
                </div>
              </div>

              <Toggle
                label="High-priority paper alerts"
                description="Get notified immediately when highly relevant papers are published."
                checked={highPriority}
                onChange={setHighPriority}
              />

              <Toggle
                label="Overlap detection alerts"
                description="Get notified when new papers overlap with your current project."
                checked={overlapAlerts}
                onChange={setOverlapAlerts}
              />

              <button
                onClick={handleSaveNotifications}
                disabled={saving}
                className="btn-primary flex items-center gap-2 text-sm"
              >
                {saving ? (
                  <div className="h-4 w-4 animate-spin rounded-full border-2 border-white border-t-transparent" />
                ) : saved ? (
                  <CheckCircle2 className="h-4 w-4" />
                ) : (
                  <Save className="h-4 w-4" />
                )}
                {saved ? "Saved!" : "Save Preferences"}
              </button>
            </div>
          ) : (
            <div className="card p-8 space-y-6">
              <div>
                <h3 className="text-lg font-bold text-primary mb-1">Account</h3>
                <p className="text-sm text-slate-500">
                  Manage your account details.
                </p>
              </div>

              <div className="space-y-4">
                <div>
                  <label className="mb-1 block text-xs font-medium text-slate-400 uppercase tracking-wider">
                    Name
                  </label>
                  <p className="text-sm font-semibold text-slate-700">
                    {user?.full_name}
                  </p>
                </div>
                <div>
                  <label className="mb-1 block text-xs font-medium text-slate-400 uppercase tracking-wider">
                    Email
                  </label>
                  <p className="text-sm font-semibold text-slate-700">
                    {user?.email}
                  </p>
                </div>
                <div>
                  <label className="mb-1 block text-xs font-medium text-slate-400 uppercase tracking-wider">
                    Institution
                  </label>
                  <p className="text-sm font-semibold text-slate-700">
                    {user?.institution || "Not set"}
                  </p>
                </div>
                <div>
                  <label className="mb-1 block text-xs font-medium text-slate-400 uppercase tracking-wider">
                    Member since
                  </label>
                  <p className="text-sm font-semibold text-slate-700">—</p>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function ChipField({
  label,
  items,
  setItems,
  input,
  setInput,
  placeholder,
}: {
  label: string;
  items: string[];
  setItems: (v: string[]) => void;
  input: string;
  setInput: (v: string) => void;
  placeholder: string;
}) {
  return (
    <div>
      <label className="mb-1.5 block text-sm font-medium text-slate-700">
        {label}
      </label>
      <div className="flex gap-2">
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              e.preventDefault();
              const v = input.trim();
              if (v && !items.includes(v)) setItems([...items, v]);
              setInput("");
            }
          }}
          placeholder={placeholder}
          className="input-field flex-1"
        />
        <button
          onClick={() => {
            const v = input.trim();
            if (v && !items.includes(v)) setItems([...items, v]);
            setInput("");
          }}
          className="btn-primary px-3"
        >
          <Plus className="h-4 w-4" />
        </button>
      </div>
      <div className="mt-2 flex flex-wrap gap-2">
        {items.map((item, i) => (
          <span key={i} className="chip chip-active flex items-center gap-1">
            {item}
            <button onClick={() => setItems(items.filter((_, j) => j !== i))}>
              <X className="h-3 w-3" />
            </button>
          </span>
        ))}
      </div>
    </div>
  );
}

function Toggle({
  label,
  description,
  checked,
  onChange,
}: {
  label: string;
  description: string;
  checked: boolean;
  onChange: (v: boolean) => void;
}) {
  return (
    <div className="flex items-start justify-between gap-4">
      <div>
        <p className="text-sm font-medium text-slate-700">{label}</p>
        <p className="text-xs text-slate-400 mt-0.5">{description}</p>
      </div>
      <button
        onClick={() => onChange(!checked)}
        className={clsx(
          "relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full transition-colors",
          checked ? "bg-primary" : "bg-slate-200"
        )}
      >
        <span
          className={clsx(
            "pointer-events-none inline-block h-5 w-5 rounded-full bg-white shadow-sm transition-transform mt-0.5",
            checked ? "translate-x-5 ml-0.5" : "translate-x-0.5"
          )}
        />
      </button>
    </div>
  );
}
