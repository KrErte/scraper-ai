import { Component, OnInit, inject, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { NgxChartsModule } from '@swimlane/ngx-charts';

import { StatsService } from '../../services/stats.service';
import { ScraperService } from '../../services/scraper.service';

/**
 * Dashboard komponent.
 * Kuvab koondstatistika, graafikud ja ülevaate.
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, NgxChartsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="dashboard">
      <h1 class="page-title">Dashboard</h1>

      <!-- Laadimise indikaator -->
      @if (statsService.loading()) {
        <div class="loading">
          <div class="spinner"></div>
          <span>Laen andmeid...</span>
        </div>
      }

      <!-- Statistika kaardid -->
      @if (statsService.dashboardStats(); as stats) {
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon">📋</div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.totalJobs }}</div>
              <div class="stat-label">Tööpakkumist kokku</div>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-icon">💰</div>
            <div class="stat-content">
              <div class="stat-value">
                {{ stats.avgSalary ? (stats.avgSalary | number:'1.0-0') + ' €' : 'N/A' }}
              </div>
              <div class="stat-label">Keskmine palk</div>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-icon">🔥</div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.topSkill || 'N/A' }}</div>
              <div class="stat-label">Populaarseim oskus</div>
            </div>
          </div>

          <div class="stat-card">
            <div class="stat-icon">🆕</div>
            <div class="stat-content">
              <div class="stat-value">{{ stats.newJobsToday }}</div>
              <div class="stat-label">Uut täna</div>
            </div>
          </div>
        </div>
      }

      <div class="charts-grid">
        <!-- Trendide graafik -->
        <div class="card chart-card">
          <div class="card-header">
            <h2 class="card-title">Tööpakkumiste trend (30 päeva)</h2>
          </div>
          @if (trendsChartData.length > 0) {
            <ngx-charts-line-chart
              [results]="trendsChartData"
              [xAxis]="true"
              [yAxis]="true"
              [showXAxisLabel]="true"
              [showYAxisLabel]="true"
              xAxisLabel="Kuupäev"
              yAxisLabel="Arv"
              [autoScale]="true"
              [timeline]="false"
              [scheme]="colorScheme">
            </ngx-charts-line-chart>
          } @else {
            <div class="empty-chart">Andmed puuduvad</div>
          }
        </div>

        <!-- Top oskused -->
        <div class="card chart-card">
          <div class="card-header">
            <h2 class="card-title">Top 10 nõutuimat oskust</h2>
          </div>
          @if (skillsChartData.length > 0) {
            <ngx-charts-bar-horizontal
              [results]="skillsChartData"
              [xAxis]="true"
              [yAxis]="true"
              [showDataLabel]="true"
              [scheme]="colorScheme">
            </ngx-charts-bar-horizontal>
          } @else {
            <div class="empty-chart">Andmed puuduvad</div>
          }
        </div>
      </div>

      <!-- Scraper nupp -->
      <div class="scraper-section card">
        <div class="card-header">
          <h2 class="card-title">Tööpakkumiste uuendamine</h2>
        </div>
        <div class="scraper-content">
          <p class="scraper-info">Tõmba värsked tööpakkumised CV.ee ja CV Keskus portaalidest.</p>

          @if (scraperService.scraping()) {
            <div class="scraping-status">
              <div class="spinner"></div>
              <span>Tõmban andmeid...</span>
            </div>
          } @else {
            <button class="btn btn-primary" (click)="triggerScraper()">
              Tõmba tööpakkumised
            </button>
          }

          @if (scraperService.lastResult(); as result) {
            <div class="scraper-result">
              <span class="result-item success">{{ result.summary.totalNew }} uut</span>
              <span class="result-item update">{{ result.summary.totalUpdated }} uuendatud</span>
              @if (result.summary.totalErrors > 0) {
                <span class="result-item error">{{ result.summary.totalErrors }} viga</span>
              }
            </div>
          }

          @if (scraperService.error(); as error) {
            <div class="scraper-error">{{ error }}</div>
          }
        </div>
      </div>

      <!-- Kiirlingid -->
      <div class="quick-links">
        <a routerLink="/jobs" class="btn btn-primary">
          Vaata kõiki tööpakkumisi
        </a>
        <a routerLink="/trends" class="btn btn-secondary">
          Vaata trende
        </a>
      </div>
    </div>
  `,
  styles: [`
    .dashboard {
      display: flex;
      flex-direction: column;
      gap: 2rem;
    }

    .page-title {
      font-size: 1.875rem;
      font-weight: 700;
      color: var(--text-primary);
    }

    .stats-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 1.5rem;
    }

    @media (max-width: 1024px) {
      .stats-grid {
        grid-template-columns: repeat(2, 1fr);
      }
    }

    @media (max-width: 640px) {
      .stats-grid {
        grid-template-columns: 1fr;
      }
    }

    .stat-card {
      background: var(--bg-primary);
      border: 1px solid var(--border-color);
      border-radius: var(--border-radius);
      padding: 1.5rem;
      display: flex;
      align-items: center;
      gap: 1rem;
      box-shadow: var(--shadow-sm);
    }

    .stat-icon {
      font-size: 2rem;
      width: 3.5rem;
      height: 3.5rem;
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--bg-tertiary);
      border-radius: var(--border-radius);
    }

    .stat-value {
      font-size: 1.5rem;
      font-weight: 700;
      color: var(--text-primary);
    }

    .stat-label {
      font-size: 0.875rem;
      color: var(--text-secondary);
    }

    .charts-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 1.5rem;
    }

    @media (max-width: 1024px) {
      .charts-grid {
        grid-template-columns: 1fr;
      }
    }

    .chart-card {
      min-height: 400px;
    }

    .chart-card ngx-charts-line-chart,
    .chart-card ngx-charts-bar-horizontal {
      height: 300px;
    }

    .empty-chart {
      height: 300px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--text-muted);
    }

    .quick-links {
      display: flex;
      gap: 1rem;
      justify-content: center;
    }

    .loading {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 1rem;
      padding: 3rem;
    }

    .scraper-section {
      margin-bottom: 1rem;
    }

    .scraper-content {
      display: flex;
      flex-direction: column;
      gap: 1rem;
      align-items: flex-start;
    }

    .scraper-info {
      color: var(--text-secondary);
      margin: 0;
    }

    .scraping-status {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      color: var(--text-accent);
    }

    .scraping-status .spinner {
      width: 1.5rem;
      height: 1.5rem;
    }

    .scraper-result {
      display: flex;
      gap: 1rem;
      flex-wrap: wrap;
    }

    .result-item {
      padding: 0.5rem 1rem;
      border-radius: var(--border-radius-sm);
      font-weight: 500;
      font-size: 0.875rem;
    }

    .result-item.success {
      background: rgba(56, 239, 125, 0.15);
      color: #38ef7d;
    }

    .result-item.update {
      background: rgba(102, 126, 234, 0.15);
      color: #8b9cf4;
    }

    .result-item.error {
      background: rgba(245, 87, 108, 0.15);
      color: #f5576c;
    }

    .scraper-error {
      color: #f5576c;
      font-size: 0.875rem;
    }
  `]
})
export class DashboardComponent implements OnInit {
  statsService = inject(StatsService);
  scraperService = inject(ScraperService);
  private cdr = inject(ChangeDetectorRef);

  // Värviskeemid graafikutele (kasutame eeldefineeritud skeemi)
  colorScheme = 'cool';

  // Graafiku andmed
  trendsChartData: any[] = [];
  skillsChartData: any[] = [];

  ngOnInit(): void {
    this.loadData();
  }

  triggerScraper(): void {
    this.scraperService.triggerAllScrapers().subscribe(result => {
      if (result) {
        // Laadi andmed uuesti peale scrapingut
        this.loadData();
      }
      this.cdr.markForCheck();
    });
  }

  private loadData(): void {
    this.statsService.loadAllDashboardData().subscribe(([stats, skills, trends]) => {
      // Valmista trendi andmed ette
      if (trends && trends.length > 0) {
        this.trendsChartData = [{
          name: 'Uued tööpakkumised',
          series: trends.map(t => ({
            name: new Date(t.date).toLocaleDateString('et-EE', { month: 'short', day: 'numeric' }),
            value: t.count
          }))
        }];
      }

      // Valmista oskuste andmed ette
      if (skills && skills.length > 0) {
        this.skillsChartData = skills.map(s => ({
          name: s.name.toUpperCase(),
          value: s.jobCount
        }));
      }

      this.cdr.markForCheck();
    });
  }
}
