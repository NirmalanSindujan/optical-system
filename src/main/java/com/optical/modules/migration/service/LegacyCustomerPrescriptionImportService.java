package com.optical.modules.migration.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.optical.modules.customer.entity.Customer;
import com.optical.modules.customer.repository.CustomerRepository;
import com.optical.modules.migration.dto.LegacyCustomerPrescriptionImportResponse;
import com.optical.modules.patient.entity.Patient;
import com.optical.modules.patient.repository.PatientRepository;
import com.optical.modules.prescription.entity.Prescription;
import com.optical.modules.prescription.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.optical.common.util.StringNormalizer.normalize;

@Service
@RequiredArgsConstructor
public class LegacyCustomerPrescriptionImportService {

    private static final String LEGACY_SOURCE = "hashvouv_saffiyahpos";
    private static final Pattern INSERT_PATTERN_TEMPLATE = Pattern.compile(
            "INSERT INTO `%s` \\((.*?)\\) VALUES\\s*(.*?);",
            Pattern.DOTALL | Pattern.CASE_INSENSITIVE
    );
    private static final DateTimeFormatter LEGACY_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CustomerRepository customerRepository;
    private final PatientRepository patientRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public LegacyCustomerPrescriptionImportResponse importFromSqlDump(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SQL dump file is required");
        }

