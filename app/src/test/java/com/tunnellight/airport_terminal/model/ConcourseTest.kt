package com.tunnellight.airport_terminal.model

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [Concourse.gateLabels] turns a free-text gate range from the dataset into individual gate
 * labels for the map. It is pure, has several fallback branches and a safety cap, and drives what
 * the interactive map draws — so the edge cases are worth pinning down.
 */
class ConcourseTest {

    @Test
    fun `expands a range with a letter prefix`() {
        assertEquals(
            listOf("A1", "A2", "A3", "A4"),
            Concourse(name = "Concourse A", gates = "A1-A4").gateLabels()
        )
    }

    @Test
    fun `expands a purely numeric range`() {
        assertEquals(
            listOf("130", "131", "132"),
            Concourse(name = "North", gates = "130-132").gateLabels()
        )
    }

    @Test
    fun `tolerates whitespace around the dash`() {
        assertEquals(
            listOf("B1", "B2", "B3"),
            Concourse(name = "B", gates = "B1 - B3").gateLabels()
        )
    }

    @Test
    fun `caps a huge range so it cannot blow up the layout`() {
        val labels = Concourse(name = "C", gates = "C1-C500").gateLabels()
        assertEquals(80, labels.size)
        assertEquals("C1", labels.first())
        assertEquals("C80", labels.last())
    }

    @Test
    fun `a single gate expands to a small placeholder run`() {
        assertEquals(
            listOf("A12", "A13", "A14", "A15", "A16", "A17"),
            Concourse(name = "A", gates = "A12").gateLabels()
        )
    }

    @Test
    fun `a reversed range falls back to the single-gate branch`() {
        // end < start, so the range is rejected and the first number seen is used instead.
        assertEquals(
            listOf("A4", "A5", "A6", "A7", "A8", "A9"),
            Concourse(name = "A", gates = "A4-A1").gateLabels()
        )
    }

    @Test
    fun `text with no digits falls back to generic labels`() {
        assertEquals(
            listOf("1", "2", "3", "4", "5", "6"),
            Concourse(name = "Main", gates = "Main pier").gateLabels()
        )
    }

    @Test
    fun `gateCount agrees with the labels it counts`() {
        val concourse = Concourse(name = "A", gates = "A1-A4")
        assertEquals(concourse.gateLabels().size, concourse.gateCount)
        assertEquals(4, concourse.gateCount)
    }
}
