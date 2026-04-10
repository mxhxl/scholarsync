"use client";

import { useState, FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import Image from "next/image";
import { Mail, Lock, ArrowRight } from "lucide-react";
import { useAuth } from "@/hooks/useAuth";

export default function LoginPage() {
  const { login } = useAuth();
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");
    setSubmitting(true);
    try {
      await login(email, password);
      router.push("/feed");
    } catch (err: any) {
      setError(err?.message || "Login failed");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="flex min-h-screen">
      {/* Left — Brand panel */}
      <div className="hidden lg:flex lg:w-1/2 flex-col justify-between bg-primary p-12 text-white">
        <div>
          <div className="flex items-center gap-3">
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-white/10">
              <Image src="/favicon.svg" alt="" width={28} height={28} className="brightness-0 invert" />
            </div>
            <span className="text-2xl font-bold">ScholarSync</span>
          </div>
          <p className="mt-1 text-xs tracking-[0.2em] text-white/60 uppercase">
            AI Research Assistant
          </p>
        </div>

        <div className="space-y-8">
          <h2 className="text-4xl font-bold leading-tight">
            Discover research
            <br />
            that matters to
            <br />
            <span className="text-accent-teal">your work.</span>
          </h2>
          <div className="space-y-4 text-white/70">
            <Feature text="Monitor 4M+ papers daily" />
            <Feature text="5-minute AI summaries for instant insights" />
            <Feature text="Prevent research duplication automatically" />
          </div>
        </div>

        <p className="text-sm text-white/40">
          &copy; {new Date().getFullYear()} ScholarSync. All rights reserved.
        </p>
      </div>

      {/* Right — Login form */}
      <div className="flex flex-1 items-center justify-center p-8">
        <div className="w-full max-w-md space-y-8">
          <div className="lg:hidden flex items-center gap-3 mb-4">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10">
              <Image src="/favicon.svg" alt="ScholarSync" width={24} height={24} />
            </div>
            <span className="text-xl font-bold text-primary">ScholarSync</span>
          </div>

          <div>
            <h1 className="text-3xl font-bold text-primary">Welcome back</h1>
            <p className="mt-2 text-slate-500">
              Sign in to continue your research journey
            </p>
          </div>

          {error && (
            <div className="rounded-xl bg-red-50 p-4 text-sm text-red-600">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700">
                Email
              </label>
              <div className="relative">
                <Mail className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="you@university.edu"
                  required
                  className="input-field pl-11"
                />
              </div>
            </div>

            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700">
                Password
              </label>
              <div className="relative">
                <Lock className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  required
                  className="input-field pl-11"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={submitting}
              className="btn-primary flex w-full items-center justify-center gap-2"
            >
              {submitting ? (
                <div className="h-5 w-5 animate-spin rounded-full border-2 border-white border-t-transparent" />
              ) : (
                <>
                  Sign In
                  <ArrowRight className="h-4 w-4" />
                </>
              )}
            </button>
          </form>

          <p className="text-center text-sm text-slate-500">
            Don&apos;t have an account?{" "}
            <Link
              href="/register"
              className="font-semibold text-primary hover:underline"
            >
              Create one
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}

function Feature({ text }: { text: string }) {
  return (
    <div className="flex items-center gap-3">
      <div className="h-2 w-2 rounded-full bg-accent-teal" />
      <span className="text-sm">{text}</span>
    </div>
  );
}
