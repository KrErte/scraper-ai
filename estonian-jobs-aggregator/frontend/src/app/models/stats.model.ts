// Statistika mudelid

// Oskuse statistika
export interface SkillStats {
  name: string;
  category: SkillCategory;
  jobCount: number;
}

export type SkillCategory = 'LANGUAGE' | 'FRAMEWORK' | 'DATABASE' | 'DEVOPS' | 'SOFT_SKILL';

// Palgastatistika
export interface SalaryStats {
  skill: string | null;
  avgSalary: number | null;
  minSalary: number | null;
  maxSalary: number | null;
  jobCount: number;
}

// Trendi andmed
export interface TrendData {
  date: string;
  count: number;
}

// Dashboard koondstatistika
export interface DashboardStats {
  totalJobs: number;
  totalCompanies: number;
  avgSalary: number | null;
  topSkill: string | null;
  newJobsToday: number;
}

// Ettevõte
export interface Company {
  id: number;
  name: string;
  jobCount: number;
}
