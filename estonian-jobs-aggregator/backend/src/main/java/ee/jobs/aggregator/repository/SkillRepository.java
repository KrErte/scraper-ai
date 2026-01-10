package ee.jobs.aggregator.repository;

import ee.jobs.aggregator.entity.Skill;
import ee.jobs.aggregator.entity.SkillCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Oskuste repositoorium.
 * Võimaldab otsida ja statistikat koguda oskuste kohta.
 */
@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {

    // Leia oskus nime järgi (case-insensitive)
    Optional<Skill> findByNameIgnoreCase(String name);

    // Kontrolli, kas oskus eksisteerib
    boolean existsByNameIgnoreCase(String name);

    // Leia oskused kategooria järgi
    List<Skill> findByCategory(SkillCategory category);

    // Top N oskust tööpakkumiste arvu järgi
    @Query("""
        SELECT s.name, s.category, COUNT(j) as jobCount
        FROM Skill s
        JOIN s.jobPostings j
        GROUP BY s.id, s.name, s.category
        ORDER BY jobCount DESC
        LIMIT :limit
        """)
    List<Object[]> findTopSkillsByJobCount(@Param("limit") int limit);

    // Oskuste populaarsus ajas (trendid)
    @Query("""
        SELECT s.name, j.postedDate, COUNT(j) as count
        FROM Skill s
        JOIN s.jobPostings j
        WHERE j.postedDate >= :startDate
        AND s.name IN :skillNames
        GROUP BY s.name, j.postedDate
        ORDER BY j.postedDate ASC
        """)
    List<Object[]> getSkillTrends(
        @Param("startDate") LocalDate startDate,
        @Param("skillNames") List<String> skillNames
    );

    // Kõik oskused koos tööpakkumiste arvuga
    @Query("""
        SELECT s.name, s.category, COUNT(j) as jobCount
        FROM Skill s
        LEFT JOIN s.jobPostings j
        GROUP BY s.id, s.name, s.category
        ORDER BY s.name ASC
        """)
    List<Object[]> findAllWithJobCount();
}
