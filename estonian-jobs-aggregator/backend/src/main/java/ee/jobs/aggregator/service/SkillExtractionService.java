package ee.jobs.aggregator.service;

import ee.jobs.aggregator.entity.JobPosting;
import ee.jobs.aggregator.entity.Skill;
import ee.jobs.aggregator.entity.SkillCategory;
import ee.jobs.aggregator.repository.JobPostingRepository;
import ee.jobs.aggregator.repository.SkillRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Oskuste eraldamise teenus.
 * Kasutab regex-põhist mustrite sobitamist, et tuvastada tehnilisi oskusi kirjeldusest.
 */
@Service
public class SkillExtractionService {

    private static final Logger log = LoggerFactory.getLogger(SkillExtractionService.class);

    private final SkillRepository skillRepository;
    private final JobPostingRepository jobPostingRepository;

    // Oskuste sõnastik koos kategooriatega
    private static final Map<String, SkillCategory> SKILL_DICTIONARY = new LinkedHashMap<>();

    // Regex mustrid igale oskusele (kompileeritud jõudluse jaoks)
    private static final Map<String, Pattern> SKILL_PATTERNS = new HashMap<>();

    static {
        // Programmeerimiskeeled
        SKILL_DICTIONARY.put("java", SkillCategory.LANGUAGE);
        SKILL_DICTIONARY.put("python", SkillCategory.LANGUAGE);
        SKILL_DICTIONARY.put("javascript", SkillCategory.LANGUAGE);
        SKILL_DICTIONARY.put("typescript", SkillCategory.LANGUAGE);
        SKILL_DICTIONARY.put("c#", SkillCategory.LANGUAGE);
        SKILL_DICTIONARY.put("c++", SkillCategory.LANGUAGE);
        SKILL_DICTIONARY.put("go", SkillCategory.LANGUAGE);
        SKILL_DICTIONARY.put("golang", SkillCategory.LANGUAGE);
        SKILL_DICTIONARY.put("rust", SkillCategory.LANGUAGE);
        SKILL_DICTIONARY.put("kotlin", SkillCategory.LANGUAGE);
        SKILL_DICTIONARY.put("scala", SkillCategory.LANGUAGE);
        SKILL_DICTIONARY.put("php", SkillCategory.LANGUAGE);
        SKILL_DICTIONARY.put("ruby", SkillCategory.LANGUAGE);
        SKILL_DICTIONARY.put("swift", SkillCategory.LANGUAGE);
        SKILL_DICTIONARY.put("sql", SkillCategory.LANGUAGE);

        // Raamistikud ja teegid
        SKILL_DICTIONARY.put("spring", SkillCategory.FRAMEWORK);
        SKILL_DICTIONARY.put("spring boot", SkillCategory.FRAMEWORK);
        SKILL_DICTIONARY.put("angular", SkillCategory.FRAMEWORK);
        SKILL_DICTIONARY.put("react", SkillCategory.FRAMEWORK);
        SKILL_DICTIONARY.put("vue", SkillCategory.FRAMEWORK);
        SKILL_DICTIONARY.put("vue.js", SkillCategory.FRAMEWORK);
        SKILL_DICTIONARY.put("node.js", SkillCategory.FRAMEWORK);
        SKILL_DICTIONARY.put("nodejs", SkillCategory.FRAMEWORK);
        SKILL_DICTIONARY.put("express", SkillCategory.FRAMEWORK);
        SKILL_DICTIONARY.put("django", SkillCategory.FRAMEWORK);
        SKILL_DICTIONARY.put("flask", SkillCategory.FRAMEWORK);
        SKILL_DICTIONARY.put("fastapi", SkillCategory.FRAMEWORK);
        SKILL_DICTIONARY.put(".net", SkillCategory.FRAMEWORK);
        SKILL_DICTIONARY.put("dotnet", SkillCategory.FRAMEWORK);
        SKILL_DICTIONARY.put("next.js", SkillCategory.FRAMEWORK);
        SKILL_DICTIONARY.put("nextjs", SkillCategory.FRAMEWORK);
        SKILL_DICTIONARY.put("nest.js", SkillCategory.FRAMEWORK);
        SKILL_DICTIONARY.put("nestjs", SkillCategory.FRAMEWORK);
        SKILL_DICTIONARY.put("hibernate", SkillCategory.FRAMEWORK);
        SKILL_DICTIONARY.put("graphql", SkillCategory.FRAMEWORK);

        // Andmebaasid
        SKILL_DICTIONARY.put("postgresql", SkillCategory.DATABASE);
        SKILL_DICTIONARY.put("postgres", SkillCategory.DATABASE);
        SKILL_DICTIONARY.put("mysql", SkillCategory.DATABASE);
        SKILL_DICTIONARY.put("mongodb", SkillCategory.DATABASE);
        SKILL_DICTIONARY.put("redis", SkillCategory.DATABASE);
        SKILL_DICTIONARY.put("elasticsearch", SkillCategory.DATABASE);
        SKILL_DICTIONARY.put("oracle", SkillCategory.DATABASE);
        SKILL_DICTIONARY.put("sql server", SkillCategory.DATABASE);
        SKILL_DICTIONARY.put("cassandra", SkillCategory.DATABASE);
        SKILL_DICTIONARY.put("dynamodb", SkillCategory.DATABASE);

        // DevOps ja pilveplatvormid
        SKILL_DICTIONARY.put("docker", SkillCategory.DEVOPS);
        SKILL_DICTIONARY.put("kubernetes", SkillCategory.DEVOPS);
        SKILL_DICTIONARY.put("k8s", SkillCategory.DEVOPS);
        SKILL_DICTIONARY.put("aws", SkillCategory.DEVOPS);
        SKILL_DICTIONARY.put("azure", SkillCategory.DEVOPS);
        SKILL_DICTIONARY.put("gcp", SkillCategory.DEVOPS);
        SKILL_DICTIONARY.put("google cloud", SkillCategory.DEVOPS);
        SKILL_DICTIONARY.put("git", SkillCategory.DEVOPS);
        SKILL_DICTIONARY.put("github", SkillCategory.DEVOPS);
        SKILL_DICTIONARY.put("gitlab", SkillCategory.DEVOPS);
        SKILL_DICTIONARY.put("ci/cd", SkillCategory.DEVOPS);
        SKILL_DICTIONARY.put("jenkins", SkillCategory.DEVOPS);
        SKILL_DICTIONARY.put("terraform", SkillCategory.DEVOPS);
        SKILL_DICTIONARY.put("ansible", SkillCategory.DEVOPS);
        SKILL_DICTIONARY.put("linux", SkillCategory.DEVOPS);
        SKILL_DICTIONARY.put("nginx", SkillCategory.DEVOPS);
        SKILL_DICTIONARY.put("kafka", SkillCategory.DEVOPS);
        SKILL_DICTIONARY.put("rabbitmq", SkillCategory.DEVOPS);

        // Pehmed oskused
        SKILL_DICTIONARY.put("agile", SkillCategory.SOFT_SKILL);
        SKILL_DICTIONARY.put("scrum", SkillCategory.SOFT_SKILL);
        SKILL_DICTIONARY.put("kanban", SkillCategory.SOFT_SKILL);
        SKILL_DICTIONARY.put("jira", SkillCategory.SOFT_SKILL);

        // Loo regex mustrid igale oskusele
        for (String skill : SKILL_DICTIONARY.keySet()) {
            String escapedSkill = Pattern.quote(skill);
            // Sõnapiiri kontroll, et vältida osalist vastet
            Pattern pattern = Pattern.compile(
                "\\b" + escapedSkill + "\\b",
                Pattern.CASE_INSENSITIVE
            );
            SKILL_PATTERNS.put(skill, pattern);
        }
    }

