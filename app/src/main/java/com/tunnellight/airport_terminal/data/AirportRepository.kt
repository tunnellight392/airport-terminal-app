package com.tunnellight.airport_terminal.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.tunnellight.airport_terminal.model.Airport
import com.tunnellight.airport_terminal.model.Concourse
import com.tunnellight.airport_terminal.model.Terminal
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

/**
 * Provides airport data from two sources, merged:
 *  1. A hand-curated bundled dataset (assets/airports.json) with full terminal / map / airline
 *     detail for major hubs.
 *  2. A large, keyless public airport dataset fetched over HTTP on first launch and cached on
 *     device. These supply broad search coverage (code / name / city) but no terminal detail.
 *
 * Curated entries always win on a code collision, so the rich data is never overwritten.
 */
object AirportRepository {

    private const val REMOTE_URL = "https://raw.githubusercontent.com/mwgg/Airports/master/airports.json"
    private const val CACHE_FILE = "airports_remote.json"
    private const val CACHE_TTL_MS = 30L * 24 * 60 * 60 * 1000 // 30 days

    private var curated: List<Airport>? = null
    private var remote: List<Airport> = emptyList()
    @Volatile private var remoteLoaded = false
    @Volatile private var loadingStarted = false

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val pendingCallbacks = mutableListOf<() -> Unit>()

    // ---- Public API ----

    /** The fully detailed, curated airports only (used as the default/featured list). */
    fun detailedAirports(context: Context): List<Airport> = curated(context)

    /** Every airport currently known (curated + any fetched remote ones). */
    fun airports(context: Context): List<Airport> = merged(context)

    /**
     * Search. An empty query returns just the curated/detailed airports (so the home screen
     * isn't a dump of thousands). A non-empty query searches the full merged set. Detailed
     * airports are ranked first.
     */
    fun search(context: Context, query: String): List<Airport> {
        val q = query.trim()
        val source = if (q.isEmpty()) curated(context) else merged(context)
        return source.asSequence()
            .filter { it.matches(q) }
            .sortedWith(compareByDescending<Airport> { it.isDetailed }.thenBy { it.name })
            .take(200)
            .toList()
    }

    fun findByCode(context: Context, code: String): Airport? =
        merged(context).firstOrNull { it.code.equals(code, ignoreCase = true) }

