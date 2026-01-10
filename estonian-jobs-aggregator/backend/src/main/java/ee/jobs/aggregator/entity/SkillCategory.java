package ee.jobs.aggregator.entity;

/**
 * Oskuse kategooria enum.
 * Liigitab oskused erinevatesse kategooriatesse.
 */
public enum SkillCategory {
    LANGUAGE("Programmeerimiskeel"),
    FRAMEWORK("Raamistik"),
    DATABASE("Andmebaas"),
    DEVOPS("DevOps"),
    SOFT_SKILL("Pehme oskus");

    private final String displayName;

    SkillCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
