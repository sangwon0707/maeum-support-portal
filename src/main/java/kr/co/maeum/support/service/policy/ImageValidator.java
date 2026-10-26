package kr.co.maeum.support.service.policy;

import kr.co.maeum.support.common.BusinessException;

import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

@Component
public class ImageValidator {
    public byte[] normalize(byte[] bytes) {
        if (bytes.length == 0 || bytes.length > 10 * 1024 * 1024) {
            throw new BusinessException(422, "이미지는 10MiB 이하로 선택해 주세요.");
        }
        try (var stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                throw new BusinessException(422, "PNG 또는 JPEG 이미지가 아닙니다.");
            }
            var reader = readers.next();
            try {
                reader.setInput(stream);
                String format = reader.getFormatName();
                if (!format.equalsIgnoreCase("png") && !format.equalsIgnoreCase("jpeg")) {
                    throw new BusinessException(422, "PNG와 JPEG만 지원합니다.");
                }
                // 파일 용량만으로 메모리 사용량을 알 수 없으므로 디코딩 전에 픽셀 수를 검사한다.
                if ((long) reader.getWidth(0) * reader.getHeight(0) > 10_000_000) {
                    throw new BusinessException(422, "이미지 해상도가 너무 큽니다.");
                }
                var output = new ByteArrayOutputStream();
                ImageIO.write(reader.read(0), "png", output);
                if (output.size() > 10 * 1024 * 1024) {
                    throw new BusinessException(422, "변환된 이미지 용량이 너무 큽니다.");
                }
                return output.toByteArray();
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new BusinessException(422, "손상된 이미지입니다. 다른 파일로 다시 시도해 주세요.");
        }
    }
}
