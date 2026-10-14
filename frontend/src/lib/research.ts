import { Sparkles, Layers3, CircleHelp } from "lucide-react";

export const suggestions = [
  {
    icon: Sparkles,
    title: "Get the big picture",
    question: "Summarize the main contribution and key findings of this paper.",
  },
  {
    icon: Layers3,
    title: "Unpack the method",
    question: "Explain the proposed method step by step, in simple terms.",
  },
  {
    icon: CircleHelp,
    title: "Look a little deeper",
    question: "What limitations or assumptions does this paper discuss?",
  },
];
export const errorText = (error: unknown) =>
  error instanceof Error ? error.message : "Something went wrong. Try again.";
