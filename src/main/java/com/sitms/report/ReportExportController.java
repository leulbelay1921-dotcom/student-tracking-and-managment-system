package com.sitms.report;

import com.lowagie.text.DocumentException;
import com.sitms.common.exception.BusinessException;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Report export endpoints (SITMS-048: PDF, SITMS-049: XLSX).
 *
 * <p>Same RBAC and scoping as {@link ReportController}: exports are rendered from the exact
 * aggregate payloads the JSON endpoints return. Supported formats are {@code pdf} and {@code xlsx}.
 */
@RestController
@RequestMapping("/reports")
public class ReportExportController {

  public static final String UNSUPPORTED_FORMAT = "UNSUPPORTED_FORMAT";
  public static final String UNKNOWN_REPORT_TYPE = "UNKNOWN_REPORT_TYPE";

  private static final String XLSX_MEDIA_TYPE =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private final ReportService reportService;
  private final ReportPdfExporter pdfExporter;
  private final ReportExcelExporter excelExporter;

  public ReportExportController(
      ReportService reportService, ReportPdfExporter pdfExporter, ReportExcelExporter excelExporter) {
    this.reportService = reportService;
    this.pdfExporter = pdfExporter;
    this.excelExporter = excelExporter;
  }

  @GetMapping("/{kind}/export")
  @PreAuthorize(ReportController.REPORTS_ACCESS)
  public ResponseEntity<byte[]> export(
      @PathVariable String kind,
      @RequestParam(required = false, defaultValue = "pdf") String format,
      @RequestParam(required = false) UUID studentId,
      Authentication authentication) throws Exception {
    if (!"pdf".equals(format) && !"xlsx".equals(format)) {
      throw new BusinessException(
          UNSUPPORTED_FORMAT, "Only format='pdf' or 'xlsx' is supported, got: " + format);
    }
    boolean xlsx = "xlsx".equals(format);
    Principal principal = principal(authentication);
    byte[] content =
        switch (kind) {
          case "internships" -> {
            InternshipReport report =
                reportService.internshipReport(
                    principal.role(), principal.appUserId(), studentId);
            yield xlsx ? excelExporter.export(report) : pdfExporter.export(report);
          }
          case "activities" -> {
            ActivityReport report =
                reportService.activityReport(
                    principal.role(), principal.appUserId(), studentId);
            yield xlsx ? excelExporter.export(report) : pdfExporter.export(report);
          }
          case "evaluations" -> {
            EvaluationReport report =
                reportService.evaluationReport(
                    principal.role(), principal.appUserId(), studentId);
            yield xlsx ? excelExporter.export(report) : pdfExporter.export(report);
          }
          default ->
              throw new BusinessException(UNKNOWN_REPORT_TYPE, "Unsupported report type: " + kind);
        };
    String filename =
        "sitms-" + kind + "-report-" + LocalDate.now() + "." + (xlsx ? "xlsx" : "pdf");
    String mediaType = xlsx ? XLSX_MEDIA_TYPE : MediaType.APPLICATION_PDF_VALUE;
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_TYPE, mediaType)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .body(content);
  }

  private static Principal principal(Authentication authentication) {
    String role =
        authentication.getAuthorities().stream()
            .map(Object::toString)
            .map(a -> a.replace("ROLE_", ""))
            .filter(a -> a.equals("ADMIN") || a.equals("SUPERVISOR") || a.equals("FACULTY") || a.equals("STUDENT"))
            .findFirst()
            .orElseThrow(
                () ->
                    new BusinessException(
                        ReportService.SUPERVISOR_PROFILE_NOT_FOUND,
                        "Unrecognized principal role."));
    UUID appUserId = "ADMIN".equals(role) ? null : UUID.fromString(authentication.getName());
    return new Principal(role, appUserId);
  }

  private record Principal(String role, UUID appUserId) {}
}