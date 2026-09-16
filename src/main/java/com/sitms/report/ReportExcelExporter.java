package com.sitms.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Renders the report payloads (SITMS-044) as XLSX workbooks (SITMS-049), mirroring the layout and
 * semantics of {@link ReportPdfExporter}.
 */
@Component
public class ReportExcelExporter {

  private static final Map<String, String> DIMENSION_LABELS =
      Map.of(
          "technicalSkills", "Technical Skills",
          "communication", "Communication",
          "teamwork", "Teamwork",
          "problemSolving", "Problem Solving",
          "professionalism", "Professionalism");

  private static final DateTimeFormatter GENERATED_AT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  public byte[] export(InternshipReport report) throws IOException {
    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = sheet(workbook, "Internship Report");
      header(sheet, "Internship Report", report.generatedAt());

      int row = 3;
      row = metric(sheet, row, "Total internships", String.valueOf(report.totalInternships()));
      row = metric(sheet, row, "Placement rate", percent(report.placementRate()));
      row = metric(sheet, row, "Avg. duration", days(report.avgDurationDays()));
      row =
          metric(
              sheet,
              row,
              "Pending approval",
              String.valueOf(report.statusCounts().getOrDefault("PENDING", 0L)));

      row = tableHeader(workbook, sheet, row + 1, "Status", "Count");
      for (Map.Entry<String, Long> entry : sortedCounts(report.statusCounts())) {
        row = bodyRow(sheet, row, 2, entry.getKey(), String.valueOf(entry.getValue()));
      }

      if (!report.byProgram().isEmpty()) {
        row = tableHeader(workbook, sheet, row + 1, "Program", "Internships", "Placement rate");
        for (InternshipReport.ProgramStat program :
            report.byProgram().stream()
                .sorted(Comparator.comparing(InternshipReport.ProgramStat::program))
                .toList()) {
          row =
              bodyRow(
                  sheet,
                  row,
                  3,
                  program.program(),
                  String.valueOf(program.internships()),
                  percent(program.placementRate()));
        }
      }
      workbook.write(out);
      return out.toByteArray();
    }
  }

  public byte[] export(ActivityReport report) throws IOException {
    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = sheet(workbook, "Activity Report");
      header(sheet, "Activity Report", report.generatedAt());

      int row = 3;
      row = metric(sheet, row, "Total activities", String.valueOf(report.totalActivities()));
      row = metric(sheet, row, "Approval rate", percent(report.approvalRate()));
      row = metric(sheet, row, "Pending reviews", String.valueOf(report.pendingReviews()));
      row = metric(sheet, row, "Logged hours", hours(report.totalHours()));

      row = tableHeader(workbook, sheet, row + 1, "Status", "Count");
      for (Map.Entry<String, Long> entry : sortedCounts(report.statusCounts())) {
        row = bodyRow(sheet, row, 2, entry.getKey(), String.valueOf(entry.getValue()));
      }
      workbook.write(out);
      return out.toByteArray();
    }
  }

  public byte[] export(EvaluationReport report) throws IOException {
    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = sheet(workbook, "Evaluation Report");
      header(sheet, "Evaluation Report", report.generatedAt());

      int row = 3;
      row = metric(sheet, row, "Total evaluations", String.valueOf(report.totalEvaluations()));
      row = metric(sheet, row, "Submitted", String.valueOf(report.submittedEvaluations()));
      row = metric(sheet, row, "Submission rate", percent(report.submissionRate()));
      row =
          metric(
              sheet,
              row,
              "Avg. overall score",
              report.averageOverallScore() == null ? "—" : report.averageOverallScore() + "/100");

      row = tableHeader(workbook, sheet, row + 1, "Dimension", "Average");
      for (Map.Entry<String, Double> entry :
          report.dimensionAverages().entrySet().stream()
              .sorted(
                  Comparator.comparing(
                      e -> DIMENSION_LABELS.getOrDefault(e.getKey(), e.getKey())))
              .toList()) {
        row =
            bodyRow(
                sheet,
                row,
                2,
                DIMENSION_LABELS.getOrDefault(entry.getKey(), entry.getKey()),
                entry.getValue() == null ? "—" : String.valueOf(entry.getValue()));
      }

      row = tableHeader(workbook, sheet, row + 1, "Score range", "Count");
      for (Map.Entry<String, Long> entry : sortedCounts(report.scoreDistribution())) {
        row = bodyRow(sheet, row, 2, entry.getKey(), String.valueOf(entry.getValue()));
      }
      workbook.write(out);
      return out.toByteArray();
    }
  }

  private Sheet sheet(Workbook workbook, String name) {
    Sheet sheet = workbook.createSheet(name);
    sheet.setColumnWidth(0, 28);
    sheet.setColumnWidth(1, 20);
    sheet.setColumnWidth(2, 20);
    sheet.setColumnWidth(3, 16);
    return sheet;
  }

  private void header(Sheet sheet, String title, OffsetDateTime generatedAt) {
    CellStyle titleStyle = style(sheet.getWorkbook().createCellStyle(), null);
    Font titleFont = sheet.getWorkbook().createFont();
    titleFont.setBold(true);
    titleFont.setFontHeightInPoints((short) 14);
    titleStyle.setFont(titleFont);

    CellStyle subtitleStyle = style(sheet.getWorkbook().createCellStyle(), null);
    Font subtitleFont = sheet.getWorkbook().createFont();
    subtitleFont.setColor(IndexedColors.GREY_40_PERCENT.getIndex());
    subtitleFont.setFontHeightInPoints((short) 10);
    subtitleStyle.setFont(subtitleFont);

    Row titleRow = sheet.createRow(0);
    Cell titleCell = titleRow.createCell(0);
    titleCell.setCellValue(title);
    titleCell.setCellStyle(titleStyle);

    Row subtitleRow = sheet.createRow(1);
    Cell subtitleCell = subtitleRow.createCell(0);
    subtitleCell.setCellValue("Generated " + GENERATED_AT.format(generatedAt));
    subtitleCell.setCellStyle(subtitleStyle);
  }

  private int metric(Sheet sheet, int rowIndex, String label, String value) {
    Row row = sheet.createRow(rowIndex);
    Cell labelCell = row.createCell(0);
    labelCell.setCellValue(label);
    labelCell.setCellStyle(style(sheet.getWorkbook().createCellStyle(), null));
    Cell valueCell = row.createCell(1);
    valueCell.setCellValue(value);
    return rowIndex + 1;
  }

  private int tableHeader(Workbook workbook, Sheet sheet, int rowIndex, String... headers) {
    Row row = sheet.createRow(rowIndex);
    CellStyle headerStyle = style(workbook.createCellStyle(), null);
    headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
    headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    Font headerFont = workbook.createFont();
    headerFont.setBold(true);
    headerStyle.setFont(headerFont);
    for (int i = 0; i < headers.length; i++) {
      Cell cell = row.createCell(i);
      cell.setCellValue(headers[i]);
      cell.setCellStyle(headerStyle);
    }
    return rowIndex + 1;
  }

  private int bodyRow(Sheet sheet, int rowIndex, int columns, String... values) {
    Row row = sheet.createRow(rowIndex);
    CellStyle bodyStyle = style(sheet.getWorkbook().createCellStyle(), null);
    for (int i = 0; i < columns && i < values.length; i++) {
      Cell cell = row.createCell(i);
      cell.setCellValue(values[i]);
      cell.setCellStyle(bodyStyle);
    }
    return rowIndex + 1;
  }

  private CellStyle style(CellStyle style, Integer fillColor) {
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderTop(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);
    return style;
  }

  private List<Map.Entry<String, Long>> sortedCounts(Map<String, Long> counts) {
    return counts.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList();
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
}