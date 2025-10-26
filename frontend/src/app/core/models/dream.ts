/**
 * Mood enum matching backend Mood enum values.
 * Backend: pl.kalin.dreamlog.dream.model.Mood
 */
export enum Mood {
  POSITIVE = 'POSITIVE',
  NEUTRAL = 'NEUTRAL',
  NEGATIVE = 'NEGATIVE',
  NIGHTMARE = 'NIGHTMARE',
  MIXED = 'MIXED',
}

/**
 * Dream entry model matching backend DreamResponse.
 * Backend: pl.kalin.dreamlog.dream.dto.DreamResponse
 */
export interface Dream {
  id: string; // UUID
  date: string; // ISO LocalDate format (YYYY-MM-DD)
  title: string;
  content: string;
  moodInDream: Mood | null;
  moodAfterDream: Mood | null;
  vividness: number; // 0-10
  lucid: boolean;
  tags: string[];
}

/**
 * Request for creating a new dream entry.
 * Title is optional - if not provided, it will be auto-generated from content on backend.
 * Backend: pl.kalin.dreamlog.dream.dto.DreamCreateRequest
 */
export interface CreateDreamRequest {
  date: string; // ISO LocalDate format (YYYY-MM-DD)
  title?: string; // Optional - auto-generated if not provided
  content: string;
  moodInDream?: Mood;
  moodAfterDream?: Mood;
  vividness?: number;
  lucid?: boolean;
  tags?: string[];
}

/**
 * Request for updating an existing dream entry.
 * Backend: pl.kalin.dreamlog.dream.dto.DreamUpdateRequest
 */
export type UpdateDreamRequest = CreateDreamRequest;

/**
 * Paginated response from backend.
 * Matches Spring Data Page<T> structure.
 */
export interface PagedResponse<T> {
  content: T[]; // Array of items
  totalElements: number; // Total count across all pages
  totalPages: number; // Total number of pages
  size: number; // Page size
  number: number; // Current page number (0-indexed)
  first: boolean; // Is first page
  last: boolean; // Is last page
  empty: boolean; // Is content empty
}
