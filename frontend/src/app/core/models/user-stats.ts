import { Mood } from './dream';

/**
 * User statistics model matching backend UserStatsDto.
 * Backend: pl.kalin.dreamlog.user.dto.UserStatsDto
 */
export interface UserStats {
  totalDreams: number;
  mostCommonMood: Mood | null;
}
