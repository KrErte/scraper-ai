import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap, catchError, of } from 'rxjs';

import { environment } from '../../environments/environment';
import { JobPosting, PagedResponse, JobFilters } from '../models/job.model';

/**
 * Tööpakkumiste teenus.
 * Haldab tööpakkumiste päringuid ja olekut signalite abil.
 */
@Injectable({
  providedIn: 'root'
})
export class JobService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/jobs`;

  // Signalid oleku haldamiseks
  private _jobs = signal<JobPosting[]>([]);
  private _loading = signal(false);
  private _error = signal<string | null>(null);
  private _totalElements = signal(0);
  private _totalPages = signal(0);
  private _currentPage = signal(0);

  // Arvutatud signalid (computed)
  readonly jobs = this._jobs.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();
  readonly totalElements = this._totalElements.asReadonly();
  readonly totalPages = this._totalPages.asReadonly();
  readonly currentPage = this._currentPage.asReadonly();

  // Kas on tööpakkumisi
  readonly hasJobs = computed(() => this._jobs().length > 0);

  /**
   * Lae tööpakkumised filtritega.
   */
  loadJobs(filters: JobFilters = {}): Observable<PagedResponse<JobPosting>> {
    this._loading.set(true);
    this._error.set(null);

    let params = new HttpParams()
      .set('page', (filters.page ?? 0).toString())
      .set('size', (filters.size ?? 20).toString());

    if (filters.skill) {
      params = params.set('skill', filters.skill);
    }
    if (filters.location) {
      params = params.set('location', filters.location);
    }
    if (filters.salaryMin) {
      params = params.set('salaryMin', filters.salaryMin.toString());
    }

    return this.http.get<PagedResponse<JobPosting>>(this.apiUrl, { params }).pipe(
      tap(response => {
        this._jobs.set(response.content);
        this._totalElements.set(response.totalElements);
        this._totalPages.set(response.totalPages);
        this._currentPage.set(response.number);
        this._loading.set(false);
      }),
      catchError(error => {
        this._error.set('Tööpakkumiste laadimine ebaõnnestus');
        this._loading.set(false);
        console.error('Viga tööpakkumiste laadimisel:', error);
        return of({ content: [], totalElements: 0, totalPages: 0, size: 20, number: 0, first: true, last: true });
      })
    );
  }

  /**
   * Lae üksik tööpakkumine ID järgi.
   */
  getJob(id: number): Observable<JobPosting | null> {
    this._loading.set(true);
    this._error.set(null);

    return this.http.get<JobPosting>(`${this.apiUrl}/${id}`).pipe(
      tap(() => this._loading.set(false)),
      catchError(error => {
        this._error.set('Tööpakkumise laadimine ebaõnnestus');
        this._loading.set(false);
        console.error('Viga tööpakkumise laadimisel:', error);
        return of(null);
      })
    );
  }
}
