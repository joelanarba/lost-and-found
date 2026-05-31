package com.lfms.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Minimal RFC-4180-style CSV writer. Quotes any cell containing a comma, quote or
 * newline, and escapes embedded quotes by doubling them.
 */
public class CsvExporter {

    private CsvExporter() {
    }

    public static void write(File file, String[] headers, List<String[]> rows) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write(toLine(headers));
            writer.newLine();
            for (String[] row : rows) {
                writer.write(toLine(row));
                writer.newLine();
            }
        }
    }

    private static String toLine(String[] cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(cells[i]));
        }
        return sb.toString();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean mustQuote = value.contains(",") || value.contains("\"")
                || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return mustQuote ? "\"" + escaped + "\"" : escaped;
    }
}
