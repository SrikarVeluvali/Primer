import { lazy, Suspense, useState, useEffect, useRef } from "react";
import {
  ArrowUp,
  ArrowUpRight,
  BookOpen,
  Check,
  ChevronDown,
  ChevronRight,
  Copy,
  FileText,
  FolderOpen,
  Layers3,
  LoaderCircle,
  LockKeyhole,
  RefreshCw,
  ShieldCheck,
  Sparkles,
  Trash2,
  X,
  GraduationCap,
  Maximize2,
  Minimize2,
  StickyNote,
} from "lucide-react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { api, post } from "../../lib/api";
import { suggestions, errorText } from "../../lib/research";
import { StatusBadge } from "../../components/StatusBadge";
import type { Paper, Connection, Turn, Source, PdfHighlight } from "../../lib/types";
import { NotesPanel, appendToNotes } from "./NotesPanel";
import { LearnPanel } from "../learn/LearnPanel";
import { PaperTags } from "../knowledge/PaperTags";

const PdfReader = lazy(() => import("./PdfReader"));

export function PaperWorkspace({
  paper,
  model,
  connection,
  onModel,
  onRefresh,
  userId,
}: {
  paper: Paper;
  model: string;
  connection: Connection | null;
  onModel: (s: string) => void;
  onRefresh: () => void;
  userId: string;
}) {
  const [turns, setTurns] = useState<Turn[]>([]);
  const [question, setQuestion] = useState("");
  const [busy, setBusy] = useState(false);
  const [historyLoading, setHistoryLoading] = useState(true);
  const [error, setError] = useState("");
  const [pending, setPending] = useState("");
  const [panel, setPanel] = useState<"overview" | "sources" | "pdf" | "notes">(
    "overview",
  );
  const [selectedTurn, setSelectedTurn] = useState<string | null>(null);
  const [sourcePage, setSourcePage] = useState(1);
  const [panelOpen, setPanelOpen] = useState(false);
  const [confirmClear, setConfirmClear] = useState(false);
  const [mode, setMode] = useState<"chat" | "learn">("chat");
  const [expanded, setExpanded] = useState<"conversation" | "details" | null>(
    null,
  );
  const [panelWidth, setPanelWidth] = useState(
    Number(localStorage.getItem(`primer-panel-width-${userId}`)) || 380,
  );
  const workspaceRef = useRef<HTMLElement>(null);
  const dragStart = useRef<{ x: number; width: number } | null>(null);
  const endRef = useRef<HTMLDivElement>(null);
  const mounted = useRef(true);
  const textarea = useRef<HTMLTextAreaElement>(null);
  const activeTurn =
    turns.find((t) => t.id === selectedTurn) || turns[turns.length - 1];
  function resize(value: number) {
    const max = Math.max(
      280,
      Math.min(
        720,
        (workspaceRef.current?.getBoundingClientRect().width || 1100) - 340,
      ),
    );
    const width = Math.max(280, Math.min(max, value));
    setPanelWidth(width);
    localStorage.setItem(`primer-panel-width-${userId}`, String(width));
  }
  function showNotes() {
    setPanel("notes");
    setPanelOpen(true);
  }
  useEffect(() => {
    const key = (e: KeyboardEvent) => {
      if (e.key === "Escape") setExpanded(null);
    };
    window.addEventListener("keydown", key);
    return () => window.removeEventListener("keydown", key);
  }, []);
  useEffect(() => {
    mounted.current = true;
    api<Turn[]>(`/papers/${paper.id}/chat`)
      .then((t) => {
        if (mounted.current) setTurns(t);
      })
      .catch((e) => {
        if (mounted.current) setError(errorText(e));
      })
      .finally(() => {
        if (mounted.current) setHistoryLoading(false);
      });
    return () => {
      mounted.current = false;
    };
  }, [paper.id]);
  useEffect(() => {
    endRef.current?.scrollIntoView({
      behavior: window.matchMedia("(prefers-reduced-motion: reduce)").matches
        ? "instant"
        : "smooth",
    });
  }, [turns, busy]);
  async function ask(value = question) {
    if (!value.trim() || busy || paper.status !== "READY" || historyLoading)
      return;
    setBusy(true);
    setError("");
    setPending(value.trim());
    setQuestion("");
    try {
      const turn = await post<Turn>(`/papers/${paper.id}/chat`, {
        question: value.trim(),
        model,
      });
      if (mounted.current) {
        setTurns((old) => [...old, turn]);
        setSelectedTurn(turn.id);
        setPanel("sources");
      }
    } catch (e) {
      if (mounted.current) {
        setError(errorText(e));
        setQuestion(value);
      }
    } finally {
      if (mounted.current) {
        setBusy(false);
        setPending("");
        textarea.current?.focus();
      }
    }
  }
  async function clear() {
    try {
      await api(`/papers/${paper.id}/chat`, { method: "DELETE" });
      setTurns([]);
      setSelectedTurn(null);
      setPanel("overview");
      setConfirmClear(false);
    } catch (e) {
      setError(errorText(e));
    }
  }
  async function explain(highlight: PdfHighlight) {
    if (busy || historyLoading) return;
    setMode("chat"); setExpanded(null); setPanelOpen(false); setBusy(true); setError("");
    setPending(`Explain this highlight from page ${highlight.page} in simple terms:\n\n> ${highlight.text.replace(/\n/g, "\n> ")}`);
    try {
      const turn = await post<Turn>(`/papers/${paper.id}/annotations/${highlight.id}/explain`, { model });
      if (mounted.current) { setTurns(old => [...old, turn]); setSelectedTurn(turn.id); }
    } catch (e) { if (mounted.current) setError(errorText(e)); }
    finally { if (mounted.current) { setBusy(false); setPending(""); } }
  }
  async function retry() {
    try {
      await post(`/papers/${paper.id}/retry`);
      onRefresh();
    } catch (e) {
      setError(errorText(e));
    }
  }
  function sourceView(turn: Turn) {
    setSelectedTurn(turn.id);
    setPanel("sources");
    setPanelOpen(true);
  }
  return (
    <main
      ref={workspaceRef}
      className={`paper-workspace ${expanded ? `expanded-${expanded}` : ""}`}
      data-mode={mode}
      style={
        { "--research-panel-width": `${panelWidth}px` } as React.CSSProperties
      }
    >
      <section className="conversation">
        <div className="conversation-toolbar">
          <span>
            <span className="connection-dot online" />
            {mode === "chat" ? "Your conversation" : "Your learning plan"}
          </span>
          <div>
            {mode === "learn" && (
              <button className="text-button" onClick={() => setMode("chat")}>
                Chat
              </button>
            )}
            <button
              className={`learn-button ${mode === "learn" ? "active" : ""}`}
              onClick={() => setMode(mode === "learn" ? "chat" : "learn")}
              aria-pressed={mode === "learn"}
            >
              <GraduationCap size={16} />
              Learn
            </button>
            <button
              className="text-button"
              onClick={() => {
                setPanelOpen(!panelOpen);
              }}
            >
              <BookOpen size={15} />
              Paper & notes
            </button>
            {turns.length > 0 && (
              <button
                className="icon-button"
                aria-label="Clear my conversation"
                title="Clear my conversation"
                onClick={() => setConfirmClear(true)}
                disabled={busy}
              >
                <Trash2 size={15} />
              </button>
            )}
            <button
              className="icon-button expand-conversation"
              aria-label={
                expanded === "conversation"
                  ? "Collapse conversation panel"
                  : "Expand conversation panel"
              }
              onClick={() =>
                setExpanded(expanded === "conversation" ? null : "conversation")
              }
            >
              {expanded === "conversation" ? (
                <Minimize2 size={15} />
              ) : (
                <Maximize2 size={15} />
              )}
            </button>
          </div>
        </div>
        {mode === "learn" && (
          <LearnPanel
            paper={paper}
            model={model}
            userId={userId}
            onNotes={showNotes}
          />
        )}
        <div className="conversation-scroll">
          {paper.status !== "READY" ? (
            <div className="index-state">
              <span
                className={`large-symbol ${paper.status === "FAILED" ? "failed-symbol" : ""}`}
              >
                {paper.status === "FAILED" ? (
                  <FileText size={30} />
                ) : (
                  <LoaderCircle size={30} className="spin" />
                )}
              </span>
              <h2>
                {paper.status === "FAILED"
                  ? "This paper needs a little attention."
                  : "Making sense of the paper."}
              </h2>
              <p>{paper.status === "FAILED" ? paper.error : paper.stage}</p>
              {paper.status === "FAILED" ? (
                <button className="button primary" onClick={retry}>
                  <RefreshCw size={16} />
                  Retry indexing
                </button>
              ) : (
                <div className="index-steps">
                  <span>Fetch paper</span>
                  <ChevronRight size={13} />
                  <span>Read pages</span>
                  <ChevronRight size={13} />
                  <span>Build index</span>
                </div>
              )}
            </div>
          ) : historyLoading ? (
            <div className="index-state">
              <LoaderCircle size={24} className="spin" />
              <p>Opening your conversation…</p>
            </div>
          ) : !turns.length && !busy ? (
            <div className="chat-welcome">
              <div className="welcome-icon">
                <Sparkles size={27} />
              </div>
              <h2>What will you discover?</h2>
              <p>
                You’ve brought the paper.
                <br />
                Now bring your questions.
              </p>
              <div className="chat-suggestions">
                {suggestions.map(({ icon: Icon, title, question }) => (
                  <button key={title} onClick={() => ask(question)}>
                    <Icon size={19} />
                    <span>
                      {title}
                      <small>{question}</small>
                    </span>
                    <ArrowUpRight size={16} />
                  </button>
                ))}
              </div>
              <span className="grounded-label">
                <ShieldCheck size={13} />
                Answers grounded in this paper, with sources.
              </span>
            </div>
          ) : (
            <div className="turn-list">
              {turns.map((turn) => (
                <article className="chat-turn" key={turn.id}>
                  <div className="question-message">
                    <span>You</span>
                    <p>{turn.question}</p>
                  </div>
                  <div className="answer-message">
                    <div className="answer-label">
                      <span className="answer-logo">
                        <Sparkles size={14} />
                      </span>
                      Primer<span>{turn.model}</span>
                    </div>
                    <div className="markdown">
                      {(turn.indexVersion || 0) < (paper.indexVersion || 0) && (
                        <p className="outdated-answer">
                          This answer used an older paper index. Ask again to
                          use the corrected sources.
                        </p>
                      )}
                      <ReactMarkdown
                        remarkPlugins={[remarkGfm]}
                        components={{
                          a: ({ children, href }) => (
                            <a
                              href={href}
                              target="_blank"
                              rel="noopener noreferrer"
                            >
                              {children}
                            </a>
                          ),
                        }}
                      >
                        {turn.answer}
                      </ReactMarkdown>
                    </div>
                    <div className="answer-actions">
                      <button
                        onClick={() => {
                          appendToNotes(
                            paper.id,
                            `## ${turn.question}\n\n${turn.answer}`,
                          );
                          showNotes();
                        }}
                      >
                        <StickyNote size={14} />
                        Keep in notes
                      </button>
                      <button onClick={() => sourceView(turn)}>
                        <Layers3 size={14} />
                        {turn.sources.length} source passages
                        <ChevronRight size={13} />
                      </button>
                      <CopyButton text={turn.answer} />
                    </div>
                  </div>
                </article>
              ))}
              {busy && (
                <article className="chat-turn">
                  <div className="question-message">
                    <span>You</span>
                    <p>{pending}</p>
                  </div>
                  <div className="answer-message">
                    <div className="answer-label">
                      <span className="answer-logo">
                        <Sparkles size={14} />
                      </span>
                      Primer
                    </div>
                    <div className="thinking">
                      <span />
                      <span />
                      <span />
                      <p>Reading the relevant passages…</p>
                    </div>
                  </div>
                </article>
              )}
              <div ref={endRef} />
            </div>
          )}
        </div>
        <div className="composer-area">
          {error && (
            <div className="error-banner" role="alert">
              {error}
            </div>
          )}
          <form
            className="composer"
            onSubmit={(e) => {
              e.preventDefault();
              ask();
            }}
          >
            <textarea
              ref={textarea}
              aria-label="Ask a question about this paper"
              rows={2}
              maxLength={4000}
              placeholder={
                paper.status === "READY"
                  ? "Ask anything about this paper…"
                  : "Your questions can begin once the paper is ready…"
              }
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
              disabled={paper.status !== "READY" || busy || historyLoading}
              onKeyDown={(e) => {
                if (
                  e.key === "Enter" &&
                  !e.shiftKey &&
                  !e.nativeEvent.isComposing
                ) {
                  e.preventDefault();
                  ask();
                }
              }}
            />
            <div className="composer-bottom">
              <ModelPicker
                model={model}
                models={connection?.models || []}
                onModel={onModel}
              />
              <button
                className="send-button"
                disabled={
                  !question.trim() ||
                  busy ||
                  paper.status !== "READY" ||
                  historyLoading
                }
                aria-label="Send question"
              >
                {busy ? (
                  <LoaderCircle size={18} className="spin" />
                ) : (
                  <ArrowUp size={20} />
                )}
              </button>
            </div>
          </form>
          <div className="composer-caption">
            <span>
              <LockKeyhole size={11} />
              Only you can see this conversation
            </span>
            <span>Check important details against the sources.</span>
          </div>
        </div>
      </section>
      <div
        className="panel-resizer"
        role="separator"
        aria-orientation="vertical"
        aria-label="Resize conversation and paper panels"
        aria-valuemin={280}
        aria-valuemax={720}
        aria-valuenow={Math.round(panelWidth)}
        tabIndex={0}
        onPointerDown={(e) => {
          dragStart.current = { x: e.clientX, width: panelWidth };
          e.currentTarget.setPointerCapture(e.pointerId);
        }}
        onPointerMove={(e) => {
          if (dragStart.current)
            resize(dragStart.current.width + dragStart.current.x - e.clientX);
        }}
        onPointerUp={(e) => {
          dragStart.current = null;
          e.currentTarget.releasePointerCapture(e.pointerId);
        }}
        onPointerCancel={() => {
          dragStart.current = null;
        }}
        onKeyDown={(e) => {
          if (e.key === "ArrowLeft" || e.key === "ArrowRight") {
            e.preventDefault();
            resize(panelWidth + (e.key === "ArrowLeft" ? 20 : -20));
          }
          if (e.key === "Home") resize(280);
          if (e.key === "End") resize(720);
        }}
      >
        <span />
      </div>
      {panelOpen && (
        <button
          className="details-scrim"
          aria-label="Close paper details"
          onClick={() => setPanelOpen(false)}
        />
      )}
      <aside className={`paper-panel ${panelOpen ? "panel-open" : ""}`}>
        <div className="paper-panel-heading">
          <span>
            <FileText size={16} />
            {panel === "notes" ? "Your notes" : "The source"}
          </span>
          <button
            className="mobile-details-close icon-button"
            aria-label="Close details"
            onClick={() => setPanelOpen(false)}
          >
            <X size={18} />
          </button>
          <button
            className="icon-button expand-details"
            aria-label={
              expanded === "details"
                ? "Collapse details panel"
                : "Expand details panel"
            }
            onClick={() =>
              setExpanded(expanded === "details" ? null : "details")
            }
          >
            {expanded === "details" ? (
              <Minimize2 size={15} />
            ) : (
              <Maximize2 size={15} />
            )}
          </button>
          <a
            href={`https://arxiv.org/abs/${paper.arxivId}`}
            target="_blank"
            rel="noopener noreferrer"
            aria-label="Open on arXiv"
          >
            <ArrowUpRight size={16} />
          </a>
        </div>
        <div className="paper-tabs">
          <button
            className={panel === "overview" ? "active" : ""}
            onClick={() => setPanel("overview")}
          >
            Overview
          </button>
          <button
            className={panel === "sources" ? "active" : ""}
            onClick={() => setPanel("sources")}
          >
            Sources{activeTurn && <span>{activeTurn.sources.length}</span>}
          </button>
          <button
            className={panel === "pdf" ? "active" : ""}
            onClick={() => setPanel("pdf")}
            disabled={!paper.pageCount}
          >
            PDF
          </button>
          <button
            className={panel === "notes" ? "active" : ""}
            onClick={() => setPanel("notes")}
          >
            <StickyNote size={12} />
            Notes
          </button>
        </div>
        {panel === "overview" && (
          <div className="paper-overview">
            <span className="arxiv-chip">arXiv {paper.arxivId}</span>
            <h2>{paper.title}</h2>
            <p className="paper-authors">
              {paper.authors || "Author details will appear here."}
            </p>
            <div className="paper-meta">
              <span>
                {paper.published
                  ? new Date(paper.published).getFullYear()
                  : "Research paper"}
              </span>
              {paper.pageCount > 0 && (
                <>
                  <span>{paper.pageCount} pages</span>
                  <span>{paper.chunkCount} passages</span>
                </>
              )}
            </div>
            <StatusBadge paper={paper} />
            <PaperTags paper={paper} model={model} onRefresh={onRefresh} />
            <div className="abstract-section">
              <h3>Abstract</h3>
              <p>
                {paper.abstractText ||
                  "The abstract will appear once the paper is fetched."}
              </p>
            </div>
            <a
              className="paper-pdf-link"
              href={`/api/papers/${paper.id}/pdf`}
              target="_blank"
              rel="noopener noreferrer"
            >
              <FileText size={17} />
              Read original paper
              <ArrowUpRight size={16} />
            </a>
            <div className="shared-note">
              <FolderOpen size={15} />
              <p>
                This paper is part of the shared library.
                <br />
                Your conversation stays private.
              </p>
            </div>
          </div>
        )}
        {panel === "sources" && (
          <div className="sources-panel">
            {activeTurn ? (
              <>
                <p className="source-explainer">
                  Passages retrieved for this answer. Compare the citations with
                  the original text.
                </p>
                {activeTurn.sources.map((source) => (
                  <SourceCard
                    key={source.number}
                    source={source}
                    onOpen={() => {
                      setSourcePage(source.page);
                      setPanel("pdf");
                    }}
                  />
                ))}
              </>
            ) : (
              <div className="sources-empty">
                <Layers3 size={29} />
                <h3>Follow the evidence.</h3>
                <p>
                  Ask a question and the relevant passages will appear here.
                </p>
              </div>
            )}
          </div>
        )}
        {panel === "pdf" && (
          <Suspense
            fallback={
              <div className="pdf-loading" role="status">
                <LoaderCircle size={18} className="spin" />
                Opening reader…
              </div>
            }
          >
            <PdfReader paperId={paper.id} initialPage={sourcePage} onExplain={explain} explainBusy={busy || historyLoading} />
          </Suspense>
        )}
        <div className="notes-pane-container" hidden={panel !== "notes"}>
          <NotesPanel paperId={paper.id} userId={userId} />
        </div>
      </aside>
      {confirmClear && (
        <div className="modal-backdrop">
          <div
            className="confirm-dialog"
            role="alertdialog"
            aria-modal="true"
            aria-labelledby="clear-title"
          >
            <h2 id="clear-title">Start a fresh conversation?</h2>
            <p>
              This deletes your questions and answers for this paper. The shared
              paper stays in the library.
            </p>
            <div>
              <button
                className="button secondary"
                onClick={() => setConfirmClear(false)}
              >
                Keep conversation
              </button>
              <button className="button primary" onClick={clear}>
                Clear my conversation
              </button>
            </div>
          </div>
        </div>
      )}
    </main>
  );
}

