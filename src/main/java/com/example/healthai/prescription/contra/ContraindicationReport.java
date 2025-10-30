package com.example.healthai.prescription.contra;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.healthai.prescription.domain.PrescriptionContraStatus;

public record ContraindicationReport(
    PrescriptionContraStatus status,
    String summary,
    List<ContraindicationViolation> violations
) {

    public static ContraindicationReport pass() {
        return new ContraindicationReport(PrescriptionContraStatus.PASS, "禁忌校验通过", List.of());
    }

    public static ContraindicationReport fromViolations(List<ContraindicationViolation> violations) {
        if (violations == null || violations.isEmpty()) {
            return pass();
        }

        PrescriptionContraStatus overall = violations.stream()
            .map(ContraindicationViolation::level)
            .reduce(PrescriptionContraStatus.PASS, ContraindicationReport::mergeLevel);

        Set<String> messages = violations.stream()
            .map(ContraindicationViolation::message)
            .collect(Collectors.toSet());
        String summary = String.join("; ", messages);

        return new ContraindicationReport(overall, summary, List.copyOf(violations));
    }

    private static PrescriptionContraStatus mergeLevel(PrescriptionContraStatus left, PrescriptionContraStatus right) {
        if (left == PrescriptionContraStatus.FAIL || right == PrescriptionContraStatus.FAIL) {
            return PrescriptionContraStatus.FAIL;
        }
        if (left == PrescriptionContraStatus.WARN || right == PrescriptionContraStatus.WARN) {
            return PrescriptionContraStatus.WARN;
        }
        return PrescriptionContraStatus.PASS;
    }
}
