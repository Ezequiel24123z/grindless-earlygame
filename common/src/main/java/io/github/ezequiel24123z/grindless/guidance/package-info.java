/**
 * Prepared player guidance under ADR-0143.
 *
 * <p>{@code ResourceGuidanceProjection} is a tool-only reader of the same canonical Result
 * as the dashboard; {@code AuditReports.relationships} supplies its existing tag-aware
 * producer/consumer provenance. Null route tiers remain unassessed. Nothing here assigns
 * classifications, a recipe's output roles, an entry kit or a world/Tier verdict.
 *
 * <p>Run {@code tools/generate-resource-guidance.ps1 -Root .} (or the .sh twin) to regenerate
 * the packaged asset from real source/pack binding. {@code -CheckOnly} rejects drift;
 * {@code VerifySustainabilityT0} checks the same bytes in its already evaluated Result.
 * Generation reuses the dashboard's Gradle-resolved tool runner, never a dependency of
 * {@code classes}, and requires a separately granted JVM window on the shared host.
 *
 * <p>The installed client loads only the versioned classpath asset at setup. The native
 * presenter reads that immutable cache and current Atlas/recipe objects; hover never
 * runs the repository/JDK audit. Missing or invalid metadata remains explicitly unavailable.
 */
package io.github.ezequiel24123z.grindless.guidance;
