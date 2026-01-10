import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap, catchError, of, forkJoin } from 'rxjs';

import { environment } from '../../environments/environment';
import { SkillStats, SalaryStats, TrendData, DashboardStats, Company } from '../models/stats.model';
import { PagedResponse } from '../models/job.model';

/**
 * Statistika teenus.
 * Haldab statistiliste andmete päringuid ja olekut.
 */
@Injectable({
  providedIn: 'root'
})
export class StatsService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  // Signalid oleku haldamiseks
  private _dashboardStats = signal<DashboardStats | null>(null);
  private _topSkills = signal<SkillStats[]>([]);
  private _trends = signal<TrendData[]>([]);
  private _salaryStats = signal<SalaryStats | null>(null);
  private _companies = signal<Company[]>([]);
  private _loading = signal(false);
  private _error = signal<string | null>(null);

  // Avalikud signalid
  readonly dashboardStats = this._dashboardStats.asReadonly();
  readonly topSkills = this._topSkills.asReadonly();
  readonly trends = this._trends.asReadonly();
  readonly salaryStats = this._salaryStats.asReadonly();
  readonly companies = this._companies.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  /**
   * Lae dashboard koondstatistika.
   */
  loadDashboardStats(): Observable<DashboardStats | null> {
    this._loading.set(true);

    return this.http.get<DashboardStats>(`${this.apiUrl}/stats/dashboard`).pipe(
      tap(stats => {
        this._dashboardStats.set(stats);
        this._loading.set(false);
      }),
      catchError(error => {
        this._error.set('Statistika laadimine ebaõnnestus');
        this._loading.set(false);
        console.error('Viga statistika laadimisel:', error);
        return of(null);
      })
    );
  }

  /**
   * Lae top oskused.
   */
  loadTopSkills(limit: number = 20): Observable<SkillStats[]> {
    const params = new HttpParams().set('limit', limit.toString());

    return this.http.get<SkillStats[]>(`${this.apiUrl}/stats/skills`, { params }).pipe(
      tap(skills => this._topSkills.set(skills)),
      catchError(error => {
        console.error('Viga oskuste laadimisel:', error);
        return of([]);
      })
    );
  }

  /**
   * Lae tööpakkumiste trend.
   */
  loadTrends(days: number = 30): Observable<TrendData[]> {
    const params = new HttpParams().set('days', days.toString());

    return this.http.get<TrendData[]>(`${this.apiUrl}/stats/trends`, { params }).pipe(
      tap(trends => this._trends.set(trends)),
      catchError(error => {
        console.error('Viga trendide laadimisel:', error);
        return of([]);
      })
    );
  }

  /**
   * Lae palgastatistika (valikuliselt oskuse järgi).
   */
  loadSalaryStats(skill?: string): Observable<SalaryStats | null> {
    let params = new HttpParams();
    if (skill) {
      params = params.set('skill', skill);
    }

    return this.http.get<SalaryStats>(`${this.apiUrl}/stats/salaries`, { params }).pipe(
      tap(stats => this._salaryStats.set(stats)),
      catchError(error => {
        console.error('Viga palgastatistika laadimisel:', error);
        return of(null);
      })
    );
  }

  /**
   * Lae ettevõtted.
   */
  loadCompanies(page: number = 0, size: number = 20): Observable<PagedResponse<Company>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PagedResponse<Company>>(`${this.apiUrl}/companies`, { params }).pipe(
      tap(response => this._companies.set(response.content)),
      catchError(error => {
        console.error('Viga ettevõtete laadimisel:', error);
        return of({ content: [], totalElements: 0, totalPages: 0, size: 20, number: 0, first: true, last: true });
      })
    );
  }

  /**
   * Lae kõik dashboard andmed korraga.
   */
  loadAllDashboardData(): Observable<[DashboardStats | null, SkillStats[], TrendData[]]> {
    this._loading.set(true);

    return forkJoin([
      this.loadDashboardStats(),
      this.loadTopSkills(10),
      this.loadTrends(30)
    ]).pipe(
      tap(() => this._loading.set(false)),
      catchError(error => {
        this._loading.set(false);
        console.error('Viga dashboard andmete laadimisel:', error);
        return of([null, [], []] as [DashboardStats | null, SkillStats[], TrendData[]]);
      })
    );
  }
}
