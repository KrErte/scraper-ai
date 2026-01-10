package ee.jobs.aggregator.controller;

import ee.jobs.aggregator.dto.*;
import ee.jobs.aggregator.service.StatsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Statistika REST kontroller.
 * Pakub API otspunkte statistiliste andmete jaoks.
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private static final Logger log = LoggerFactory.getLogger(StatsController.class);

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    /**
     * GET /api/stats/skills
     * Tagasta top 20 nõutuimat oskust koos tööpakkumiste arvuga.
     */
    @GetMapping("/skills")
    public ResponseEntity<List<SkillStatsDto>> getTopSkills(
            @RequestParam(defaultValue = "20") int limit
    ) {
        log.info("GET /api/stats/skills - limit={}", limit);

        List<SkillStatsDto> skills = statsService.getTopSkills(Math.min(limit, 50));
        return ResponseEntity.ok(skills);
    }

    /**
     * GET /api/stats/salaries
     * Tagasta palgastatistika (valikuliselt oskuse järgi).
     *
     * @param skill Oskuse nimi (valikuline)
     */
    @GetMapping("/salaries")
    public ResponseEntity<SalaryStatsDto> getSalaryStats(
            @RequestParam(required = false) String skill
    ) {
        log.info("GET /api/stats/salaries - skill={}", skill);

        SalaryStatsDto stats = statsService.getSalaryStatsBySkill(skill);
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/stats/trends
     * Tagasta tööpakkumiste trend viimase N päeva jooksul.
     *
     * @param days Päevade arv (vaikimisi 30)
     */
    @GetMapping("/trends")
    public ResponseEntity<List<TrendDataDto>> getTrends(
            @RequestParam(defaultValue = "30") int days
    ) {
        log.info("GET /api/stats/trends - days={}", days);

        // Piira maksimaalselt 365 päevale
        List<TrendDataDto> trends = statsService.getJobTrends(Math.min(days, 365));
        return ResponseEntity.ok(trends);
    }

    /**
     * GET /api/stats/dashboard
     * Tagasta dashboard koondstatistika.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatsDto> getDashboardStats() {
        log.info("GET /api/stats/dashboard");

        DashboardStatsDto stats = statsService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }
}
