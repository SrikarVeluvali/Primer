export type User = { id: string; name: string; email: string };
export type Paper = {
  id: string;
  arxivId: string;
  title: string;
  authors: string;
  abstractText: string;
  published: string;
  status: "QUEUED" | "INDEXING" | "READY" | "FAILED";
  stage: string;
  error: string | null;
  pageCount: number;
  chunkCount: number;
  indexVersion: number;
  createdAt: string;
  tags: string[];
};
export type Connection = {
  connected: boolean;
  models: string[];
  chatModel: string;
  embeddingModel: string;
  message: string;
};
export type Source = {
  number: number;
  page: number;
  text: string;
  similarity: number;
};
export type Turn = {
  id: string;
  question: string;
  answer: string;
  sources: Source[];
  indexVersion: number;
  model: string;
  createdAt: string;
};
export type Session = { user: User | null; csrfToken: string };
export type Theme = {
  id: string;
  name: string;
  mode: "light" | "dark";
  background: string;
  surface: string;
  panel: string;
  text: string;
  muted: string;
  accent: string;
  border: string;
};
export type Preferences = { activeTheme: string; themes: Theme[] };
export type Note = { text: string; updatedAt: string | null };
export type LearningOption = { id: string; text: string };
export type LearningTopic = {
  id: string;
  name: string;
  why: string;
};
export type Assessment = { overview: string; topics: LearningTopic[] };
export type LearningAnswers = {
  answers: Record<string, string>;
  confidence: Record<string, number>;
  goal: string;
  minutesPerDay: number;
  preference: string;
};
export type Prerequisite = {
  name: string;
  reason: string;
  readiness: "learn" | "review" | "comfortable";
};
export type LearningStep = {
  id: string;
  title: string;
  goal: string;
  activity: string;
  checkpoint: string;
  minutes: number;
  topics: string[];
};
export type LearningResource = {
  id: string;
  title: string;
  url: string;
  type: "youtube" | "blog" | "reddit";
  description: string;
  topic: string;
  origin: string;
};
export type LearningPlan = {
  summary: string;
  prerequisites: Prerequisite[];
  roadmap: LearningStep[];
  resources: LearningResource[];
  resourceStatus: string;
  generatedAt: string;
};
export type LearningState = {
  assessment: Assessment | null;
  answers: LearningAnswers | null;
  plan: LearningPlan | null;
  completedSteps: string[];
};
export type KnowledgeTag = { id: string; name: string; level: number; interested: boolean; updatedAt: string };
export type KnowledgeProfile = { tags: KnowledgeTag[] };
export type CatalogTag = { id: string; name: string; paperCount: number };
export type Recommendation = { paper: Paper; reasons: string[]; matchedTags: string[]; score: number };
export type HighlightRect = { x: number; y: number; width: number; height: number };
export type PdfHighlight = { id: string; page: number; text: string; rectangles: HighlightRect[]; color: "amber" | "green" | "blue" | "pink"; createdAt: string };
