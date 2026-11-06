package org.jahia.modules.contentreports.service;

import org.jahia.modules.contentreports.exception.ContentReportException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The request values the factory passes on to the report queries: dates and sort direction.
 */
class ContentReportFactoryTest {

    @Test
    void isoDateKeepsADateAndEmptiesNothing() throws ContentReportException {
        assertEquals("2026-11-06", ContentReportFactory.isoDate("2026-11-06"));
        assertEquals("2026-11-06", ContentReportFactory.isoDate(" 2026-11-06 "));
        assertEquals("", ContentReportFactory.isoDate(null));
        assertEquals("", ContentReportFactory.isoDate(" "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"06/11/2026", "2026-13-01", "2026-11-06T00:00:00", "yesterday"})
    void isoDateRefusesAnythingElse(String value) {
        assertThrows(ContentReportException.class, () -> ContentReportFactory.isoDate(value));
    }

    @Test
    void reportWithAnInvalidDateIsRefusedBeforeItRuns() {
        assertThrows(ContentReportException.class, () -> ContentReportFactory.build(
                "3", null, Collections.singletonMap("date", "6 November 2026"), null, null));
    }

    @Test
    void sortOrderIsAscOrDesc() {
        assertEquals("DESC", ContentReportFactory.sortOrderOf("desc"));
        assertEquals("DESC", ContentReportFactory.sortOrderOf(" DESC "));
        assertEquals("ASC", ContentReportFactory.sortOrderOf("asc"));
        assertEquals("ASC", ContentReportFactory.sortOrderOf(""));
        assertEquals("ASC", ContentReportFactory.sortOrderOf(null));
        assertEquals("ASC", ContentReportFactory.sortOrderOf("desc, item.[jcr:path]"));
    }
}
