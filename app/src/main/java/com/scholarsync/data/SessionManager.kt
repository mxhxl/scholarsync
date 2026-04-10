package com.scholarsync.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Holds the current user's display name (signed up or logged in).
 * Persists across app restarts.
 */
class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDisplayName(): String? =
        prefs.getString(KEY_DISPLAY_NAME, null)?.takeIf { it.isNotBlank() }

    fun setDisplayName(name: String) {
        prefs.edit().putString(KEY_DISPLAY_NAME, name.trim()).apply()
    }

    fun clearDisplayName() {
        prefs.edit().remove(KEY_DISPLAY_NAME).apply()
    }

    fun getApiBaseUrl(): String = prefs.getString(KEY_API_BASE_URL, DEFAULT_API_BASE_URL) ?: DEFAULT_API_BASE_URL

    fun setApiBaseUrl(url: String) {
        prefs.edit().putString(KEY_API_BASE_URL, url.trim()).apply()
    }

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)?.takeIf { it.isNotBlank() }

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)?.takeIf { it.isNotBlank() }

    fun setTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken.trim())
            .putString(KEY_REFRESH_TOKEN, refreshToken.trim())
            .apply()
    }

    fun clearTokens() {
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .apply()
    }

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)?.takeIf { it.isNotBlank() }

    fun setUserId(id: String) {
        prefs.edit().putString(KEY_USER_ID, id.trim()).apply()
    }

    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)?.takeIf { it.isNotBlank() }

    fun setUserEmail(email: String) {
        prefs.edit().putString(KEY_USER_EMAIL, email.trim()).apply()
    }

    fun isLoggedIn(): Boolean = getAccessToken() != null

    fun clearSession() {
        prefs.edit()
            .remove(KEY_DISPLAY_NAME)
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_USER_EMAIL)
            .apply()
    }

    // Legacy alias for backward compatibility
    fun getApiKey(): String? = getAccessToken()

    fun setApiKey(key: String?) {
        if (key.isNullOrBlank()) prefs.edit().remove(KEY_ACCESS_TOKEN).apply()
        else prefs.edit().putString(KEY_ACCESS_TOKEN, key.trim()).apply()
    }

    // ── Onboarding temp data (accumulated across setup steps) ─────────────

    fun setOnboardingData(key: String, value: String) {
        prefs.edit().putString("onboarding_$key", value).apply()
    }

    fun getOnboardingData(key: String): String? =
        prefs.getString("onboarding_$key", null)?.takeIf { it.isNotBlank() }

    fun setOnboardingList(key: String, items: List<String>) {
        val arr = JSONArray()
        items.forEach { arr.put(it) }
        prefs.edit().putString("onboarding_$key", arr.toString()).apply()
    }

    fun getOnboardingList(key: String): List<String> {
        val json = prefs.getString("onboarding_$key", null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun clearOnboardingData() {
        val editor = prefs.edit()
        prefs.all.keys.filter { it.startsWith("onboarding_") }.forEach { editor.remove(it) }
        editor.apply()
    }

    /** Login activity entries: (timestampMs, deviceLabel). Newest first. */
    fun getLoginActivityList(): List<Pair<Long, String>> {
        val json = prefs.getString(KEY_LOGIN_ACTIVITY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                obj.getLong("t") to obj.optString("d", "Unknown device")
            }.sortedByDescending { it.first }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Append a login event (e.g. "This device", "Android"). Keeps last MAX_LOGIN_ACTIVITY entries. */
    fun recordLoginActivity(deviceLabel: String = "This device") {
        val list = getLoginActivityList().toMutableList()
        list.add(0, System.currentTimeMillis() to deviceLabel)
        val trimmed = list.take(MAX_LOGIN_ACTIVITY)
        val arr = JSONArray()
        trimmed.forEach { (t, d) ->
            arr.put(JSONObject().put("t", t).put("d", d))
        }
        prefs.edit().putString(KEY_LOGIN_ACTIVITY, arr.toString()).apply()
    }

    // ── Alert check tracking ────────────────────────────────────────────────

    fun getLastAlertCheckTime(): Long = prefs.getLong(KEY_LAST_ALERT_CHECK, 0L)

    fun setLastAlertCheckTime(timeMillis: Long) {
        prefs.edit().putLong(KEY_LAST_ALERT_CHECK, timeMillis).apply()
    }

    // ── Profile picture ──────────────────────────────────────────────────────

    fun getProfilePicturePath(): String? =
        prefs.getString(KEY_PROFILE_PIC, null)?.takeIf { it.isNotBlank() }

    fun setProfilePicturePath(path: String) {
        prefs.edit().putString(KEY_PROFILE_PIC, path.trim()).apply()
    }

    fun clearProfilePicture() {
        prefs.edit().remove(KEY_PROFILE_PIC).apply()
    }

    companion object {
        private const val PREFS_NAME = "scholarsync_session"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_API_BASE_URL = "api_base_url"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_LOGIN_ACTIVITY = "login_activity"
        private const val KEY_PROFILE_PIC = "profile_picture_path"
        private const val KEY_LAST_ALERT_CHECK = "last_alert_check_time"
        private const val MAX_LOGIN_ACTIVITY = 50
        /** Default backend URL. Use 10.0.2.2 for emulator, LAN IP for physical device. */
        const val DEFAULT_API_BASE_URL = "http://180.235.121.253:8035"
    }
}
