import { LoaderCircle } from "lucide-react";
import type { Paper } from "../lib/types";

export function StatusBadge({ paper }: { paper: Paper }) {
  return (
    <span className={`status-badge status-${paper.status.toLowerCase()}`}>
      {paper.status === "READY" ? (
        <>
          <span />
          Ready to explore
        </>
      ) : paper.status === "FAILED" ? (
        "Needs attention"
      ) : (
        <>
          <LoaderCircle size={11} className="spin" />
          Indexing
        </>
      )}
    </span>
  );
}
