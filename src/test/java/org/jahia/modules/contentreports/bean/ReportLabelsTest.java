package org.jahia.modules.contentreports.bean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The labels reports 6 and 24 write into their results: one file per UI language, every key in
 * every file, ASCII only, and no fallback to the server's default locale.
 */
class ReportLabelsTest {

    private static final List<String> KEYS = Arrays.asList(
            "cgnt_contentReports.status.workInProgress",
            "cgnt_contentReports.status.modifiedNotPublished",
            "cgnt_contentReports.status.neverPublished",
            "cgnt_contentReports.status.unpublished",
            "cgnt_contentReports.status.published",
            "cgnt_contentReports.status.notPublished");

    private final Locale serverLocale = Locale.getDefault();

    @AfterEach
    void restoreServerLocale() {
        Locale.setDefault(serverLocale);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "_fr", "_de", "_es", "_it", "_pt"})
    void everyLanguageFileHoldsEveryKeyInAscii(String suffix) throws IOException {
        String path = "/resources/contentReportReact" + suffix + ".properties";
        try (InputStream in = ReportLabelsTest.class.getResourceAsStream(path)) {
            assertNotNull(in, path + " is missing");
            byte[] bytes = in.readAllBytes();
            for (byte b : bytes) {
                assertTrue(b >= 0, path + " holds a non-ASCII byte: write it as a \\uXXXX escape");
            }
            Properties labels = new Properties();
            labels.load(new java.io.ByteArrayInputStream(bytes));
            for (String key : KEYS) {
                assertFalse(labels.getProperty(key, "").isEmpty(), path + " misses " + key);
            }
        }
    }

    @Test
    void labelIsReadInTheReportLanguage() {
        assertEquals("Publié", BaseReport.label("cgnt_contentReports.status.published", Locale.FRENCH));
        assertEquals("Published", BaseReport.label("cgnt_contentReports.status.published", Locale.ENGLISH));
    }

    @Test
    void languageWithoutFileGetsEnglishNotTheServerLocale() {
        Locale.setDefault(Locale.FRENCH);
        assertEquals("Published", BaseReport.label("cgnt_contentReports.status.published", Locale.JAPANESE));
    }

    @Test
    void missingKeyAnswersTheKey() {
        assertEquals("no.such.key", BaseReport.label("no.such.key", Locale.ENGLISH));
    }
}
