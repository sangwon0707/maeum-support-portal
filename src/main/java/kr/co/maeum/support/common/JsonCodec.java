package kr.co.maeum.support.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

@Component
public class JsonCodec {
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    public String write(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("업무 스냅샷을 만들지 못했습니다.", exception);
        }
    }
}