        return importFromSqlContent(file.getOriginalFilename(), readFile(file), null);
    }

    @Transactional
    public LegacyCustomerPrescriptionImportResponse importFromSqlDump(
            String sourceFileName,
            byte[] fileContent,
            ImportProgressListener progressListener
    ) {
        if (fileContent == null || fileContent.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SQL dump file is required");
        }

        return importFromSqlContent(sourceFileName, new String(fileContent, StandardCharsets.UTF_8), progressListener);
    }

    private LegacyCustomerPrescriptionImportResponse importFromSqlContent(
            String sourceFileName,
            String sql,
            ImportProgressListener progressListener
    ) {
        if (progressListener != null) {
            progressListener.onStarted();
        }

        Map<Long, LegacyCustomer> legacyCustomers = parseCustomers(sql);
        Map<Long, LegacyCustomerBill> legacyBills = parseCustomerBills(sql);
        List<LegacyPrescription> legacyPrescriptions = parsePrescriptions(sql);

        ImportCounters counters = new ImportCounters();
        Map<Long, Customer> customersByLegacyId = new HashMap<>();

        legacyCustomers.values().stream()
                .sorted(Comparator.comparing(LegacyCustomer::id))
                .forEach(legacyCustomer -> {
                    Customer customer = upsertCustomer(legacyCustomer, counters);
                    customersByLegacyId.put(legacyCustomer.id(), customer);
                    publishProgress(sourceFileName, counters, progressListener);
                });

        legacyPrescriptions.stream()
                .sorted(Comparator.comparing(LegacyPrescription::id))
                .forEach(legacyPrescription -> importPrescription(
                        legacyPrescription,
                        legacyBills,
                        customersByLegacyId,
                        counters,
                        sourceFileName,
                        progressListener
                ));

        LegacyCustomerPrescriptionImportResponse response = buildResponse(sourceFileName, counters);
        if (progressListener != null) {
            progressListener.onCompleted(response);
        }
        return response;
    }

    private LegacyCustomerPrescriptionImportResponse buildResponse(
            String sourceFileName,
            ImportCounters counters
    ) {
        return LegacyCustomerPrescriptionImportResponse.builder()
                .sourceFileName(sourceFileName)
                .customersProcessed(counters.customersProcessed)
                .customersCreated(counters.customersCreated)
                .customersUpdated(counters.customersUpdated)
                .patientsCreated(counters.patientsCreated)
                .patientsUpdated(counters.patientsUpdated)
                .prescriptionsProcessed(counters.prescriptionsProcessed)
                .prescriptionsCreated(counters.prescriptionsCreated)
                .prescriptionsSkipped(counters.prescriptionsSkipped)
                .build();
    }

    private void importPrescription(
            LegacyPrescription legacyPrescription,
            Map<Long, LegacyCustomerBill> legacyBills,
            Map<Long, Customer> customersByLegacyId,
            ImportCounters counters,
            String sourceFileName,
            ImportProgressListener progressListener
    ) {
        counters.prescriptionsProcessed++;

        LegacyCustomerBill legacyBill = legacyBills.get(legacyPrescription.customerBillId());
        if (legacyBill == null) {
            counters.prescriptionsSkipped++;
            publishProgress(sourceFileName, counters, progressListener);
            return;
        }

        Customer customer = customersByLegacyId.get(legacyBill.customerId());
        if (customer == null) {
            counters.prescriptionsSkipped++;
            publishProgress(sourceFileName, counters, progressListener);
            return;
        }

        Patient patient = findOrCreatePatient(customer, legacyPrescription, counters);
        String importNote = buildPrescriptionImportNote(legacyPrescription);
        if (prescriptionRepository.findFirstByNotesAndDeletedAtIsNull(importNote).isPresent()) {
            counters.prescriptionsSkipped++;
            publishProgress(sourceFileName, counters, progressListener);
            return;
        }

        Prescription prescription = new Prescription();
        prescription.setPatient(patient);
        prescription.setPrescriptionDate(resolvePrescriptionDate(legacyPrescription, legacyBill));
        prescription.setValues(buildPrescriptionValues(legacyPrescription, legacyBill, customer));
        prescription.setNotes(importNote);
        prescriptionRepository.save(prescription);
        counters.prescriptionsCreated++;
        publishProgress(sourceFileName, counters, progressListener);
    }

    private Customer upsertCustomer(LegacyCustomer legacyCustomer, ImportCounters counters) {
        counters.customersProcessed++;

        String normalizedPhone = normalizePhone(legacyCustomer.number());
        String normalizedName = normalize(legacyCustomer.name());
        LocalDate dob = parseDate(legacyCustomer.dob());

        Optional<Customer> existing = Optional.empty();
        if (normalizedPhone != null) {
            existing = customerRepository.findFirstByPhoneAndDeletedAtIsNull(normalizedPhone);
        }
        if (existing.isEmpty() && normalizedName != null && dob != null) {
            existing = customerRepository.findFirstByNameIgnoreCaseAndDobAndDeletedAtIsNull(normalizedName, dob);
        }
        if (existing.isEmpty() && normalizedName != null) {
            existing = customerRepository.findFirstByNameIgnoreCaseAndDeletedAtIsNull(normalizedName);
        }

        BigDecimal pendingAmount = parseDecimal(legacyCustomer.receivables());
        if (existing.isPresent()) {
            Customer customer = existing.get();
            boolean changed = false;

            changed |= setIfBlank(() -> customer.getName(), customer::setName, normalizedName);
            changed |= setIfBlank(() -> customer.getPhone(), customer::setPhone, normalizedPhone);
            changed |= setIfBlank(() -> customer.getAddress(), customer::setAddress, normalize(legacyCustomer.address()));
            changed |= setIfBlank(() -> customer.getGender(), customer::setGender, normalize(legacyCustomer.gender()));
            changed |= setIfNull(customer.getDob(), customer::setDob, dob);

            BigDecimal currentPending = customer.getPendingAmount() == null ? BigDecimal.ZERO : customer.getPendingAmount();
            if (pendingAmount.compareTo(currentPending) > 0) {
                customer.setPendingAmount(pendingAmount);
                changed = true;
            }

            if (changed) {
                customerRepository.save(customer);
                counters.customersUpdated++;
            }
            return customer;
        }

        Customer customer = new Customer();
        customer.setName(normalizedName == null ? "LEGACY-CUSTOMER-" + legacyCustomer.id() : normalizedName);
        customer.setPhone(normalizedPhone);
        customer.setAddress(normalize(legacyCustomer.address()));
        customer.setGender(normalize(legacyCustomer.gender()));
        customer.setDob(dob);
        customer.setPendingAmount(pendingAmount);
        customer.setNotes("Legacy import from " + LEGACY_SOURCE + " customerId=" + legacyCustomer.id());
        counters.customersCreated++;
        return customerRepository.save(customer);
    }

    private Patient findOrCreatePatient(Customer customer, LegacyPrescription legacyPrescription, ImportCounters counters) {
        String patientName = normalize(legacyPrescription.patientName());
        String resolvedName = patientName == null ? customer.getName() : patientName;
        Optional<Patient> existing = patientRepository.findFirstByCustomerIdAndNameIgnoreCase(customer.getId(), resolvedName);

        if (existing.isPresent()) {
            Patient patient = existing.get();
            boolean changed = false;
            changed |= setIfBlank(() -> patient.getGender(), patient::setGender, normalize(legacyPrescription.gender()));
            if (isSamePerson(resolvedName, customer.getName())) {
                changed |= setIfNull(patient.getDob(), patient::setDob, customer.getDob());
            }
            if (changed) {
                patientRepository.save(patient);
                counters.patientsUpdated++;
            }
            return patient;
        }

        Patient patient = new Patient();
        patient.setCustomer(customer);
        patient.setName(resolvedName);
        patient.setGender(normalize(legacyPrescription.gender()));
        if (isSamePerson(resolvedName, customer.getName())) {
            patient.setDob(customer.getDob());
        }
        patient.setNotes("Legacy import from " + LEGACY_SOURCE + " patientName=" + resolvedName);
        counters.patientsCreated++;
        return patientRepository.save(patient);
    }

    private ObjectNode buildPrescriptionValues(
            LegacyPrescription legacyPrescription,
            LegacyCustomerBill legacyBill,
            Customer customer
    ) {
        ObjectNode values = objectMapper.createObjectNode();
        values.put("legacySource", LEGACY_SOURCE);
        values.put("legacyPrescriptionId", legacyPrescription.id());
        values.put("legacyCustomerBillId", legacyPrescription.customerBillId());
        values.put("legacyCustomerId", legacyBill.customerId());
        values.put("patientName", defaultString(legacyPrescription.patientName(), customer.getName()));
        putIfPresent(values, "age", parseInteger(legacyPrescription.fields().get("age")));
        putIfPresent(values, "gender", normalize(legacyPrescription.gender()));
        putIfPresent(values, "billDate", legacyBill.date());
        putIfPresent(values, "billCreatedAt", legacyBill.createdAt());

        values.set("right", buildEyeNode("r", legacyPrescription.fields()));
        values.set("left", buildEyeNode("l", legacyPrescription.fields()));
        values.set("pd", buildPdNode(legacyPrescription.fields()));
        values.set("legacyRaw", buildLegacyRawNode(legacyPrescription.fields()));
        return values;
    }

    private ObjectNode buildEyeNode(String eyePrefix, Map<String, String> fields) {
        ObjectNode eye = objectMapper.createObjectNode();
        eye.set("distance", buildVisionSection(eyePrefix + "_d", fields));
        eye.set("near", buildVisionSection(eyePrefix + "_n", fields));
        eye.set("contactLens", buildVisionSection(eyePrefix + "_cl", fields));

        putIfPresent(eye, "add", buildPowerValueNode(
                fields.get(eyePrefix + "_add1"),
                fields.get(eyePrefix + "_add2"),
                fields.get(eyePrefix + "_add_type")
        ));
        putIfPresent(eye, "ph", normalize(fields.get(eyePrefix + "_ph")));
        putIfPresent(eye, "va", normalize(fields.get(eyePrefix + "_va")));
        return eye;
    }

    private ObjectNode buildVisionSection(String sectionPrefix, Map<String, String> fields) {
        ObjectNode section = objectMapper.createObjectNode();
        putIfPresent(section, "sph", buildPowerValueNode(
                fields.get(sectionPrefix + "_sph1"),
                fields.get(sectionPrefix + "_sph2"),
                fields.get(sectionPrefix + "_sph_type")
        ));
        putIfPresent(section, "cyl", buildPowerValueNode(
                fields.get(sectionPrefix + "_cyl1"),
                fields.get(sectionPrefix + "_cyl2"),
                fields.get(sectionPrefix + "_cyl_type")
        ));
        putIfPresent(section, "axis", normalize(fields.get(sectionPrefix + "_axis")));
        putIfPresent(section, "va", normalize(fields.get(sectionPrefix + "_va")));
        return section;
    }

    private ObjectNode buildPdNode(Map<String, String> fields) {
        ObjectNode pd = objectMapper.createObjectNode();
        putIfPresent(pd, "right1", normalize(fields.get("r_pd1")));
        putIfPresent(pd, "right2", normalize(fields.get("r_pd2")));
        putIfPresent(pd, "left1", normalize(fields.get("l_pd1")));
        putIfPresent(pd, "left2", normalize(fields.get("l_pd2")));
        return pd;
    }

    private ObjectNode buildLegacyRawNode(Map<String, String> fields) {
        ObjectNode raw = objectMapper.createObjectNode();
        fields.forEach((key, value) -> putIfPresent(raw, key, normalize(value)));
        return raw;
    }

    private ObjectNode buildPowerValueNode(String majorPart, String minorPart, String typeValue) {
        String normalizedMajor = normalizeNumericPiece(majorPart);
        String normalizedMinor = normalizeNumericPiece(minorPart);
        Integer type = parseInteger(typeValue);
        String combined = combineSignedPower(normalizedMajor, normalizedMinor, type);
        if (combined == null && type == null && normalizedMajor == null && normalizedMinor == null) {
            return null;
        }

        ObjectNode node = objectMapper.createObjectNode();
        putIfPresent(node, "major", normalizedMajor);
        putIfPresent(node, "minor", normalizedMinor);
        putIfPresent(node, "type", type);
        putIfPresent(node, "signedValue", combined);
        return node;
    }

    private String combineSignedPower(String majorPart, String minorPart, Integer type) {
        String major = majorPart == null ? "0" : majorPart;
        String minor = minorPart == null ? ".00" : minorPart;
        if (majorPart == null && minorPart == null) {
            return null;
        }

        String sign = switch (type == null ? 0 : type) {
            case 1 -> "+";
            case 2 -> "-";
            default -> "";
        };
        if (!minor.startsWith(".")) {
            minor = "." + minor;
        }
        return sign + major + minor;
    }

    private LocalDate resolvePrescriptionDate(LegacyPrescription legacyPrescription, LegacyCustomerBill legacyBill) {
        LocalDate billDate = parseDate(legacyBill.date());
        if (billDate != null) {
            return billDate;
        }

        LocalDateTime createdAt = parseDateTime(legacyPrescription.fields().get("createdAt"));
        if (createdAt != null) {
            return createdAt.toLocalDate();
        }

        LocalDateTime billCreatedAt = parseDateTime(legacyBill.createdAt());
        if (billCreatedAt != null) {
            return billCreatedAt.toLocalDate();
        }

        return LocalDate.now();
    }

    private String buildPrescriptionImportNote(LegacyPrescription legacyPrescription) {
        return "Legacy import from " + LEGACY_SOURCE
                + " prescriptionId=" + legacyPrescription.id()
                + ", customerBillId=" + legacyPrescription.customerBillId();
    }

    private Map<Long, LegacyCustomer> parseCustomers(String sql) {
        return parseRows(sql, "customers").stream()
                .map(this::toLegacyCustomer)
                .collect(LinkedHashMap::new, (map, customer) -> map.put(customer.id(), customer), Map::putAll);
    }

    private Map<Long, LegacyCustomerBill> parseCustomerBills(String sql) {
        return parseRows(sql, "customerBills").stream()
                .map(this::toLegacyCustomerBill)
                .collect(LinkedHashMap::new, (map, bill) -> map.put(bill.id(), bill), Map::putAll);
    }

    private List<LegacyPrescription> parsePrescriptions(String sql) {
        return parseRows(sql, "prescriptions").stream()
                .map(this::toLegacyPrescription)
                .toList();
    }

    private List<Map<String, String>> parseRows(String sql, String tableName) {
        Pattern pattern = Pattern.compile(
                String.format(Locale.ROOT, INSERT_PATTERN_TEMPLATE.pattern(), Pattern.quote(tableName)),
                INSERT_PATTERN_TEMPLATE.flags()
        );
        Matcher matcher = pattern.matcher(sql);
        List<Map<String, String>> rows = new ArrayList<>();

        while (matcher.find()) {
            List<String> columns = parseColumns(matcher.group(1));
            List<List<String>> tuples = parseTuples(matcher.group(2));
            for (List<String> tuple : tuples) {
                if (tuple.size() != columns.size()) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Legacy dump has mismatched column count for table " + tableName
                    );
                }
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < columns.size(); i++) {
                    row.put(columns.get(i), tuple.get(i));
                }
                rows.add(row);
            }
        }

        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No rows found for legacy table " + tableName);
        }
        return rows;
    }

    private List<String> parseColumns(String columnsSql) {
        Matcher matcher = Pattern.compile("`([^`]+)`").matcher(columnsSql);
        List<String> columns = new ArrayList<>();
        while (matcher.find()) {
            columns.add(matcher.group(1));
        }
        return columns;
    }

    private List<List<String>> parseTuples(String valuesSql) {
        List<List<String>> tuples = new ArrayList<>();
        StringBuilder currentTuple = null;
        boolean inString = false;
        boolean escaping = false;

        for (int i = 0; i < valuesSql.length(); i++) {
            char current = valuesSql.charAt(i);
            if (currentTuple == null) {
                if (current == '(') {
                    currentTuple = new StringBuilder();
                }
                continue;
            }

            if (inString) {
                currentTuple.append(current);
                if (escaping) {
                    escaping = false;
                } else if (current == '\\') {
                    escaping = true;
                } else if (current == '\'' && i + 1 < valuesSql.length() && valuesSql.charAt(i + 1) == '\'') {
                    currentTuple.append(valuesSql.charAt(++i));
                } else if (current == '\'') {
                    inString = false;
                }
                continue;
            }

            if (current == '\'') {
                inString = true;
                currentTuple.append(current);
                continue;
            }

            if (current == ')') {
                tuples.add(parseTupleValues(currentTuple.toString()));
                currentTuple = null;
                continue;
            }

            currentTuple.append(current);
        }

        return tuples;
    }

    private List<String> parseTupleValues(String tupleSql) {
        List<String> values = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        boolean inString = false;
        boolean escaping = false;

        for (int i = 0; i < tupleSql.length(); i++) {
            char current = tupleSql.charAt(i);
            if (inString) {
                token.append(current);
                if (escaping) {
                    escaping = false;
                } else if (current == '\\') {
                    escaping = true;
                } else if (current == '\'' && i + 1 < tupleSql.length() && tupleSql.charAt(i + 1) == '\'') {
                    token.append(tupleSql.charAt(++i));
                } else if (current == '\'') {
                    inString = false;
                }
                continue;
            }

            if (current == '\'') {
                inString = true;
                token.append(current);
                continue;
            }

            if (current == ',') {
                values.add(parseValueToken(token.toString()));
                token.setLength(0);
                continue;
            }

            token.append(current);
        }

        values.add(parseValueToken(token.toString()));
        return values;
    }

    private String parseValueToken(String token) {
        String trimmed = token.trim();
        if (trimmed.equalsIgnoreCase("NULL")) {
            return null;
        }
        if (trimmed.length() >= 2 && trimmed.startsWith("'") && trimmed.endsWith("'")) {
            String unquoted = trimmed.substring(1, trimmed.length() - 1);
            return unquoted
                    .replace("\\\\", "\\")
                    .replace("\\'", "'")
                    .replace("''", "'")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t");
        }
        return trimmed;
    }

    private LegacyCustomer toLegacyCustomer(Map<String, String> row) {
        return new LegacyCustomer(
                parseLong(row.get("id")),
                row.get("name"),
                row.get("number"),
                row.get("address"),
                row.get("gender"),
                row.get("dob"),
                row.get("receivables")
        );
    }

    private LegacyCustomerBill toLegacyCustomerBill(Map<String, String> row) {
        return new LegacyCustomerBill(
                parseLong(row.get("id")),
                parseLong(row.get("customerId")),
                row.get("date"),
                row.get("createdAt")
        );
    }

    private LegacyPrescription toLegacyPrescription(Map<String, String> row) {
        return new LegacyPrescription(
                parseLong(row.get("id")),
                row.get("patient_name"),
                row.get("gender"),
                parseLong(row.get("customerBillId")),
                row
        );
    }

    private String readFile(MultipartFile file) {
        try {
            return new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to read uploaded SQL dump", ex);
        }
    }

    private void publishProgress(
            String sourceFileName,
            ImportCounters counters,
            ImportProgressListener progressListener
    ) {
        if (progressListener != null) {
            progressListener.onProgress(buildResponse(sourceFileName, counters));
        }
    }

    private Long parseLong(String value) {
        String normalizedValue = normalize(value);
        return normalizedValue == null ? null : Long.parseLong(normalizedValue);
    }

    private Integer parseInteger(String value) {
        String normalizedValue = normalize(value);
        if (normalizedValue == null) {
            return null;
        }
        try {
            return Integer.parseInt(normalizedValue);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal parseDecimal(String value) {
        String normalizedValue = normalize(value);
        if (normalizedValue == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(normalizedValue).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            return BigDecimal.ZERO;
        }
    }

    private LocalDate parseDate(String value) {
        String normalizedValue = normalize(value);
        if (normalizedValue == null || normalizedValue.equals("0000-00-00")) {
            return null;
        }
        try {
            return LocalDate.parse(normalizedValue);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(String value) {
        String normalizedValue = normalize(value);
        if (normalizedValue == null || normalizedValue.equals("0000-00-00 00:00:00")) {
            return null;
        }
        try {
            return LocalDateTime.parse(normalizedValue, LEGACY_DATE_TIME);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private String normalizePhone(String value) {
        String normalizedValue = normalize(value);
        if (normalizedValue == null || normalizedValue.equals("0")) {
            return null;
        }
        return normalizedValue;
    }

    private String normalizeNumericPiece(String value) {
        String normalizedValue = normalize(value);
        if (normalizedValue == null) {
            return null;
        }
        if (normalizedValue.equals("0") || normalizedValue.equals("0.0") || normalizedValue.equals("0.00")) {
            return normalizedValue.startsWith("0.") ? normalizedValue : "0";
        }
        return normalizedValue;
    }

    private boolean isSamePerson(String left, String right) {
        String normalizedLeft = normalize(left);
        String normalizedRight = normalize(right);
        return normalizedLeft != null && normalizedLeft.equalsIgnoreCase(normalizedRight);
    }

    private String defaultString(String primary, String fallback) {
        String normalizedPrimary = normalize(primary);
        return normalizedPrimary == null ? normalize(fallback) : normalizedPrimary;
    }

    private void putIfPresent(ObjectNode node, String fieldName, String value) {
        String normalizedValue = normalize(value);
        if (normalizedValue != null) {
            node.put(fieldName, normalizedValue);
        }
    }

    private void putIfPresent(ObjectNode node, String fieldName, Integer value) {
        if (value != null) {
            node.put(fieldName, value);
        }
    }

    private void putIfPresent(ObjectNode node, String fieldName, LocalDate value) {
        if (value != null) {
            node.put(fieldName, value.toString());
        }
    }

    private void putIfPresent(ObjectNode node, String fieldName, LocalDateTime value) {
        if (value != null) {
            node.put(fieldName, value.toString());
        }
    }

    private void putIfPresent(ObjectNode node, String fieldName, ObjectNode value) {
        if (value != null && value.size() > 0) {
            node.set(fieldName, value);
        }
    }

    private boolean setIfBlank(ValueSupplier<String> getter, ValueConsumer<String> setter, String incomingValue) {
        String normalizedIncoming = normalize(incomingValue);
        if (normalizedIncoming == null) {
            return false;
        }
        String currentValue = normalize(getter.get());
        if (currentValue == null) {
            setter.accept(normalizedIncoming);
            return true;
        }
        return false;
    }

    private boolean setIfNull(LocalDate currentValue, ValueConsumer<LocalDate> setter, LocalDate incomingValue) {
        if (currentValue == null && incomingValue != null) {
            setter.accept(incomingValue);
            return true;
        }
        return false;
    }

    @FunctionalInterface
    private interface ValueSupplier<T> {
        T get();
    }

    @FunctionalInterface
    private interface ValueConsumer<T> {
        void accept(T value);
    }

    private static final class ImportCounters {
        private long customersProcessed;
        private long customersCreated;
        private long customersUpdated;
        private long patientsCreated;
        private long patientsUpdated;
        private long prescriptionsProcessed;
        private long prescriptionsCreated;
        private long prescriptionsSkipped;
    }

    private record LegacyCustomer(
            Long id,
            String name,
            String number,
            String address,
            String gender,
            String dob,
            String receivables
    ) {
    }

    private record LegacyCustomerBill(
            Long id,
            Long customerId,
            String date,
            String createdAt
    ) {
    }

    private record LegacyPrescription(
            Long id,
            String patientName,
            String gender,
            Long customerBillId,
            Map<String, String> fields
    ) {
    }

    public interface ImportProgressListener {
        default void onStarted() {
        }

        default void onProgress(LegacyCustomerPrescriptionImportResponse progress) {
        }

        default void onCompleted(LegacyCustomerPrescriptionImportResponse result) {
        }
    }
}
