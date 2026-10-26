package kr.co.maeum.pdf;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

/** PDF 처리를 OCR 런타임과 분리한다. 표준 출력에는 계약된 JSON 또는 PDF만 쓴다. */
public final class PdfEngine {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int MAX_PAGES = 12;

    private PdfEngine() {}

    public static void main(String[] args) {
        try {
            String command = args[0];
            if ("version".equals(command)) {
                JSON.writeValue(System.out, Map.of("engine", "Apache PDFBox", "version", "3.0.8"));
                return;
            }
            Path source = Path.of(args[1]);
            try (PDDocument document = open(source)) {
                switch (command) {
                    case "validate" ->
                            JSON.writeValue(
                                    System.out, Map.of("pageCount", document.getNumberOfPages()));
                    case "render" ->
                            render(document, args.length > 2 ? Float.parseFloat(args[2]) : 120);
                    case "split" -> split(document, source, args[2], System.out);
                    case "scan" -> scan(document, Path.of(args[2]));
                    default -> throw new IllegalArgumentException("UNKNOWN_COMMAND");
                }
            }
        } catch (Exception exception) {
            // 문서 경로·원문·공급자 설정을 오류 응답에 포함하지 않는다.
            System.err.println("PDF_PROCESSING_FAILED");
            System.exit(2);
        }
    }

    static PDDocument open(Path source) throws IOException {
        if (Files.size(source) > 10L * 1024 * 1024) {
            throw new IOException("FILE_TOO_LARGE");
        }
        byte[] signature;
        try (var input = Files.newInputStream(source)) {
            signature = input.readNBytes(5);
        }
        PDDocument document;
        if (new String(signature, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-")) {
            document = Loader.loadPDF(source.toFile(), "", IOUtils.createTempFileOnlyStreamCache());
        } else {
            try (var input = ImageIO.createImageInputStream(source.toFile())) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw new IOException("INVALID_IMAGE");
                var reader = readers.next();
                try {
                    reader.setInput(input);
                    String format = reader.getFormatName();
                    if (!(format.equalsIgnoreCase("png") || format.equalsIgnoreCase("jpeg"))
                            || (long) reader.getWidth(0) * reader.getHeight(0) > 10_000_000) {
                        throw new IOException("INVALID_IMAGE_SIZE");
                    }
                    BufferedImage image = reader.read(0);
                    document = new PDDocument(IOUtils.createTempFileOnlyStreamCache());
                    var page = new PDPage(new PDRectangle(image.getWidth(), image.getHeight()));
                    document.addPage(page);
                    try (var stream = new PDPageContentStream(document, page)) {
                        stream.drawImage(
                                LosslessFactory.createFromImage(document, image),
                                0,
                                0,
                                image.getWidth(),
                                image.getHeight());
                    }
                } finally {
                    reader.dispose();
                }
            }
        }
        if (document.isEncrypted()
                || document.getNumberOfPages() < 1
                || document.getNumberOfPages() > MAX_PAGES) {
            document.close();
            throw new IOException("INVALID_PAGE_COUNT_OR_ENCRYPTION");
        }
        for (var page : document.getPages()) {
            var box = page.getCropBox();
            if (box.getWidth() <= 0
                    || box.getHeight() <= 0
                    || box.getWidth() > 5000
                    || box.getHeight() > 5000) {
                document.close();
                throw new IOException("INVALID_PAGE_SIZE");
            }
        }
        return document;
    }

    static void render(PDDocument document, float dpi) throws IOException {
        if (!Float.isFinite(dpi) || dpi < 20 || dpi > 150) throw new IOException("INVALID_DPI");
        var renderer = new PDFRenderer(document);
        renderer.setSubsamplingAllowed(true);
        emit(Map.of("event", "metadata", "pageCount", document.getNumberOfPages()));
        for (int index = 0; index < document.getNumberOfPages(); index++) {
            PDPage page = document.getPage(index);
            var box = page.getCropBox();
            boolean sideways = page.getRotation() % 180 != 0;
            float width = sideways ? box.getHeight() : box.getWidth();
            float height = sideways ? box.getWidth() : box.getHeight();
            float scale = Math.min(dpi / 72, Math.min(1600 / width, 2400 / height));
            BufferedImage image = renderer.renderImage(index, scale, ImageType.RGB);
            var text = new PositionedText(index + 1, scale, page);
            text.getText(document);
            var graphics = new ImageRegions(page, scale);
            graphics.processPage(page);
            var output = new LinkedHashMap<String, Object>();
            output.put("event", "page");
            output.put("pageNumber", index + 1);
            output.put("pageCount", document.getNumberOfPages());
            output.put("width", image.getWidth());
            output.put("height", image.getHeight());
            output.put("pageWidth", width);
            output.put("pageHeight", height);
            output.put("lines", text.lines);
            output.put("images", graphics.boxes);
            var buffer = new ByteArrayOutputStream();
            ImageIO.write(image, "png", buffer);
            output.put("preview", Base64.getEncoder().encodeToString(buffer.toByteArray()));
            emit(output);
            image.flush();
        }
    }

    private static void emit(Object value) throws IOException {
        // 직렬화기가 표준 출력을 닫지 않도록 JSON 문자열을 줄 단위로 출력한다.
        System.out.println(JSON.writeValueAsString(value));
        System.out.flush();
    }

