package ee.jobs.aggregator.entity;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * Ettevõtte entiteet.
 * Esindab tööandjat, kellel on tööpakkumisi.
 */
@Entity
@Table(name = "companies", indexes = {
    @Index(name = "idx_company_name", columnList = "name"),
    @Index(name = "idx_company_job_count", columnList = "jobCount")
})
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Ettevõtte nimi
    @Column(nullable = false, unique = true, length = 255)
    private String name;

    // Denormaliseeritud tööpakkumiste arv (kiirema päringu jaoks)
    @Column(nullable = false)
    private Integer jobCount = 0;

    // Konstruktorid

    public Company() {
    }

    public Company(String name) {
        this.name = name;
        this.jobCount = 0;
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
        this.name = name;
    }

    public Integer getJobCount() {
        return jobCount;
    }

    public void setJobCount(Integer jobCount) {
        this.jobCount = jobCount;
    }

    // Abimeetodid tööpakkumiste arvu haldamiseks

    public void incrementJobCount() {
        this.jobCount++;
    }

    public void decrementJobCount() {
        if (this.jobCount > 0) {
            this.jobCount--;
        }
    }

    // equals ja hashCode põhinevad name väljal

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Company company = (Company) o;
        return Objects.equals(name, company.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Company{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", jobCount=" + jobCount +
                '}';
    }
}
