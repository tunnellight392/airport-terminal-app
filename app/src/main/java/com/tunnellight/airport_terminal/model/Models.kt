package com.tunnellight.airport_terminal.model

/** A single airside concourse / pier within a terminal, used to draw the schematic map. */
data class Concourse(
    val name: String,
    val gates: String
) {
    /** Best-effort count of gates from a range like "A1-A30" or "130-159". */
    val gateCount: Int
        get() = gateLabels().size

    /**
     * Expands the gate range into individual gate labels, e.g. "A1-A4" -> [A1, A2, A3, A4]
     * and "130-132" -> [130, 131, 132]. Falls back to a small placeholder set if it can't
     * parse a numeric range. Capped so a huge range can't blow up the layout.
     */
    fun gateLabels(): List<String> {
        val match = Regex("([A-Za-z]*)(\\d+)\\s*-\\s*([A-Za-z]*)(\\d+)").find(gates)
        if (match != null) {
            val prefix = match.groupValues[1]
            val start = match.groupValues[2].toIntOrNull()
            val end = match.groupValues[4].toIntOrNull()
            if (start != null && end != null && end >= start) {
                val capped = (start..end).take(80)
                return capped.map { "$prefix$it" }
            }
        }
        // Single value like "A12" or unparseable -> a few generic gates.
        val single = Regex("([A-Za-z]*)(\\d+)").find(gates)
        if (single != null) {
            val prefix = single.groupValues[1]
            val n = single.groupValues[2].toIntOrNull() ?: 1
            return (n until n + 6).map { "$prefix$it" }
        }
        return (1..6).map { it.toString() }
    }
}

/** A terminal at an airport, with its concourses (the map) and the airlines that use it. */
data class Terminal(
    val name: String,
    val description: String,
    val concourses: List<Concourse>,
    val airlines: List<String>,
    /** How travellers move between concourses/terminals here, e.g. "Plane Train". Null if walkable. */
    val transit: String? = null
)

/** A US airport identified by its IATA code and full name. */
data class Airport(
    val code: String,
    val name: String,
    val city: String,
    val state: String,
    val terminals: List<Terminal>,
    /** Link to the airport's official terminal-map page, opened from the map card. */
    val mapUrl: String? = null
) {
    /**
     * Lowercased once when the airport is built, not per comparison. Search re-runs on every
     * keystroke across the whole dataset, so folding case inside [matches] meant allocating four
     * throwaway strings per airport per keystroke. These are declared in the class body rather
     * than the constructor so they stay out of equals/hashCode/copy.
     */
    private val codeLower = code.lowercase()
    private val nameLower = name.lowercase()
    private val cityLower = city.lowercase()
    private val stateLower = state.lowercase()

    val location: String get() = if (city.isBlank()) state else "$city, $state"

    /** True when we have the full terminal / map / airline breakdown (curated airports). */
    val isDetailed: Boolean get() = terminals.isNotEmpty()

    /**
     * Returns true if the query matches the IATA code or any part of the name/city.
     *
     * [normalizedQuery] must already be trimmed and lowercased by the caller — hoisted out so
     * it happens once per search rather than once per airport.
     */
    fun matches(normalizedQuery: String): Boolean {
        if (normalizedQuery.isEmpty()) return true
        return codeLower.startsWith(normalizedQuery) ||
            nameLower.contains(normalizedQuery) ||
            cityLower.contains(normalizedQuery) ||
            stateLower == normalizedQuery
    }
}
