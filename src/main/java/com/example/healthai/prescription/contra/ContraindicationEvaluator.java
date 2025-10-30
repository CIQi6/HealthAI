package com.example.healthai.prescription.contra;

import java.util.List;
import java.util.Map;

import com.example.healthai.drug.domain.Medicine;
import com.example.healthai.prescription.domain.Prescription;
import com.example.healthai.prescription.domain.PrescriptionItem;
import com.example.healthai.profile.domain.HealthProfile;

public interface ContraindicationEvaluator {

    ContraindicationReport evaluate(Prescription prescription,
                                    List<PrescriptionItem> items,
                                    Map<Long, Medicine> medicineById,
                                    HealthProfile profile);
}
