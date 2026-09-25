package com.packsmart.service.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.packsmart.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** QR codes that open the public verify page {@code {FRONTEND_URL}/report/{shareId}}. */
@Service
@RequiredArgsConstructor
public class QrService {

    private static final int DEFAULT_SIZE_PX = 320;

    private final AppProperties app;

    public String verifyUrl(String shareId) {
        return app.frontendBase() + "/report/" + shareId;
    }

    public byte[] png(String text, int sizePx) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, sizePx, sizePx,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 1));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Could not create QR code", e);
        }
    }

    public byte[] verifyQr(String shareId) {
        return png(verifyUrl(shareId), DEFAULT_SIZE_PX);
    }
}