    public SkillExtractionService(
            SkillRepository skillRepository,
            JobPostingRepository jobPostingRepository
    ) {
        this.skillRepository = skillRepository;
        this.jobPostingRepository = jobPostingRepository;
    }

    /**
     * Eralda oskused tööpakkumisest ja salvesta need.
     */
    @Transactional
    public Set<Skill> extractAndSaveSkills(JobPosting jobPosting) {
        String text = prepareTextForExtraction(jobPosting);
        Set<String> foundSkillNames = extractSkillNames(text);

        log.debug("Leiti {} oskust kuulutusest: {}",
                 foundSkillNames.size(), jobPosting.getTitle());

        Set<Skill> skills = new HashSet<>();
        for (String skillName : foundSkillNames) {
            Skill skill = getOrCreateSkill(skillName);
            skills.add(skill);
        }

        // Uuenda tööpakkumise oskused
        jobPosting.setSkills(skills);
        jobPostingRepository.save(jobPosting);

        return skills;
    }

    /**
     * Eralda oskuste nimed tekstist.
     */
    public Set<String> extractSkillNames(String text) {
        Set<String> found = new HashSet<>();

        if (text == null || text.isEmpty()) {
            return found;
        }

        for (Map.Entry<String, Pattern> entry : SKILL_PATTERNS.entrySet()) {
            Matcher matcher = entry.getValue().matcher(text);
            if (matcher.find()) {
                // Normaliseeri oskuse nimi (eralda aliased)
                String normalizedName = normalizeSkillName(entry.getKey());
                found.add(normalizedName);
            }
        }

        return found;
    }

