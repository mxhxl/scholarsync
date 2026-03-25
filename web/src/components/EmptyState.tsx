"use client";

import { LucideIcon } from "lucide-react";

interface Props {
  icon: LucideIcon;
  title: string;
  description: string;
  action?: { label: string; onClick: () => void };
}

export default function EmptyState({
  icon: Icon,
  title,
  description,
  action,
}: Props) {
  return (
    <div className="flex flex-col items-center justify-center py-20 text-center">
      <div className="flex h-16 w-16 items-center justify-center rounded-2xl bg-primary/5 mb-4">
        <Icon className="h-8 w-8 text-primary/40" />
      </div>
      <h3 className="text-lg font-semibold text-slate-700">{title}</h3>
      <p className="mt-1 max-w-sm text-sm text-slate-400">{description}</p>
      {action && (
        <button onClick={action.onClick} className="btn-primary mt-6 text-sm">
          {action.label}
        </button>
      )}
    </div>
  );
}
