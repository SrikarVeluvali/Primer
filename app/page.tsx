"use client";

import { FormEvent, useEffect, useMemo, useState } from "react";
import { signOut, useSession } from "next-auth/react";

type Page = "library" | "workspace";
type StudyState = "library" | "paper" | "processing" | "questions" | "report";
type QuestionType = "SINGLE_CHOICE" | "MULTI_CHOICE" | "FREE_TEXT";
type Question = { id: string; concept: string; type: QuestionType; importance: "CORE" | "HELPFUL" | "PERIPHERAL"; prompt: string; options: { id: string; label: string }[] };
type Questionnaire = { paperTitle: string; intro: string; questions: Question[] };

const demoQuestions: Question[] = [
  { id: "attention", concept: "Self-attention", type: "SINGLE_CHOICE", importance: "CORE", prompt: "How comfortable are you with the purpose of self-attention in a transformer?", options: [{ id: "new", label: "I’m new to it" }, { id: "working", label: "I know the basics" }, { id: "confident", label: "I can explain it" }] },
  { id: "architecture", concept: "Transformer architecture", type: "MULTI_CHOICE", importance: "CORE", prompt: "Which components have you encountered before?", options: [{ id: "encoder", label: "Encoder–decoder models" }, { id: "multihead", label: "Multi-head attention" }, { id: "positional", label: "Positional encodings" }] },
  { id: "context", concept: "Research context", type: "FREE_TEXT", importance: "HELPFUL", prompt: "What are you hoping to take away from this paper?", options: [] },
];

function Icon({ children }: { children: string }) { return <span className="icon" aria-hidden="true">{children}</span>; }

export default function Home() {
  const [screen, setScreen] = useState<StudyState>("library");
  const [paperId, setPaperId] = useState<string | null>(null);
  const [source, setSource] = useState("https://arxiv.org/abs/1706.03762");
  const [error, setError] = useState<string | null>(null);
  const [ingesting, setIngesting] = useState(false);
  const [questionnaire, setQuestionnaire] = useState<Questionnaire>({ paperTitle: "Attention Is All You Need", intro: "A few questions help Primer shape a useful reading path for you.", questions: demoQuestions });
  const [answers, setAnswers] = useState<Record<string, string[] | string>>({});
  const [sidebar, setSidebar] = useState<"summary" | "prereqs" | "recommendations">("summary");

  useEffect(() => {
    if (screen !== "processing" || !paperId) return;
    let cancelled = false;
    const check = async () => {
      try {
        const response = await fetch(`/api/papers/${paperId}/questionnaire`);
        const data = await response.json();
        if (cancelled) return;
        if (data.status === "READY" && data.questionnaire) {
          setQuestionnaire(JSON.parse(data.questionnaire));
          setScreen("questions");
        } else if (data.error) setError(data.error);
      } catch { /* Backend may be off; the demo flow stays useful. */ }
    };
    check();
    const interval = window.setInterval(check, 3500);
    return () => { cancelled = true; window.clearInterval(interval); };
  }, [screen, paperId]);

  const answered = useMemo(() => Object.keys(answers).filter((key) => Array.isArray(answers[key]) ? (answers[key] as string[]).length : answers[key]).length, [answers]);

  async function addPaper(event: FormEvent) {
    event.preventDefault();
    setError(null); setIngesting(true);
    try {
      const response = await fetch("/api/papers", { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ arxivReference: source }) });
      if (!response.ok) throw new Error("Primer couldn’t fetch that arXiv paper. Check the link and try again.");
      const id = response.headers.get("X-Paper-Id");
      if (!id) throw new Error("The paper was added, but its identifier is missing.");
      setPaperId(id); setScreen("paper");
    } catch (caught) {
      setError(caught instanceof Error ? caught.message : "Something went wrong.");
    } finally { setIngesting(false); }
  }

  function pick(question: Question, optionId: string) {
    setAnswers((current) => {
      const selected = (current[question.id] as string[] | undefined) ?? [];
      return { ...current, [question.id]: question.type === "SINGLE_CHOICE" ? [optionId] : selected.includes(optionId) ? selected.filter((id) => id !== optionId) : [...selected, optionId] };
    });
  }

  if (screen === "library") return <Landing source={source} setSource={setSource} onSubmit={addPaper} ingesting={ingesting} error={error} />;
  return <main className="app-shell"><AppNav onHome={() => setScreen("library")} /><section className="workspace">
    <section className="reader-pane">
      <div className="paper-toolbar"><span className="paper-dot red" /><span className="paper-dot yellow" /><span className="paper-dot green" /><span>Attention Is All You Need</span><button className="icon-button">⌕</button><button className="icon-button">↗</button></div>
      {paperId ? <iframe className="paper-frame" title="Research paper" src={`/api/papers/${paperId}/pdf#toolbar=0`} /> : <article className="paper-preview"><p className="eyebrow">arXiv:1706.03762 · 2017</p><h1>Attention Is All You Need</h1><p className="authors">Ashish Vaswani, Noam Shazeer, Niki Parmar, and collaborators</p><hr /><h2>Abstract</h2><p>The dominant sequence transduction models are based on complex recurrent or convolutional neural networks. We propose a new simple network architecture, the Transformer, based solely on attention mechanisms.</p></article>}
    </section>
    {screen === "paper" && <PaperDetails onStudy={() => setScreen("processing")} onBack={() => setScreen("library")} />}
    {screen === "processing" && <Processing />}
    {screen === "questions" && <QuestionnairePanel questionnaire={questionnaire} answers={answers} answered={answered} pick={pick} setAnswers={setAnswers} onSkip={() => setScreen("report")} onSubmit={() => setScreen("report")} />}
    {screen === "report" && <ReportPanel active={sidebar} setActive={setSidebar} />}
  </section></main>;
}

