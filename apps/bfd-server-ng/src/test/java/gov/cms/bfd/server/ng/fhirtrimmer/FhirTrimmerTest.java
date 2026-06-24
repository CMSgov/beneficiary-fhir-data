package gov.cms.bfd.server.ng.fhirtrimmer;

import ca.uhn.fhir.context.FhirContext;
import java.math.BigDecimal;
import java.util.List;
import org.apache.commons.lang3.time.StopWatch;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Money;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Manual benchmark load test comparing Whitelist vs Blacklist trimming execution speed against a
 * realistically-sized ExplanationOfBenefit, built from the actual production FHIRPath expression
 * set (sourced from the dictionary-support-files YAML).
 */
class FhirTrimmerTest {

    private final FhirContext ctx = FhirContext.forR4();
    private FhirTrimmer fhirTrimmer;

    @BeforeEach
    void setUp(){
        var basisProfileMap = new BasisProfileMap();
        basisProfileMap.generateProfileBasisMap();
        this.fhirTrimmer = new FhirTrimmer(basisProfileMap, ctx);
    }

    @Test
    @Disabled("Manual benchmark load test to compare Whitelist vs Blacklist execution speeds")
    void benchmarkTrimmingPerformance() {
        ExplanationOfBenefit templateEob = buildTemplateEob();

        int totalIterations = 50_000;
        int warmupIterations = 2_000;

        // --- Warm up JVM JIT compiler on both code paths ---
        System.out.println("Warming up Trimmer engine paths...");
        for (int i = 0; i < warmupIterations; i++) {
            fhirTrimmer.trim(createBloatedEob(templateEob), BasisProfile.BASIS);
        }
        System.gc();

        // --- Benchmark Blacklist Mode ---
        System.out.println("Starting Benchmark: Blacklist Mode...");
        StopWatch blacklistWatch = StopWatch.createStarted();
        for (int i = 0; i < totalIterations; i++) {
            blacklistWatch.suspend();
            ExplanationOfBenefit target = createBloatedEob(templateEob);
            blacklistWatch.resume();

            fhirTrimmer.trim(target, BasisProfile.BASIS);
        }
        blacklistWatch.stop();
        printResults("BLACKLIST STRATEGY", totalIterations, blacklistWatch.getTime());
    }

