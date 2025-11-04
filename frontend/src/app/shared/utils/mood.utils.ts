import {Mood} from '../../core/models/dream';

/**
 * Get CSS class for mood badge styling.
 * Pure function for mood-to-class mapping.
 */
export function getMoodClass(mood: Mood | null | undefined): string {
  if (!mood) return 'mood-neutral';

  const moodMap: Record<Mood, string> = {
    [Mood.POSITIVE]: 'mood-happy',
    [Mood.NEUTRAL]: 'mood-neutral',
    [Mood.NEGATIVE]: 'mood-sad',
    [Mood.NIGHTMARE]: 'mood-anxious',
    [Mood.MIXED]: 'mood-neutral',
  };

  return moodMap[mood] || 'mood-neutral';
}

/**
 * Get human-readable mood label.
 * Converts "POSITIVE" -> "Positive"
 */
export function getMoodLabel(mood: string | null): string {
  if (!mood) return 'No data';
  return mood.charAt(0) + mood.slice(1).toLowerCase();
}

/**
 * Get emoji representation for mood.
 * Supports both Mood enum values and string mood names.
 */
export function getMoodEmoji(mood: string | null | undefined): string {
  if (!mood) return '😐';

  const moodMap: Record<string, string> = {
    HAPPY: '😊',
    SAD: '😢',
    NEUTRAL: '😐',
    EXCITED: '🤩',
    ANXIOUS: '😰',
    PEACEFUL: '😌',
    CONFUSED: '🤔',
    SCARED: '😨',
    POSITIVE: '😊',
    NEGATIVE: '😢',
    NIGHTMARE: '😰',
    MIXED: '🤔',
  };

  return moodMap[mood.toUpperCase()] || '😐';
}