function Landing({ source, setSource, onSubmit, ingesting, error }: { source: string; setSource: (value: string) => void; onSubmit: (event: FormEvent) => void; ingesting: boolean; error: string | null }) {
  return <main className="landing"><nav><div className="brand"><span>✦</span> Primer</div><div className="nav-links"><a href="#how-it-works">How it works</a><AccountButton /></div></nav><section className="hero"><p className="eyebrow">YOUR RESEARCH, MADE LEGIBLE</p><h1>Read papers with<br /><em>real</em> understanding.</h1><p className="hero-copy">Primer turns dense research into a guided learning journey shaped around what you already know.</p><form className="paper-form" onSubmit={onSubmit}><label htmlFor="arxiv">Start with an arXiv paper</label><div><Icon>↗</Icon><input id="arxiv" value={source} onChange={(event) => setSource(event.target.value)} placeholder="Paste an arXiv link or ID" /><button disabled={ingesting}>{ingesting ? "Adding…" : "Open paper"} <span>→</span></button></div>{error && <p className="form-error">{error}</p>}</form><p className="quiet">No account required to get started.</p></section><section className="feature-strip" id="how-it-works"><div><span>01</span><h3>Bring a paper</h3><p>Paste any arXiv reference.</p></div><div><span>02</span><h3>Find your footing</h3><p>A short check-in maps your context.</p></div><div><span>03</span><h3>Read with a guide</h3><p>See the ideas that matter, when they matter.</p></div></section></main>;
}

