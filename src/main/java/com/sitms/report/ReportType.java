package com.sitms.report;

/**
 * The report categories served by the reporting API (SITMS-044, EPIC 11).
 *
 * <p>Each {@link ReportType} maps to a generated report payload and, from SITMS-048/049, to a
 * PDF/Excel export.
 */
public enum ReportType {
  INTERNSHIP,
  ACTIVITY,
  EVALUATION
}