    static void split(PDDocument document, Path source, String selected, OutputStream output)
            throws Exception {
        var indexes = new ArrayList<Integer>();
        for (String number : selected.split(",")) indexes.add(Integer.parseInt(number) - 1);
        if (indexes.isEmpty()
                || indexes.size() > MAX_PAGES
                || new HashSet<>(indexes).size() != indexes.size()
                || indexes.stream()
                        .anyMatch(index -> index < 0 || index >= document.getNumberOfPages())) {
            throw new IOException("INVALID_SELECTION");
        }
        try (var result = new PDDocument(IOUtils.createTempFileOnlyStreamCache())) {
            for (int index : indexes) {
                var page = document.getPage(index);
                var imported = result.importPage(page);
                imported.setResources(page.getResources());
                imported.setCropBox(page.getCropBox());
                imported.setRotation(page.getRotation());
            }
            // 같은 원본·선택의 분할 파일은 동일한 해시로 공유할 수 있게 문서 ID를 고정한다.
            var digest = MessageDigest.getInstance("SHA-256");
            digest.update(Files.readAllBytes(source));
            digest.update(selected.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] id = java.util.Arrays.copyOf(digest.digest(), 16);
            var ids = new COSArray();
            ids.add(new COSString(id));
            ids.add(new COSString(id));
            result.getDocument().setDocumentID(ids);
            result.save(output);
        }
    }

    private static void scan(PDDocument document, Path destination) throws IOException {
        var renderer = new PDFRenderer(document);
        renderer.setSubsamplingAllowed(true);
        try (var result = new PDDocument(IOUtils.createTempFileOnlyStreamCache())) {
            for (int index = 0; index < document.getNumberOfPages(); index++) {
                var sourcePage = document.getPage(index);
                boolean sideways = sourcePage.getRotation() % 180 != 0;
                var box = sourcePage.getCropBox();
                float width = sideways ? box.getHeight() : box.getWidth();
                float height = sideways ? box.getWidth() : box.getHeight();
                float scale = Math.min(1.5f, Math.min(1600 / width, 2400 / height));
                var image = renderer.renderImage(index, scale, ImageType.RGB);
                var page = new PDPage(new PDRectangle(width, height));
                result.addPage(page);
                try (var stream = new PDPageContentStream(result, page)) {
                    stream.drawImage(
                            org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory.createFromImage(
                                    result, image, .88f),
                            0,
                            0,
                            width,
                            height);
                }
                image.flush();
            }
            result.save(destination.toFile());
        }
    }

    private static final class PositionedText extends PDFTextStripper {
        private final List<Map<String, Object>> lines = new ArrayList<>();
        private final float scale;
        private final PDPage page;

        PositionedText(int pageNumber, float scale, PDPage page) throws IOException {
            setStartPage(pageNumber);
            setEndPage(pageNumber);
            setSortByPosition(true);
            this.scale = scale;
            this.page = page;
        }

        @Override
        protected void writeString(String text, List<TextPosition> positions) {
            var segment = new ArrayList<TextPosition>();
            TextPosition previous = null;
            for (var position : positions) {
                if (previous != null
                        && position.getXDirAdj()
                                        - (previous.getXDirAdj() + previous.getWidthDirAdj())
                                > Math.max(4, position.getFontSizeInPt() * .6)) {
                    addSegment(segment);
                    segment.clear();
                }
                segment.add(position);
                previous = position;
            }
            addSegment(segment);
        }

        private void addSegment(List<TextPosition> positions) {
            if (positions.isEmpty()) return;
            String text =
                    positions.stream()
                            .map(TextPosition::getUnicode)
                            .collect(java.util.stream.Collectors.joining())
                            .strip();
            if (text.isEmpty()) return;
            float x0 = Float.MAX_VALUE, y0 = Float.MAX_VALUE, x1 = 0, y1 = 0;
            for (var position : positions) {
                x0 = Math.min(x0, position.getXDirAdj());
                y0 = Math.min(y0, position.getYDirAdj() - position.getHeightDir());
                x1 = Math.max(x1, position.getXDirAdj() + position.getWidthDirAdj());
                y1 = Math.max(y1, position.getYDirAdj() + position.getHeightDir() * .2f);
            }
            float width = page.getCropBox().getWidth();
            float height = page.getCropBox().getHeight();
            List<Float> bounds =
                    switch (Math.floorMod(page.getRotation(), 360)) {
                        case 90 ->
                                List.of(
                                        (height - y1) * scale,
                                        x0 * scale,
                                        (height - y0) * scale,
                                        x1 * scale);
                        case 180 ->
                                List.of(
                                        (width - x1) * scale,
                                        (height - y1) * scale,
                                        (width - x0) * scale,
                                        (height - y0) * scale);
                        case 270 ->
                                List.of(
                                        y0 * scale,
                                        (width - x1) * scale,
                                        y1 * scale,
                                        (width - x0) * scale);
                        default -> List.of(x0 * scale, y0 * scale, x1 * scale, y1 * scale);
                    };
            lines.add(Map.of("text", text, "bbox", bounds, "source", "PDF_TEXT"));
        }
    }
}
