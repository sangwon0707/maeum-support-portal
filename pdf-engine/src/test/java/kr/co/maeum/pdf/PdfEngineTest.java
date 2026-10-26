package kr.co.maeum.pdf;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

class PdfEngineTest {
    @TempDir Path directory;

    @Test
    @DisplayName("암호화·과도한 페이지·손상 문서를 거절한다")
    void rejects_encrypted_excessive_and_corrupt_documents() throws Exception {
        Path encrypted = directory.resolve("encrypted.pdf");
        try (var document = new PDDocument()) {
            document.addPage(new PDPage());
            document.protect(
                    new StandardProtectionPolicy("owner-password", "", new AccessPermission()));
            document.save(encrypted.toFile());
        }
        assertThrows(Exception.class, () -> PdfEngine.open(encrypted));
        Path excessive = directory.resolve("excessive.pdf");
        try (var document = new PDDocument()) {
            for (int index = 0; index < 13; index++) document.addPage(new PDPage());
            document.save(excessive.toFile());
        }
        assertThrows(Exception.class, () -> PdfEngine.open(excessive));
        Path corrupt = directory.resolve("corrupt.pdf");
        Files.writeString(corrupt, "%PDF-broken");
        assertThrows(Exception.class, () -> PdfEngine.open(corrupt));
    }

    @Test
    @DisplayName("분할한 페이지의 회전·순서와 재생성 결과를 유지한다")
    void split_preserves_rotation_order_and_deterministic_bytes() throws Exception {
        Path source = directory.resolve("source.pdf");
        try (var document = new PDDocument()) {
            document.addPage(new PDPage());
            var rotated = new PDPage();
            rotated.setRotation(90);
            document.addPage(rotated);
            document.save(source.toFile());
        }
        try (var document = PdfEngine.open(source)) {
            var first = new ByteArrayOutputStream();
            var second = new ByteArrayOutputStream();
            PdfEngine.split(document, source, "2,1", first);
            PdfEngine.split(document, source, "2,1", second);
            assertArrayEquals(first.toByteArray(), second.toByteArray());
            try (var result = Loader.loadPDF(first.toByteArray())) {
                assertEquals(2, result.getNumberOfPages());
                assertEquals(90, result.getPage(0).getRotation());
            }
            assertThrows(
                    Exception.class,
                    () -> PdfEngine.split(document, source, "1,1", new ByteArrayOutputStream()));
            assertThrows(
                    Exception.class,
                    () -> PdfEngine.split(document, source, "0", new ByteArrayOutputStream()));
        }
    }

    @Test
    @DisplayName("회전·잘린 페이지의 텍스트와 사진 영역을 렌더링 좌표에 맞춘다")
    void rendering_preserves_rotated_crop_text_and_image_positions() throws Exception {
        try (var document = new PDDocument()) {
            var page = new PDPage(new org.apache.pdfbox.pdmodel.common.PDRectangle(200, 300));
            page.setCropBox(new org.apache.pdfbox.pdmodel.common.PDRectangle(10, 20, 180, 260));
            page.setRotation(90);
            document.addPage(page);
            var image =
                    new java.awt.image.BufferedImage(
                            40, 80, java.awt.image.BufferedImage.TYPE_INT_RGB);
            try (var stream = new org.apache.pdfbox.pdmodel.PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(
                        new org.apache.pdfbox.pdmodel.font.PDType1Font(
                                org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA),
                        12);
                stream.newLineAtOffset(20, 250);
                stream.showText("TEST");
                stream.endText();
                stream.drawImage(
                        org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory.createFromImage(
                                document, image),
                        60,
                        70,
                        40,
                        80);
            }
            var capture = new ByteArrayOutputStream();
            var original = System.out;
            try (var output =
                    new java.io.PrintStream(
                            capture, true, java.nio.charset.StandardCharsets.UTF_8)) {
                System.setOut(output);
                PdfEngine.render(document, 72);
            } finally {
                System.setOut(original);
            }
            var json = new com.fasterxml.jackson.databind.ObjectMapper();
            var result =
                    json.readTree(
                            capture.toString(java.nio.charset.StandardCharsets.UTF_8)
                                    .split("\n")[1]);
            assertEquals(260, result.path("width").asInt());
            assertEquals(180, result.path("height").asInt());
            var photo = result.path("images").get(0).path("bbox");
            assertEquals(50, photo.get(0).asDouble(), .1);
            assertEquals(50, photo.get(1).asDouble(), .1);
            assertEquals(130, photo.get(2).asDouble(), .1);
            assertEquals(90, photo.get(3).asDouble(), .1);
            var text = result.path("lines").get(0).path("bbox");
            assertTrue(text.get(0).asDouble() > 200);
            assertTrue(text.get(1).asDouble() < 20);
        }
    }
}