function AccountButton() { const { data: session, status } = useSession(); if (status === "loading") return <span className="account-loading" />; if (!session?.user) return <a className="text-button" href="/signin">Sign in with Google</a>; return <button className="account-button" onClick={() => signOut({ callbackUrl: "/" })} title="Sign out"><span className="avatar">{session.user.name?.slice(0, 1).toUpperCase() ?? "P"}</span><span>Sign out</span></button>; }
function AppNav({ onHome: _onHome }: { onHome: () => void }) { return <nav className="app-nav"><div className="brand"><span>✦</span> Primer</div><div className="nav-spacer" /><button className="round-button" title="Appearance">◐</button><AccountButton /></nav>; }
function PaperDetails({ onStudy, onBack }: { onStudy: () => void; onBack: () => void }) { return <aside className="side-panel details"><button className="back" onClick={onBack}>← Library</button><p className="eyebrow">PAPER OVERVIEW</p><h2>Attention Is All You Need</h2><dl><div><dt>Published</dt><dd>June 2017</dd></div><div><dt>Authors</dt><dd>Vaswani et al.</dd></div><div><dt>Field</dt><dd>Machine learning</dd></div></dl><div className="study-card"><span>✦</span><h3>Is this a paper you want to study?</h3><p>Primer will build a reading path around your current knowledge.</p><button className="primary wide" onClick={onStudy}>Yes, let’s study it →</button><button className="secondary wide" onClick={onBack}>Not right now</button></div></aside>; }
function Processing() { return <aside className="side-panel processing"><div className="orb" /><p className="eyebrow">PREPARING YOUR PATH</p><h2>Finding the best place to begin.</h2><p>Primer is reading the paper and preparing a few questions for you.</p><div className="progress"><i /></div><span>This can take a minute</span></aside>; }
function QuestionnairePanel({ questionnaire, answers, answered, pick, setAnswers, onSkip, onSubmit }: { questionnaire: Questionnaire; answers: Record<string, string[] | string>; answered: number; pick: (q: Question, id: string) => void; setAnswers: (updater: (value: Record<string, string[] | string>) => Record<string, string[] | string>) => void; onSkip: () => void; onSubmit: () => void }) { return <aside className="side-panel questionnaire"><p className="eyebrow">PERSONALIZE YOUR PATH</p><h2>Before we begin</h2><p className="intro">{questionnaire.intro}</p><div className="question-scroll">{questionnaire.questions.map((question, index) => <fieldset key={question.id}><legend><span>{String(index + 1).padStart(2, "0")}</span>{question.concept}<b className={`importance ${question.importance.toLowerCase()}`}>{question.importance}</b></legend><p>{question.prompt}</p>{question.type === "FREE_TEXT" ? <textarea value={(answers[question.id] as string) ?? ""} onChange={(event) => setAnswers((current) => ({ ...current, [question.id]: event.target.value }))} placeholder="Write a few words…" /> : <div className="choices">{question.options.map((option) => { const selected = ((answers[question.id] as string[]) ?? []).includes(option.id); return <button type="button" onClick={() => pick(question, option.id)} className={selected ? "selected" : ""} key={option.id}><i>{selected ? "✓" : ""}</i>{option.label}</button>; })}</div>}</fieldset>)}</div><footer><span>{answered} of {questionnaire.questions.length} answered</span><div><button className="secondary" onClick={onSkip}>Skip</button><button className="primary" onClick={onSubmit}>Build my path →</button></div></footer></aside>; }
function ReportPanel({ active, setActive }: { active: "summary" | "prereqs" | "recommendations"; setActive: (value: "summary" | "prereqs" | "recommendations") => void }) { const content = active === "summary" ? <><p>Transformer models replace recurrence with attention, enabling more parallel computation and stronger long-range modeling.</p><h3>The central claim</h3><p>Attention alone can serve as the architecture&apos;s core sequence-processing mechanism, removing the need for recurrent layers.</p><h3>What to focus on</h3><ul><li>How scaled dot-product attention works</li><li>Why multiple attention heads help</li><li>The role of position information</li></ul></> : active === "prereqs" ? <><p className="panel-lead">A few ideas will make the paper substantially easier to follow.</p><StudyItem title="Vectors & matrix multiplication" level="Quick refresh · 12 min" /><StudyItem title="Neural network basics" level="Comfortable already" done /><StudyItem title="Sequence-to-sequence models" level="Build intuition · 18 min" /></> : <><p className="panel-lead">Once you finish, these are natural next reads.</p><StudyItem title="BERT: Pre-training of Deep Bidirectional Transformers" level="Language understanding" /><StudyItem title="The Annotated Transformer" level="Interactive walkthrough" /><StudyItem title="Visualizing Attention" level="Conceptual deep dive" /></>; return <aside className="side-panel report"><div className="tabs">{(["summary", "prereqs", "recommendations"] as const).map((tab) => <button onClick={() => setActive(tab)} className={active === tab ? "active" : ""} key={tab}>{tab === "prereqs" ? "Prerequisites" : tab}</button>)}</div><div className="report-body"><p className="eyebrow">YOUR READING GUIDE</p><h2>{active === "summary" ? "The paper, in plain language." : active === "prereqs" ? "A foundation for this read." : "Where to go next."}</h2>{content}</div><button className="chat-button">✦ <span>Ask Primer</span></button></aside>; }
function StudyItem({ title, level, done }: { title: string; level: string; done?: boolean }) { return <article className="study-item"><span className={done ? "done" : ""}>{done ? "✓" : "→"}</span><div><h4>{title}</h4><p>{level}</p></div></article>; }