    /**
     * Builds a realistically-populated EOB: identifiers, meta tags, related claims, careTeam,
     * multiple diagnoses/procedures, supportingInfo entries, total/adjudication amounts, and a
     * contained Organization — enough breadth that every blacklist/whitelist path in
     * EOB_BLACKLIST_PATHS actually matches at least one real node, so the benchmark exercises real
     * evaluate() + removeChild() cost rather than walking paths that match nothing.
     */
    private ExplanationOfBenefit buildTemplateEob() {
        var eob = new ExplanationOfBenefit();
        eob.setId("eob-template");

        eob.addIdentifier(
                new Identifier()
                        .setSystem("https://bluebutton.cms.gov/identifiers/CLM-CNTL-NUM")
                        .setValue("1234567890"));
        eob.addIdentifier(
                new Identifier()
                        .setValue("uc-claim-id-001")
                        .setType(
                                new CodeableConcept()
                                        .addCoding(
                                                new Coding(
                                                        "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBIdentifierType",
                                                        "uc",
                                                        "Unique Claim ID"))));

        eob.getMeta()
                .addTag("https://bluebutton.cms.gov/fhir/CodeSystem/Final-Action", "F", "Final")
                .addTag("https://bluebutton.cms.gov/fhir/CodeSystem/System-Type", "FISS", "FISS");

        eob.addRelated()
                .setRelationship(
                        new CodeableConcept()
                                .addCoding(
                                        new Coding(
                                                "http://terminology.hl7.org/CodeSystem/ex-relatedclaimrelationship",
                                                "prior",
                                                "Prior Claim")))
                .setReference(
                        new Identifier()
                                .setSystem("https://bluebutton.cms.gov/identifiers/CLM-CNTL-NUM")
                                .setValue("0987654321"));

        eob.getType()
                .addCoding(new Coding("https://bluebutton.cms.gov/fhir/CodeSystem/CLM-TYPE-CD", "60", "Inpatient"));

        eob.setPatient(new Reference("Patient/1234567890123"));

        eob.setBillablePeriod(
                new Period()
                        .setStartElement(new org.hl7.fhir.r4.model.DateTimeType("2025-01-01"))
                        .setEndElement(new org.hl7.fhir.r4.model.DateTimeType("2025-01-10")));
        eob.setCreated(new java.util.Date());

        eob.addSupportingInfo()
                .setCategory(
                        new CodeableConcept()
                                .addCoding(
                                        new Coding(
                                                "https://bluebutton.cms.gov/fhir/CodeSystem/Supporting-Information",
                                                "CLM_IDR_LD_DT",
                                                "Load Date")))
                .setTiming(new org.hl7.fhir.r4.model.DateType("2025-01-11"));

        eob.addSupportingInfo()
                .setCategory(
                        new CodeableConcept()
                                .addCoding(
                                        new Coding(
                                                "https://bluebutton.cms.gov/fhir/CodeSystem/Supporting-Information",
                                                "CLM_QUERY_CD",
                                                "Query Code")))
                .setCode(new CodeableConcept().addCoding(new Coding(null, "1", "Query")));

        eob.addSupportingInfo()
                .setCode(
                        new CodeableConcept()
                                .addCoding(
                                        new Coding(
                                                "https://bluebutton.cms.gov/fhir/CodeSystem/CLM-NRLN-RIC-CD", "U", "RIC")));

        var contained = new org.hl7.fhir.r4.model.Organization();
        contained.setId("provider-org");
        contained.addIdentifier(
                new Identifier().setSystem("http://hl7.org/fhir/sid/us-npi").setValue("1122334455"));
        contained.addIdentifier(
                new Identifier().setSystem("urn:oid:2.16.840.1.113883.4.4").setValue("99-1234567"));
        contained.addAddress(new Address().setPostalCode("22030"));
        eob.addContained(contained);
        eob.setProvider(new Reference("#provider-org"));

        eob.addTotal()
                .setCategory(
                        new CodeableConcept()
                                .addCoding(
                                        new Coding(
                                                "http://hl7.org/fhir/us/carin-bb/CodeSystem/C4BBAdjudication",
                                                "noncovered",
                                                "Noncovered"))
                                .addCoding(
                                        new Coding(
                                                "https://bluebutton.cms.gov/fhir/CodeSystem/Adjudication",
                                                "CLM_NCVRD_CHRG_AMT",
                                                "Noncovered Charge")))
                .setAmount(new Money().setValue(new BigDecimal("125.50")).setCurrency("USD"));

        eob.addTotal()
                .setCategory(
                        new CodeableConcept()
                                .addCoding(
                                        new Coding(
                                                "http://terminology.hl7.org/CodeSystem/adjudication",
                                                "deductible",
                                                "Deductible"))
                                .addCoding(
                                        new Coding(
                                                "https://bluebutton.cms.gov/fhir/CodeSystem/Adjudication",
                                                "CLM_MDCR_DDCTBL_AMT",
                                                "Deductible Amount")))
                .setAmount(new Money().setValue(new BigDecimal("75.00")).setCurrency("USD"));

        eob.addAdjudication()
                .setCategory(
                        new CodeableConcept()
                                .addCoding(
                                        new Coding(
                                                "https://bluebutton.cms.gov/fhir/CodeSystem/Adjudication",
                                                "CLM_OPRTNL_DSPRTNT_AMT",
                                                "DSH")))
                .setAmount(new Money().setValue(new BigDecimal("10.00")).setCurrency("USD"));

        eob.addAdjudication()
                .setCategory(
                        new CodeableConcept()
                                .addCoding(
                                        new Coding(
                                                "https://bluebutton.cms.gov/fhir/CodeSystem/Adjudication",
                                                "CLM_OPRTNL_IME_AMT",
                                                "IME")))
                .setAmount(new Money().setValue(new BigDecimal("20.00")).setCurrency("USD"));

        eob.setPayment(
                new ExplanationOfBenefit.PaymentComponent()
                        .setAmount(new Money().setValue(new BigDecimal("500.00")).setCurrency("USD")));

        eob.addCareTeam()
                .setRole(new CodeableConcept().addCoding(new Coding(null, "attending", "Attending")))
                .setProvider(
                        new Reference()
                                .setIdentifier(
                                        new Identifier()
                                                .setSystem("http://hl7.org/fhir/sid/us-npi")
                                                .setValue("1112223334")));
        eob.addCareTeam()
                .setRole(new CodeableConcept().addCoding(new Coding(null, "otheroperating", "Operating")))
                .setProvider(
                        new Reference()
                                .setIdentifier(
                                        new Identifier()
                                                .setSystem("http://hl7.org/fhir/sid/us-npi")
                                                .setValue("5556667778")));

        // 12 diagnoses, mirroring real EOBs with multiple ICD codes
        for (int i = 0; i < 12; i++) {
            eob.addDiagnosis()
                    .setDiagnosis(new CodeableConcept().addCoding(new Coding("ICD-10", "A%02d.9".formatted(i), "Diagnosis " + i)))
                    .setType(List.of(new CodeableConcept().addCoding(new Coding(null, "principal", "Principal"))))
                    .setOnAdmission(new CodeableConcept().addCoding(new Coding(null, "Y", "Yes")));
        }

        // 8 procedures
        for (int i = 0; i < 8; i++) {
            eob.addProcedure()
                    .setProcedure(new CodeableConcept().addCoding(new Coding("ICD-10-PCS", "B%02d".formatted(i), "Procedure " + i)))
                    .setDate(new java.util.Date());
        }

        return eob;
    }

    /**
     * Returns a deep copy of the template with extra noise extensions added at multiple levels, so
     * each iteration exercises both the matched-path removal logic and the cost of evaluating
     * expressions against a resource with additional unrelated nodes present.
     */
    private ExplanationOfBenefit createBloatedEob(ExplanationOfBenefit templateEob) {
        ExplanationOfBenefit copy = templateEob.copy();
        for (int i = 0; i < 10; i++) {
            copy.addExtension(
                    new Extension("http://example.com/ext/noise-" + i, new StringType("PruneMe-" + i)));
        }
        for (var diagnosis : copy.getDiagnosis()) {
            diagnosis.addExtension(
                    new Extension("http://example.com/ext/diagnosis-noise", new StringType("PruneMe")));
        }
        return copy;
    }




    private void printResults(String strategyName, int iterations, long totalMillis) {
        double throughput = (iterations * 1000.0) / totalMillis;
        System.out.println("\n=================== " + strategyName + " ===================");
        System.out.println("Total Execution Time: " + totalMillis + " ms");
        System.out.printf("Throughput Rate:      %.2f Bundles/sec%n", throughput);
        System.out.printf("Average Latency:      %.4f ms per Bundle%n", ((double) totalMillis / iterations));
        System.out.println("=========================================================");
    }
}