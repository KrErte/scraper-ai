package ee.jobs.aggregator.service;

import ee.jobs.aggregator.dto.*;
import ee.jobs.aggregator.repository.CompanyRepository;
import ee.jobs.aggregator.repository.JobPostingRepository;
import ee.jobs.aggregator.repository.SkillRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Statistika teenus.
 * Arvutab ja tagastab erinevaid statistilisi näitajaid.
 */
@Service
@Transactional(readOnly = true)
public class StatsService {

    private static final Logger log = LoggerFactory.getLogger(StatsService.class);

    private final JobPostingRepository jobPostingRepository;
    private final SkillRepository skillRepository;
    private final CompanyRepository companyRepository;

    public StatsService(
            JobPostingRepository jobPostingRepository,
            SkillRepository skillRepository,
            CompanyRepository companyRepository
    ) {
        this.jobPostingRepository = jobPostingRepository;
        this.skillRepository = skillRepository;
        this.companyRepository = companyRepository;
    }

    /**
     * Tagasta top N oskust koos tööpakkumiste arvuga.
     */
    public List<SkillStatsDto> getTopSkills(int limit) {
        log.debug("Pärimine top {} oskust", limit);

        return skillRepository.findTopSkillsByJobCount(limit)
            .stream()
            .map(SkillStatsDto::fromQueryResult)
            .toList();
    }

    /**
     * Tagasta palgastatistika oskuse järgi.
     */
    public SalaryStatsDto getSalaryStatsBySkill(String skillName) {
        log.debug("Palgastatistika oskusele: {}", skillName);

        if (skillName == null || skillName.isEmpty()) {
            // Üldine statistika
            List<Object[]> results = jobPostingRepository.getOverallSalaryStats();
            if (results.isEmpty() || results.get(0)[0] == null) {
                return new SalaryStatsDto(null, null, null, null, 0L);
            }
            return SalaryStatsDto.fromQueryResult(null, results.get(0));
        }

        List<Object[]> results = jobPostingRepository.getSalaryStatsBySkill(skillName);
        if (results.isEmpty() || results.get(0)[0] == null) {
            return new SalaryStatsDto(skillName, null, null, null, 0L);
        }

        return SalaryStatsDto.fromQueryResult(skillName, results.get(0));
    }

    /**
     * Tagasta tööpakkumiste trend viimase N päeva jooksul.
     */
    public List<TrendDataDto> getJobTrends(int days) {
        log.debug("Tööpakkumiste trend viimase {} päeva jooksul", days);

        LocalDate startDate = LocalDate.now().minusDays(days);

        return jobPostingRepository.countByPostedDateAfter(startDate)
            .stream()
            .map(TrendDataDto::fromQueryResult)
            .toList();
    }

    /**
     * Tagasta dashboard statistika.
     */
    public DashboardStatsDto getDashboardStats() {
        log.debug("Dashboard statistika pärimine");

        // Tööpakkumiste koguarv
        long totalJobs = jobPostingRepository.count();

        // Ettevõtete arv
        long totalCompanies = companyRepository.count();

        // Keskmine palk
        List<Object[]> salaryStats = jobPostingRepository.getOverallSalaryStats();
        BigDecimal avgSalary = null;
        if (!salaryStats.isEmpty() && salaryStats.get(0)[0] != null) {
            avgSalary = BigDecimal.valueOf(((Number) salaryStats.get(0)[0]).doubleValue());
        }

        // Top oskus
        String topSkill = null;
        List<Object[]> topSkills = skillRepository.findTopSkillsByJobCount(1);
        if (!topSkills.isEmpty()) {
            topSkill = (String) topSkills.get(0)[0];
        }

        // Tänased uued kuulutused
        LocalDate today = LocalDate.now();
        List<Object[]> todayTrends = jobPostingRepository.countByPostedDateAfter(today);
        long newJobsToday = 0;
        if (!todayTrends.isEmpty()) {
            for (Object[] row : todayTrends) {
                if (today.equals(row[0])) {
                    newJobsToday = ((Number) row[1]).longValue();
                    break;
                }
            }
        }

        return new DashboardStatsDto(
            totalJobs,
            totalCompanies,
            avgSalary,
            topSkill,
            newJobsToday
        );
    }
}
