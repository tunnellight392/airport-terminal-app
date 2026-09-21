package com.tunnellight.airport_terminal.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Covers the search predicate and the derived display fields on [Airport]. */
class AirportTest {

    private val lax = Airport(
        code = "LAX",
        name = "Los Angeles International Airport",
        city = "Los Angeles",
        state = "CA",
        terminals = listOf(
            Terminal(
                name = "Terminal 1",
                description = "",
                concourses = emptyList(),
                airlines = emptyList()
            )
        )
    )

    private val undetailed = Airport(
        code = "MFV",
        name = "Accomack County Airport",
        city = "Melfa",
        state = "VA",
        terminals = emptyList()
    )

    @Test
    fun `an empty query matches everything`() {
        assertTrue(lax.matches(""))
    }

    @Test
    fun `matches on an IATA code prefix`() {
        assertTrue(lax.matches("la"))
        assertTrue(lax.matches("lax"))
    }

    @Test
    fun `does not match a code fragment that is not a prefix`() {
        assertFalse(lax.matches("ax"))
    }

    @Test
    fun `matches anywhere within the name`() {
        assertTrue(lax.matches("angeles"))
        assertTrue(lax.matches("international"))
    }

    @Test
    fun `matches anywhere within the city`() {
        assertTrue(undetailed.matches("melf"))
    }

    @Test
    fun `matches a state only on an exact code`() {
        assertTrue(lax.matches("ca"))
        assertFalse(lax.matches("c"))
    }

    @Test
    fun `rejects a query that matches nothing`() {
        assertFalse(lax.matches("zzzz"))
    }

    @Test
    fun `the caller is responsible for lower-casing the query`() {
        // matches() deliberately does no case folding: the query is normalized once per search
        // rather than once per airport. An un-normalized query therefore finds nothing, which is
        // the contract this test exists to pin down.
        assertFalse(lax.matches("LAX"))
    }

    @Test
    fun `isDetailed reflects whether terminals are known`() {
        assertTrue(lax.isDetailed)
        assertFalse(undetailed.isDetailed)
    }

    @Test
    fun `location joins city and state`() {
        assertEquals("Los Angeles, CA", lax.location)
    }

    @Test
    fun `location falls back to the state when the city is blank`() {
        assertEquals("AK", lax.copy(city = "  ", state = "AK").location)
    }
}
