package eu.fasten.core.data.metadatadb.license;

/**
 * Where a certain licenses has been retrieved from.
 */
public enum DetectedLicenseSource {

    LOCAL_POM("Local pom file"),
    MAVEN_CENTRAL("Maven central"),
    PYPI("PyPi APIs"),
    DEBIAN_PACKAGES("Debian packages"),
    GITHUB("GitHub APIs");

    private final String description;

    DetectedLicenseSource(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return this.description;
    }
}
