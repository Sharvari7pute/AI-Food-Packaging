package com.packsmart.controller;

import com.packsmart.service.RecommendationService;
import com.packsmart.service.report.PdfReportService;
import com.packsmart.service.report.QrService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final PdfReportService pdf;
    private final QrService qr;
    private final RecommendationService recommendations;

    /** QR-verified packaging spec sheet (A4 PDF). */
    @GetMapping("/{shareId}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable String shareId) {
        byte[] body = pdf.specSheet(shareId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("packsmart-spec-" + shareId.substring(0, Math.min(8, shareId.length())) + ".pdf").build().toString())
                .body(body);
    }

    /** PNG QR code linking to the public verify page. */
    @GetMapping("/{shareId}/qr")
    public ResponseEntity<byte[]> qr(@PathVariable String shareId) {
        recommendations.entityByShareId(shareId);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(1)))
                .body(qr.verifyQr(shareId));
    }
}
