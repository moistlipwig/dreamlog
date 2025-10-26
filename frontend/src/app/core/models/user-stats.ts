import { Mood } from './dream';

/**
 * User statistics model matching backend UserStatsDto.
 * Backend: pl.kalin.dreamlog.user.dto.UserStatsDto
 */
export interface UserStats {
  totalDreams: number;
  mostCommonMood: Mood | null;
  streak?: number; // Day streak (optional for now)
  aiAnalyses?: number; // Number of AI analyses (optional for now)
}
