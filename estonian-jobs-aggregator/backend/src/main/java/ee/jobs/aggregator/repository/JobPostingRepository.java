package ee.jobs.aggregator.repository;

import ee.jobs.aggregator.entity.JobPosting;
import ee.jobs.aggregator.entity.JobSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Tööpakkumiste repositoorium.
 * Sisaldab keerukamaid päringuid filtreerimiseks ja statistikaks.
 */
@Repository
public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {

    // Leia tööpakkumine URL-i järgi (upsert kontrolliks)
    Optional<JobPosting> findByUrl(String url);

    // Kontrolli, kas URL eksisteerib
    boolean existsByUrl(String url);

    // Filtreeritav tööpakkumiste otsing
    @Query("""
        SELECT DISTINCT j FROM JobPosting j
        LEFT JOIN j.skills s
        WHERE (:skill IS NULL OR LOWER(s.name) = LOWER(:skill))
        AND (:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%')))
        AND (:salaryMin IS NULL OR j.salaryMin >= :salaryMin OR j.salaryMax >= :salaryMin)
        ORDER BY j.postedDate DESC
        """)
    Page<JobPosting> findByFilters(
        @Param("skill") String skill,
        @Param("location") String location,
        @Param("salaryMin") BigDecimal salaryMin,
        Pageable pageable
    );

    // Kuulutuste arv päevade kaupa (trendid)
    @Query("""
        SELECT j.postedDate as date, COUNT(j) as count
        FROM JobPosting j
        WHERE j.postedDate >= :startDate
        GROUP BY j.postedDate
        ORDER BY j.postedDate ASC
        """)
    List<Object[]> countByPostedDateAfter(@Param("startDate") LocalDate startDate);

    // Kuulutuste arv allika järgi
    @Query("SELECT j.source, COUNT(j) FROM JobPosting j GROUP BY j.source")
    List<Object[]> countBySource();

    // Tööpakkumised ettevõtte nime järgi
    Page<JobPosting> findByCompanyIgnoreCase(String company, Pageable pageable);

    // Tööpakkumised allika järgi
    Page<JobPosting> findBySource(JobSource source, Pageable pageable);

    // Viimased N tööpakkumist
    List<JobPosting> findTop10ByOrderByScrapedAtDesc();

    // Kustuta aegunud kuulutused
    void deleteByExpiresAtBefore(LocalDate date);

    // Palgastatistika (keskmine, min, max) oskuse järgi
    @Query("""
        SELECT
            AVG((COALESCE(j.salaryMin, 0) + COALESCE(j.salaryMax, 0)) / 2) as avgSalary,
            MIN(j.salaryMin) as minSalary,
            MAX(j.salaryMax) as maxSalary,
            COUNT(j) as jobCount
        FROM JobPosting j
        JOIN j.skills s
        WHERE LOWER(s.name) = LOWER(:skillName)
        AND (j.salaryMin IS NOT NULL OR j.salaryMax IS NOT NULL)
        """)
    List<Object[]> getSalaryStatsBySkill(@Param("skillName") String skillName);

    // Üldine palgastatistika
    @Query("""
        SELECT
            AVG((COALESCE(j.salaryMin, 0) + COALESCE(j.salaryMax, 0)) / 2) as avgSalary,
            MIN(j.salaryMin) as minSalary,
            MAX(j.salaryMax) as maxSalary,
            COUNT(j) as jobCount
        FROM JobPosting j
        WHERE j.salaryMin IS NOT NULL OR j.salaryMax IS NOT NULL
        """)
    List<Object[]> getOverallSalaryStats();
}