    /**
     * Kicks off the remote fetch (or cache load) on a background thread. [onLoaded] runs on the
     * main thread once data is available. Safe to call repeatedly; only one load runs at a time.
     */
    fun ensureRemoteLoaded(context: Context, onLoaded: () -> Unit) {
        if (remoteLoaded) {
            onLoaded()
            return
        }
        synchronized(pendingCallbacks) { pendingCallbacks.add(onLoaded) }
        if (loadingStarted) return
        loadingStarted = true

        val appContext = context.applicationContext
        executor.execute {
            val parsed = try {
                loadRemoteJson(appContext)?.let { parseRemote(it) } ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            mainHandler.post {
                remote = parsed
                remoteLoaded = true
                val callbacks = synchronized(pendingCallbacks) {
                    pendingCallbacks.toList().also { pendingCallbacks.clear() }
                }
                callbacks.forEach { it() }
            }
        }
    }

    // ---- Internal ----

    private fun merged(context: Context): List<Airport> {
        val base = curated(context)
        if (!remoteLoaded || remote.isEmpty()) return base
        val knownCodes = base.mapTo(HashSet()) { it.code }
        return base + remote.filter { it.code !in knownCodes }
    }

    private fun curated(context: Context): List<Airport> {
        curated?.let { return it }
        val loaded = parseCurated(readAsset(context, "airports.json")).sortedBy { it.name }
        curated = loaded
        return loaded
    }

    private fun loadRemoteJson(context: Context): String? {
        val cache = File(context.filesDir, CACHE_FILE)
        val fresh = cache.exists() && (System.currentTimeMillis() - cache.lastModified()) < CACHE_TTL_MS
        if (fresh) return cache.readText()

        val fetched = httpGet(REMOTE_URL)
        if (fetched != null) {
            runCatching { cache.writeText(fetched) }
            return fetched
        }
        // Network failed: fall back to a stale cache if we have one.
        return if (cache.exists()) cache.readText() else null
    }

    private fun httpGet(urlString: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 20000
                requestMethod = "GET"
            }
            if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun readAsset(context: Context, fileName: String): String =
        context.assets.open(fileName).bufferedReader().use { it.readText() }

    /** Parse the big keyless dataset: keep US airports that have a 3-letter IATA code. */
    private fun parseRemote(json: String): List<Airport> {
        val root = JSONObject(json)
        val byCode = LinkedHashMap<String, Airport>()
        val keys = root.keys()
        while (keys.hasNext()) {
            val obj = root.getJSONObject(keys.next())
            if (obj.optString("country") != "US") continue
            val iata = obj.optString("iata").trim().uppercase()
            if (iata.length != 3 || !iata.all { it.isLetter() }) continue
            val name = obj.optString("name").ifBlank { "$iata Airport" }
            val city = obj.optString("city")
            val state = normalizeState(obj.optString("state"))
            byCode[iata] = Airport(
                code = iata,
                name = name,
                city = city,
                state = state,
                terminals = emptyList(),
                mapUrl = searchMapUrl(iata, name)
            )
        }
        return byCode.values.toList()
    }

    private fun searchMapUrl(code: String, name: String): String {
        val q = URLEncoder.encode("$code $name terminal map", "UTF-8")
        return "https://www.google.com/search?q=$q"
    }

    /**
     * Normalises a state to its two-letter postal abbreviation so fetched airports match the
     * curated style. Handles full names ("California"), "US-CA" style prefixes, and values that
     * are already abbreviations; falls back to the original string if unrecognised.
     */
    private fun normalizeState(raw: String): String {
        val state = raw.trim().removePrefix("US-").trim()
        if (state.isEmpty()) return state
        if (state.length == 2 && state.all { it.isLetter() }) return state.uppercase()
        return STATE_ABBREVIATIONS[state.lowercase()] ?: state
    }

    private val STATE_ABBREVIATIONS: Map<String, String> = mapOf(
        "alabama" to "AL", "alaska" to "AK", "arizona" to "AZ", "arkansas" to "AR",
        "california" to "CA", "colorado" to "CO", "connecticut" to "CT", "delaware" to "DE",
        "district of columbia" to "DC", "washington dc" to "DC", "florida" to "FL",
        "georgia" to "GA", "hawaii" to "HI", "idaho" to "ID", "illinois" to "IL",
        "indiana" to "IN", "iowa" to "IA", "kansas" to "KS", "kentucky" to "KY",
        "louisiana" to "LA", "maine" to "ME", "maryland" to "MD", "massachusetts" to "MA",
        "michigan" to "MI", "minnesota" to "MN", "mississippi" to "MS", "missouri" to "MO",
        "montana" to "MT", "nebraska" to "NE", "nevada" to "NV", "new hampshire" to "NH",
        "new jersey" to "NJ", "new mexico" to "NM", "new york" to "NY", "north carolina" to "NC",
        "north dakota" to "ND", "ohio" to "OH", "oklahoma" to "OK", "oregon" to "OR",
        "pennsylvania" to "PA", "rhode island" to "RI", "south carolina" to "SC",
        "south dakota" to "SD", "tennessee" to "TN", "texas" to "TX", "utah" to "UT",
        "vermont" to "VT", "virginia" to "VA", "washington" to "WA", "west virginia" to "WV",
        "wisconsin" to "WI", "wyoming" to "WY",
        "puerto rico" to "PR", "guam" to "GU", "american samoa" to "AS",
        "u.s. virgin islands" to "VI", "us virgin islands" to "VI", "virgin islands" to "VI",
        "northern mariana islands" to "MP"
    )

    private fun parseCurated(json: String): List<Airport> {
        val root = JSONObject(json)
        val airportsArray = root.getJSONArray("airports")
        return buildList {
            for (i in 0 until airportsArray.length()) {
                val a = airportsArray.getJSONObject(i)
                val terminalsArray = a.getJSONArray("terminals")
                val terminals = buildList {
                    for (t in 0 until terminalsArray.length()) {
                        val term = terminalsArray.getJSONObject(t)
                        val concoursesArray = term.getJSONArray("concourses")
                        val concourses = buildList {
                            for (c in 0 until concoursesArray.length()) {
                                val con = concoursesArray.getJSONObject(c)
                                add(Concourse(con.getString("name"), con.getString("gates")))
                            }
                        }
                        add(
                            Terminal(
                                name = term.getString("name"),
                                description = term.getString("description"),
                                concourses = concourses,
                                airlines = term.getJSONArray("airlines").toStringList(),
                                transit = if (term.has("transit")) term.getString("transit") else null
                            )
                        )
                    }
                }
                add(
                    Airport(
                        code = a.getString("code"),
                        name = a.getString("name"),
                        city = a.getString("city"),
                        state = a.getString("state"),
                        terminals = terminals,
                        mapUrl = if (a.has("mapUrl")) a.getString("mapUrl") else null
                    )
                )
            }
        }
    }

    private fun org.json.JSONArray.toStringList(): List<String> =
        buildList { for (i in 0 until length()) add(getString(i)) }
}
