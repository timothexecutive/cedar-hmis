package ke.cedar.hmis.maternity;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import ke.cedar.hmis.audit.AuditTrail;
import ke.cedar.hmis.reception.Patient;
import ke.cedar.hmis.ipd.Admission;
import ke.cedar.hmis.ipd.Bed;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class MaternityService {

    @Transactional
    public Pregnancy registerPregnancy(Long patientId,
            Pregnancy request,
            Long userId, String userName) {

        Patient patient = Patient.findById(patientId);
        if (patient == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Patient not found\"}")
                    .build());
        }

        Pregnancy existing =
            Pregnancy.findActiveByPatient(patientId);
        if (existing != null) {
            throw new WebApplicationException(
                Response.status(409)
                    .entity("{\"error\":\"Patient already " +
                            "has an active pregnancy\"}")
                    .build());
        }

        long count = Pregnancy.count() + 1;
        request.pregnancyNo = String.format(
            "PRG-%d-%05d",
            Year.now().getValue(), count);
        request.patient = patient;

        if (request.edd == null && request.lmp != null) {
            request.edd = request.lmp.plusDays(280);
        }

        request.registeredBy = userName;
        request.assessRisk();
        request.persist();

        AuditTrail.log(
            userId, userName, "NURSE",
            "PREGNANCY_REGISTERED", "MATERNITY",
            "Pregnancy", String.valueOf(request.id),
            "Pregnancy: " + request.pregnancyNo +
            " | Patient: " + patient.fullName +
            " | Gravida: " + request.gravida +
            " | EDD: " + request.edd +
            " | Risk: " + request.riskLevel +
            " | HIV: " + request.hivStatus +
            " | By: " + userName);

        return request;
    }

    @Transactional
    public ANCVisit recordANCVisit(Long pregnancyId,
            ANCVisit request,
            Long userId, String userName) {

        Pregnancy pregnancy =
            Pregnancy.findById(pregnancyId);
        if (pregnancy == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Pregnancy " +
                            "not found\"}")
                    .build());
        }

        long visitCount =
            ANCVisit.countByPregnancy(pregnancyId) + 1;
        request.visitNo   = (int) visitCount;
        request.pregnancy = pregnancy;
        request.patient   = pregnancy.patient;
        request.seenBy    = userName;

        request.detectRiskFlags();
        request.persist();

        if (request.riskFlags != null &&
                !request.riskFlags.isBlank()) {
            pregnancy.riskLevel = "HIGH";
            String existing =
                pregnancy.riskFlags != null ?
                pregnancy.riskFlags : "";
            pregnancy.riskFlags = existing +
                request.riskFlags;
            pregnancy.persist();
        }

        AuditTrail.log(
            userId, userName, "NURSE",
            "ANC_VISIT_RECORDED", "MATERNITY",
            "Pregnancy", String.valueOf(pregnancyId),
            "ANC Visit " + request.visitNo +
            " | Patient: " +
            pregnancy.patient.fullName +
            " | Gestation: " +
            request.gestationWeeks + "wks" +
            " | BP: " + request.bpSystolic +
            "/" + request.bpDiastolic +
            " | FHR: " + request.fetalHeartRate +
            " | Flags: " + (request.riskFlags == null
                || request.riskFlags.isBlank() ?
                "None" : request.riskFlags) +
            " | By: " + userName);

        return request;
    }

    @Transactional
    public MaternityAdmission admitForLabour(
            Long patientId, Long pregnancyId,
            Long bedId, Integer gestationWeeks,
            String membranesStatus,
            String onsetOfLabour,
            String admittedBy, String notes,
            Long userId, String userName) {

        Patient patient = Patient.findById(patientId);
        if (patient == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Patient not found\"}")
                    .build());
        }

        MaternityAdmission existing =
            MaternityAdmission
                .findByPatientActive(patientId);
        if (existing != null) {
            throw new WebApplicationException(
                Response.status(409)
                    .entity("{\"error\":\"Patient already " +
                            "admitted in labour\"}")
                    .build());
        }

        Admission ipdAdmission = null;
        if (bedId != null) {
            Bed bed = Bed.findById(bedId);
            if (bed == null ||
                    !bed.status.equals("AVAILABLE")) {
                throw new WebApplicationException(
                    Response.status(409)
                        .entity("{\"error\":\"Bed not " +
                                "available\"}")
                        .build());
            }
            bed.status = "OCCUPIED";
            bed.persist();

            ipdAdmission = new Admission();
            ipdAdmission.admissionNo = "MAT-" +
                LocalDateTime.now().toString()
                    .replace("-","")
                    .replace(":","")
                    .substring(0,14);
            ipdAdmission.patient = patient;
            ipdAdmission.bed     = bed;
            ipdAdmission.ward    = bed.ward;
            ipdAdmission.admittingDoctor  = userName;
            ipdAdmission.admissionDiagnosis =
                "Admitted in Labour";
            ipdAdmission.persist();
        }

        String admNo = String.format("MADT-%d-%05d",
            Year.now().getValue(),
            MaternityAdmission.count() + 1);

        MaternityAdmission admission =
            new MaternityAdmission();
        admission.admissionNo    = admNo;
        admission.patient        = patient;
        admission.gestationWeeks = gestationWeeks;
        admission.membranesStatus =
            membranesStatus != null ?
            membranesStatus : "INTACT";
        admission.onsetOfLabour  =
            onsetOfLabour != null ?
            onsetOfLabour : "SPONTANEOUS";
        admission.labourStartedAt = LocalDateTime.now();
        admission.admittedBy     = userName;
        admission.notes          = notes;
        admission.ipdAdmission   = ipdAdmission;

        if (pregnancyId != null) {
            Pregnancy pregnancy =
                Pregnancy.findById(pregnancyId);
            if (pregnancy != null)
                admission.pregnancy = pregnancy;
        }

        admission.persist();

        AuditTrail.log(
            userId, userName, "NURSE",
            "MATERNITY_ADMISSION", "MATERNITY",
            "MaternityAdmission",
            String.valueOf(admission.id),
            "Labour admission: " + admNo +
            " | Patient: " + patient.fullName +
            " | Gestation: " + gestationWeeks + "wks" +
            " | Membranes: " +
            admission.membranesStatus +
            " | By: " + userName);

        return admission;
    }

    @Transactional
    public Delivery recordDelivery(
            Long maternityAdmissionId,
            Delivery deliveryRequest,
            List<Map<String, Object>> babies,
            Long userId, String userName) {

        MaternityAdmission admission =
            MaternityAdmission
                .findById(maternityAdmissionId);
        if (admission == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Maternity " +
                            "admission not found\"}")
                    .build());
        }

        String deliveryNo = String.format(
            "DEL-%d-%05d",
            Year.now().getValue(),
            Delivery.count() + 1);

        deliveryRequest.deliveryNo =
            deliveryNo;
        deliveryRequest.maternityAdmission = admission;
        deliveryRequest.patient = admission.patient;
        deliveryRequest.pregnancy = admission.pregnancy;
        deliveryRequest.conductedBy = userName;
        deliveryRequest.persist();

        admission.status = "DELIVERED";
        admission.persist();

        if (admission.pregnancy != null) {
            admission.pregnancy.status = "DELIVERED";
            admission.pregnancy.persist();
        }

        if (admission.ipdAdmission != null &&
                admission.ipdAdmission.bed != null) {
            admission.ipdAdmission.bed.status =
                "CLEANING";
            admission.ipdAdmission.bed.persist();
        }

        AuditTrail.log(
            userId, userName, "NURSE",
            "DELIVERY_RECORDED", "MATERNITY",
            "Delivery",
            String.valueOf(deliveryRequest.id),
            "Delivery: " + deliveryNo +
            " | Mother: " +
            admission.patient.fullName +
            " | Type: " +
            deliveryRequest.deliveryType +
            " | Condition: " +
            deliveryRequest.maternalCondition +
            " | By: " + userName);

        if (babies != null) {
            for (Map<String, Object> b : babies) {
                BabyRecord baby = new BabyRecord();
                baby.delivery   = deliveryRequest;
                baby.gender     = (String) b.get("gender");
                baby.birthWeight =
                    b.get("birthWeight") != null
                    ? new BigDecimal(
                        b.get("birthWeight").toString())
                    : null;
                baby.birthTime =
                    b.get("birthTime") != null
                    ? LocalTime.parse(
                        b.get("birthTime").toString())
                    : LocalTime.now();
                baby.gestationWeeks =
                    b.get("gestationWeeks") != null
                    ? Integer.parseInt(
                        b.get("gestationWeeks").toString())
                    : admission.gestationWeeks;
                baby.apgar1Min =
                    b.get("apgar1Min") != null
                    ? Integer.parseInt(
                        b.get("apgar1Min").toString())
                    : null;
                baby.apgar5Min =
                    b.get("apgar5Min") != null
                    ? Integer.parseInt(
                        b.get("apgar5Min").toString())
                    : null;
                baby.birthOutcome =
                    b.get("birthOutcome") != null
                    ? (String) b.get("birthOutcome")
                    : "LIVE_BIRTH";
                baby.resuscitation =
                    b.get("resuscitation") != null &&
                    Boolean.parseBoolean(
                        b.get("resuscitation").toString());
                baby.vitaminKGiven =
                    b.get("vitaminKGiven") != null &&
                    Boolean.parseBoolean(
                        b.get("vitaminKGiven").toString());
                baby.bcgGiven =
                    b.get("bcgGiven") != null &&
                    Boolean.parseBoolean(
                        b.get("bcgGiven").toString());
                baby.polio0Given =
                    b.get("polio0Given") != null &&
                    Boolean.parseBoolean(
                        b.get("polio0Given").toString());
                baby.nevirapineGiven =
                    b.get("nevirapineGiven") != null &&
                    Boolean.parseBoolean(
                        b.get("nevirapineGiven").toString());
                baby.breastfed1Hr =
                    b.get("breastfed1Hr") != null &&
                    Boolean.parseBoolean(
                        b.get("breastfed1Hr").toString());

                if ("LIVE_BIRTH".equals(
                        baby.birthOutcome)) {
                    Patient babyPatient = new Patient();
                    babyPatient.fullName = "Baby of " +
                        admission.patient.fullName;
                    babyPatient.gender  = baby.gender;
                    babyPatient.dateOfBirth =
                        LocalDate.now();
                    long pCount = Patient.count() + 1;
                    babyPatient.patientNo =
                        String.format("CDR-%d-%05d",
                        Year.now().getValue(), pCount);
                    babyPatient.isSHAMember = false;
                    babyPatient.persist();
                    baby.patient = babyPatient;

                    baby.birthNotification =
                        String.format("BN-%d-%05d",
                        Year.now().getValue(),
                        BabyRecord.count() + 1);
                }

                baby.babyNo = String.format(
                    "BABY-%d-%05d",
                    Year.now().getValue(),
                    BabyRecord.count() + 1);
                baby.persist();

                AuditTrail.log(
                    userId, userName, "NURSE",
                    "BABY_REGISTERED", "MATERNITY",
                    "BabyRecord",
                    String.valueOf(baby.id),
                    "Baby: " + baby.babyNo +
                    " | Gender: " + baby.gender +
                    " | Weight: " +
                    baby.birthWeight + "kg" +
                    " | Outcome: " +
                    baby.birthOutcome +
                    " | APGAR 1min: " +
                    baby.apgar1Min +
                    " | LBW: " +
                    baby.lowBirthWeight +
                    " | Mother: " +
                    admission.patient.fullName +
                    " | By: " + userName);
            }
        }

        return deliveryRequest;
    }

    @Transactional
    public PostnatalVisit recordPostnatalVisit(
            Long deliveryId, PostnatalVisit request,
            Long userId, String userName) {

        Delivery delivery =
            Delivery.findById(deliveryId);
        if (delivery == null) {
            throw new WebApplicationException(
                Response.status(404)
                    .entity("{\"error\":\"Delivery " +
                            "not found\"}")
                    .build());
        }

        request.delivery = delivery;
        request.patient  = delivery.patient;
        request.seenBy   = userName;
        request.detectRiskFlags();
        request.persist();

        AuditTrail.log(
            userId, userName, "NURSE",
            "POSTNATAL_VISIT", "MATERNITY",
            "Delivery", String.valueOf(deliveryId),
            "Postnatal: " + request.visitType +
            " | Patient: " +
            delivery.patient.fullName +
            " | BP: " + request.bpSystolic +
            "/" + request.bpDiastolic +
            " | Flags: " +
            (request.riskFlags == null ||
                request.riskFlags.isBlank() ?
                "None" : request.riskFlags) +
            " | By: " + userName);

        return request;
    }

    public List<Pregnancy> getActivePregnancies() {
        return Pregnancy.findActive(); }
    public List<Pregnancy> getHighRiskPregnancies() {
        return Pregnancy.findHighRisk(); }
    public List<Pregnancy> getPatientPregnancies(Long p) {
        return Pregnancy.findByPatient(p); }
    public List<ANCVisit> getANCVisits(Long id) {
        return ANCVisit.findByPregnancy(id); }
    public List<MaternityAdmission> getActiveLabour() {
        return MaternityAdmission.findActive(); }
    public List<Delivery> getTodaysDeliveries() {
        return Delivery.findByDate(LocalDate.now()); }
    public List<Delivery> getDeliveriesByMonth(
            int y, int m) {
        return Delivery.findByMonth(y, m); }
    public List<BabyRecord> getBabiesByDelivery(Long d) {
        return BabyRecord.findByDelivery(d); }
    public List<BabyRecord> getLowBirthWeightBabies() {
        return BabyRecord.findLowBirthWeight(); }
    public List<PostnatalVisit> getPostnatalVisits(Long d){
        return PostnatalVisit.findByDelivery(d); }
    public List<ANCVisit> getANCWithRiskFlags() {
        return ANCVisit.findWithRiskFlags(); }
    public List<PostnatalVisit> getPostnatalWithRiskFlags(){
        return PostnatalVisit.findWithRiskFlags(); }
}