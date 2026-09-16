package com.sitms.organization;

/** Aggregate organization KPIs shown on the organizations page (SITMS-012). */
public record OrganizationStats(
    long organizationCount,
    long activeOrganizations,
    long availablePositions,
    long studentsPlaced,
    long activeInterns) {}