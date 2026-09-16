package com.sitms.common.security;

/** The realm role of the authenticated principal, used for data-access scoping. */
public enum PrincipalRole {
  ADMIN,
  SUPERVISOR,
  FACULTY,
  STUDENT
}