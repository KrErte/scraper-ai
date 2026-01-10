package ee.jobs.aggregator.controller;

import ee.jobs.aggregator.dto.CompanyDto;
import ee.jobs.aggregator.service.CompanyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Ettevõtete REST kontroller.
 * Pakub API otspunkte ettevõtete andmete jaoks.
 */
@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    private static final Logger log = LoggerFactory.getLogger(CompanyController.class);

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    /**
     * GET /api/companies
     * Tagasta ettevõtete nimekiri (sorteerituna tööpakkumiste arvu järgi).
     */
    @GetMapping
    public ResponseEntity<Page<CompanyDto>> getCompanies(
            @PageableDefault(size = 20, sort = "jobCount", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        log.info("GET /api/companies - page={}", pageable.getPageNumber());

        Page<CompanyDto> companies = companyService.findActive(pageable);
        return ResponseEntity.ok(companies);
    }
}
