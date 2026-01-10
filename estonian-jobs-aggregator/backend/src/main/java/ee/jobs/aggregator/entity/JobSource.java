package ee.jobs.aggregator.entity;

/**
 * Tööpakkumise allika enum.
 * Määrab, millisest portaalist tööpakkumine pärineb.
 */
public enum JobSource {
    CV_EE("CV.ee"),
    CV_KESKUS("CV Keskus");

    private final String displayName;

    JobSource(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
