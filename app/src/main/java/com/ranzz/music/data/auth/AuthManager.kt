package com.ranzz.music.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class UserAccount(
    val name: String,
    val email: String,
    val channelHandle: String,
    val avatarUrl: String,
    val cookie: String = "",
    val isCustomAvatar: Boolean = false,
    val isCustomName: Boolean = false
) {
    val isGoogleLinked: Boolean
        get() = cookie.isNotBlank()
}

class AuthManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val httpClient = com.ranzz.music.data.remote.HttpClientProvider.client

    private val _authState = MutableStateFlow<UserAccount?>(loadStoredAccount())
    val authState: StateFlow<UserAccount?> = _authState.asStateFlow()

    private fun loadStoredAccount(): UserAccount? {
        val cookie = prefs.getString(KEY_COOKIE, null)
        val isCustomName = prefs.getBoolean(KEY_IS_CUSTOM_NAME, false)
        val isCustomAvatar = prefs.getBoolean(KEY_IS_CUSTOM_AVATAR, false)
        val customName = prefs.getString(KEY_CUSTOM_NAME, "") ?: ""
        val customAvatar = prefs.getString(KEY_CUSTOM_AVATAR, "") ?: ""

        val rawSavedName = prefs.getString(KEY_NAME, if (cookie != null) "YouTube User" else "Ranzz Music Lover") ?: "Ranzz Music Lover"
        var name = if (isCustomName && customName.isNotBlank()) customName else rawSavedName

        val rawSavedAvatar = prefs.getString(KEY_AVATAR, "") ?: ""
        val avatar = if (isCustomAvatar && customAvatar.isNotBlank()) customAvatar else rawSavedAvatar

        var email = prefs.getString(KEY_EMAIL, "") ?: ""
        var handle = prefs.getString(KEY_HANDLE, "") ?: ""

        // Sanitize if previously saved as raw JSON object string
        if (name.startsWith("{") || name.contains("\"runs\"")) {
            name = parseRawJsonRuns(name).ifBlank { "YouTube User" }
        }
        if (email.startsWith("{") || email.contains("\"runs\"")) {
            email = parseRawJsonRuns(email)
        }
        if (handle.startsWith("{") || handle.contains("\"runs\"")) {
            handle = parseRawJsonRuns(handle)
        }

        // Return account if logged in OR if user has set a custom profile locally
        if (cookie.isNullOrBlank() && !isCustomName && !isCustomAvatar) {
            return null
        }

        return UserAccount(
            name = name,
            email = email,
            channelHandle = handle,
            avatarUrl = avatar,
            cookie = cookie ?: "",
            isCustomAvatar = isCustomAvatar,
            isCustomName = isCustomName
        )
    }

    private fun parseRawJsonRuns(jsonStr: String): String {
        return try {
            val json = JSONObject(jsonStr)
            val runs = json.optJSONArray("runs")
            if (runs != null && runs.length() > 0) {
                runs.optJSONObject(0)?.optString("text", "") ?: ""
            } else {
                json.optString("simpleText", "")
            }
        } catch (_: Exception) {
            jsonStr
        }
    }

    fun isLoggedIn(): Boolean = _authState.value != null

    fun getCookie(): String? = _authState.value?.cookie

    fun parseCookies(cookieStr: String): Map<String, String> {
        return cookieStr.split(";")
            .map { it.trim() }
            .filter { it.contains("=") }
            .associate {
                val idx = it.indexOf("=")
                it.substring(0, idx).trim() to it.substring(idx + 1).trim()
            }
    }

    fun getSapisidHash(origin: String = "https://music.youtube.com"): String? {
        val cookie = getCookie() ?: return null
        val map = parseCookies(cookie)
        val sapisid = map["SAPISID"] ?: map["__Secure-3PAPISID"] ?: return null
        val time = System.currentTimeMillis() / 1000
        val sha1 = sha1Hex("$time $sapisid $origin")
        return "SAPISIDHASH ${time}_$sha1"
    }

    private fun sha1Hex(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun handleLoginSuccess(cookie: String, initialName: String = "", initialAvatar: String = "") {
        val isCustomName = prefs.getBoolean(KEY_IS_CUSTOM_NAME, false)
        val isCustomAvatar = prefs.getBoolean(KEY_IS_CUSTOM_AVATAR, false)
        val customName = prefs.getString(KEY_CUSTOM_NAME, "") ?: ""
        val customAvatar = prefs.getString(KEY_CUSTOM_AVATAR, "") ?: ""

        val googleName = if (initialName.isNotBlank()) initialName else (prefs.getString(KEY_NAME, "YouTube User") ?: "YouTube User")
        val googleAvatar = if (initialAvatar.isNotBlank()) initialAvatar else (prefs.getString(KEY_AVATAR, "") ?: "")

        val editor = prefs.edit()
            .putString(KEY_COOKIE, cookie)
            .putString(KEY_ORIGINAL_GOOGLE_NAME, googleName)
            .putString(KEY_ORIGINAL_GOOGLE_AVATAR, googleAvatar)

        if (initialName.isNotBlank()) editor.putString(KEY_NAME, initialName)
        if (initialAvatar.isNotBlank()) editor.putString(KEY_AVATAR, initialAvatar)
        editor.apply()

        val resolvedName = if (isCustomName && customName.isNotBlank()) customName else googleName
        val resolvedAvatar = if (isCustomAvatar && customAvatar.isNotBlank()) customAvatar else googleAvatar

        val initialAccount = UserAccount(
            name = resolvedName,
            email = prefs.getString(KEY_EMAIL, "") ?: "",
            channelHandle = prefs.getString(KEY_HANDLE, "") ?: "",
            avatarUrl = resolvedAvatar,
            cookie = cookie,
            isCustomAvatar = isCustomAvatar,
            isCustomName = isCustomName
        )
        _authState.value = initialAccount

        // Fetch detailed profile from InnerTube in background
        CoroutineScope(Dispatchers.IO).launch {
            fetchAccountInfo(cookie)
        }
    }

    fun updateProfile(name: String, email: String, avatarUrl: String) {
        val current = _authState.value

        val editor = prefs.edit()
        if (name.isNotBlank()) {
            editor.putString(KEY_NAME, name)
            editor.putString(KEY_ORIGINAL_GOOGLE_NAME, name)
        }
        if (email.isNotBlank()) editor.putString(KEY_EMAIL, email)
        if (avatarUrl.isNotBlank()) {
            editor.putString(KEY_AVATAR, avatarUrl)
            editor.putString(KEY_ORIGINAL_GOOGLE_AVATAR, avatarUrl)
        }
        editor.apply()

        val isCustomName = prefs.getBoolean(KEY_IS_CUSTOM_NAME, false)
        val isCustomAvatar = prefs.getBoolean(KEY_IS_CUSTOM_AVATAR, false)
        val customName = prefs.getString(KEY_CUSTOM_NAME, "") ?: ""
        val customAvatar = prefs.getString(KEY_CUSTOM_AVATAR, "") ?: ""

        val finalName = if (isCustomName && customName.isNotBlank()) customName else (if (name.isNotBlank()) name else (current?.name ?: "YouTube User"))
        val finalAvatar = if (isCustomAvatar && customAvatar.isNotBlank()) customAvatar else (if (avatarUrl.isNotBlank()) avatarUrl else (current?.avatarUrl ?: ""))

        _authState.value = current?.copy(
            name = finalName,
            email = if (email.isNotBlank()) email else current.email,
            avatarUrl = finalAvatar,
            isCustomAvatar = isCustomAvatar,
            isCustomName = isCustomName
        ) ?: UserAccount(
            name = finalName,
            email = email,
            channelHandle = "",
            avatarUrl = finalAvatar,
            cookie = "",
            isCustomAvatar = isCustomAvatar,
            isCustomName = isCustomName
        )
        Log.d("RanzzAuth", "Profile updated: $finalName ($finalAvatar)")
    }

    /**
     * User explicitly updates their display name and/or avatar picture.
     * Works both when logged in with Google and for offline/guest profiles.
     */
    fun updateCustomProfile(newName: String, newAvatarPathOrUrl: String?) {
        val current = _authState.value
        val nameToSet = newName.trim().ifBlank { current?.name ?: "Ranzz Music Lover" }
        val avatarToSet = if (newAvatarPathOrUrl != null) newAvatarPathOrUrl else (current?.avatarUrl ?: "")

        val editor = prefs.edit()
            .putString(KEY_CUSTOM_NAME, nameToSet)
            .putBoolean(KEY_IS_CUSTOM_NAME, true)

        if (newAvatarPathOrUrl != null) {
            editor.putString(KEY_CUSTOM_AVATAR, newAvatarPathOrUrl)
            editor.putBoolean(KEY_IS_CUSTOM_AVATAR, true)
        }
        editor.apply()

        _authState.value = UserAccount(
            name = nameToSet,
            email = current?.email ?: "",
            channelHandle = current?.channelHandle ?: "",
            avatarUrl = avatarToSet,
            cookie = current?.cookie ?: "",
            isCustomAvatar = if (newAvatarPathOrUrl != null) true else (current?.isCustomAvatar ?: false),
            isCustomName = true
        )
        Log.d("RanzzAuth", "Custom profile set: name=$nameToSet, avatar=$avatarToSet")
    }

    /**
     * Copies an image from a content URI (e.g. from gallery Photo Picker) to app's internal files
     * so it permanently persists and never suffers from permission expiration.
     */
    fun saveAvatarFromUri(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val profileDir = File(context.filesDir, "profiles").apply { mkdirs() }
            
            // Clean up old avatar files to prevent storage leak
            profileDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("custom_avatar_")) {
                    file.delete()
                }
            }

            val avatarFile = File(profileDir, "custom_avatar_${System.currentTimeMillis()}.jpg")
            FileOutputStream(avatarFile).use { out ->
                inputStream.copyTo(out)
            }
            avatarFile.absolutePath
        } catch (e: Exception) {
            Log.e("RanzzAuth", "Error saving avatar from Uri: ${e.message}", e)
            null
        }
    }

    /**
     * Resets custom name & avatar back to the original Google account profile (if logged in),
     * or back to default guest profile.
     */
    fun resetToOriginalProfile() {
        val current = _authState.value
        val originalName = prefs.getString(KEY_ORIGINAL_GOOGLE_NAME, prefs.getString(KEY_NAME, "YouTube User")) ?: "YouTube User"
        val originalAvatar = prefs.getString(KEY_ORIGINAL_GOOGLE_AVATAR, prefs.getString(KEY_AVATAR, "")) ?: ""

        prefs.edit()
            .putBoolean(KEY_IS_CUSTOM_NAME, false)
            .putBoolean(KEY_IS_CUSTOM_AVATAR, false)
            .remove(KEY_CUSTOM_NAME)
            .remove(KEY_CUSTOM_AVATAR)
            .apply()

        if (current != null && current.cookie.isNotBlank()) {
            _authState.value = current.copy(
                name = originalName,
                avatarUrl = originalAvatar,
                isCustomAvatar = false,
                isCustomName = false
            )
        } else {
            _authState.value = null
        }
    }

    suspend fun fetchAccountInfo(cookie: String) {
        // Try YouTube Music first, then YouTube Web fallback
        val success = tryFetchAccountMenu(cookie, isMusic = true)
        if (!success) {
            tryFetchAccountMenu(cookie, isMusic = false)
        }
    }

    private fun tryFetchAccountMenu(cookie: String, isMusic: Boolean): Boolean {
        try {
            val origin = if (isMusic) "https://music.youtube.com" else "https://www.youtube.com"
            val clientName = if (isMusic) "WEB_REMIX" else "WEB"
            val clientNum = if (isMusic) "67" else "1"
            val clientVer = if (isMusic) "1.20240101.01.00" else "2.20240101.01.00"
            val sapisidHash = getSapisidHash(origin)

            val jsonBody = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", clientName)
                        put("clientVersion", clientVer)
                        put("hl", "en")
                        put("gl", "US")
                    })
                })
            }

            val reqBuilder = Request.Builder()
                .url("$origin/youtubei/v1/account/account_menu")
                .header("Content-Type", "application/json")
                .header("Cookie", cookie)
                .header("Origin", origin)
                .header("Referer", "$origin/")
                .header("X-Origin", origin)
                .header("X-YouTube-Client-Name", clientNum)
                .header("X-YouTube-Client-Version", clientVer)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))

            if (sapisidHash != null) {
                reqBuilder.header("Authorization", sapisidHash)
            }

            val resp = httpClient.newCall(reqBuilder.build()).execute()
            val respString = resp.body?.string().orEmpty()
            if (resp.isSuccessful && respString.isNotBlank()) {
                val json = JSONObject(respString)
                val actions = json.optJSONArray("actions")
                val openPopup = actions?.optJSONObject(0)?.optJSONObject("openPopupAction")
                val multiPage = openPopup?.optJSONObject("popup")?.optJSONObject("multiPageMenuRenderer")
                val activeAccount = multiPage?.optJSONObject("header")?.optJSONObject("activeAccountHeaderRenderer")

                if (activeAccount != null) {
                    val nameObj = activeAccount.opt("accountName")
                    val name = when (nameObj) {
                        is JSONObject -> nameObj.optJSONArray("runs")?.optJSONObject(0)?.optString("text", "")
                            ?: nameObj.optString("simpleText", "")
                        is String -> nameObj
                        else -> ""
                    }

                    val emailObj = activeAccount.opt("email")
                    val email = when (emailObj) {
                        is JSONObject -> emailObj.optJSONArray("runs")?.optJSONObject(0)?.optString("text", "")
                            ?: emailObj.optString("simpleText", "")
                        is String -> emailObj
                        else -> ""
                    }

                    val handleObj = activeAccount.opt("channelHandle")
                    val handle = when (handleObj) {
                        is JSONObject -> handleObj.optJSONArray("runs")?.optJSONObject(0)?.optString("text", "")
                            ?: handleObj.optString("simpleText", "")
                        is String -> handleObj
                        else -> ""
                    }

                    var avatarUrl = ""
                    val accountPhoto = activeAccount.optJSONObject("accountPhoto")
                    val thumbs = accountPhoto?.optJSONArray("thumbnails")
                    if (thumbs != null && thumbs.length() > 0) {
                        avatarUrl = thumbs.optJSONObject(thumbs.length() - 1)?.optString("url") ?: ""
                    }

                    if (name.isNotBlank() || avatarUrl.isNotBlank()) {
                        updateProfile(name, email, avatarUrl)
                        return true
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("RanzzAuth", "Account menu fetch error: ${e.message}")
        }
        return false
    }

    fun logout() {
        prefs.edit()
            .remove(KEY_COOKIE)
            .remove(KEY_EMAIL)
            .remove(KEY_HANDLE)
            .remove(KEY_ORIGINAL_GOOGLE_NAME)
            .remove(KEY_ORIGINAL_GOOGLE_AVATAR)
            .apply()

        val isCustomName = prefs.getBoolean(KEY_IS_CUSTOM_NAME, false)
        val isCustomAvatar = prefs.getBoolean(KEY_IS_CUSTOM_AVATAR, false)
        if (isCustomName || isCustomAvatar) {
            val customName = prefs.getString(KEY_CUSTOM_NAME, "Ranzz Music Lover") ?: "Ranzz Music Lover"
            val customAvatar = prefs.getString(KEY_CUSTOM_AVATAR, "") ?: ""
            _authState.value = UserAccount(
                name = customName,
                email = "",
                channelHandle = "",
                avatarUrl = customAvatar,
                cookie = "",
                isCustomAvatar = isCustomAvatar,
                isCustomName = isCustomName
            )
        } else {
            _authState.value = null
        }

        try {
            android.webkit.CookieManager.getInstance().removeAllCookies(null)
            android.webkit.CookieManager.getInstance().flush()
        } catch (e: Exception) {
            Log.e("RanzzAuth", "Failed to clear WebView cookies: ${e.message}")
        }
    }

    companion object {
        private const val PREF_NAME = "ranzz_auth_prefs"
        private const val KEY_COOKIE = "yt_cookie"
        private const val KEY_NAME = "yt_name"
        private const val KEY_EMAIL = "yt_email"
        private const val KEY_HANDLE = "yt_handle"
        private const val KEY_AVATAR = "yt_avatar"

        private const val KEY_CUSTOM_NAME = "custom_profile_name"
        private const val KEY_CUSTOM_AVATAR = "custom_profile_avatar"
        private const val KEY_IS_CUSTOM_NAME = "is_custom_name"
        private const val KEY_IS_CUSTOM_AVATAR = "is_custom_avatar"
        private const val KEY_ORIGINAL_GOOGLE_NAME = "original_google_name"
        private const val KEY_ORIGINAL_GOOGLE_AVATAR = "original_google_avatar"

        @Volatile
        private var instance: AuthManager? = null

        fun init(context: Context): AuthManager {
            return instance ?: synchronized(this) {
                instance ?: AuthManager(context.applicationContext).also { instance = it }
            }
        }

        fun getInstance(): AuthManager {
            return instance ?: throw IllegalStateException("AuthManager must be initialized in Application.onCreate")
        }
    }
}
