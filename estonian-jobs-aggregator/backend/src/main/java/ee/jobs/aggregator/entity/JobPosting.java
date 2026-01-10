package ee.jobs.aggregator.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Tööpakkumise entiteet.
 * Esindab ühte tööpakkumist koos kõigi detailidega.
 */
@Entity
@Table(name = "job_postings", indexes = {
    @Index(name = "idx_job_url", columnList = "url"),
    @Index(name = "idx_job_source", columnList = "source"),
    @Index(name = "idx_job_location", columnList = "location"),
    @Index(name = "idx_job_posted_date", columnList = "postedDate"),
    @Index(name = "idx_job_salary", columnList = "salaryMin, salaryMax")
})
public class JobPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tööpakkumise pealkiri
    @Column(nullable = false, length = 500)
    private String title;

    // Ettevõtte nimi (denormaliseeritud kiireks otsinguks)
    @Column(nullable = false, length = 255)
    private String company;

    // Asukoht
    @Column(length = 255)
    private String location;

    // Palk - minimaalne
    @Column(precision = 10, scale = 2)
    private BigDecimal salaryMin;

    // Palk - maksimaalne
    @Column(precision = 10, scale = 2)
    private BigDecimal salaryMax;

    // Palga valuuta (EUR, USD jne)
    @Column(length = 3)
    private String salaryCurrency = "EUR";

    // Tööpakkumise kirjeldus
    @Column(columnDefinition = "TEXT")
    private String description;

    // Unikaalne URL (identifitseerimiseks ja dubleerimise vältimiseks)
    @Column(nullable = false, unique = true, length = 1000)
    private String url;

    // Allika portaal
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JobSource source;

    // Kuulutuse postitamise kuupäev
    private LocalDate postedDate;

    // Scrapimise aeg
    @Column(nullable = false)
    private LocalDateTime scrapedAt;

    // Aegumise kuupäev
    private LocalDate expiresAt;

    // Seotud oskused (ManyToMany seos)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "job_posting_skills",
        joinColumns = @JoinColumn(name = "job_posting_id"),
        inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private Set<Skill> skills = new HashSet<>();

    // Konstruktorid

    public JobPosting() {
        this.scrapedAt = LocalDateTime.now();
    }

    // Getters ja setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public BigDecimal getSalaryMin() {
        return salaryMin;
    }

    public void setSalaryMin(BigDecimal salaryMin) {
        this.salaryMin = salaryMin;
    }

    public BigDecimal getSalaryMax() {
        return salaryMax;
    }

    public void setSalaryMax(BigDecimal salaryMax) {
        this.salaryMax = salaryMax;
    }

    public String getSalaryCurrency() {
        return salaryCurrency;
    }

    public void setSalaryCurrency(String salaryCurrency) {
        this.salaryCurrency = salaryCurrency;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public JobSource getSource() {
        return source;
    }

    public void setSource(JobSource source) {
        this.source = source;
    }

    public LocalDate getPostedDate() {
        return postedDate;
    }

    public void setPostedDate(LocalDate postedDate) {
        this.postedDate = postedDate;
    }

    public LocalDateTime getScrapedAt() {
        return scrapedAt;
    }

    public void setScrapedAt(LocalDateTime scrapedAt) {
        this.scrapedAt = scrapedAt;
    }

    public LocalDate getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDate expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Set<Skill> getSkills() {
        return skills;
    }

    public void setSkills(Set<Skill> skills) {
        this.skills = skills;
    }

    // Abimeetodid oskuste haldamiseks

    public void addSkill(Skill skill) {
        this.skills.add(skill);
        skill.getJobPostings().add(this);
    }

    public void removeSkill(Skill skill) {
        this.skills.remove(skill);
        skill.getJobPostings().remove(this);
    }

    // equals ja hashCode põhinevad URL väljal (unikaalne identifikaator)

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JobPosting that = (JobPosting) o;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }

    @Override
    public String toString() {
        return "JobPosting{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", company='" + company + '\'' +
                ", location='" + location + '\'' +
                ", source=" + source +
                '}';
    }
}
