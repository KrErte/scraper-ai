package ee.jobs.aggregator.dto;

import ee.jobs.aggregator.entity.Skill;
import ee.jobs.aggregator.entity.SkillCategory;

/**
 * Oskuse DTO.
 */
public record SkillDto(
    Long id,
    String name,
    SkillCategory category
) {
    /**
     * Teisenda entiteet DTO-ks.
     */
    public static SkillDto fromEntity(Skill entity) {
        return new SkillDto(
            entity.getId(),
            entity.getName(),
            entity.getCategory()
        );
    }
}
