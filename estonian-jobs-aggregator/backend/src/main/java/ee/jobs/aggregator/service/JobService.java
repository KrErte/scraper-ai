package ee.jobs.aggregator.service;

import ee.jobs.aggregator.dto.JobPostingDto;
import ee.jobs.aggregator.entity.JobPosting;
import ee.jobs.aggregator.exception.ResourceNotFoundException;
import ee.jobs.aggregator.repository.JobPostingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Tööpakkumiste teenus.
 * Haldab tööpakkumiste otsimist ja kuvamist.
 */
@Service
@Transactional(readOnly = true)
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    private final JobPostingRepository jobPostingRepository;

    public JobService(JobPostingRepository jobPostingRepository) {
        this.jobPostingRepository = jobPostingRepository;
    }

    /**
     * Leia tööpakkumised filtritega.
     */
    public Page<JobPostingDto> findJobs(
            String skill,
            String location,
            BigDecimal salaryMin,
            Pageable pageable
    ) {
        log.debug("Otsimine: skill={}, location={}, salaryMin={}", skill, location, salaryMin);

        Page<JobPosting> jobs = jobPostingRepository.findByFilters(
            skill,
            location,
            salaryMin,
            pageable
        );

        return jobs.map(JobPostingDto::fromEntityShort);
    }

    /**
     * Leia tööpakkumine ID järgi.
     */
    public JobPostingDto findById(Long id) {
        log.debug("Otsimine ID järgi: {}", id);

        JobPosting job = jobPostingRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tööpakkumine", "id", id));

        return JobPostingDto.fromEntity(job);
    }

    /**
     * Loe tööpakkumiste koguarv.
     */
    public long getTotalCount() {
        return jobPostingRepository.count();
    }
}
