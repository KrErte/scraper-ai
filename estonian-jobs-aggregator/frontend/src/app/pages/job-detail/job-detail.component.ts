import { Component, OnInit, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { JobService } from '../../services/job.service';
import { JobPosting } from '../../models/job.model';

/**
 * Tööpakkumise detailvaate komponent.
 */
@Component({
  selector: 'app-job-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="job-detail-page">
      <!-- Tagasi link -->
      <a routerLink="/jobs" class="back-link">
        ← Tagasi tööpakkumiste juurde
      </a>

      @if (jobService.loading()) {
        <div class="loading">
          <div class="spinner"></div>
          <span>Laen tööpakkumist...</span>
        </div>
      }

      @if (job(); as jobData) {
        <div class="job-detail card">
          <header class="job-header">
            <h1 class="job-title">{{ jobData.title }}</h1>
            <div class="job-company">{{ jobData.company }}</div>
          </header>

          <div class="job-meta-grid">
            @if (jobData.location) {
              <div class="meta-item">
                <span class="meta-label">📍 Asukoht</span>
                <span class="meta-value">{{ jobData.location }}</span>
              </div>
            }

            @if (jobData.salaryMin || jobData.salaryMax) {
              <div class="meta-item">
                <span class="meta-label">💰 Palk</span>
                <span class="meta-value">{{ formatSalary(jobData) }}</span>
              </div>
            }

            @if (jobData.postedDate) {
              <div class="meta-item">
                <span class="meta-label">📅 Avaldatud</span>
                <span class="meta-value">{{ jobData.postedDate | date:'dd.MM.yyyy' }}</span>
              </div>
            }

            <div class="meta-item">
              <span class="meta-label">🌐 Allikas</span>
              <span class="meta-value">{{ getSourceLabel(jobData.source) }}</span>
            </div>
          </div>

          @if (jobData.skills && jobData.skills.length > 0) {
            <section class="job-skills-section">
              <h2>Nõutud oskused</h2>
              <div class="skills-list">
                @for (skill of jobData.skills; track skill) {
                  <span class="badge badge-primary">{{ skill }}</span>
                }
              </div>
            </section>
          }

          @if (jobData.description) {
            <section class="job-description">
              <h2>Kirjeldus</h2>
              <div class="description-content">{{ jobData.description }}</div>
            </section>
          }

          <footer class="job-actions">
            <a [href]="jobData.url" target="_blank" class="btn btn-primary">
              Vaata originaalkuulutust →
            </a>
          </footer>
        </div>
      }

      @if (jobService.error()) {
        <div class="error-state card">
          <div class="error-icon">⚠️</div>
          <h3>Viga laadimisel</h3>
          <p>{{ jobService.error() }}</p>
          <a routerLink="/jobs" class="btn btn-primary">Tagasi nimekirja</a>
        </div>
      }
    </div>
  `,
  styles: [`
    .job-detail-page {
      max-width: 800px;
      margin: 0 auto;
    }

    .back-link {
      display: inline-block;
      margin-bottom: 1.5rem;
      color: var(--text-secondary);
    }

    .back-link:hover {
      color: var(--primary-color);
    }

    .job-detail {
      padding: 2rem;
    }

    .job-header {
      margin-bottom: 1.5rem;
      padding-bottom: 1.5rem;
      border-bottom: 1px solid var(--border-color);
    }

    .job-title {
      font-size: 1.75rem;
      font-weight: 700;
      color: var(--text-primary);
      margin-bottom: 0.5rem;
    }

    .job-company {
      font-size: 1.125rem;
      color: var(--text-secondary);
    }

    .job-meta-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 1rem;
      margin-bottom: 1.5rem;
    }

    @media (max-width: 640px) {
      .job-meta-grid {
        grid-template-columns: 1fr;
      }
    }

    .meta-item {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }

    .meta-label {
      font-size: 0.875rem;
      color: var(--text-muted);
    }

    .meta-value {
      font-weight: 500;
      color: var(--text-primary);
    }

    .job-skills-section,
    .job-description {
      margin-bottom: 1.5rem;
    }

    .job-skills-section h2,
    .job-description h2 {
      font-size: 1.125rem;
      font-weight: 600;
      margin-bottom: 0.75rem;
    }

    .skills-list {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
    }

    .description-content {
      white-space: pre-wrap;
      line-height: 1.7;
      color: var(--text-secondary);
    }

    .job-actions {
      padding-top: 1.5rem;
      border-top: 1px solid var(--border-color);
    }

    .error-state {
      text-align: center;
      padding: 3rem;
    }

    .error-icon {
      font-size: 3rem;
      margin-bottom: 1rem;
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
export class JobDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  jobService = inject(JobService);

  job = signal<JobPosting | null>(null);

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.jobService.getJob(+id).subscribe(job => {
        this.job.set(job);
      });
    }
  }

  formatSalary(job: JobPosting): string {
    if (job.salaryMin && job.salaryMax) {
      return `${job.salaryMin.toLocaleString()} - ${job.salaryMax.toLocaleString()} €/kuu`;
    }
    if (job.salaryMin) {
      return `alates ${job.salaryMin.toLocaleString()} €/kuu`;
    }
    if (job.salaryMax) {
      return `kuni ${job.salaryMax.toLocaleString()} €/kuu`;
    }
    return 'Pole määratud';
  }

  getSourceLabel(source: string): string {
    switch (source) {
      case 'CV_EE': return 'CV.ee';
      case 'CV_KESKUS': return 'CV Keskus';
      default: return source;
    }
  }
}
