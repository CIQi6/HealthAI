package com.example.healthai.prescription.contra;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.example.healthai.drug.domain.Medicine;
import com.example.healthai.prescription.domain.Prescription;
import com.example.healthai.prescription.domain.PrescriptionContraStatus;
import com.example.healthai.prescription.domain.PrescriptionItem;
import com.example.healthai.profile.domain.HealthProfile;

@Component
public class SimpleContraindicationEvaluator implements ContraindicationEvaluator {

    @Override
    public ContraindicationReport evaluate(Prescription prescription,
                                           List<PrescriptionItem> items,
                                           Map<Long, Medicine> medicineById,
                                           HealthProfile profile) {
        List<ContraindicationViolation> violations = new ArrayList<>();

        Set<String> patientAllergies = extractTokens(profile == null ? null : profile.getAllergyHistory());
        Set<String> patientDiseases = extractTokens(profile == null ? null : profile.getChronicDiseases());

        for (int i = 0; i < items.size(); i++) {
            PrescriptionItem item = items.get(i);
            Medicine medicine = medicineById.get(item.getMedicineId());
            if (medicine == null) {
                continue;
            }
            checkAllergy(violations, i, medicine, patientAllergies);
            checkDisease(violations, i, medicine, patientDiseases);
            checkDosage(violations, i, medicine, item);
            checkSpecialPopulation(violations, i, medicine, profile);
        }

        checkInteractions(violations, items, medicineById);

        return ContraindicationReport.fromViolations(violations);
    }

    private void checkAllergy(List<ContraindicationViolation> violations,
                              int index,
                              Medicine medicine,
                              Set<String> patientAllergies) {
        Set<String> drugAllergies = extractTokens(medicine.getContraindications());
        if (drugAllergies.isEmpty() || patientAllergies.isEmpty()) {
            return;
        }
        for (String allergy : drugAllergies) {
            if (patientAllergies.contains(allergy)) {
                violations.add(new ContraindicationViolation(index, medicine.getId(), medicine.getGenericName(),
                    "ALLERGY", PrescriptionContraStatus.FAIL,
                    "患者过敏史与药品禁忌冲突: " + allergy));
            }
        }
    }

    private void checkDisease(List<ContraindicationViolation> violations,
                               int index,
                               Medicine medicine,
                               Set<String> patientDiseases) {
        Set<String> drugDiseases = extractTokens(medicine.getIndications());
        if (drugDiseases.isEmpty() || patientDiseases.isEmpty()) {
            return;
        }
        for (String disease : drugDiseases) {
            if (patientDiseases.contains(disease)) {
                violations.add(new ContraindicationViolation(index, medicine.getId(), medicine.getGenericName(),
                    "DISEASE", PrescriptionContraStatus.WARN,
                    "患者慢性病需要注意药品禁忌: " + disease));
            }
        }
    }

    private void checkDosage(List<ContraindicationViolation> violations,
                              int index,
                              Medicine medicine,
                              PrescriptionItem item) {
        String dosageGuideline = medicine.getDosageGuideline();
        if (!StringUtils.hasText(dosageGuideline)) {
            return;
        }

        String instruction = item.getDosageInstruction() == null ? "" : item.getDosageInstruction().toLowerCase();
        if (instruction.contains("overdose") || instruction.contains("超剂量")) {
            violations.add(new ContraindicationViolation(index, medicine.getId(), medicine.getGenericName(),
                "DOSAGE", PrescriptionContraStatus.FAIL,
                "处方剂量说明疑似超量: " + item.getDosageInstruction()));
            return;
        }

        if (dosageGuideline.toLowerCase().contains("max")) {
            // Simple heuristic: check quantity against max daily supply expressed as number
            int maxDaily = extractFirstInteger(dosageGuideline, 4);
            if (maxDaily > 0 && item.getFrequency() != null && item.getFrequency().toLowerCase().contains("daily")
                && item.getDaySupply() != null && item.getDaySupply() > maxDaily) {
                violations.add(new ContraindicationViolation(index, medicine.getId(), medicine.getGenericName(),
                    "DOSAGE", PrescriptionContraStatus.WARN,
                    "处方日剂量可能超过指南上限: " + dosageGuideline));
            }
        }
    }

    private void checkSpecialPopulation(List<ContraindicationViolation> violations,
                                         int index,
                                         Medicine medicine,
                                         HealthProfile profile) {
        if (profile == null) {
            return;
        }
        Set<String> contraindications = extractTokens(medicine.getContraindications());
        if (contraindications.isEmpty()) {
            return;
        }
        String bloodType = profile.getBloodType() == null ? "" : profile.getBloodType().toLowerCase();
        if (contraindications.contains("pregnancy") && profile.getGeneticRisk() != null
            && profile.getGeneticRisk().toLowerCase().contains("pregnant")) {
            violations.add(new ContraindicationViolation(index, medicine.getId(), medicine.getGenericName(),
                "SPECIAL_POPULATION", PrescriptionContraStatus.FAIL,
                "孕期患者不建议使用该药: " + medicine.getGenericName()));
        }
        if (contraindications.contains("bloodtype:" + bloodType)) {
            violations.add(new ContraindicationViolation(index, medicine.getId(), medicine.getGenericName(),
                "SPECIAL_POPULATION", PrescriptionContraStatus.WARN,
                "血型受限药品，需要谨慎使用"));
        }
    }

    private void checkInteractions(List<ContraindicationViolation> violations,
                                    List<PrescriptionItem> items,
                                    Map<Long, Medicine> medicineById) {
        for (int i = 0; i < items.size(); i++) {
            Medicine first = medicineById.get(items.get(i).getMedicineId());
            if (first == null || !StringUtils.hasText(first.getDrugInteractions())) {
                continue;
            }
            Set<String> interactionTokens = extractTokens(first.getDrugInteractions());
            if (interactionTokens.isEmpty()) {
                continue;
            }
            for (int j = i + 1; j < items.size(); j++) {
                Medicine second = medicineById.get(items.get(j).getMedicineId());
                if (second == null) {
                    continue;
                }
                if (interactionTokens.contains(second.getGenericName().toLowerCase())
                    || interactionTokens.contains(String.valueOf(second.getId()))) {
                    violations.add(new ContraindicationViolation(i, first.getId(), first.getGenericName(),
                        "INTERACTION", PrescriptionContraStatus.FAIL,
                        "药物组合存在高危相互作用: " + first.getGenericName() + " + " + second.getGenericName()));
                    violations.add(new ContraindicationViolation(j, second.getId(), second.getGenericName(),
                        "INTERACTION", PrescriptionContraStatus.FAIL,
                        "药物组合存在高危相互作用: " + first.getGenericName() + " + " + second.getGenericName()));
                }
            }
        }
    }

    private Set<String> extractTokens(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Set.of();
        }
        String[] parts = raw.split("[,;|\\n]");
        Set<String> tokens = new HashSet<>();
        for (String part : parts) {
            String token = part.trim().toLowerCase();
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private int extractFirstInteger(String text, int digitsLimit) {
        if (!StringUtils.hasText(text)) {
            return -1;
        }
        for (String token : Arrays.stream(text.split("[^0-9]"))
            .filter(t -> !t.isBlank())
            .toList()) {
            if (token.length() <= digitsLimit) {
                try {
                    return Integer.parseInt(token);
                } catch (NumberFormatException ignored) {
                    // continue
                }
            }
        }
        return -1;
    }
}
