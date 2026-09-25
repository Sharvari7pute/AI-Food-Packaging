package com.packsmart.service.report;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.FontSelector;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.packsmart.dto.RecommendResponse;
import com.packsmart.dto.RecommendResponse.LayerDto;
import com.packsmart.dto.RecommendResponse.MapDto;
import com.packsmart.dto.RecommendResponse.OptionDto;
import com.packsmart.entity.Recommendation;
import com.packsmart.service.CatalogService;
import com.packsmart.service.RecommendationService;
import com.packsmart.service.engine.LaminateService;
import com.packsmart.service.engine.Num;
import com.packsmart.service.engine.model.MaterialData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/** Builds the A4 "Packaging Spec Sheet" PDF a business can send to its packaging supplier. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfReportService {

    private static final Color GREEN = new Color(0x05, 0x96, 0x69);
    private static final Color BLUE = new Color(0x1e, 0x3a, 0x8a);
    private static final Color RED = new Color(0xb9, 0x1c, 0x1c);
    private static final Color GREY = new Color(0x6b, 0x72, 0x80);
    private static final Color LIGHT = new Color(0xf1, 0xf5, 0xf9);
    private static final Color AMBER = new Color(0xfe, 0xf3, 0xc7);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm 'UTC'").withZone(ZoneId.of("UTC"));
    private static final int QR_PX = 240;
    private static final float QR_SIZE_PT = 90;
    private static final float MARGIN = 36;

    private final RecommendationService recommendations;
    private final CatalogService catalog;
    private final LaminateService laminates;
    private final QrService qr;

    private volatile Fonts fonts;

    private record Fonts(BaseFont regular, BaseFont bold, BaseFont devanagari) {
    }

    public byte[] specSheet(String shareId) {
        Recommendation rec = recommendations.entityByShareId(shareId);
        RecommendResponse r = recommendations.read(rec);
        try {
            return render(rec, r);
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("Could not build PDF", e);
        }
    }

    private byte[] render(Recommendation rec, RecommendResponse r) throws DocumentException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, MARGIN, MARGIN, MARGIN, MARGIN);
        PdfWriter.getInstance(doc, out);
        doc.addTitle("PackSmart Packaging Spec Sheet - " + r.commodity());
        doc.addCreator("PackSmart");
        doc.open();

        header(doc, r);
        section(doc, "1. Food and conditions");
        var in = r.inputs();
        PdfPTable inputs = table(new float[]{1.3f, 2f, 1.3f, 2f});
        kv(inputs, "Food", r.commodity()
                + (r.aiEstimatedFood() ? " - AI-estimated properties" : ""));
        kv(inputs, "Pack weight", fmt(in.packWeightG()) + " g");
        kv(inputs, "Pack area", fmt(in.packAreaM2()) + " m²" + (in.areaEstimated() ? " (estimated)" : ""));
        kv(inputs, "Target shelf life", in.shelfLifeDays() + " days");
        kv(inputs, "Storage", in.storageType() + ", " + fmt(in.storageTempC()) + " °C, " + fmt(in.relativeHumidityPct()) + " % RH");
        kv(inputs, "Transport", in.transport());
        kv(inputs, "Water activity", fmt(in.waterActivity()));
        kv(inputs, "Fat", fmt(in.fatPct()) + " %");
        doc.add(inputs);

        section(doc, "2. Decision trace - why these requirements");
        for (String reason : r.requirements().reasons()) {
            doc.add(para("✓  " + reason, 9, false, Color.BLACK));
        }
        String req = "Required OTR ≤ " + (r.requiredOtr() != null ? fmt(r.requiredOtr()) + " cc/m²·day·atm" : "no limit")
                + "     Required WVTR ≤ " + (r.requiredWvtr() != null ? fmt(r.requiredWvtr()) + " g/m²·day" : "no limit");
        doc.add(para(req, 9, true, BLUE));

        OptionDto top = r.options().isEmpty() ? null : r.options().get(0);
        section(doc, "3. Recommended structure");
        if (top == null) {
            doc.add(para("No structure met every requirement. See the near misses on the web report.", 10, true, RED));
        } else {
            doc.add(para(top.name() + "  (" + top.kind().toLowerCase() + ", total " + fmt(top.totalThicknessUm()) + " µm"
                    + (top.perforationNeeded() ? ", micro-perforated" : "") + ")", 12, true, GREEN));
            PdfPTable layers = table(new float[]{0.6f, 2.2f, 1.2f, 1.6f, 1.6f});
            headerRow(layers, "#", "Material (outside → inside)", "Thickness", "OTR", "WVTR");
            int i = 1;
            for (LayerDto l : top.layers()) {
                Optional<MaterialData> m = catalog.catalog().material(l.material());
                cells(layers, String.valueOf(i++), l.material(), fmt(l.thicknessUm()) + " µm",
                        m.map(x -> fmt(Num.sig(laminates.layerTransmission(x.otr25(), l.thicknessUm())))).orElse("-"),
                        m.map(x -> fmt(Num.sig(laminates.layerTransmission(x.wvtr25(), l.thicknessUm())))).orElse("-"));
            }
            boldCells(layers, "", "Whole structure", fmt(top.totalThicknessUm()) + " µm", fmt(top.otr()), fmt(top.wvtr()));
            boldCells(layers, "", "Required", "",
                    r.requiredOtr() != null ? "≤ " + fmt(r.requiredOtr()) : (r.map() != null ? "≥ " + fmt(r.map().requiredOtr()) : "no limit"),
                    r.requiredWvtr() != null ? "≤ " + fmt(r.requiredWvtr()) : "no limit");
            doc.add(layers);
            doc.add(para("OTR in cc/(m²·day·atm) at 23 °C, WVTR in g/(m²·day) at 38 °C / 90 % RH.", 7, false, GREY));

            section(doc, "4. Shelf life, cost and environment");
            PdfPTable facts = table(new float[]{1.6f, 2f, 1.6f, 2f});
            kv(facts, "Estimated shelf life", top.estimatedShelfLifeDays() + " days");
            kv(facts, "Limiting factor", top.limitingFactor());
            kv(facts, "Cost per 1000 packs", "₹ " + fmt(top.costPer1000Inr()));
            kv(facts, "CO₂e per 1000 packs", fmt(top.co2eKgPer1000()) + " kg");
            kv(facts, "Recyclability", top.recyclable() ? "Recyclable mono-material (" + top.family() + ")" : "Not recyclable (" + top.family() + ")");
            kv(facts, "Biodegradable", top.biodegradable() ? "Yes" : "No");
            kv(facts, "Heat sealable", top.heatSealable() ? "Yes (inner layer)" : "No");
            kv(facts, "Use temperature", fmt(top.minTempC()) + " to " + fmt(top.maxTempC()) + " °C");
            doc.add(facts);
            if (top.approxData()) {
                PdfPTable warn = table(new float[]{1});
                PdfPCell c = cell("Some material values are approximate (marked approx/VERIFY in our data). Confirm with supplier datasheets.", 8, false, Color.BLACK);
                c.setBackgroundColor(AMBER);
                warn.addCell(c);
                doc.add(warn);
            }
        }

        if (r.options().size() > 1) {
            section(doc, "Other good options");
            PdfPTable others = table(new float[]{0.5f, 2.6f, 1.2f, 1.3f, 1.3f, 1.2f});
            headerRow(others, "#", "Structure", "Shelf life", "₹/1000", "CO₂e kg/1000", "Recyclable");
            for (OptionDto o : r.options().subList(1, r.options().size())) {
                cells(others, String.valueOf(o.rank()), o.name() + " (" + layersText(o) + ")", o.estimatedShelfLifeDays() + " d",
                        fmt(o.costPer1000Inr()), fmt(o.co2eKgPer1000()), o.recyclable() ? "Yes" : "No");
            }
            doc.add(others);
        }

        MapDto map = r.map();
        if (map != null) {
            section(doc, "Modified atmosphere (fresh produce)");
            PdfPTable m = table(new float[]{1.6f, 2f, 1.6f, 2f});
            kv(m, "Target O₂", map.targetO2Min() != null ? fmt(map.targetO2Min()) + "–" + fmt(map.targetO2Max()) + " %" : "generic " + fmt(map.gasMix().o2Pct()) + " %");
            kv(m, "Target CO₂", map.targetCo2Min() != null ? fmt(map.targetCo2Min()) + "–" + fmt(map.targetCo2Max()) + " %" : "-");
            kv(m, "Breathable film", map.film() + " " + map.filmThicknessUm() + " µm" + (map.perforationNeeded() ? " + micro-perforations" : ""));
            kv(m, "Storage temperature", map.storageTempC() != null ? fmt(map.storageTempC()) + " °C" : "-");
            kv(m, "Required film OTR", "≥ " + fmt(map.requiredOtr()));
            kv(m, "Respiration", fmt(map.respirationMlPerDay()) + " mL O₂/day");
            doc.add(m);
            if (map.note() != null) {
                doc.add(para(map.note(), 8, false, GREY));
            }
        }

        if (r.avoid() != null) {
            section(doc, "Avoid");
            doc.add(para("✗  " + r.avoid().name() + " " + fmt(r.avoid().thicknessUm()) + " µm - " + r.avoid().reason(), 9, false, RED));
        }

        if (rec.getExplanation() != null && Boolean.TRUE.equals(rec.getExplanationAiUsed())) {
            section(doc, "In simple words (AI explanation of the engine result)");
            doc.add(para(rec.getExplanation(), 9, false, Color.BLACK));
        }

        doc.add(Chunk.NEWLINE);
        doc.add(para("Disclaimer: Prototype estimates from literature data; validate with lab shelf-life tests.", 8, false, GREY));
        doc.add(para("Verify this spec online: " + qr.verifyUrl(r.shareId()), 8, false, BLUE));
        doc.close();
        return out.toByteArray();
    }

    // ------------------------------------------------------------------ layout helpers

    private void header(Document doc, RecommendResponse r) throws DocumentException, IOException {
        PdfPTable head = new PdfPTable(new float[]{4, 1});
        head.setWidthPercentage(100);
        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.addElement(para("PackSmart", 22, true, GREEN));
        left.addElement(para("Packaging Spec Sheet - " + r.commodity(), 14, true, BLUE));
        left.addElement(para("Generated " + (r.createdAt() != null ? DATE.format(r.createdAt()) : "") + "   ·   ID " + r.shareId(), 8, false, GREY));
        left.addElement(para("Scan the QR code to verify this spec online.", 8, false, GREY));
        head.addCell(left);
        Image img = Image.getInstance(qr.png(qr.verifyUrl(r.shareId()), QR_PX));
        img.scaleAbsolute(QR_SIZE_PT, QR_SIZE_PT);
        PdfPCell right = new PdfPCell(img, false);
        right.setBorder(Rectangle.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        head.addCell(right);
        doc.add(head);
    }

    private void section(Document doc, String title) throws DocumentException {
        Paragraph p = para(title, 11, true, BLUE);
        p.setSpacingBefore(10);
        p.setSpacingAfter(4);
        doc.add(p);
    }

    private Fonts fonts() {
        if (fonts == null) {
            synchronized (this) {
                if (fonts == null) {
                    fonts = new Fonts(load("fonts/DejaVuSans.ttf"), load("fonts/DejaVuSans-Bold.ttf"),
                            load("fonts/NotoSansDevanagari-Regular.ttf"));
                }
            }
        }
        return fonts;
    }

    private static BaseFont load(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            byte[] bytes = in.readAllBytes();
            return BaseFont.createFont(path.substring(path.lastIndexOf('/') + 1), BaseFont.IDENTITY_H, BaseFont.EMBEDDED,
                    true, bytes, null);
        } catch (IOException | DocumentException e) {
            throw new IllegalStateException("Could not load font " + path, e);
        }
    }

    /** Text with automatic font fallback (Latin/symbols → DejaVu, Devanagari → Noto). */
    private Phrase phrase(String text, float size, boolean bold, Color color) {
        Fonts f = fonts();
        FontSelector sel = new FontSelector();
        sel.addFont(new Font(bold ? f.bold() : f.regular(), size, Font.NORMAL, color));
        sel.addFont(new Font(f.devanagari(), size, bold ? Font.BOLD : Font.NORMAL, color));
        return sel.process(text == null ? "" : text);
    }

    private Paragraph para(String text, float size, boolean bold, Color color) {
        Paragraph p = new Paragraph();
        p.add(phrase(text, size, bold, color));
        p.setLeading(size * 1.35f);
        return p;
    }

    private static PdfPTable table(float[] widths) {
        PdfPTable t = new PdfPTable(widths);
        t.setWidthPercentage(100);
        t.setSpacingBefore(2);
        t.setSpacingAfter(4);
        return t;
    }

    private PdfPCell cell(String text, float size, boolean bold, Color color) {
        PdfPCell c = new PdfPCell(phrase(text, size, bold, color));
        c.setPadding(4);
        c.setBorderColor(new Color(0xe2, 0xe8, 0xf0));
        return c;
    }

    private void kv(PdfPTable t, String k, String v) {
        PdfPCell key = cell(k, 8, true, GREY);
        key.setBackgroundColor(LIGHT);
        t.addCell(key);
        t.addCell(cell(v, 9, false, Color.BLACK));
    }

    private void headerRow(PdfPTable t, String... texts) {
        for (String s : texts) {
            PdfPCell c = cell(s, 8, true, Color.WHITE);
            c.setBackgroundColor(BLUE);
            t.addCell(c);
        }
    }

    private void cells(PdfPTable t, String... texts) {
        for (String s : texts) {
            t.addCell(cell(s, 9, false, Color.BLACK));
        }
    }

    private void boldCells(PdfPTable t, String... texts) {
        for (String s : texts) {
            PdfPCell c = cell(s, 9, true, Color.BLACK);
            c.setBackgroundColor(LIGHT);
            t.addCell(c);
        }
    }

    private static String layersText(OptionDto o) {
        List<String> parts = o.layers().stream().map(l -> l.material() + " " + fmt(l.thicknessUm())).toList();
        return String.join(" / ", parts);
    }

    private static String fmt(Double v) {
        return v == null ? "-" : Num.fmt(v);
    }
}
