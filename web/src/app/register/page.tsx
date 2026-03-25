"use client";

import { useState, FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import Image from "next/image";
import { Mail, Lock, User, Building2, ArrowRight } from "lucide-react";
import { useAuth } from "@/hooks/useAuth";

export default function RegisterPage() {
  const { register } = useAuth();
  const router = useRouter();
  const [form, setForm] = useState({
    name: "",
    email: "",
    password: "",
    institution: "",
  });
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  function set(key: keyof typeof form) {
    return (e: React.ChangeEvent<HTMLInputElement>) =>
      setForm((f) => ({ ...f, [key]: e.target.value }));
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError("");
    setSubmitting(true);
    try {
      await register({
        name: form.name,
        email: form.email,
        password: form.password,
        institution: form.institution || undefined,
      });
      router.push("/profile-setup");
    } catch (err: any) {
      setError(err.message || "Registration failed");
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
            Start your
            <br />
            AI-powered
            <br />
            <span className="text-accent-gold">research journey.</span>
          </h2>
          <p className="text-white/60 leading-relaxed max-w-md">
            Join thousands of PhD students and researchers who use ScholarSync to
            stay ahead of the latest discoveries in their field.
          </p>
        </div>

        <p className="text-sm text-white/40">
          &copy; {new Date().getFullYear()} ScholarSync
        </p>
      </div>

      {/* Right — Form */}
      <div className="flex flex-1 items-center justify-center p-8">
        <div className="w-full max-w-md space-y-8">
          <div className="lg:hidden flex items-center gap-3 mb-4">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10">
              <Image src="/favicon.svg" alt="ScholarSync" width={24} height={24} />
            </div>
            <span className="text-xl font-bold text-primary">ScholarSync</span>
          </div>

          <div>
            <h1 className="text-3xl font-bold text-primary">
              Create your account
            </h1>
            <p className="mt-2 text-slate-500">
              Set up in under 2 minutes — no credit card needed
            </p>
          </div>

          {error && (
            <div className="rounded-xl bg-red-50 p-4 text-sm text-red-600">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700">
                Full name
              </label>
              <div className="relative">
                <User className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                <input
                  type="text"
                  value={form.name}
                  onChange={set("name")}
                  placeholder="Alex Rivera"
                  required
                  className="input-field pl-11"
                />
              </div>
            </div>

            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700">
                Email
              </label>
              <div className="relative">
                <Mail className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                <input
                  type="email"
                  value={form.email}
                  onChange={set("email")}
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
                  value={form.password}
                  onChange={set("password")}
                  placeholder="Min. 8 characters"
                  required
                  minLength={8}
                  className="input-field pl-11"
                />
              </div>
            </div>

            <div>
              <label className="mb-1.5 block text-sm font-medium text-slate-700">
                Institution{" "}
                <span className="text-slate-400 font-normal">(optional)</span>
              </label>
              <div className="relative">
                <Building2 className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                <input
                  type="text"
                  value={form.institution}
                  onChange={set("institution")}
                  placeholder="MIT, Stanford, etc."
                  className="input-field pl-11"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={submitting}
              className="btn-primary flex w-full items-center justify-center gap-2 mt-2"
            >
              {submitting ? (
                <div className="h-5 w-5 animate-spin rounded-full border-2 border-white border-t-transparent" />
              ) : (
                <>
                  Create Account
                  <ArrowRight className="h-4 w-4" />
                </>
              )}
            </button>
          </form>

          <p className="text-center text-sm text-slate-500">
            Already have an account?{" "}
            <Link
              href="/login"
              className="font-semibold text-primary hover:underline"
            >
              Sign in
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
