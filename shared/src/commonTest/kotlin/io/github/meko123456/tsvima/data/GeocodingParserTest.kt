package io.github.meko123456.tsvima.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeocodingParserTest {

    @Test
    fun parsesResultsWithLabels() {
        val body = """
            {"results":[
              {"name":"London","latitude":51.5,"longitude":-0.12,"admin1":"England","country":"United Kingdom"},
              {"name":"Tbilisi","latitude":41.7,"longitude":44.8,"country":"Georgia"}
            ]}
        """.trimIndent()
        val places = GeocodingParser.parse(body)
        assertEquals(2, places.size)
        assertEquals("London, England, United Kingdom", places[0].label)
        assertEquals("Tbilisi, Georgia", places[1].label)
        assertEquals(41.7, places[1].latitude, 0.001)
    }

    @Test
    fun noResultsIsEmpty() {
        assertTrue(GeocodingParser.parse("""{}""").isEmpty())
        assertTrue(GeocodingParser.parse("""{"results":[]}""").isEmpty())
    }

    @Test
    fun malformedIsEmpty() {
        assertTrue(GeocodingParser.parse("nonsense").isEmpty())
    }
}
