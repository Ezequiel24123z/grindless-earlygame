package io.github.ezequiel24123z.grindless.sustainability;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Exact, versioned debt ratchet. A waiver affects the ratchet, never the sustainability verdict. */
public record DebtManifest(int schemaVersion, int version, Map<String, Entry> entries) {
    /** Every exception has a concrete origin, remediation and owning task. */
    public record Entry(String fingerprint, String origin, String remediation, String owningTask) {
        public Entry {
            SustainabilityModel.requireText(fingerprint, "debt fingerprint");
            SustainabilityModel.requireText(origin, "debt origin");
            SustainabilityModel.requireText(remediation, "debt remediation");
            SustainabilityModel.requireText(owningTask, "debt owning task");
            if (!fingerprint.contains(":") || fingerprint.contains("*")) {
                throw new IllegalArgumentException("Debt fingerprints must be exact rule:subject[:detail]");
            }
        }
    }

    /** Ratchet eligibility and failure-visible sustainability debt are intentionally separate. */
    public record Applied(List<AuditFinding> findings, boolean ratchetPass,
                          long activeErrors, long waivedDebt, long warnings, long unknowns) {
        /** Returns false while any error remains, including a waived one. */
        public boolean errorFree() {
            return activeErrors + waivedDebt == 0;
        }
    }

    public DebtManifest {
        if (schemaVersion != 1 || version < 1) {
            throw new IllegalArgumentException("Unsupported debt schema or version");
        }
        entries = SustainabilityModel.sortedMap(entries);
        entries.forEach((key, entry) -> {
            if (!key.equals(entry.fingerprint())) {
                throw new IllegalArgumentException("Debt entry key must equal its fingerprint");
            }
        });
    }

    /** Reads an explicit manifest; malformed or duplicate entries are not silently ignored. */
    public static DebtManifest read(Path path) throws IOException {
        JsonObject json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        Map<String, Entry> entries = new TreeMap<>();
        for (JsonElement element : json.getAsJsonArray("entries")) {
            JsonObject value = element.getAsJsonObject();
            Entry entry = new Entry(value.get("fingerprint").getAsString(), value.get("origin").getAsString(),
                    value.get("remediation").getAsString(), value.get("owning_task").getAsString());
            if (entries.putIfAbsent(entry.fingerprint(), entry) != null) {
                throw new IllegalArgumentException("Duplicate debt: " + entry.fingerprint());
            }
        }
        return new DebtManifest(json.get("schema_version").getAsInt(), json.get("version").getAsInt(), entries);
    }

    /** Returns a versioned empty manifest, not a blanket baseline waiver. */
    public static DebtManifest empty() {
        return new DebtManifest(1, 1, Map.of());
    }

    /** Applies exact error waivers and rejects entries which no longer match an error. */
    public Applied apply(List<AuditFinding> raw) {
        Map<String, AuditFinding> produced = new TreeMap<>();
        raw.forEach(finding -> produced.merge(finding.fingerprint(), finding.active(), AuditFinding::merge));
        Map<String, AuditFinding> original = Map.copyOf(produced);
        Map<String, AuditFinding> findings = new TreeMap<>(original);
        List<AuditFinding> staleFindings = new ArrayList<>();
        for (Entry entry : entries.values()) {
            AuditFinding finding = original.get(entry.fingerprint());
            if (finding == null || finding.severity() != AuditFinding.Severity.ERROR) {
                staleFindings.add(new AuditFinding("stale_debt", entry.fingerprint(), "",
                        AuditFinding.Severity.ERROR, "Debt no longer matches a produced error; remove the entry.",
                        List.of(entry.origin())));
            } else {
                findings.put(finding.fingerprint(), finding.waived());
            }
        }
        // Generated ratchet errors are added only after all original-error waiver decisions.
        staleFindings.forEach(stale -> findings.merge(stale.fingerprint(), stale, AuditFinding::merge));
        List<AuditFinding> ordered = new ArrayList<>(findings.values());
        long active = ordered.stream().filter(f -> f.severity() == AuditFinding.Severity.ERROR
                && f.disposition() == AuditFinding.Disposition.ACTIVE).count();
        long waived = ordered.stream().filter(f -> f.disposition() == AuditFinding.Disposition.WAIVED_DEBT).count();
        long warnings = ordered.stream().filter(f -> f.severity() == AuditFinding.Severity.WARNING).count();
        long unknowns = ordered.stream().filter(f -> f.severity() == AuditFinding.Severity.UNKNOWN).count();
        return new Applied(List.copyOf(ordered), active == 0, active, waived, warnings, unknowns);
    }
}
