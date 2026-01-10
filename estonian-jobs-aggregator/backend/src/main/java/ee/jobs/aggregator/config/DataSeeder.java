package ee.jobs.aggregator.config;

import ee.jobs.aggregator.entity.*;
import ee.jobs.aggregator.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Mock andmete laadija arenduse ja demo jaoks.
 */
@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    CommandLineRunner seedData(
            JobPostingRepository jobRepo,
            SkillRepository skillRepo,
            CompanyRepository companyRepo
    ) {
        return args -> {
            // Loo ainult oskused, kui neid veel pole
            // Tööpakkumised tulevad päris scrapingust
            if (skillRepo.count() == 0) {
                log.info("Laadin oskuste andmed...");
                createSkills(skillRepo);
                log.info("Oskused laetud! Tööpakkumised tuleb scrapida: POST /api/scraper/trigger");
            } else {
                log.info("Oskused on juba olemas, jätan vahele");
            }
        };
    }

    private Map<String, Skill> createSkills(SkillRepository repo) {
        Map<String, Skill> skills = new HashMap<>();

        // Programmeerimiskeeled
        skills.put("java", repo.save(new Skill("java", SkillCategory.LANGUAGE)));
        skills.put("python", repo.save(new Skill("python", SkillCategory.LANGUAGE)));
        skills.put("javascript", repo.save(new Skill("javascript", SkillCategory.LANGUAGE)));
        skills.put("typescript", repo.save(new Skill("typescript", SkillCategory.LANGUAGE)));
        skills.put("c#", repo.save(new Skill("c#", SkillCategory.LANGUAGE)));
        skills.put("go", repo.save(new Skill("go", SkillCategory.LANGUAGE)));
        skills.put("rust", repo.save(new Skill("rust", SkillCategory.LANGUAGE)));
        skills.put("kotlin", repo.save(new Skill("kotlin", SkillCategory.LANGUAGE)));
        skills.put("sql", repo.save(new Skill("sql", SkillCategory.LANGUAGE)));

        // Raamistikud
        skills.put("spring", repo.save(new Skill("spring", SkillCategory.FRAMEWORK)));
        skills.put("angular", repo.save(new Skill("angular", SkillCategory.FRAMEWORK)));
        skills.put("react", repo.save(new Skill("react", SkillCategory.FRAMEWORK)));
        skills.put("vue", repo.save(new Skill("vue", SkillCategory.FRAMEWORK)));
        skills.put("node.js", repo.save(new Skill("node.js", SkillCategory.FRAMEWORK)));
        skills.put(".net", repo.save(new Skill(".net", SkillCategory.FRAMEWORK)));
        skills.put("django", repo.save(new Skill("django", SkillCategory.FRAMEWORK)));
        skills.put("next.js", repo.save(new Skill("next.js", SkillCategory.FRAMEWORK)));

        // Andmebaasid
        skills.put("postgresql", repo.save(new Skill("postgresql", SkillCategory.DATABASE)));
        skills.put("mongodb", repo.save(new Skill("mongodb", SkillCategory.DATABASE)));
        skills.put("redis", repo.save(new Skill("redis", SkillCategory.DATABASE)));
        skills.put("elasticsearch", repo.save(new Skill("elasticsearch", SkillCategory.DATABASE)));

        // DevOps
        skills.put("docker", repo.save(new Skill("docker", SkillCategory.DEVOPS)));
        skills.put("kubernetes", repo.save(new Skill("kubernetes", SkillCategory.DEVOPS)));
        skills.put("aws", repo.save(new Skill("aws", SkillCategory.DEVOPS)));
        skills.put("azure", repo.save(new Skill("azure", SkillCategory.DEVOPS)));
        skills.put("git", repo.save(new Skill("git", SkillCategory.DEVOPS)));
        skills.put("ci/cd", repo.save(new Skill("ci/cd", SkillCategory.DEVOPS)));
        skills.put("terraform", repo.save(new Skill("terraform", SkillCategory.DEVOPS)));

        // Soft skills
        skills.put("agile", repo.save(new Skill("agile", SkillCategory.SOFT_SKILL)));
        skills.put("scrum", repo.save(new Skill("scrum", SkillCategory.SOFT_SKILL)));

        return skills;
    }

    private List<Company> createCompanies(CompanyRepository repo) {
        List<String> companyNames = List.of(
            "Wise", "Bolt", "Pipedrive", "Veriff", "Starship Technologies",
            "Skeleton Technologies", "Nortal", "Helmes", "Playtech", "Transferwise",
            "Swedbank", "SEB", "LHV", "Telia", "Elisa",
            "Codeborne", "Mooncascade", "Ignite", "Proekspert", "Icefire",
            "Ridango", "Cleveron", "Guardtime", "Cybernetica", "Reach-U"
        );

        List<Company> companies = new ArrayList<>();
        for (String name : companyNames) {
            Company company = new Company(name);
            companies.add(repo.save(company));
        }
        return companies;
    }

    private void createJobPostings(
            JobPostingRepository repo,
            Map<String, Skill> skills,
            List<Company> companies
    ) {
        Random random = new Random(42);
        List<String[]> jobTemplates = getJobTemplates();

        int jobCount = 0;
        for (int daysAgo = 30; daysAgo >= 0; daysAgo--) {
            // 2-8 tööpakkumist päevas
            int jobsPerDay = 2 + random.nextInt(7);

            for (int j = 0; j < jobsPerDay; j++) {
                String[] template = jobTemplates.get(random.nextInt(jobTemplates.size()));
                Company company = companies.get(random.nextInt(companies.size()));

                JobPosting job = new JobPosting();
                job.setTitle(template[0]);
                job.setCompany(company.getName());
                job.setLocation(getRandomLocation(random));
                job.setDescription(template[1]);
                job.setUrl("https://cv.ee/job/" + (++jobCount));
                job.setSource(JobSource.CV_EE);
                job.setPostedDate(LocalDate.now().minusDays(daysAgo));
                job.setScrapedAt(LocalDateTime.now().minusDays(daysAgo));

                // Palk
                int baseSalary = 2500 + random.nextInt(5500);
                job.setSalaryMin(BigDecimal.valueOf(baseSalary));
                job.setSalaryMax(BigDecimal.valueOf(baseSalary + 1000 + random.nextInt(2000)));
                job.setSalaryCurrency("EUR");

                // Oskused
                Set<Skill> jobSkills = getRandomSkills(skills, template[2], random);
                job.setSkills(jobSkills);

                repo.save(job);

                // Uuenda ettevõtte job count
                company.incrementJobCount();
            }
        }

        // Salvesta uuendatud ettevõtted
        // (juba salvestatud läbi JPA)
    }

    private List<String[]> getJobTemplates() {
        return List.of(
            new String[]{"Senior Java Developer", "Otsime kogenud Java arendajat meie fintech meeskonda. Töötad Spring Boot ja mikroteenuste arhitektuuriga.", "java,spring,postgresql,docker,kubernetes"},
            new String[]{"Full Stack Developer", "Arendad modernseid veebirakendusi kasutades React ja Node.js. Agile meeskond.", "javascript,typescript,react,node.js,postgresql"},
            new String[]{"Frontend Developer", "Loo kasutajasõbralikke kasutajaliideseid Angular raamistikuga. Remote töö võimalus.", "typescript,angular,javascript,git"},
            new String[]{"Backend Developer", "Python/Django arendaja andmetöötluse projektidesse. ML kogemus pluss.", "python,django,postgresql,docker,aws"},
            new String[]{"DevOps Engineer", "Halda ja optimeeri pilveinfrastruktuuri AWS keskkonnas. Kubernetes kogemus vajalik.", "docker,kubernetes,aws,terraform,ci/cd"},
            new String[]{"Tech Lead", "Juhi 5-liikmelist arendusmeeskonda. Arhitektuursed otsused ja mentorlus.", "java,spring,postgresql,agile,scrum"},
            new String[]{".NET Developer", "C# ja .NET Core arendaja enterprise lahendustesse. Hübriidtöö.", "c#,.net,sql,azure,docker"},
            new String[]{"React Native Developer", "Mobiilirakenduste arendaja iOS ja Android platvormidele.", "javascript,typescript,react,git"},
            new String[]{"Data Engineer", "Ehita andmetorustikke ja ETL protsesse. Big data kogemus.", "python,sql,postgresql,aws,docker"},
            new String[]{"Go Developer", "Backend arendaja kõrge jõudlusega süsteemidesse. Mikroteenused.", "go,postgresql,docker,kubernetes,redis"},
            new String[]{"Cloud Architect", "Disaini ja implementeeri pilvelahendusi. Multi-cloud strateegia.", "aws,azure,terraform,kubernetes,docker"},
            new String[]{"Security Engineer", "Taga rakenduste ja infrastruktuuri turvalisus. Pentest kogemus.", "python,docker,kubernetes,aws,git"},
            new String[]{"ML Engineer", "Masinõppe mudelite arendus ja deploy. NLP projektid.", "python,docker,aws,postgresql"},
            new String[]{"Vue.js Developer", "Frontend arendaja Vue 3 ja Nuxt projektidesse.", "javascript,typescript,vue,node.js,git"},
            new String[]{"Kotlin Developer", "Android ja backend arendus Kotlin keeles.", "kotlin,java,spring,postgresql,docker"},
            new String[]{"Platform Engineer", "Arendusplatvormi ehitamine ja hooldus. Developer experience.", "docker,kubernetes,terraform,aws,ci/cd"},
            new String[]{"Rust Developer", "Systems programming kõrge jõudlusega rakendustes.", "rust,docker,postgresql,git"},
            new String[]{"Site Reliability Engineer", "Taga süsteemide töökindlus ja skaleeritavus.", "docker,kubernetes,aws,terraform,python"},
            new String[]{"Next.js Developer", "Full stack arendus Next.js ja React ökosüsteemis.", "typescript,react,next.js,node.js,postgresql"},
            new String[]{"Junior Developer", "Alustav arendaja meie mentorprogrammi. Õppimisvõime oluline.", "javascript,git,sql,agile"}
        );
    }

    private Set<Skill> getRandomSkills(Map<String, Skill> allSkills, String requiredSkills, Random random) {
        Set<Skill> result = new HashSet<>();

        // Lisa nõutud oskused
        for (String skillName : requiredSkills.split(",")) {
            Skill skill = allSkills.get(skillName.trim().toLowerCase());
            if (skill != null) {
                result.add(skill);
            }
        }

        // Lisa mõned juhuslikud oskused
        List<Skill> skillList = new ArrayList<>(allSkills.values());
        int extraSkills = random.nextInt(3);
        for (int i = 0; i < extraSkills; i++) {
            result.add(skillList.get(random.nextInt(skillList.size())));
        }

        return result;
    }

    private String getRandomLocation(Random random) {
        String[] locations = {"Tallinn", "Tartu", "Tallinn", "Tallinn", "Remote", "Tallinn/Remote", "Pärnu", "Tallinn"};
        return locations[random.nextInt(locations.length)];
    }
}