function ModelPicker({
  model,
  models,
  onModel,
}: {
  model: string;
  models: string[];
  onModel: (m: string) => void;
}) {
  const options = [...new Set([model, ...models])].filter(Boolean);
  return (
    <label className="model-picker">
      <span className="connection-dot online" />
      <select
        aria-label="Answer model"
        value={model}
        onChange={(e) => onModel(e.target.value)}
      >
        {options.map((m) => (
          <option key={m} value={m}>
            {m}
          </option>
        ))}
      </select>
      <ChevronDown size={12} />
    </label>
  );
}
function SourceCard({
  source,
  onOpen,
}: {
  source: Source;
  onOpen: () => void;
}) {
  const [expanded, setExpanded] = useState(false);
  return (
    <div className="source-card">
      <div className="source-card-heading">
        <span className="citation-number">[{source.number}]</span>
        <button onClick={onOpen}>
          Page {source.page}
          <ArrowUpRight size={13} />
        </button>
      </div>
      <p className={expanded ? "" : "source-truncated"}>{source.text}</p>
      <button className="source-expand" onClick={() => setExpanded(!expanded)}>
        {expanded ? "Show less" : "Read passage"}
        <ChevronDown size={13} className={expanded ? "rotated" : ""} />
      </button>
    </div>
  );
}
function CopyButton({ text }: { text: string }) {
  const [copied, setCopied] = useState(false);
  async function copy() {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      setCopied(false);
    }
  }
  return (
    <button
      className="copy-answer"
      aria-label={copied ? "Answer copied" : "Copy answer"}
      onClick={copy}
    >
      {copied ? <Check size={14} /> : <Copy size={14} />}
      {copied ? "Copied" : "Copy"}
    </button>
  );
}
