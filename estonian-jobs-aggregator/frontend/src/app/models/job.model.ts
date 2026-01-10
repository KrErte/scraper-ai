// Tööpakkumise mudel

export interface JobPosting {
  id: number;
  title: string;
  company: string;
  location: string | null;
  salaryMin: number | null;
  salaryMax: number | null;
  salaryCurrency: string;
  description: string | null;
  url: string;
  source: JobSource;
  postedDate: string | null;
  scrapedAt: string;
  expiresAt: string | null;
  skills: string[];
}

export type JobSource = 'CV_EE' | 'CV_KESKUS';

// Tööpakkumiste pagineeritud vastus
export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// Otsingu filtrid
export interface JobFilters {
  skill?: string;
  location?: string;
  salaryMin?: number;
  page?: number;
  size?: number;
}
