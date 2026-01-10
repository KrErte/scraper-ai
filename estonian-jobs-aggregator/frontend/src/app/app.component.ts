import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';

/**
 * Rakenduse juurkomponent.
 * Sisaldab navigatsiooni ja peamist sisu ala.
 */
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="app-container">
      <!-- Navigatsioon -->
      <nav class="navbar">
        <div class="container">
          <a routerLink="/" class="logo">
            <span class="logo-icon">💼</span>
            <span class="logo-text">IT Töökohad</span>
          </a>

          <ul class="nav-links">
            <li>
              <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: true }">
                Dashboard
              </a>
            </li>
            <li>
              <a routerLink="/jobs" routerLinkActive="active">
                Tööpakkumised
              </a>
            </li>
            <li>
              <a routerLink="/trends" routerLinkActive="active">
                Trendid
              </a>
            </li>
          </ul>
        </div>
      </nav>

      <!-- Peamine sisu -->
      <main class="main-content">
        <div class="container">
          <router-outlet />
        </div>
      </main>

      <!-- Jalus -->
      <footer class="footer">
        <div class="container">
          <p>&copy; 2024 Eesti IT Töökohad. Andmed kogutud CV.ee-st.</p>
        </div>
      </footer>
    </div>
  `,
  styles: [`
    .app-container {
      min-height: 100vh;
      display: flex;
      flex-direction: column;
    }

    .navbar {
      background: var(--bg-primary);
      border-bottom: 1px solid var(--border-color);
      padding: 0.75rem 0;
      position: sticky;
      top: 0;
      z-index: 100;
    }

    .navbar .container {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .logo {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 1.25rem;
      font-weight: 700;
      color: var(--text-primary);
      text-decoration: none;
    }

    .logo:hover {
      text-decoration: none;
    }

    .logo-icon {
      font-size: 1.5rem;
    }

    .nav-links {
      display: flex;
      list-style: none;
      gap: 0.5rem;
    }

    .nav-links a {
      display: block;
      padding: 0.5rem 1rem;
      color: var(--text-secondary);
      font-weight: 500;
      border-radius: var(--border-radius);
      text-decoration: none;
      transition: all 0.15s ease;
    }

    .nav-links a:hover {
      color: var(--text-primary);
      background: var(--bg-tertiary);
      text-decoration: none;
    }

    .nav-links a.active {
      color: var(--primary-color);
      background: rgba(37, 99, 235, 0.1);
    }

    .main-content {
      flex: 1;
      padding: 2rem 0;
    }

    .footer {
      background: var(--bg-primary);
      border-top: 1px solid var(--border-color);
      padding: 1.5rem 0;
      text-align: center;
      color: var(--text-muted);
      font-size: 0.875rem;
    }

    @media (max-width: 768px) {
      .navbar .container {
        flex-direction: column;
        gap: 1rem;
      }

      .nav-links {
        width: 100%;
        justify-content: center;
      }
    }
  `]
})
export class AppComponent {
  title = 'Eesti IT Töökohad';
}
