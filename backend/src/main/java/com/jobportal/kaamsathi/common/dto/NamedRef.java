package com.jobportal.kaamsathi.common.dto;

import java.util.UUID;

/**
 * A linked item shown with its name, e.g. {"id": "...", "name": "Java"}.
 * The id lets the frontend build filter links; the name is for display.
 */
public record NamedRef(UUID id, String name) {
}
