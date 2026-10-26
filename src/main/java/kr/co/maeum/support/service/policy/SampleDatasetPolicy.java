package kr.co.maeum.support.service.policy;

import kr.co.maeum.support.model.Program;

import java.util.List;

/** 예시 종류와 대응 공고를 고정해 다른 사업의 계획·견적을 섞지 않는다. */
public final class SampleDatasetPolicy {
    private SampleDatasetPolicy() {}

    private static final List<Option> OPTIONS =
            List.of(
                    new Option("original", "가상상점 · 매장 환경개선", "local-store-environment-v1"),
                    new Option("comparison", "가상문구점 · 환경개선 비교 사례", "local-store-environment-v1"),
                    new Option("digital", "가상달빛식당 · 주문·재고 관리", "local-digital-store-v1"),
                    new Option("energy", "가상새벽꽃집 · 냉장·조명 설비", "local-energy-saving-v1"),
                    new Option("access", "가상모퉁이책방 · 출입·이동 동선", "local-accessible-store-v1"),
                    new Option("online", "가상온기공방 · 상품 소개 자료", "local-online-channel-v1"),
                    new Option("safety", "가상바른분식 · 환기·바닥·급배수", "local-store-safety-v1"));

    public static List<Option> all() {
        return OPTIONS;
    }

    public static boolean exists(String dataset) {
        return OPTIONS.stream().anyMatch(option -> option.code().equals(dataset));
    }

    public static List<Option> choices(Program program) {
        return program == null
                ? List.of()
                : OPTIONS.stream()
                        .filter(option -> option.programKey().equals(program.getSeedKey()))
                        .toList();
    }

    public record Option(String code, String label, String programKey) {
        public String getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }
    }
}
