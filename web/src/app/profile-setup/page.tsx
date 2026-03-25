"use client";

import { useState, FormEvent } from "react";
import { useRouter } from "next/navigation";
import Image from "next/image";
import {
  FlaskConical,
  Tags,
  Users,
  ArrowRight,
  ArrowLeft,
  X,
  Plus,
} from "lucide-react";
import { profile } from "@/lib/api";
import clsx from "clsx";

const STEPS = [
  { label: "Research Field", icon: FlaskConical },
  { label: "Topics & Keywords", icon: Tags },
  { label: "Authors", icon: Users },
];

export default function ProfileSetupPage() {
  const router = useRouter();
  const [step, setStep] = useState(0);
  const [researchField, setResearchField] = useState("");
  const [topicInput, setTopicInput] = useState("");
  const [topics, setTopics] = useState<string[]>([]);
  const [keywordInput, setKeywordInput] = useState("");
  const [keywords, setKeywords] = useState<string[]>([]);
  const [authorInput, setAuthorInput] = useState("");
  const [authors, setAuthors] = useState<string[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  function addItem(
    value: string,
    setter: (v: string) => void,
    list: string[],
    setList: (v: string[]) => void
  ) {
    const v = value.trim();
    if (v && !list.includes(v)) {
      setList([...list, v]);
    }
    setter("");
  }

  function removeItem(
    index: number,
    list: string[],
    setList: (v: string[]) => void
  ) {
    setList(list.filter((_, i) => i !== index));
  }

  function canProceed() {
    if (step === 0) return researchField.trim().length > 0;
    if (step === 1) return topics.length > 0 && keywords.length > 0;
    return true;
  }

  async function handleFinish() {
    setError("");
    setSubmitting(true);
    try {
      await profile.setup({
        research_field: researchField,
        topics,
        keywords,
        authors_following: authors.length > 0 ? authors : undefined,
      });
      router.push("/feed");
    } catch (err: any) {
      setError(err.message || "Setup failed");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="flex min-h-screen bg-slate-50">
      {/* Left — Step indicator */}
      <div className="hidden lg:flex lg:w-80 flex-col bg-white border-r border-slate-100 p-8">
        <div className="flex items-center gap-3 mb-12">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10">
            <Image src="/favicon.svg" alt="ScholarSync" width={24} height={24} />
          </div>
          <span className="text-lg font-bold text-primary">ScholarSync</span>
        </div>

        <div className="space-y-2">
          {STEPS.map((s, i) => {
            const Icon = s.icon;
            return (
              <div
                key={i}
                className={clsx(
                  "flex items-center gap-3 rounded-xl px-4 py-3 transition-all",
                  i === step
                    ? "bg-primary text-white"
                    : i < step
                    ? "text-primary"
                    : "text-slate-400"
                )}
              >
                <div
                  className={clsx(
                    "flex h-8 w-8 items-center justify-center rounded-lg text-sm font-bold",
                    i === step
                      ? "bg-white/20"
                      : i < step
                      ? "bg-primary/10"
                      : "bg-slate-100"
                  )}
                >
                  {i < step ? "✓" : i + 1}
                </div>
                <span className="text-sm font-medium">{s.label}</span>
              </div>
            );
          })}
        </div>
      </div>

      {/* Right — Form content */}
      <div className="flex flex-1 items-center justify-center p-8">
        <div className="w-full max-w-lg space-y-8">
          {/* Mobile step dots */}
          <div className="flex justify-center gap-2 lg:hidden">
            {STEPS.map((_, i) => (
              <div
                key={i}
                className={clsx(
                  "h-2 rounded-full transition-all",
                  i === step ? "w-8 bg-primary" : "w-2 bg-slate-200"
                )}
              />
            ))}
          </div>

          {error && (
            <div className="rounded-xl bg-red-50 p-4 text-sm text-red-600">
              {error}
            </div>
          )}

          {/* Step 1: Research Field */}
          {step === 0 && (
            <div className="space-y-6">
              <div>
                <h2 className="text-2xl font-bold text-primary">
                  What&apos;s your research field?
                </h2>
                <p className="mt-2 text-slate-500">
                  This helps us find the most relevant papers for you.
                </p>
              </div>
              <input
                type="text"
                value={researchField}
                onChange={(e) => setResearchField(e.target.value)}
                placeholder="e.g., Machine Learning, Molecular Biology, Quantum Physics"
                className="input-field text-lg py-4"
                autoFocus
              />
              <div className="flex flex-wrap gap-2">
                {[
                  "Machine Learning",
                  "Neuroscience",
                  "Quantum Computing",
                  "Bioinformatics",
                  "NLP",
                  "Computer Vision",
                ].map((field) => (
                  <button
                    key={field}
                    onClick={() => setResearchField(field)}
                    className={clsx(
                      "chip cursor-pointer",
                      researchField === field ? "chip-active" : "chip-inactive"
                    )}
                  >
                    {field}
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Step 2: Topics & Keywords */}
          {step === 1 && (
            <div className="space-y-6">
              <div>
                <h2 className="text-2xl font-bold text-primary">
                  Topics &amp; Keywords
                </h2>
                <p className="mt-2 text-slate-500">
                  Add specific topics and keywords you&apos;re researching.
                </p>
              </div>

              <div>
                <label className="section-label mb-2 block">Topics</label>
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={topicInput}
                    onChange={(e) => setTopicInput(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") {
                        e.preventDefault();
                        addItem(topicInput, setTopicInput, topics, setTopics);
                      }
                    }}
                    placeholder="Add a topic and press Enter"
                    className="input-field flex-1"
                  />
                  <button
                    onClick={() =>
                      addItem(topicInput, setTopicInput, topics, setTopics)
                    }
                    className="btn-primary px-3"
                  >
                    <Plus className="h-4 w-4" />
                  </button>
                </div>
                <div className="mt-2 flex flex-wrap gap-2">
                  {topics.map((t, i) => (
                    <span key={i} className="chip chip-active flex items-center gap-1">
                      {t}
                      <button
                        onClick={() => removeItem(i, topics, setTopics)}
                      >
                        <X className="h-3 w-3" />
                      </button>
                    </span>
                  ))}
                </div>
              </div>

              <div>
                <label className="section-label mb-2 block">Keywords</label>
                <div className="flex gap-2">
                  <input
                    type="text"
                    value={keywordInput}
                    onChange={(e) => setKeywordInput(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") {
                        e.preventDefault();
                        addItem(
                          keywordInput,
                          setKeywordInput,
                          keywords,
                          setKeywords
                        );
                      }
                    }}
                    placeholder="Add a keyword and press Enter"
                    className="input-field flex-1"
                  />
                  <button
                    onClick={() =>
                      addItem(
                        keywordInput,
                        setKeywordInput,
                        keywords,
                        setKeywords
                      )
                    }
                    className="btn-primary px-3"
                  >
                    <Plus className="h-4 w-4" />
                  </button>
                </div>
                <div className="mt-2 flex flex-wrap gap-2">
                  {keywords.map((k, i) => (
                    <span key={i} className="chip chip-active flex items-center gap-1">
                      {k}
                      <button
                        onClick={() => removeItem(i, keywords, setKeywords)}
                      >
                        <X className="h-3 w-3" />
                      </button>
                    </span>
                  ))}
                </div>
              </div>
            </div>
          )}

          {/* Step 3: Authors */}
          {step === 2 && (
            <div className="space-y-6">
              <div>
                <h2 className="text-2xl font-bold text-primary">
                  Follow researchers
                </h2>
                <p className="mt-2 text-slate-500">
                  Get alerts when they publish new work. You can skip this step.
                </p>
              </div>

              <div className="flex gap-2">
                <input
                  type="text"
                  value={authorInput}
                  onChange={(e) => setAuthorInput(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter") {
                      e.preventDefault();
                      addItem(authorInput, setAuthorInput, authors, setAuthors);
                    }
                  }}
                  placeholder="e.g., Yoshua Bengio"
                  className="input-field flex-1"
                  autoFocus
                />
                <button
                  onClick={() =>
                    addItem(authorInput, setAuthorInput, authors, setAuthors)
                  }
                  className="btn-primary px-3"
                >
                  <Plus className="h-4 w-4" />
                </button>
              </div>

              <div className="flex flex-wrap gap-2">
                {authors.map((a, i) => (
                  <span key={i} className="chip chip-active flex items-center gap-1">
                    {a}
                    <button onClick={() => removeItem(i, authors, setAuthors)}>
                      <X className="h-3 w-3" />
                    </button>
                  </span>
                ))}
              </div>

              {authors.length === 0 && (
                <div className="rounded-xl bg-slate-50 p-6 text-center">
                  <Users className="mx-auto h-8 w-8 text-slate-300 mb-2" />
                  <p className="text-sm text-slate-400">
                    No researchers added yet. You can always add them later from
                    Settings.
                  </p>
                </div>
              )}
            </div>
          )}

          {/* Navigation */}
          <div className="flex items-center justify-between pt-4">
            {step > 0 ? (
              <button
                onClick={() => setStep(step - 1)}
                className="btn-ghost flex items-center gap-2"
              >
                <ArrowLeft className="h-4 w-4" />
                Back
              </button>
            ) : (
              <div />
            )}

            {step < 2 ? (
              <button
                onClick={() => setStep(step + 1)}
                disabled={!canProceed()}
                className="btn-primary flex items-center gap-2"
              >
                Continue
                <ArrowRight className="h-4 w-4" />
              </button>
            ) : (
              <button
                onClick={handleFinish}
                disabled={submitting}
                className="btn-primary flex items-center gap-2"
              >
                {submitting ? (
                  <div className="h-5 w-5 animate-spin rounded-full border-2 border-white border-t-transparent" />
                ) : (
                  <>
                    Finish Setup
                    <ArrowRight className="h-4 w-4" />
                  </>
                )}
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
