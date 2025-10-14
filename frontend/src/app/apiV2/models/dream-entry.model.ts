export enum Mood {
  Happy = 'HAPPY',
  Sad = 'SAD',
  Fear = 'FEAR',
  Neutral = 'NEUTRAL',
  Anxious = 'ANXIOUS',
  Excited = 'EXCITED',
}

export interface DreamEntry {
  id?: string;
  userId: string;
  date: string;
  title: string;
  content: string;
  moodInDream?: Mood;
  moodAfterDream?: Mood;
  vividness?: number;
  lucid?: boolean;
  tags?: string[];
}

export interface CreateDreamEntryRequest {
  date: string;
  title: string;
  content: string;
  moodInDream?: Mood;
  moodAfterDream?: Mood;
  vividness?: number;
  lucid?: boolean;
  tags?: string[];
}

export type UpdateDreamEntryRequest = CreateDreamEntryRequest;
