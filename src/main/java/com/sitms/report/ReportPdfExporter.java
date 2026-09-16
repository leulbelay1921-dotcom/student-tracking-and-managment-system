package com.sitms.report;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.RGBColor;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Renders the report payloads (SITMS-044) as PDF documents (SITMS-048).
 *
 * <p>Each export composes the same aggregate values the JSON endpoints return, so the PDF is
 * always in lock-step with the REST report. DRAFT evaluations and pending-state placement math are
 * handled upstream in {@link ReportService}.
 */
@Component
public class ReportPdfExporter {

  private static final RGBColor INK = new RGBColor(0x0f, 0x17, 0x2a);
  private static final RGBColor MUTED = new RGBColor(0x64, 0x74, 0x8b);
  private static final RGBColor HEADER_BG = new RGBColor(0x1d, 0x4e, 0xd8);

  private static final Font TITLE = new Font(Font.HELVETICA, 18, Font.BOLD, INK);
  private static final Font SUBTITLE = new Font(Font.HELVETICA, 10, Font.NORMAL, MUTED);
  private static final Font H2 = new Font(Font.HELVETICA, 13, Font.BOLD, INK);
  private static final Font STAT_VALUE = new Font(Font.HELVETICA, 14, Font.BOLD, INK);
  private static final Font STAT_LABEL = new Font(Font.HELVETICA, 9, Font.NORMAL, MUTED);
  private static final Font HEADER_CELL = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
  private static final Font BODY = new Font(Font.HELVETICA, 9, Font.NORMAL, INK);

  private static final Map<String, String> DIMENSION_LABELS =
      Map.of(
          "technicalSkills", "Technical Skills",
          "communication", "Communication",
          "teamwork", "Teamwork",
          "problemSolving", "Problem Solving",
          "professionalism", "Professionalism");

  private static final DateTimeFormatter GENERATED_AT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  public byte[] export(InternshipReport report) throws DocumentException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Document document = new Document(PageSize.A4, 36, 36, 36, 36);
    PdfWriter.getInstance(document, out);
    document.open();

    header(document, "Internship Report", report.generatedAt());
    stats(
        document,
        stat("Total internships", String.valueOf(report.totalInternships())),
        stat("Placement rate", percent(report.placementRate())),
        stat("Avg. duration", days(report.avgDurationDays())),
        stat(
            "Pending approval",
            String.valueOf(report.statusCounts().getOrDefault("PENDING", 0L))));
    section(document, "Status distribution");
    document.add(twoColumnTable(report.statusCounts()));

    if (!report.byProgram().isEmpty()) {
      section(document, "By program");
      PdfPTable table = new PdfPTable(3);
      headerRow(table, "Program", "Internships", "Placement rate");
      report.byProgram().stream()
          .sorted(Comparator.comparing(InternshipReport.ProgramStat::program))
          .forEach(
              program ->
                  bodyRow(
                      table,
                      program.program(),
                      String.valueOf(program.internships()),
                      percent(program.placementRate())));
      document.add(table);
    }
    document.close();
    return out.toByteArray();
  }

  public byte[] export(ActivityReport report) throws DocumentException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Document document = new Document(PageSize.A4, 36, 36, 36, 36);
    PdfWriter.getInstance(document, out);
    document.open();

    header(document, "Activity Report", report.generatedAt());
    stats(
        document,
        stat("Total activities", String.valueOf(report.totalActivities())),
        stat("Approval rate", percent(report.approvalRate())),
        stat("Pending reviews", String.valueOf(report.pendingReviews())),
        stat("Logged hours", hours(report.totalHours())));
    section(document, "Status distribution");
    document.add(twoColumnTable(report.statusCounts()));
    document.close();
    return out.toByteArray();
  }

  public byte[] export(EvaluationReport report) throws DocumentException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Document document = new Document(PageSize.A4, 36, 36, 36, 36);
    PdfWriter.getInstance(document, out);
    document.open();

    header(document, "Evaluation Report", report.generatedAt());
    stats(
        document,
        stat("Total evaluations", String.valueOf(report.totalEvaluations())),
        stat("Submitted", String.valueOf(report.submittedEvaluations())),
        stat("Submission rate", percent(report.submissionRate())),
        stat(
            "Avg. overall score",
            report.averageOverallScore() == null ? "—" : report.averageOverallScore() + "/100"));
    section(document, "Dimension averages (out of 5)");
    PdfPTable dimensions = new PdfPTable(2);
    headerRow(dimensions, "Dimension", "Average");
    report
        .dimensionAverages()
        .entrySet()
        .stream()
        .sorted(Comparator.comparing(e -> DIMENSION_LABELS.getOrDefault(e.getKey(), e.getKey())))
        .forEach(
            entry ->
                bodyRow(
                    dimensions,
                    DIMENSION_LABELS.getOrDefault(entry.getKey(), entry.getKey()),
                    entry.getValue() == null ? "—" : String.valueOf(entry.getValue())));
    document.add(dimensions);

    section(document, "Score distribution");
    document.add(twoColumnTable(report.scoreDistribution()));
    document.close();
    return out.toByteArray();
  }

  private void header(Document document, String title, OffsetDateTime generatedAt)
      throws DocumentException {
    document.add(new Paragraph(title, TITLE));
    document.add(new Paragraph("Generated " + GENERATED_AT.format(generatedAt), SUBTITLE));
    document.add(new Paragraph(" ", SUBTITLE));
  }

  private void section(Document document, String title) throws DocumentException {
    Paragraph heading = new Paragraph(title, H2);
    heading.setSpacingBefore(16);
    heading.setSpacingAfter(8);
    document.add(heading);
  }

  private PdfPTable twoColumnTable(Map<String, Long> counts) throws DocumentException {
    PdfPTable table = new PdfPTable(2);
    headerRow(table, "Status", "Count");
    counts.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(entry -> bodyRow(table, entry.getKey(), String.valueOf(entry.getValue())));
    return table;
  }

  private PdfPTable stats(Document document, Stat... stats) throws DocumentException {
    PdfPTable grid = new PdfPTable(4);
    grid.setWidthPercentage(100);
    grid.setSpacingBefore(12);
    for (Stat stat : stats) {
      PdfPCell cell = new PdfPCell();
      cell.setBorder(PdfPCell.NO_BORDER);
      cell.setPaddingTop(6);
      cell.setPaddingRight(12);
      cell.setPaddingBottom(6);
      cell.addElement(new Paragraph(stat.value(), STAT_VALUE));
      cell.addElement(new Paragraph(stat.label(), STAT_LABEL));
      grid.addCell(cell);
    }
    document.add(grid);
    return grid;
  }

  private void headerRow(PdfPTable table, String... headers) throws DocumentException {
    for (String header : headers) {
      PdfPCell cell = new PdfPCell(new Phrase(header, HEADER_CELL));
      cell.setBackgroundColor(HEADER_BG);
      cell.setPadding(6);
      table.addCell(cell);
    }
  }

  private void bodyRow(PdfPTable table, String... values) throws DocumentException {
    for (String value : values) {
      PdfPCell cell = new PdfPCell(new Phrase(value, BODY));
      cell.setPadding(6);
      table.addCell(cell);
    }
  }

  private Stat stat(String label, String value) {
    return new Stat(label, value);
  }

  private static String percent(double value) {
    return String.format("%.0f%%", value * 100);
  }

  private static String days(Double value) {
    return value == null ? "—" : value + " days";
  }

  private static String hours(Double value) {
    return value == null ? "—" : value + " hrs";
  }

  private record Stat(String label, String value) {}
}