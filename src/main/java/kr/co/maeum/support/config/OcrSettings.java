package kr.co.maeum.support.config;

import org.springframework.stereotype.Component;

@Component
public class OcrSettings {
    public boolean liveEnabled() {
        return Boolean.parseBoolean(DatabaseConfig.env("OCR_LIVE_ENABLED", "false"));
    }

    public boolean aiEnabled() {
        return liveEnabled() && Boolean.parseBoolean(DatabaseConfig.env("OCR_AI_ENABLED", "false"));
    }

    public int actorAiLimit() {
        return limit("AI_LIMIT_PER_DAY", 2);
    }

    public int globalAiLimit() {
        return limit("AI_LIMIT_GLOBAL_PER_DAY", 20);
    }

    private int limit(String name, int fallback) {
        return Math.max(0, Integer.parseInt(DatabaseConfig.env(name, Integer.toString(fallback))));
    }

    public int documentTimeoutSeconds() {
        return Math.max(
                60,
                Math.min(
                        900,
                        Integer.parseInt(
                                DatabaseConfig.env("OCR_DOCUMENT_TIMEOUT_SECONDS", "300"))));
    }

    public String endpoint() {
        return DatabaseConfig.env("OCR_API_URL", "http://127.0.0.1:8091");
    }

    public String token() {
        return DatabaseConfig.env("OCR_API_TOKEN", "");
    }
}
