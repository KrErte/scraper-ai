import { Component, OnInit, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { JobService } from '../../services/job.service';
import { StatsService } from '../../services/stats.service';
import { JobFilters, JobPosting } from '../../models/job.model';

/**
 * Tööpakkumiste nimekirja komponent.
 * Kuvab filtreeritava ja pagineeritud tööpakkumiste tabeli.
 */
@Component({
  selector: 'app-jobs',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="jobs-page">
      <h1 class="page-title">Tööpakkumised</h1>

      <!-- Filtrid -->
      <div class="filters card">
        <div class="filters-grid">
          <div class="filter-group">
            <label for="skill">Oskus</label>
            <select
              id="skill"
              class="select"
              [(ngModel)]="filters.skill"
              (change)="applyFilters()">
              <option value="">Kõik oskused</option>
              @for (skill of availableSkills(); track skill.name) {
                <option [value]="skill.name">{{ skill.name | uppercase }} ({{ skill.jobCount }})</option>
              }
            </select>
          </div>

          <div class="filter-group">
            <label for="location">Asukoht</label>
            <input
              id="location"
              type="text"
              class="input"
              placeholder="nt. Tallinn"
              [(ngModel)]="filters.location"
              (keyup.enter)="applyFilters()">
          </div>

          <div class="filter-group">
            <label for="salaryMin">Min palk (€)</label>
            <input
              id="salaryMin"
              type="number"
              class="input"
              placeholder="nt. 3000"
              [(ngModel)]="filters.salaryMin"
              (keyup.enter)="applyFilters()">
          </div>

          <div class="filter-group filter-actions">
            <button class="btn btn-primary" (click)="applyFilters()">
              Otsi
            </button>
            <button class="btn btn-secondary" (click)="resetFilters()">
              Lähtesta
            </button>
          </div>
        </div>
      </div>

      <!-- Tulemuste arv -->
      <div class="results-count">
        Leitud: <strong>{{ jobService.totalElements() }}</strong> tööpakkumist
      </div>

      <!-- Laadimise indikaator -->
      @if (jobService.loading()) {
        <div class="loading">
          <div class="spinner"></div>
          <span>Laen tööpakkumisi...</span>
        </div>
      }

      <!-- Tööpakkumiste nimekiri -->
      @if (!jobService.loading() && jobService.hasJobs()) {
        <div class="jobs-list">
          @for (job of jobService.jobs(); track job.id) {
            <div class="job-card card" (click)="openJob(job)">
              <div class="job-header">
                <h3 class="job-title">{{ job.title }}</h3>
                <span class="job-company">{{ job.company }}</span>
              </div>

              <div class="job-meta">
                @if (job.location) {
                  <span class="job-location">
                    📍 {{ job.location }}
                  </span>
                }
                @if (job.salaryMin || job.salaryMax) {
                  <span class="job-salary">
                    💰 {{ formatSalary(job) }}
                  </span>
                }
                @if (job.postedDate) {
                  <span class="job-date">
                    📅 {{ job.postedDate | date:'dd.MM.yyyy' }}
                  </span>
                }
              </div>

              @if (job.skills && job.skills.length > 0) {
                <div class="job-skills">
                  @for (skill of job.skills.slice(0, 5); track skill) {
                    <span class="badge badge-primary">{{ skill }}</span>
                  }
                  @if (job.skills.length > 5) {
                    <span class="badge">+{{ job.skills.length - 5 }}</span>
                  }
                </div>
              }

              <div class="job-source">
                <span class="badge">{{ getSourceLabel(job.source) }}</span>
              </div>
            </div>
          }
        </div>

        <!-- Pagineerimine -->
        @if (jobService.totalPages() > 1) {
          <div class="pagination">
            <button
              class="page-btn"
              [disabled]="jobService.currentPage() === 0"
              (click)="goToPage(0)">
              ««
            </button>
            <button
              class="page-btn"
              [disabled]="jobService.currentPage() === 0"
              (click)="goToPage(jobService.currentPage() - 1)">
              «
            </button>

            <span class="page-info">
              {{ jobService.currentPage() + 1 }} / {{ jobService.totalPages() }}
            </span>

            <button
              class="page-btn"
              [disabled]="jobService.currentPage() >= jobService.totalPages() - 1"
              (click)="goToPage(jobService.currentPage() + 1)">
              »
            </button>
            <button
              class="page-btn"
              [disabled]="jobService.currentPage() >= jobService.totalPages() - 1"
              (click)="goToPage(jobService.totalPages() - 1)">
              »»
            </button>
          </div>
        }
      }

      <!-- Tühi olek -->
      @if (!jobService.loading() && !jobService.hasJobs()) {
        <div class="empty-state card">
          <div class="empty-icon">🔍</div>
          <h3>Tööpakkumisi ei leitud</h3>
          <p>Proovige muuta filtreid või laiendada otsingut.</p>
        </div>
      }
    </div>
  `,
  styles: [`
    .jobs-page {
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
    }

    .page-title {
      font-size: 1.875rem;
      font-weight: 700;
      color: var(--text-primary);
    }

    .filters {
      padding: 1.5rem;
    }

    .filters-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 1rem;
      align-items: end;
    }

    @media (max-width: 1024px) {
      .filters-grid {
        grid-template-columns: repeat(2, 1fr);
      }
    }

    @media (max-width: 640px) {
      .filters-grid {
        grid-template-columns: 1fr;
      }
    }

    .filter-group {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .filter-group label {
      font-size: 0.875rem;
      font-weight: 500;
      color: var(--text-secondary);
    }

    .filter-actions {
      flex-direction: row;
      gap: 0.5rem;
      align-items: flex-end;
    }

    .results-count {
      color: var(--text-secondary);
    }

    .jobs-list {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .job-card {
      cursor: pointer;
      transition: all 0.15s ease;
    }

    .job-card:hover {
      border-color: var(--primary-color);
      box-shadow: var(--shadow-md);
    }

    .job-header {
      margin-bottom: 0.75rem;
    }

    .job-title {
      font-size: 1.125rem;
      font-weight: 600;
      color: var(--text-primary);
      margin-bottom: 0.25rem;
    }

    .job-company {
      color: var(--text-secondary);
    }

    .job-meta {
      display: flex;
      flex-wrap: wrap;
      gap: 1rem;
      margin-bottom: 0.75rem;
      font-size: 0.875rem;
      color: var(--text-secondary);
    }

    .job-skills {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
      margin-bottom: 0.75rem;
    }

    .job-source {
      display: flex;
      justify-content: flex-end;
    }

    .page-info {
      padding: 0.5rem 1rem;
      color: var(--text-secondary);
    }

    .empty-state {
      text-align: center;
      padding: 3rem;
    }

    .empty-icon {
      font-size: 3rem;
      margin-bottom: 1rem;
    }

    .empty-state h3 {
      font-size: 1.25rem;
      margin-bottom: 0.5rem;
    }

    .empty-state p {
      color: var(--text-secondary);
    }

    .loading {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 1rem;
      padding: 3rem;
    }
  `]
})
export class JobsComponent implements OnInit {
  jobService = inject(JobService);
  private statsService = inject(StatsService);

  // Filtrite olek
  filters: JobFilters = {
    skill: '',
    location: '',
    salaryMin: undefined,
    page: 0,
    size: 20
  };

  // Saadaval olevad oskused filtriks
  availableSkills = signal<{ name: string; jobCount: number }[]>([]);

  // Valitud tööpakkumine modaalile
  selectedJob = signal<JobPosting | null>(null);

  ngOnInit(): void {
    this.loadJobs();
    this.loadSkills();
  }

  loadJobs(): void {
    this.jobService.loadJobs(this.filters).subscribe();
  }

  loadSkills(): void {
    this.statsService.loadTopSkills(30).subscribe(skills => {
      this.availableSkills.set(skills.map(s => ({ name: s.name, jobCount: s.jobCount })));
    });
  }

  applyFilters(): void {
    this.filters.page = 0;
    this.loadJobs();
  }

  resetFilters(): void {
    this.filters = {
      skill: '',
      location: '',
      salaryMin: undefined,
      page: 0,
      size: 20
    };
    this.loadJobs();
  }

  goToPage(page: number): void {
    this.filters.page = page;
    this.loadJobs();
  }

  openJob(job: JobPosting): void {
    // Navigeeri detailvaatesse või ava modaal
    window.open(job.url, '_blank');
  }

  formatSalary(job: JobPosting): string {
    if (job.salaryMin && job.salaryMax) {
      return `${job.salaryMin.toLocaleString()} - ${job.salaryMax.toLocaleString()} €`;
    }
    if (job.salaryMin) {
      return `alates ${job.salaryMin.toLocaleString()} €`;
    }
    if (job.salaryMax) {
      return `kuni ${job.salaryMax.toLocaleString()} €`;
    }
    return '';
  }

  getSourceLabel(source: string): string {
    switch (source) {
      case 'CV_EE': return 'CV.ee';
      case 'CV_KESKUS': return 'CV Keskus';
      default: return source;
    }
  }
}
