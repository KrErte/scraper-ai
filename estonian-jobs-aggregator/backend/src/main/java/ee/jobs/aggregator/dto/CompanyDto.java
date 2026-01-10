package ee.jobs.aggregator.dto;

import ee.jobs.aggregator.entity.Company;

/**
 * Ettevõtte DTO.
 */
public record CompanyDto(
    Long id,
    String name,
    Integer jobCount
) {
    /**
     * Teisenda entiteet DTO-ks.
     */
    public static CompanyDto fromEntity(Company entity) {
        return new CompanyDto(
            entity.getId(),
            entity.getName(),
            entity.getJobCount()
        );
    }
}
