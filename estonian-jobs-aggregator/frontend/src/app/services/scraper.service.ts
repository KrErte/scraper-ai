import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, catchError, of } from 'rxjs';

import { environment } from '../../environments/environment';

export interface ScraperResult {
  newJobs: number;
  updatedJobs: number;
  errors: number;
  errorMessages: string[];
}

export interface ScraperSummary {
  totalNew: number;
  totalUpdated: number;
  totalErrors: number;
}

export interface AllScrapersResult {
  cvEe: ScraperResult;
  cvKeskus: ScraperResult;
  summary: ScraperSummary;
}

/**
 * Scraperi teenus.
 * Võimaldab käivitada scrapingut frontend'ist.
 */
@Injectable({
  providedIn: 'root'
})
export class ScraperService {
  private http = inject(HttpClient);
  private apiUrl = environment.apiUrl;

  // Signalid oleku haldamiseks
  private _scraping = signal(false);
  private _lastResult = signal<AllScrapersResult | null>(null);
  private _error = signal<string | null>(null);

  // Avalikud signalid
  readonly scraping = this._scraping.asReadonly();
  readonly lastResult = this._lastResult.asReadonly();
  readonly error = this._error.asReadonly();

  /**
   * Käivita kõikide allikate scraping.
   */
  triggerAllScrapers(): Observable<AllScrapersResult | null> {
    this._scraping.set(true);
    this._error.set(null);

    return this.http.post<AllScrapersResult>(`${this.apiUrl}/scraper/trigger`, {}).pipe(
      tap(result => {
        this._lastResult.set(result);
        this._scraping.set(false);
      }),
      catchError(error => {
        this._error.set('Scraping ebaõnnestus: ' + error.message);
        this._scraping.set(false);
        console.error('Viga scrapimisel:', error);
        return of(null);
      })
    );
  }

  /**
   * Käivita ainult CV.ee scraping.
   */
  triggerCvEe(): Observable<ScraperResult | null> {
    this._scraping.set(true);
    this._error.set(null);

    return this.http.post<ScraperResult>(`${this.apiUrl}/scraper/trigger/cv-ee`, {}).pipe(
      tap(() => this._scraping.set(false)),
      catchError(error => {
        this._error.set('CV.ee scraping ebaõnnestus');
        this._scraping.set(false);
        return of(null);
      })
    );
  }

  /**
   * Käivita ainult CV Keskus scraping.
   */
  triggerCvKeskus(): Observable<ScraperResult | null> {
    this._scraping.set(true);
    this._error.set(null);

    return this.http.post<ScraperResult>(`${this.apiUrl}/scraper/trigger/cv-keskus`, {}).pipe(
      tap(() => this._scraping.set(false)),
      catchError(error => {
        this._error.set('CV Keskus scraping ebaõnnestus');
        this._scraping.set(false);
        return of(null);
      })
    );
  }

  /**
   * Kontrolli scraperi staatust.
   */
  getStatus(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/scraper/status`);
  }
}
