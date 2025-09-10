export interface Dream {
  id: number;
  title: string;
  content: string;
  date: string; // ISO
  tags: string[];
  mood: number; // 1-5
}

export type Tag = string;

export type Mood = 1 | 2 | 3 | 4 | 5;
