import { ArrowRight, ArrowUpRight, FileText, LockKeyhole } from "lucide-react";
import { PaperArt } from "../../components/PaperArt";
import { StatusBadge } from "../../components/StatusBadge";
import { ImportForm } from "../library/ImportPaper";
import { suggestions } from "../../lib/research";
import type { User, Paper } from "../../lib/types";

export function Home({
  user,
  papers,
  onImport,
  onSelect,
  onLibrary,
}: {
  user: User;
  papers: Paper[];
  onImport: (p: Paper) => void;
  onSelect: (p: Paper) => void;
  onLibrary: () => void;
}) {
  return (
    <main className="home-page">
      <div className="home-intro">
        <div className="greeting">
          <span className="greeting-line" />A space for your curiosity
        </div>
        <h1>
          A paper is just
          <br />
          the beginning.
        </h1>
        <p>
          Go from reading to understanding.
          <br />
          Explore research with answers grounded in the paper.
        </p>
        <div className="home-import">
          <ImportForm onImport={onImport} />
          <div className="import-caption">
            <LockKeyhole size={12} />
            Powered by your local Ollama
            <span />
            No cloud AI required
          </div>
        </div>
        <div className="example-line">
          Need a starting point?
          <button onClick={() => fillExample()}>
            Try Attention Is All You Need <ArrowUpRight size={13} />
          </button>
        </div>
      </div>
      <PaperArt />
      <div className="home-bottom">
        <div className="section-heading">
          <div>
            <h2>
              {papers.length
                ? "Pick up a paper."
                : `Follow your curiosity, ${user.name.split(" ")[0]}.`}
            </h2>
            <p>
              {papers.length
                ? "A shared library. An individual perspective."
                : "Start with a question. Find your way into the details."}
            </p>
          </div>
          {papers.length > 0 && (
            <button className="text-button" onClick={onLibrary}>
              View library <ArrowRight size={15} />
            </button>
          )}
        </div>
        {papers.length ? (
          <div className="home-recent-grid">
            {papers.slice(0, 3).map((p) => (
              <button
                key={p.id}
                className="recent-card"
                onClick={() => onSelect(p)}
              >
                <div>
                  <FileText size={19} />
                  <StatusBadge paper={p} />
                </div>
                <h3>{p.title}</h3>
                <span>
                  arXiv {p.arxivId}
                  <ArrowUpRight size={16} />
                </span>
              </button>
            ))}
          </div>
        ) : (
          <div className="possibilities">
            {suggestions.map(({ icon: Icon, title, question }) => (
              <div className="possibility" key={title}>
                <Icon size={22} />
                <h3>{title}</h3>
                <p>{question.replace("this paper", "a paper")}</p>
              </div>
            ))}
          </div>
        )}
      </div>
      <div className="home-footer">
        <span>
          <span className="tiny-folio" />
          Less searching. More understanding.
        </span>
        <span>Built around your questions.</span>
      </div>
    </main>
  );
}

function fillExample() {
  window.dispatchEvent(
    new CustomEvent("example-paper", {
      detail: "https://arxiv.org/abs/1706.03762",
    }),
  );
}
