package com.tunnellight.airport_terminal.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The bulk dataset writes states inconsistently, so [AirportRepository.normalizeState] folds them
 * to two-letter postal codes to match the curated entries. Getting this wrong shows up as an
 * airport labelled "Melfa, Virginia" next to "Austin, TX".
 */
class StateNormalizationTest {

    @Test
    fun `maps a full state name to its postal code`() {
        assertEquals("CA", AirportRepository.normalizeState("California"))
        assertEquals("NY", AirportRepository.normalizeState("New York"))
    }

    @Test
    fun `is case insensitive for full names`() {
        assertEquals("TX", AirportRepository.normalizeState("texas"))
        assertEquals("TX", AirportRepository.normalizeState("TEXAS"))
    }

    @Test
    fun `strips the US- prefix`() {
        assertEquals("CA", AirportRepository.normalizeState("US-CA"))
    }

    @Test
    fun `upper-cases a value that is already an abbreviation`() {
        assertEquals("WA", AirportRepository.normalizeState("wa"))
    }

    @Test
    fun `trims surrounding whitespace`() {
        assertEquals("OR", AirportRepository.normalizeState("  Oregon  "))
    }

    @Test
    fun `handles the District of Columbia spellings`() {
        assertEquals("DC", AirportRepository.normalizeState("District of Columbia"))
        assertEquals("DC", AirportRepository.normalizeState("Washington DC"))
    }

    @Test
    fun `handles territories`() {
        assertEquals("PR", AirportRepository.normalizeState("Puerto Rico"))
        assertEquals("VI", AirportRepository.normalizeState("US Virgin Islands"))
    }

    @Test
    fun `passes an unrecognised value through unchanged`() {
        assertEquals("Atlantis", AirportRepository.normalizeState("Atlantis"))
    }

    @Test
    fun `returns blank for blank input`() {
        assertEquals("", AirportRepository.normalizeState("   "))
    }
}
