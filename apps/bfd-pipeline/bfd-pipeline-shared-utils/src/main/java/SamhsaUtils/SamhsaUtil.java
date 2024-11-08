package SamhsaUtils;
import SamhsaUtils.model.FissTag;
import SamhsaUtils.model.McsTag;
import SamhsaUtils.model.TagCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import gov.cms.bfd.model.rda.entities.RdaFissClaim;
import gov.cms.bfd.model.rda.entities.RdaFissDiagnosisCode;
import gov.cms.bfd.model.rda.entities.RdaFissProcCode;
import gov.cms.bfd.model.rda.entities.RdaFissRevenueLine;
import gov.cms.bfd.model.rda.entities.RdaMcsClaim;
import gov.cms.bfd.model.rda.entities.RdaMcsDetail;
import gov.cms.bfd.model.rda.entities.RdaMcsDiagnosisCode;
import SamhsaUtils.model.SamhsaEntry;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class SamhsaUtil {
    private static final Map<String, SamhsaEntry> samhsaMap = new HashMap<>();
    private static SamhsaUtil samhsaLookup;
    private static final String SAMHSA_LIST_RESOURCE = "security_labels.yml";

    private static InputStream getFileInputStream(String fileName) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
    }
    public Optional<List<FissTag>> processBatch(List<RdaFissClaim> claims)  {
        return Optional.empty();
    }
    public static String generateHash(String... values) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String combined = String.join(":", values);
            byte[] hashBytes = md.digest(combined.getBytes(StandardCharsets.UTF_8));
            return String.format("%064x", new BigInteger(1, hashBytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("Error generating hash ID", ex);
        }

    }
    public static Optional<List<McsTag>> checkAndProcessMcsClaim(RdaMcsClaim mcsClaim) {
        List<String> samhsaFields = getPossibleMcsSamhsaFields(mcsClaim);
        if (checkSamhsaFields(Objects.requireNonNull(samhsaFields))) {
            List<McsTag> mcsTags = new ArrayList<>();
            mcsTags.add(McsTag.builder()
                    .tagId(generateHash(mcsClaim.getIdrClmHdIcn(), TagCode._42CFRPart2.name()))
                    .claim(mcsClaim)
                    .code(TagCode._42CFRPart2)
                    .build());
            mcsTags.add(McsTag.builder()
                    .tagId(generateHash(mcsClaim.getIdrClmHdIcn(), TagCode.R.name()))
                    .claim(mcsClaim)
                    .code(TagCode.R)
                    .build());
            return Optional.of(mcsTags);
        }
        return Optional.empty();
    }
    public static Optional<List<FissTag>> checkAndProcessFissClaim(RdaFissClaim fissClaim) {
        List<String> samhsaFields = getPossibleFissSamhsaFields(fissClaim);
        if (checkSamhsaFields(Objects.requireNonNull(samhsaFields))) {
            List<FissTag> fissTags = new ArrayList<>();
            fissTags.add(FissTag.builder()
                    .tagId(generateHash(fissClaim.getClaimId(), TagCode._42CFRPart2.name()))
                    .claim(fissClaim)
                    .code(TagCode._42CFRPart2)
                    .build());
            fissTags.add(FissTag.builder()
                    .tagId(generateHash(fissClaim.getClaimId(), TagCode.R.name()))
                    .claim(fissClaim)
                    .code(TagCode.R)
                    .build());
            return Optional.of(fissTags);
        }
        return Optional.empty();
    }
    private static boolean checkSamhsaFields(List<String> fields) {
        for (String field: fields) {
            // TODO: Return the SamhsaEntry object to populate the tag details.
            if (isSamhsaCode(Optional.ofNullable(field)).isPresent()) {
                return true;
            }
        }
        return false;
    }
    private static List<String> getPossibleMcsSamhsaFields(RdaMcsClaim mcsClaim) {
        List<String> samhsaFields = new ArrayList<>();
        for (RdaMcsDiagnosisCode diagCode: mcsClaim.getDiagCodes()) {
            samhsaFields.add(diagCode.getIdrDiagCode());
        }
        for (RdaMcsDetail detail: mcsClaim.getDetails()) {
            samhsaFields.add(detail.getIdrDtlPrimaryDiagCode());
            samhsaFields.add(detail.getIdrProcCode());
        }
        return samhsaFields;
    }
    private static List<String> getPossibleFissSamhsaFields(RdaFissClaim fissClaim) {
        List<String> samhsaFields = new ArrayList<>();
        samhsaFields.add(fissClaim.getAdmitDiagCode());
        for (RdaFissRevenueLine revenueLine: fissClaim.getRevenueLines()) {
            samhsaFields.add(revenueLine.getApcHcpcsApc());
            samhsaFields.add(revenueLine.getHcpcCd());
        }
        samhsaFields.add(fissClaim.getDrgCd());
        samhsaFields.add(fissClaim.getPrincipleDiag());
        for (RdaFissDiagnosisCode diagCode: fissClaim.getDiagCodes()) {
            samhsaFields.add(diagCode.getDiagCd2());
        }
        for (RdaFissProcCode procCode: fissClaim.getProcCodes()) {
            samhsaFields.add(procCode.getProcCode());
        }
        return samhsaFields;
    }
    public static Optional<SamhsaEntry> isSamhsaCode(Optional<String> code) {
        if (!code.isPresent()) {
            return Optional.empty();
        }
        if (samhsaMap.isEmpty()) {
            try {
                initializeSamhsaMap(getFileInputStream(SAMHSA_LIST_RESOURCE));
            } catch (IOException ioe) {
                throw new RuntimeException("Cannot retrieve list of SAMHSA codes.");
            }
        }
        if (samhsaMap.containsKey(code.get())) {
            return Optional.of(samhsaMap.get(code.get()));
        }
        return Optional.empty();
    }
    private static Map<String, SamhsaEntry> initializeSamhsaMap(InputStream stream) throws IOException {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        List<SamhsaEntry> entries = mapper.readValue(stream,
                mapper.getTypeFactory().constructCollectionType(List.class, SamhsaEntry.class));
        return entries.stream()
                .collect(Collectors.toMap(SamhsaEntry::getCode, entry -> entry));
    }
}
