package ee.jobs.aggregator.service;

import ee.jobs.aggregator.dto.CompanyDto;
import ee.jobs.aggregator.entity.Company;
import ee.jobs.aggregator.repository.CompanyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Ettevõtete teenus.
 * Haldab ettevõtete andmeid ja statistikat.
 */
@Service
@Transactional(readOnly = true)
public class CompanyService {

    private static final Logger log = LoggerFactory.getLogger(CompanyService.class);

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    /**
     * Leia kõik ettevõtted (pagineeritud).
     */
    public Page<CompanyDto> findAll(Pageable pageable) {
        log.debug("Ettevõtete pärimine");

        return companyRepository.findAllByOrderByJobCountDesc(pageable)
            .map(CompanyDto::fromEntity);
    }

    /**
     * Leia aktiivsed ettevõtted (kellel on tööpakkumisi).
     */
    public Page<CompanyDto> findActive(Pageable pageable) {
        log.debug("Aktiivsete ettevõtete pärimine");

        return companyRepository.findActiveCompanies(pageable)
            .map(CompanyDto::fromEntity);
    }

    /**
     * Loo või uuenda ettevõte.
     * Kasutab scraper tööpakkumise salvestamisel.
     */
    @Transactional
    public Company getOrCreateCompany(String name) {
        Optional<Company> existing = companyRepository.findByNameIgnoreCase(name);

        if (existing.isPresent()) {
            return existing.get();
        }

        Company company = new Company(name);
        log.info("Loodud uus ettevõte: {}", name);
        return companyRepository.save(company);
    }

    /**
     * Suurenda ettevõtte tööpakkumiste arvu.
     */
    @Transactional
    public void incrementJobCount(String companyName) {
        companyRepository.findByNameIgnoreCase(companyName)
            .ifPresent(company -> {
                company.incrementJobCount();
                companyRepository.save(company);
            });
    }

    /**
     * Vähenda ettevõtte tööpakkumiste arvu.
     */
    @Transactional
    public void decrementJobCount(String companyName) {
        companyRepository.findByNameIgnoreCase(companyName)
            .ifPresent(company -> {
                company.decrementJobCount();
                companyRepository.save(company);
            });
    }
}
