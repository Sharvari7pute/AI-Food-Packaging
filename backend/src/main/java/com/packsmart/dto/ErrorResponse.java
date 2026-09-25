package com.packsmart.dto;

import java.util.List;

/** Uniform error body: {@code { "error": "...", "details": [...] }}. */
public record ErrorResponse(String error, List<String> details) {
}
