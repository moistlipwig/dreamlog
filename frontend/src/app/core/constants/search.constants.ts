/** Minimum query length for search (characters) */
export const MIN_QUERY_LENGTH = 3;

/** Debounce delay for search input (milliseconds) */
export const SEARCH_DEBOUNCE_MS = 300;

/** Routes where search state persists */
export const SEARCH_RELEVANT_ROUTES = [
  '/app/dreams',
  '/app/search',
  '/app/dashboard',
] as const;
