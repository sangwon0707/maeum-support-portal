package kr.co.maeum.support.document;

import org.springframework.stereotype.Component;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

/** 실제 서류와 혼동하지 않도록 테스트 표시를 이미지 자체에 넣는다. */
@Component
public class FixtureImages {
    private final Font documentFont;

    public FixtureImages() {
        try (var stream = FixtureImages.class.getResourceAsStream("/fonts/NotoSansKR.ttf")) {
            if (stream == null) {
                throw new IllegalStateException("가상 서류용 글꼴이 없습니다.");
            }
            documentFont = Font.createFont(Font.TRUETYPE_FONT, stream);
        } catch (IOException | FontFormatException exception) {
            throw new IllegalStateException("가상 서류용 글꼴을 읽지 못했습니다.", exception);
        }
    }

    public byte[] prepared(String type, String dataset) {
        String filename = "BUSINESS_CERT".equals(type) ? "business-cert" : "estimate";
        try (var stream =
                FixtureImages.class.getResourceAsStream(
                        "/fixtures/" + dataset + "/" + filename + ".png")) {
            if (stream == null) {
                throw new IllegalStateException("가상 서류 자료가 없습니다.");
            }
            return stream.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("가상 서류를 읽지 못했습니다.", exception);
        }
    }

    public byte[] create(String type, String businessName) {
        try {
            var image = new BufferedImage(1000, 800, BufferedImage.TYPE_INT_RGB);
            var graphics = image.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, 1000, 800);
            graphics.setColor(new Color(28, 50, 78));
            graphics.setFont(documentFont.deriveFont(Font.BOLD, 32f));
            graphics.drawString("SAMPLE / TEST DOCUMENT", 60, 80);
            graphics.setFont(documentFont.deriveFont(Font.PLAIN, 24f));
            String[] lines = {
                "BUSINESS_CERT".equals(type) ? "사업자등록증 (가상)" : "견적서 (가상)",
                "상호: " + businessName,
                "대표자: 가상대표",
                "등록번호: 000-00-00000",
                "개업일: 2024-01-01",
                "견적금액: 3,000,000원 / 세부 항목: 간판·조명",
                "포트폴리오 테스트 전용 · 법적 효력 없음"
            };
            for (int i = 0; i < lines.length; i++) {
                graphics.drawString(lines[i], 60, 160 + i * 70);
            }
            graphics.dispose();
            var output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
