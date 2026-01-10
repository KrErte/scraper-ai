package ee.jobs.aggregator.repository;

import ee.jobs.aggregator.entity.Company;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Ettevõtete repositoorium.
 * Võimaldab hallata ettevõtteid ja nende statistikat.
 */
@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

    // Leia ettevõte nime järgi (case-insensitive)
    Optional<Company> findByNameIgnoreCase(String name);

    // Kontrolli, kas ettevõte eksisteerib
    boolean existsByNameIgnoreCase(String name);

    // Top ettevõtted tööpakkumiste arvu järgi
    List<Company> findTop20ByOrderByJobCountDesc();

    // Kõik ettevõtted sorteerituna
    Page<Company> findAllByOrderByJobCountDesc(Pageable pageable);

    // Ettevõtted, kellel on vähemalt üks tööpakkumine
    @Query("SELECT c FROM Company c WHERE c.jobCount > 0 ORDER BY c.jobCount DESC")
    Page<Company> findActiveCompanies(Pageable pageable);

    // Otsing nime järgi
    Page<Company> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
