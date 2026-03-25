"use client";

import { useEffect, useState, useCallback, useRef } from "react";
import { admin, AdminUser } from "@/lib/api";
import {
  Search,
  ChevronLeft,
  ChevronRight,
  Shield,
  ShieldOff,
  UserCheck,
  UserX,
  Trash2,
  MoreVertical,
  UserPlus,
  X,
  Eye,
  EyeOff,
} from "lucide-react";
import clsx from "clsx";

const PAGE_SIZE = 20;

export default function AdminUsersPage() {
  const [users, setUsers] = useState<AdminUser[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState<string>("");
  const [activeFilter, setActiveFilter] = useState<string>("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionMenu, setActionMenu] = useState<string | null>(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const debounceRef = useRef<ReturnType<typeof setTimeout>>();

  // Debounce search input
  useEffect(() => {
    debounceRef.current = setTimeout(() => setDebouncedSearch(search), 300);
    return () => clearTimeout(debounceRef.current);
  }, [search]);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await admin.users({
        search: debouncedSearch || undefined,
        role: roleFilter || undefined,
        is_active: activeFilter === "" ? undefined : activeFilter === "true",
        limit: PAGE_SIZE,
        offset: page * PAGE_SIZE,
      });
      setUsers(res.items);
      setTotal(res.total);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to load users");
    }
    setLoading(false);
  }, [debouncedSearch, roleFilter, activeFilter, page]);

  useEffect(() => {
    load();
  }, [load]);

  const totalPages = Math.ceil(total / PAGE_SIZE);

  const handleUpdate = async (
    id: string,
    data: { is_active?: boolean; is_verified?: boolean; role?: string }
  ) => {
    try {
      const updated = await admin.updateUser(id, data);
      setUsers((prev) => prev.map((u) => (u.id === id ? updated : u)));
    } catch (e) {
      alert(e instanceof Error ? e.message : "Update failed");
    }
    setActionMenu(null);
  };

  const handleDelete = async (id: string, name: string) => {
    if (!confirm(`Are you sure you want to delete user "${name}"? This cannot be undone.`))
      return;
    try {
      await admin.deleteUser(id);
      setUsers((prev) => prev.filter((u) => u.id !== id));
      setTotal((t) => t - 1);
    } catch (e) {
      alert(e instanceof Error ? e.message : "Delete failed");
    }
    setActionMenu(null);
  };

  const handleUserCreated = (newUser: AdminUser) => {
    setUsers((prev) => [newUser, ...prev]);
    setTotal((t) => t + 1);
    setShowCreateModal(false);
  };

  return (
    <div className="p-8">
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-primary">User Management</h1>
          <p className="text-sm text-slate-400 mt-1">
            {total} total user{total !== 1 ? "s" : ""}
          </p>
        </div>
        <button
          onClick={() => setShowCreateModal(true)}
          className="flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-medium text-white hover:bg-primary-dark transition-colors"
        >
          <UserPlus className="h-4 w-4" />
          Add User
        </button>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap items-center gap-3 mb-6">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
          <input
            type="text"
            placeholder="Search by name or email..."
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
            className="w-full rounded-xl border border-slate-200 bg-white pl-10 pr-4 py-2.5 text-sm text-slate-800 placeholder-slate-400 focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
          />
        </div>
        <select
          value={roleFilter}
          onChange={(e) => {
            setRoleFilter(e.target.value);
            setPage(0);
          }}
          className="rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-800 focus:border-primary focus:outline-none"
        >
          <option value="">All Roles</option>
          <option value="user">User</option>
          <option value="admin">Admin</option>
        </select>
        <select
          value={activeFilter}
          onChange={(e) => {
            setActiveFilter(e.target.value);
            setPage(0);
          }}
          className="rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-800 focus:border-primary focus:outline-none"
        >
          <option value="">All Status</option>
          <option value="true">Active</option>
          <option value="false">Inactive</option>
        </select>
      </div>

      {/* Table */}
      <div className="rounded-2xl border border-slate-200 bg-white overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-slate-100 bg-slate-50">
                <th className="px-5 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  User
                </th>
                <th className="px-5 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  Role
                </th>
                <th className="px-5 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  Status
                </th>
                <th className="px-5 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  Papers Read
                </th>
                <th className="px-5 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  Saved
                </th>
                <th className="px-5 py-3 text-left text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  Joined
                </th>
                <th className="px-5 py-3 text-right text-xs font-semibold text-slate-500 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={7} className="px-5 py-12 text-center text-slate-400">
                    <div className="flex justify-center">
                      <div className="h-6 w-6 animate-spin rounded-full border-2 border-primary border-t-transparent" />
                    </div>
                  </td>
                </tr>
              ) : error ? (
                <tr>
                  <td colSpan={7} className="px-5 py-12 text-center text-red-400">
                    {error}
                    <button onClick={load} className="ml-2 underline hover:text-red-600">Retry</button>
                  </td>
                </tr>
              ) : users.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-5 py-12 text-center text-slate-400">
                    No users found
                  </td>
                </tr>
              ) : (
                users.map((u) => (
                  <tr
                    key={u.id}
                    className="border-b border-slate-50 hover:bg-slate-50 transition-colors"
                  >
                    <td className="px-5 py-4">
                      <div className="flex items-center gap-3">
                        <div className="flex h-9 w-9 items-center justify-center rounded-full bg-primary/10 text-sm font-bold text-primary">
                          {u.full_name[0]?.toUpperCase() ?? "?"}
                        </div>
                        <div>
                          <p className="font-medium text-slate-800">{u.full_name}</p>
                          <p className="text-xs text-slate-400">{u.email}</p>
                          {u.institution && (
                            <p className="text-xs text-slate-300">{u.institution}</p>
                          )}
                        </div>
                      </div>
                    </td>
                    <td className="px-5 py-4">
                      <span
                        className={clsx(
                          "inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-medium",
                          u.role === "admin"
                            ? "bg-primary/10 text-primary"
                            : "bg-blue-100 text-blue-600"
                        )}
                      >
                        {u.role === "admin" && <Shield className="h-3 w-3" />}
                        {u.role}
                      </span>
                    </td>
                    <td className="px-5 py-4">
                      <div className="flex flex-col gap-1">
                        <span
                          className={clsx(
                            "inline-flex w-fit rounded-full px-2.5 py-1 text-xs font-medium",
                            u.is_active
                              ? "bg-green-100 text-green-600"
                              : "bg-red-100 text-red-600"
                          )}
                        >
                          {u.is_active ? "Active" : "Inactive"}
                        </span>
                        {u.is_verified && (
                          <span className="inline-flex w-fit rounded-full bg-teal-100 px-2.5 py-1 text-xs font-medium text-teal-600">
                            Verified
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="px-5 py-4 text-slate-600">{u.papers_read}</td>
                    <td className="px-5 py-4 text-slate-600">{u.saved_papers}</td>
                    <td className="px-5 py-4 text-slate-400 text-xs">
                      {new Date(u.created_at).toLocaleDateString()}
                    </td>
                    <td className="px-5 py-4 text-right">
                      <div className="relative inline-block">
                        <button
                          onClick={() =>
                            setActionMenu(actionMenu === u.id ? null : u.id)
                          }
                          className="rounded-lg p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-700 transition-colors"
                        >
                          <MoreVertical className="h-4 w-4" />
                        </button>
                        {actionMenu === u.id && (
                          <div className="absolute right-0 top-full z-50 mt-1 w-48 rounded-xl border border-slate-200 bg-white py-1 shadow-xl">
                            {u.is_active ? (
                              <button
                                onClick={() =>
                                  handleUpdate(u.id, { is_active: false })
                                }
                                className="flex w-full items-center gap-2 px-4 py-2 text-sm text-slate-600 hover:bg-slate-50 hover:text-slate-800"
                              >
                                <UserX className="h-4 w-4" />
                                Deactivate
                              </button>
                            ) : (
                              <button
                                onClick={() =>
                                  handleUpdate(u.id, { is_active: true })
                                }
                                className="flex w-full items-center gap-2 px-4 py-2 text-sm text-slate-600 hover:bg-slate-50 hover:text-slate-800"
                              >
                                <UserCheck className="h-4 w-4" />
                                Activate
                              </button>
                            )}
                            {u.role === "user" ? (
                              <button
                                onClick={() =>
                                  handleUpdate(u.id, { role: "admin" })
                                }
                                className="flex w-full items-center gap-2 px-4 py-2 text-sm text-slate-600 hover:bg-slate-50 hover:text-slate-800"
                              >
                                <Shield className="h-4 w-4" />
                                Promote to Admin
                              </button>
                            ) : (
                              <button
                                onClick={() =>
                                  handleUpdate(u.id, { role: "user" })
                                }
                                className="flex w-full items-center gap-2 px-4 py-2 text-sm text-slate-600 hover:bg-slate-50 hover:text-slate-800"
                              >
                                <ShieldOff className="h-4 w-4" />
                                Remove Admin
                              </button>
                            )}
                            <div className="my-1 border-t border-slate-100" />
                            <button
                              onClick={() => handleDelete(u.id, u.full_name)}
                              className="flex w-full items-center gap-2 px-4 py-2 text-sm text-red-500 hover:bg-red-50"
                            >
                              <Trash2 className="h-4 w-4" />
                              Delete User
                            </button>
                          </div>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between mt-4">
          <p className="text-sm text-slate-400">
            Showing {page * PAGE_SIZE + 1}–
            {Math.min((page + 1) * PAGE_SIZE, total)} of {total}
          </p>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="rounded-lg border border-slate-200 p-2 text-slate-500 hover:bg-slate-100 disabled:opacity-30 disabled:cursor-not-allowed"
            >
              <ChevronLeft className="h-4 w-4" />
            </button>
            <span className="text-sm text-slate-500">
              {page + 1} / {totalPages}
            </span>
            <button
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1}
              className="rounded-lg border border-slate-200 p-2 text-slate-500 hover:bg-slate-100 disabled:opacity-30 disabled:cursor-not-allowed"
            >
              <ChevronRight className="h-4 w-4" />
            </button>
          </div>
        </div>
      )}

      {/* Create User Modal */}
      {showCreateModal && (
        <CreateUserModal
          onClose={() => setShowCreateModal(false)}
          onCreated={handleUserCreated}
        />
      )}
    </div>
  );
}

// ── Create User Modal ────────────────────────────────────────────────

function CreateUserModal({
  onClose,
  onCreated,
}: {
  onClose: () => void;
  onCreated: (user: AdminUser) => void;
}) {
  const [form, setForm] = useState({
    full_name: "",
    email: "",
    password: "",
    institution: "",
    role: "user",
  });
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");

    if (!form.full_name.trim() || !form.email.trim() || !form.password.trim()) {
      setError("Name, email, and password are required.");
      return;
    }
    if (form.password.length < 6) {
      setError("Password must be at least 6 characters.");
      return;
    }

    setSubmitting(true);
    try {
      const newUser = await admin.createUser({
        email: form.email.trim(),
        password: form.password,
        full_name: form.full_name.trim(),
        institution: form.institution.trim() || undefined,
        role: form.role,
      });
      onCreated(newUser);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Failed to create user");
    }
    setSubmitting(false);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/40 backdrop-blur-sm"
        onClick={onClose}
      />

      {/* Modal */}
      <div className="relative w-full max-w-md rounded-2xl border border-slate-200 bg-white p-6 shadow-2xl">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-lg font-bold text-slate-800">Create New User</h2>
          <button
            onClick={onClose}
            className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600 transition-colors"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Full Name */}
          <div>
            <label className="block text-xs font-medium text-slate-500 mb-1.5">
              Full Name *
            </label>
            <input
              type="text"
              value={form.full_name}
              onChange={(e) => setForm((f) => ({ ...f, full_name: e.target.value }))}
              placeholder="John Doe"
              className="w-full rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-800 placeholder-slate-300 focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
            />
          </div>

          {/* Email */}
          <div>
            <label className="block text-xs font-medium text-slate-500 mb-1.5">
              Email *
            </label>
            <input
              type="email"
              value={form.email}
              onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))}
              placeholder="john@example.com"
              className="w-full rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-800 placeholder-slate-300 focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
            />
          </div>

          {/* Password */}
          <div>
            <label className="block text-xs font-medium text-slate-500 mb-1.5">
              Password *
            </label>
            <div className="relative">
              <input
                type={showPassword ? "text" : "password"}
                value={form.password}
                onChange={(e) => setForm((f) => ({ ...f, password: e.target.value }))}
                placeholder="Min 6 characters"
                className="w-full rounded-xl border border-slate-200 bg-white px-4 py-2.5 pr-10 text-sm text-slate-800 placeholder-slate-300 focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
              />
              <button
                type="button"
                onClick={() => setShowPassword((s) => !s)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
              >
                {showPassword ? (
                  <EyeOff className="h-4 w-4" />
                ) : (
                  <Eye className="h-4 w-4" />
                )}
              </button>
            </div>
          </div>

          {/* Institution */}
          <div>
            <label className="block text-xs font-medium text-slate-500 mb-1.5">
              Institution
            </label>
            <input
              type="text"
              value={form.institution}
              onChange={(e) =>
                setForm((f) => ({ ...f, institution: e.target.value }))
              }
              placeholder="University / Organization (optional)"
              className="w-full rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-800 placeholder-slate-300 focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
            />
          </div>

          {/* Role */}
          <div>
            <label className="block text-xs font-medium text-slate-500 mb-1.5">
              Role
            </label>
            <div className="flex gap-3">
              <button
                type="button"
                onClick={() => setForm((f) => ({ ...f, role: "user" }))}
                className={clsx(
                  "flex-1 rounded-xl border px-4 py-2.5 text-sm font-medium transition-colors",
                  form.role === "user"
                    ? "border-primary bg-primary/10 text-primary"
                    : "border-slate-200 bg-white text-slate-400 hover:bg-slate-50"
                )}
              >
                User
              </button>
              <button
                type="button"
                onClick={() => setForm((f) => ({ ...f, role: "admin" }))}
                className={clsx(
                  "flex-1 flex items-center justify-center gap-2 rounded-xl border px-4 py-2.5 text-sm font-medium transition-colors",
                  form.role === "admin"
                    ? "border-primary bg-primary/10 text-primary"
                    : "border-slate-200 bg-white text-slate-400 hover:bg-slate-50"
                )}
              >
                <Shield className="h-4 w-4" />
                Admin
              </button>
            </div>
          </div>

          {/* Error */}
          {error && (
            <p className="rounded-lg bg-red-50 border border-red-200 px-4 py-2.5 text-sm text-red-600">
              {error}
            </p>
          )}

          {/* Actions */}
          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 rounded-xl border border-slate-200 px-4 py-2.5 text-sm font-medium text-slate-500 hover:bg-slate-50 transition-colors"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="flex-1 rounded-xl bg-primary px-4 py-2.5 text-sm font-medium text-white hover:bg-primary-dark disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {submitting ? "Creating..." : "Create User"}
            </button>
          </div>
        </form>

        {/* Info note */}
        <p className="mt-4 text-xs text-slate-400 text-center">
          Users created here can log in on both the web app and mobile app.
        </p>
      </div>
    </div>
  );
}
