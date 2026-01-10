package ee.jobs.aggregator.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Oskuse entiteet.
 * Esindab tehnilist või pehmet oskust, mida tööandjad otsivad.
 */
@Entity
@Table(name = "skills", indexes = {
    @Index(name = "idx_skill_name", columnList = "name"),
    @Index(name = "idx_skill_category", columnList = "category")
})
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Oskuse nimi (normaliseeritud väiketähtedeks)
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    // Oskuse kategooria
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SkillCategory category;

    // Seotud tööpakkumised (bidirectional ManyToMany)
    @ManyToMany(mappedBy = "skills")
    private Set<JobPosting> jobPostings = new HashSet<>();

    // Konstruktorid

    public Skill() {
    }

    public Skill(String name, SkillCategory category) {
        this.name = name.toLowerCase();
        this.category = category;
    }

    // Getters ja setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        // Normaliseeri väiketähtedeks
        this.name = name != null ? name.toLowerCase() : null;
    }

    public SkillCategory getCategory() {
        return category;
    }

    public void setCategory(SkillCategory category) {
        this.category = category;
    }

    public Set<JobPosting> getJobPostings() {
        return jobPostings;
    }

    public void setJobPostings(Set<JobPosting> jobPostings) {
        this.jobPostings = jobPostings;
    }

    // equals ja hashCode põhinevad name väljal

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Skill skill = (Skill) o;
        return Objects.equals(name, skill.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Skill{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", category=" + category +
                '}';
    }
}
