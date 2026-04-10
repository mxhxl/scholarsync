"use client";

import Link from "next/link";
import { BookOpen, ArrowLeft } from "lucide-react";

export default function PrivacyPolicyPage() {
  return (
    <div className="min-h-screen bg-gradient-to-b from-slate-50 to-white">
      {/* Header */}
      <header className="bg-[#1E3A5F] text-white">
        <div className="max-w-4xl mx-auto px-6 py-10">
          <Link
            href="/login"
            className="inline-flex items-center gap-2 text-white/70 hover:text-white text-sm mb-6 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            Back
          </Link>
          <div className="flex items-center gap-3 mb-4">
            <div className="w-10 h-10 bg-white/10 rounded-xl flex items-center justify-center">
              <BookOpen className="w-5 h-5" />
            </div>
            <span className="text-xl font-bold">ScholarSync</span>
          </div>
          <h1 className="text-3xl font-bold mb-2">Privacy Policy</h1>
          <p className="text-white/70">Last updated: March 2026</p>
        </div>
      </header>

      {/* Content */}
      <main className="max-w-4xl mx-auto px-6 py-12">
        <div className="prose prose-slate max-w-none space-y-10">
          {/* Intro */}
          <p className="text-lg text-slate-600 leading-relaxed">
            Welcome to <strong>ScholarSync</strong>, an AI-powered research
            paper discovery and summarization platform for PhD students and
            researchers. Your privacy is important to us, and we are committed to
            protecting your personal data.
          </p>

          {/* 1. Information We Collect */}
          <section>
            <h2 className="text-2xl font-bold text-[#1E3A5F] mb-4">
              1. Information We Collect
            </h2>

            <h3 className="text-lg font-semibold text-slate-700 mt-6 mb-3">
              1.1 Personal Information
            </h3>
            <ul className="list-disc pl-6 text-slate-600 space-y-1">
              <li>Full name</li>
              <li>Email address</li>
              <li>Password (hashed and encrypted)</li>
              <li>University or institution</li>
              <li>Academic role (PhD Student, Postdoc, Professor, etc.)</li>
            </ul>

            <h3 className="text-lg font-semibold text-slate-700 mt-6 mb-3">
              1.2 Research Profile Data
            </h3>
            <ul className="list-disc pl-6 text-slate-600 space-y-1">
              <li>Research interests and topics</li>
              <li>Preferred paper sources (arXiv, PubMed, Semantic Scholar, etc.)</li>
              <li>Followed authors</li>
              <li>Reading history and saved papers</li>
              <li>Library folders and bookmarks</li>
              <li>Paper highlights and notes</li>
            </ul>

            <h3 className="text-lg font-semibold text-slate-700 mt-6 mb-3">
              1.3 Usage & Device Data
            </h3>
            <ul className="list-disc pl-6 text-slate-600 space-y-1">
              <li>Device information and platform (Android, Web)</li>
              <li>App activity and reading patterns</li>
              <li>Reading streaks and engagement metrics</li>
              <li>Error reports and diagnostics</li>
            </ul>

            <h3 className="text-lg font-semibold text-slate-700 mt-6 mb-3">
              1.4 Notification Data
            </h3>
            <ul className="list-disc pl-6 text-slate-600 space-y-1">
              <li>Device tokens for push notifications</li>
              <li>Notification preferences and alert settings</li>
            </ul>
          </section>

          {/* 2. How We Use Your Information */}
          <section>
            <h2 className="text-2xl font-bold text-[#1E3A5F] mb-4">
              2. How We Use Your Information
            </h2>
            <ul className="list-disc pl-6 text-slate-600 space-y-2">
              <li>To create and manage your account</li>
              <li>To personalize your research paper feed using AI recommendations</li>
              <li>To generate AI-powered paper summaries</li>
              <li>To detect research overlap and suggest related work</li>
              <li>To identify trending topics in your research areas</li>
              <li>To send alerts about new papers matching your interests</li>
              <li>To track reading streaks and engagement for gamification features</li>
              <li>To improve app features and performance</li>
            </ul>
          </section>

          {/* 3. AI Usage */}
          <section>
            <h2 className="text-2xl font-bold text-[#1E3A5F] mb-4">
              3. AI Usage
            </h2>
            <p className="text-slate-600 leading-relaxed">
              ScholarSync uses AI models to provide intelligent research
              assistance, including paper summarization, relevance scoring,
              overlap detection, and trend analysis. Your research profile and
              reading history are used to tailor recommendations. AI processing
              is performed locally on our servers. No personal data is sold or
              shared with third parties for advertising purposes.
            </p>
          </section>

          {/* 4. Data Storage & Security */}
          <section>
            <h2 className="text-2xl font-bold text-[#1E3A5F] mb-4">
              4. Data Storage & Security
            </h2>
            <ul className="list-disc pl-6 text-slate-600 space-y-2">
              <li>Passwords are securely hashed using industry-standard algorithms</li>
              <li>All data is transferred over encrypted connections (HTTPS)</li>
              <li>JWT-based authentication with token refresh</li>
              <li>Restricted access to sensitive data with role-based permissions</li>
              <li>Regular security audits and updates</li>
            </ul>
          </section>

          {/* 5. Data Sharing */}
          <section>
            <h2 className="text-2xl font-bold text-[#1E3A5F] mb-4">
              5. Data Sharing
            </h2>
            <p className="text-slate-600 leading-relaxed">
              We do not sell your personal data. We may share limited data with
              the following types of services:
            </p>
            <ul className="list-disc pl-6 text-slate-600 space-y-2 mt-3">
              <li>
                <strong>Paper sources</strong> — arXiv, PubMed, and Semantic
                Scholar APIs to fetch research papers
              </li>
              <li>
                <strong>Hosting & infrastructure</strong> — cloud providers for
                database and application hosting
              </li>
              <li>
                <strong>Analytics</strong> — anonymized usage data to improve the
                platform
              </li>
            </ul>
          </section>

          {/* 6. User Rights */}
          <section>
            <h2 className="text-2xl font-bold text-[#1E3A5F] mb-4">
              6. User Rights
            </h2>
            <p className="text-slate-600 mb-3">You have the right to:</p>
            <ul className="list-disc pl-6 text-slate-600 space-y-2">
              <li>Access and view your personal data</li>
              <li>Update or correct your profile information</li>
              <li>Export your saved papers and reading history</li>
              <li>Request deletion of your account and associated data</li>
              <li>Opt out of push notifications and email alerts</li>
            </ul>
          </section>

          {/* 7. Children's Privacy */}
          <section>
            <h2 className="text-2xl font-bold text-[#1E3A5F] mb-4">
              7. Children&apos;s Privacy
            </h2>
            <p className="text-slate-600 leading-relaxed">
              ScholarSync is designed for academic researchers and is not
              intended for users under 13 years of age. We do not knowingly
              collect personal information from children.
            </p>
          </section>

          {/* 8. Policy Updates */}
          <section>
            <h2 className="text-2xl font-bold text-[#1E3A5F] mb-4">
              8. Policy Updates
            </h2>
            <p className="text-slate-600 leading-relaxed">
              This privacy policy may be updated from time to time. Any changes
              will be reflected on this page with an updated revision date. We
              encourage you to review this policy periodically.
            </p>
          </section>

          {/* 9. Contact Us */}
          <section>
            <h2 className="text-2xl font-bold text-[#1E3A5F] mb-4">
              9. Contact Us
            </h2>
            <p className="text-slate-600 leading-relaxed">
              If you have any questions or concerns about this privacy policy,
              please contact us:
            </p>
            <ul className="list-disc pl-6 text-slate-600 space-y-2 mt-3">
              <li>
                <strong>Email:</strong>{" "}
                <a
                  href="mailto:support@scholarsync.com"
                  className="text-[#1E3A5F] underline hover:text-[#2a4f7f]"
                >
                  support@scholarsync.com
                </a>
              </li>
              <li>
                <strong>In-App:</strong> Settings &rarr; Help Center &rarr;
                Email Support
              </li>
            </ul>
          </section>
        </div>

        {/* Footer */}
        <div className="mt-16 pt-8 border-t border-slate-200 text-center text-sm text-slate-500">
          &copy; {new Date().getFullYear()} ScholarSync. All rights reserved.
        </div>
      </main>
    </div>
  );
}
