package ee.jobs.aggregator.controller;

import ee.jobs.aggregator.dto.JobPostingDto;
import ee.jobs.aggregator.service.JobService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * Tööpakkumiste REST kontroller.
 * Pakub API otspunkte tööpakkumiste otsimiseks ja kuvamiseks.
 */
@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private static final Logger log = LoggerFactory.getLogger(JobController.class);

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    /**
     * GET /api/jobs
     * Tagasta tööpakkumiste nimekiri filtritega.
     *
     * @param skill    Filtreeri oskuse järgi
     * @param location Filtreeri asukoha järgi
     * @param salaryMin Minimaalne palk
     * @param pageable Pagineerimise parameetrid
     */
    @GetMapping
    public ResponseEntity<Page<JobPostingDto>> getJobs(
            @RequestParam(required = false) String skill,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) BigDecimal salaryMin,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        log.info("GET /api/jobs - skill={}, location={}, salaryMin={}, page={}",
                skill, location, salaryMin, pageable.getPageNumber());

        Page<JobPostingDto> jobs = jobService.findJobs(skill, location, salaryMin, pageable);
        return ResponseEntity.ok(jobs);
    }

    /**
     * GET /api/jobs/{id}
     * Tagasta üksiku tööpakkumise detailid.
     *
     * @param id Tööpakkumise ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<JobPostingDto> getJob(@PathVariable Long id) {
        log.info("GET /api/jobs/{}", id);

        JobPostingDto job = jobService.findById(id);
        return ResponseEntity.ok(job);
    }
}
