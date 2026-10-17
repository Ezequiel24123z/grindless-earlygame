package io.github.ezequiel24123z.grindless.sustainability;

import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/** A stable, exact, source-bound audit finding (ADR-0135). */
public record AuditFinding(String rule, String subject, String detail, Severity severity,
                           Disposition disposition, String message, List<String> sources)
        implements Comparable<AuditFinding> {
    /** Unknown is evidence debt, not an error or an implicit pass. */
    public enum Severity { ERROR, WARNING, UNKNOWN }

    /** Waived debt remains an error in every report. */
    public enum Disposition { ACTIVE, WAIVED_DEBT }

    public AuditFinding {
        SustainabilityModel.requireText(rule, "finding rule");
        SustainabilityModel.requireText(subject, "finding subject");
        SustainabilityModel.requireText(message, "finding message");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(disposition, "disposition");
        if (rule.contains(":") || rule.contains("*") || subject.contains("*")) {
            throw new IllegalArgumentException("Finding identities must be exact");
        }
        detail = detail == null ? "" : detail;
        sources = sources.stream().sorted().distinct().toList();
    }

    /** Creates an active finding without presentation state. */
    public AuditFinding(String rule, String subject, String detail, Severity severity,
                        String message, List<String> sources) {
        this(rule, subject, detail, severity, Disposition.ACTIVE, message, sources);
    }

    /** Returns the versioned debt identity, independent of wording and collection order. */
    public String fingerprint() {
        return rule + ":" + subject + (detail.isEmpty() ? "" : ":" + detail);
    }

    /** Marks this exact error as visible debt without changing its severity. */
    public AuditFinding waived() {
        return new AuditFinding(rule, subject, detail, severity, Disposition.WAIVED_DEBT, message, sources);
    }

    /** Returns the raw active finding; only the manifest may grant a waiver. */
    public AuditFinding active() {
        return new AuditFinding(rule, subject, detail, severity, Disposition.ACTIVE, message, sources);
    }

    static AuditFinding merge(AuditFinding left, AuditFinding right) {
        if (!left.rule.equals(right.rule) || !left.subject.equals(right.subject) || !left.detail.equals(right.detail)) {
            throw new IllegalArgumentException("Ambiguous finding fingerprint: " + left.fingerprint());
        }
        AuditFinding strongest = left.severity.ordinal() < right.severity.ordinal() ? left
                : right.severity.ordinal() < left.severity.ordinal() ? right
                : left.message.compareTo(right.message) <= 0 ? left : right;
        TreeSet<String> sources = new TreeSet<>(left.sources);
        sources.addAll(right.sources);
        return new AuditFinding(strongest.rule, strongest.subject, strongest.detail, strongest.severity,
                Disposition.ACTIVE, strongest.message, List.copyOf(sources));
    }

    @Override
    public int compareTo(AuditFinding other) {
        return fingerprint().compareTo(other.fingerprint());
    }
}
