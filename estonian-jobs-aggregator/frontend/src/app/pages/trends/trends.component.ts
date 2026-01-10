import { Component, OnInit, inject, signal, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgxChartsModule } from '@swimlane/ngx-charts';

import { StatsService } from '../../services/stats.service';
import { SkillStats, SalaryStats } from '../../models/stats.model';

/**
 * Trendide komponent.
 * Kuvab oskuste populaarsuse, palgastatistika ja ettevõtete edetabeli.
 */
@Component({
  selector: 'app-trends',
  standalone: true,
  imports: [CommonModule, FormsModule, NgxChartsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="trends-page">
      <h1 class="page-title">Trendid ja statistika</h1>

      <!-- Palgastatistika -->
      <section class="card">
        <div class="card-header">
          <h2 class="card-title">Palgastatistika oskuse järgi</h2>
        </div>

        <div class="salary-filter">
          <select
            class="select"
            [(ngModel)]="selectedSkill"
            (change)="loadSalaryStats()">
            <option value="">Kõik oskused</option>
            @for (skill of statsService.topSkills(); track skill.name) {
              <option [value]="skill.name">{{ skill.name | uppercase }}</option>
            }
          </select>
        </div>

        @if (salaryStats(); as stats) {
          <div class="salary-stats-grid">
            <div class="salary-stat">
              <div class="salary-value">
                {{ stats.avgSalary ? (stats.avgSalary | number:'1.0-0') + ' €' : 'N/A' }}
              </div>
              <div class="salary-label">Keskmine palk</div>
            </div>
            <div class="salary-stat">
              <div class="salary-value">
                {{ stats.minSalary ? (stats.minSalary | number:'1.0-0') + ' €' : 'N/A' }}
              </div>
              <div class="salary-label">Minimaalne</div>
            </div>
            <div class="salary-stat">
              <div class="salary-value">
                {{ stats.maxSalary ? (stats.maxSalary | number:'1.0-0') + ' €' : 'N/A' }}
              </div>
              <div class="salary-label">Maksimaalne</div>
            </div>
            <div class="salary-stat">
              <div class="salary-value">{{ stats.jobCount }}</div>
              <div class="salary-label">Tööpakkumisi</div>
            </div>
          </div>
        }
      </section>

      <div class="charts-grid">
        <!-- Oskuste populaarsus -->
        <section class="card chart-card">
          <div class="card-header">
            <h2 class="card-title">Top 20 nõutuimat oskust</h2>
          </div>
          @if (skillsChartData().length > 0) {
            <ngx-charts-bar-horizontal
              [results]="skillsChartData()"
              [xAxis]="true"
              [yAxis]="true"
              [showDataLabel]="true"
              [scheme]="colorScheme">
            </ngx-charts-bar-horizontal>
          } @else {
            <div class="empty-chart">Andmed puuduvad</div>
          }
        </section>

        <!-- Oskuste kategooriad -->
        <section class="card chart-card">
          <div class="card-header">
            <h2 class="card-title">Oskused kategooriate kaupa</h2>
          </div>
          @if (categoryChartData().length > 0) {
            <ngx-charts-pie-chart
              [results]="categoryChartData()"
              [legend]="true"
              [labels]="true"
              [doughnut]="true"
              [scheme]="colorScheme">
            </ngx-charts-pie-chart>
          } @else {
            <div class="empty-chart">Andmed puuduvad</div>
          }
        </section>
      </div>

      <!-- Ettevõtete edetabel -->
      <section class="card">
        <div class="card-header">
          <h2 class="card-title">Ettevõtete edetabel</h2>
        </div>
        <div class="companies-table">
          <table class="table">
            <thead>
              <tr>
                <th>#</th>
                <th>Ettevõte</th>
                <th>Tööpakkumisi</th>
              </tr>
            </thead>
            <tbody>
              @for (company of statsService.companies(); track company.id; let i = $index) {
                <tr>
                  <td>{{ i + 1 }}</td>
                  <td>{{ company.name }}</td>
                  <td>
                    <span class="badge badge-primary">{{ company.jobCount }}</span>
                  </td>
                </tr>
              }
              @empty {
                <tr>
                  <td colspan="3" class="empty-row">Andmed puuduvad</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </section>
    </div>
  `,
  styles: [`
    .trends-page {
      display: flex;
      flex-direction: column;
      gap: 2rem;
    }

    .page-title {
      font-size: 1.875rem;
      font-weight: 700;
      color: var(--text-primary);
    }

    .salary-filter {
      max-width: 300px;
      margin-bottom: 1.5rem;
    }

    .salary-stats-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 1.5rem;
    }

    @media (max-width: 768px) {
      .salary-stats-grid {
        grid-template-columns: repeat(2, 1fr);
      }
    }

    .salary-stat {
      text-align: center;
      padding: 1rem;
      background: var(--bg-secondary);
      border-radius: var(--border-radius);
    }

    .salary-value {
      font-size: 1.5rem;
      font-weight: 700;
      color: var(--primary-color);
      margin-bottom: 0.25rem;
    }

    .salary-label {
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
      min-height: 450px;
    }

    .chart-card ngx-charts-bar-horizontal,
    .chart-card ngx-charts-pie-chart {
      height: 380px;
    }

    .empty-chart {
      height: 380px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--text-muted);
    }

    .companies-table {
      overflow-x: auto;
    }

    .empty-row {
      text-align: center;
      color: var(--text-muted);
    }
  `]
})
export class TrendsComponent implements OnInit {
  statsService = inject(StatsService);

  // Värviskeemid graafikutele
  colorScheme = {
    domain: ['#2563eb', '#7c3aed', '#db2777', '#ea580c', '#16a34a', '#0891b2', '#4f46e5', '#be123c', '#15803d', '#0369a1']
  };

  // Valitud oskus palgastatistika jaoks
  selectedSkill = '';

  // Graafiku andmed
  skillsChartData = signal<any[]>([]);
  categoryChartData = signal<any[]>([]);
  salaryStats = signal<SalaryStats | null>(null);

  ngOnInit(): void {
    this.loadData();
  }

  private loadData(): void {
    // Lae top oskused
    this.statsService.loadTopSkills(20).subscribe(skills => {
      this.processSkillsData(skills);
    });

    // Lae ettevõtted
    this.statsService.loadCompanies(0, 20).subscribe();

    // Lae üldine palgastatistika
    this.loadSalaryStats();
  }

  loadSalaryStats(): void {
    const skill = this.selectedSkill || undefined;
    this.statsService.loadSalaryStats(skill).subscribe(stats => {
      this.salaryStats.set(stats);
    });
  }

  private processSkillsData(skills: SkillStats[]): void {
    // Horisontaalne tulpdiagramm
    this.skillsChartData.set(
      skills.map(s => ({
        name: s.name.toUpperCase(),
        value: s.jobCount
      }))
    );

    // Kategooriate pie chart
    const categoryMap = new Map<string, number>();
    skills.forEach(skill => {
      const category = this.getCategoryLabel(skill.category);
      categoryMap.set(category, (categoryMap.get(category) || 0) + skill.jobCount);
    });

    this.categoryChartData.set(
      Array.from(categoryMap.entries()).map(([name, value]) => ({ name, value }))
    );
  }

  private getCategoryLabel(category: string): string {
    switch (category) {
      case 'LANGUAGE': return 'Programmeerimiskeeled';
      case 'FRAMEWORK': return 'Raamistikud';
      case 'DATABASE': return 'Andmebaasid';
      case 'DEVOPS': return 'DevOps';
      case 'SOFT_SKILL': return 'Pehmed oskused';
      default: return category;
    }
  }
}