    /**
     * Valmista tekst ette eraldamiseks.
     * Ühendab pealkirja ja kirjelduse.
     */
    private String prepareTextForExtraction(JobPosting jobPosting) {
        StringBuilder sb = new StringBuilder();

        if (jobPosting.getTitle() != null) {
            sb.append(jobPosting.getTitle()).append(" ");
        }

        if (jobPosting.getDescription() != null) {
            sb.append(jobPosting.getDescription());
        }

        return sb.toString().toLowerCase();
    }

    /**
     * Normaliseeri oskuse nimi (ühenda aliased).
     */
    private String normalizeSkillName(String skillName) {
        // Aliaste normaliseerimine
        return switch (skillName.toLowerCase()) {
            case "golang" -> "go";
            case "nodejs", "node.js" -> "node.js";
            case "vue.js" -> "vue";
            case "postgres" -> "postgresql";
            case "k8s" -> "kubernetes";
            case "dotnet" -> ".net";
            case "nextjs" -> "next.js";
            case "nestjs" -> "nest.js";
            case "spring boot" -> "spring";
            default -> skillName.toLowerCase();
        };
    }

    /**
     * Leia või loo oskus andmebaasist.
     */
    private Skill getOrCreateSkill(String skillName) {
        String normalized = normalizeSkillName(skillName);

        return skillRepository.findByNameIgnoreCase(normalized)
            .orElseGet(() -> {
                // Loo uus oskus
                SkillCategory category = SKILL_DICTIONARY.getOrDefault(
                    skillName.toLowerCase(),
                    SkillCategory.LANGUAGE
                );

                // Kontrolli ka normaliseeritud nime
                if (!SKILL_DICTIONARY.containsKey(skillName.toLowerCase())) {
                    category = SKILL_DICTIONARY.getOrDefault(
                        normalized,
                        SkillCategory.LANGUAGE
                    );
                }

                Skill newSkill = new Skill(normalized, category);
                log.info("Loodud uus oskus: {} ({})", normalized, category);
                return skillRepository.save(newSkill);
            });
    }

    /**
     * Tagasta kõik tuntud oskused.
     */
    public Set<String> getKnownSkills() {
        return new HashSet<>(SKILL_DICTIONARY.keySet());
    }
}
