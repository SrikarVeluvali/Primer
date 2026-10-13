import { useEffect, useRef, useState } from "react";
import {
  ArrowLeft,
  ArrowRight,
  ArrowUpRight,
  BookOpen,
  Check,
  CheckCircle2,
  ChevronDown,
  GraduationCap,
  Layers3,
  LoaderCircle,
  LockKeyhole,
  Play,
  RefreshCw,
  Route,
  Search,
  StickyNote,
} from "lucide-react";
import { api, post } from "../../lib/api";
import { errorText } from "../../lib/research";
import type { LearningAnswers, LearningState, Paper } from "../../lib/types";
import { appendToNotes } from "../research/NotesPanel";

const emptyAnswers: LearningAnswers = {
  answers: {},
  confidence: {},
  goal: "Understand this paper well enough to explain it.",
  minutesPerDay: 30,
  preference: "mixed",
};
export function LearnPanel({
  paper,
  model,
  userId,
  onNotes,
}: {
  paper: Paper;
  model: string;
  userId: string;
  onNotes: () => void;
}) {
  const [state, setState] = useState<LearningState>({
    assessment: null,
    answers: null,
    plan: null,
    completedSteps: [],
  });
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState("");
  const [error, setError] = useState("");
  const [answers, setAnswers] = useState<LearningAnswers>(emptyAnswers);
  const [question, setQuestion] = useState(0);
  const [editing, setEditing] = useState(false);
  const [filter, setFilter] = useState("all");
  const [retake, setRetake] = useState(false);
  const [progressSaving, setProgressSaving] = useState(false);
  const mounted = useRef(true);
  const key = `primer-learn-draft-${userId}-${paper.id}`;
  useEffect(() => {
    mounted.current = true;
    api<LearningState>(`/papers/${paper.id}/learn`)
      .then((value) => {
        if (!mounted.current) return;
        setState(value);
        let draft = value.answers || emptyAnswers;
        try {
          const cached = localStorage.getItem(key);
          if (cached && !value.plan) {
            const stored = JSON.parse(cached);
            if (
              stored.topicIds ===
              value.assessment?.topics.map((t) => t.id).join(",")
            )
              draft = stored.answers;
          }
        } catch {}
        setAnswers(draft);
      })
      .catch((e) => {
        if (mounted.current) setError(errorText(e));
      })
      .finally(() => {
        if (mounted.current) setLoading(false);
      });
    return () => {
      mounted.current = false;
    };
  }, [paper.id, key]);
  useEffect(() => {
    if (!state.assessment || loading || (state.plan && !editing)) return;
    try {
      localStorage.setItem(
        key,
        JSON.stringify({
          topicIds: state.assessment.topics.map((t) => t.id).join(","),
          answers,
        }),
      );
    } catch {}
  }, [answers, state.assessment, state.plan, editing, loading, key]);
  async function assessment() {
    setBusy("Building a questionnaire around this paper…");
    setError("");
    try {
      const next = await post<LearningState>(
        `/papers/${paper.id}/learn/assessment`,
        { model },
      );
      if (mounted.current) {
        setState(next);
        setAnswers(next.answers || emptyAnswers);
        setQuestion(0);
        setEditing(false);
        setRetake(false);
        localStorage.removeItem(key);
      }
    } catch (e) {
      if (mounted.current) setError(errorText(e));
    } finally {
      if (mounted.current) setBusy("");
    }
  }
  async function plan() {
    setBusy("Building your roadmap and finding learning resources…");
    setError("");
    try {
      const next = await post<LearningState>(`/papers/${paper.id}/learn/plan`, {
        answers,
        model,
      });
      if (mounted.current) {
        setState(next);
        setEditing(false);
        localStorage.removeItem(key);
      }
    } catch (e) {
      if (mounted.current) setError(errorText(e));
    } finally {
      if (mounted.current) setBusy("");
    }
  }
  async function complete(id: string) {
    if (progressSaving) return;
    const previous = state;
    const completed = state.completedSteps.includes(id)
      ? state.completedSteps.filter((s) => s !== id)
      : [...state.completedSteps, id];
    setState({ ...state, completedSteps: completed });
    setProgressSaving(true);
    setError("");
    try {
      const result = await api<LearningState>(
        `/papers/${paper.id}/learn/progress`,
        { method: "PUT", body: JSON.stringify({ completedSteps: completed }) },
      );
      if (mounted.current) setState(result);
    } catch (e) {
      if (mounted.current) {
        setState(previous);
        setError(errorText(e));
      }
    } finally {
      if (mounted.current) setProgressSaving(false);
    }
  }
  async function refresh() {
    setBusy("Finding resources for your learning gaps…");
    setError("");
    try {
      const next = await post<LearningState>(
        `/papers/${paper.id}/learn/resources`,
      );
      if (mounted.current) setState(next);
    } catch (e) {
      if (mounted.current) setError(errorText(e));
    } finally {
      if (mounted.current) setBusy("");
    }
  }
  const assessmentData = state.assessment;
  const topic = assessmentData?.topics[question];
  const result = state.plan;
  async function rememberTopic() {
    if (!topic || progressSaving) return;
    setProgressSaving(true); setError("");
    try {
      const profile = await api<{ tags: { name: string; interested: boolean }[] }>("/knowledge");
      const interested = profile.tags.some(t => t.name.toLowerCase() === topic.name.toLowerCase() && t.interested);
      await post("/knowledge/tags", { name: topic.name, level: answers.confidence[topic.id], interested });
      setQuestion(q => q + 1);
    } catch (e) { setError(errorText(e)); } finally { setProgressSaving(false); }
  }
  function keepRoadmap() {
    if (!result) return;
    const text = `# Learning roadmap\n\n${result.summary}\n\n## Prerequisites\n${result.prerequisites.map((p) => `- **${p.name}**: ${p.reason}`).join("\n")}\n\n## Roadmap\n${result.roadmap.map((s, i) => `${i + 1}. **${s.title}** (${s.minutes} minutes)\n   ${s.activity}\n   Check: ${s.checkpoint}`).join("\n\n")}`;
    appendToNotes(paper.id, text);
    onNotes();
  }
  return (
    <div className="learn-panel">
      {error && (
        <div className="error-banner" role="alert">
          {error}
        </div>
      )}
      {loading ? (
        <div className="learn-busy">
          <LoaderCircle size={24} className="spin" />
          <p>Opening your learning space…</p>
        </div>
      ) : busy ? (
        <div className="learn-busy" role="status">
          <span className="learn-symbol">
            <GraduationCap size={30} />
          </span>
          <h2>A path built around you.</h2>
          <LoaderCircle size={23} className="spin" />
          <p>{busy}</p>
          <small>Your answers and learning plan stay private.</small>
        </div>
      ) : !assessmentData ? (
        <div className="learn-welcome">
          <span className="learn-symbol">
            <GraduationCap size={32} />
          </span>
          <h2>Make this paper make sense.</h2>
          <p>
            Find your starting point, fill the gaps, and work your way into the
            research.
          </p>
          <div className="learn-promises">
            <div>
              <Layers3 size={18} />
              <span>
                A short knowledge questionnaire
                <small>
                  Tell us how familiar each concept feels. No quiz required.
                </small>
              </span>
            </div>
            <div>
              <Route size={18} />
              <span>
                A roadmap that fits your background
                <small>
                  Prerequisites, activities, and a checkpoint at every step.
                </small>
              </span>
            </div>
            <div>
              <Play size={18} />
              <span>
                Resources worth exploring
                <small>
                  Videos, articles, and community discussions by topic.
                </small>
              </span>
            </div>
          </div>
          <button
            className="button primary"
            disabled={paper.status !== "READY"}
            onClick={assessment}
          >
            Build my questionnaire
            <ArrowRight size={16} />
          </button>
          <span className="learn-private">
            <LockKeyhole size={12} />
            Personal to your account. The paper remains shared.
          </span>
        </div>
      ) : !result || editing ? (
        <div className="questionnaire">
          <div className="learning-heading">
            <span>
              <GraduationCap size={18} />
              Find your starting point
            </span>
            <p>Tell us how familiar each concept feels. There are no correct answers; your ratings help tailor the roadmap.</p>
          </div>
          <div className="question-progress">
            <span>
              {Math.min(question + 1, assessmentData.topics.length + 1)} of{" "}
              {assessmentData.topics.length + 1}
            </span>
            <div>
              {Array.from(
                { length: assessmentData.topics.length + 1 },
                (_, i) => (
                  <i key={i} className={i <= question ? "active" : ""} />
                ),
              )}
            </div>
          </div>
          {topic ? (
            <>
              <div className="topic-purpose">
                <span>{topic.name}</span>
                <p>{topic.why}</p>
              </div>
              <fieldset className="knowledge-question">
                <legend>How much do you know about {topic.name.toLowerCase()}?</legend>
                {[
                  { title: "New to me", text: "I haven’t learned this yet. Start with the basics." },
                  { title: "I recognize it", text: "I’ve heard of it, but would like a refresher." },
                  { title: "Comfortable", text: "I understand the basics and have used them before." },
                  { title: "I could explain it", text: "I can work with this concept and explain it to someone else." },
                ].map((option, level) => (
                  <label
                    key={level}
                    className={
                      answers.confidence[topic.id] === level ? "selected" : ""
                    }
                  >
                    <input
                      type="radio"
                      name={`question-${topic.id}`}
                      checked={answers.confidence[topic.id] === level}
                      onChange={() =>
                        setAnswers({
                          ...answers,
                          confidence: { ...answers.confidence, [topic.id]: level },
                        })
                      }
                    />
                    <span><strong>{option.title}</strong><small className="familiarity-description">{option.text}</small></span>
                    {answers.confidence[topic.id] === level && (
                      <Check size={15} />
                    )}
                  </label>
                ))}
              </fieldset>
              <p className="familiarity-note">Your rating becomes a private knowledge tag. You can update it any time in Knowledge.</p>
            </>
          ) : (
            <div className="learner-preferences">
              <h2>Make the path yours.</h2>
              <label>
                What would you like to get out of this paper?
                <textarea
                  value={answers.goal}
                  maxLength={2000}
                  rows={3}
                  onChange={(e) =>
                    setAnswers({ ...answers, goal: e.target.value })
                  }
                />
              </label>
              <div>
                <label>
                  Daily learning time
                  <select
                    value={answers.minutesPerDay}
                    onChange={(e) =>
                      setAnswers({
                        ...answers,
                        minutesPerDay: Number(e.target.value),
                      })
                    }
                  >
                    {[15, 30, 45, 60, 90, 120].map((n) => (
                      <option value={n} key={n}>
                        {n} minutes
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  Preferred way to learn
                  <select
                    value={answers.preference}
                    onChange={(e) =>
                      setAnswers({ ...answers, preference: e.target.value })
                    }
                  >
                    <option value="mixed">A mix of formats</option>
                    <option value="video">Video first</option>
                    <option value="reading">Reading first</option>
                    <option value="hands-on">Hands-on practice</option>
                  </select>
                </label>
              </div>
              <p>
                Your plan is generated locally. Resource searches use public
                topic names, never your notes, name, or questionnaire responses.
              </p>
            </div>
          )}
          <div className="question-navigation">
            <button
              className="button secondary"
              disabled={question === 0}
              onClick={() => setQuestion((q) => q - 1)}
            >
              <ArrowLeft size={15} />
              Back
            </button>
            {topic ? (
              <button
                className="button primary"
                disabled={answers.confidence[topic.id] === undefined || progressSaving}
                onClick={rememberTopic}
              >
                Continue
                <ArrowRight size={15} />
              </button>
            ) : (
              <button
                className="button primary"
                onClick={plan}
                disabled={assessmentData.topics.some(
                  (t) => answers.confidence[t.id] === undefined,
                )}
              >
                Create my roadmap
                <Route size={16} />
              </button>
            )}
          </div>
          {editing && (
            <button className="text-button" onClick={() => setEditing(false)}>
              Return to saved plan
            </button>
          )}
        </div>
      ) : (
        <div className="learning-result">
          <div className="learning-heading">
            <span>
              <Route size={18} />
              Your path into this paper
            </span>
            <h2>Start where you are.</h2>
            <p>{result.summary}</p>
            <div className="learning-result-actions">
              <button
                onClick={() => {
                  setEditing(true);
                  setQuestion(assessmentData.topics.length);
                }}
              >
                Adjust my plan
              </button>
              <button onClick={() => setRetake(true)}>
                Retake questionnaire
              </button>
              <button onClick={keepRoadmap}>
                <StickyNote size={13} />
                Save roadmap to notes
              </button>
            </div>
          </div>
          {retake && (
            <div className="retake-confirm">
              <p>
                A new questionnaire will replace your current roadmap and its
                progress.
              </p>
              <button
                className="button secondary"
                onClick={() => setRetake(false)}
              >
                Keep plan
              </button>
              <button className="button primary" onClick={assessment}>
                Start again
              </button>
            </div>
          )}
          <section className="prerequisite-section">
            <h3>The prerequisites</h3>
            <p>Suggested from this paper’s concepts and your answers.</p>
            <div className="prerequisite-list">
              {result.prerequisites.map((prereq, i) => (
                <div key={i}>
                  <span className={`readiness ${prereq.readiness}`}>
                    {prereq.readiness === "learn"
                      ? "Build this first"
                      : prereq.readiness === "review"
                        ? "A quick review"
                        : "Familiar ground"}
                  </span>
                  <h4>{prereq.name}</h4>
                  <p>{prereq.reason}</p>
                </div>
              ))}
            </div>
          </section>
          <section className="roadmap-section">
            <div className="roadmap-heading">
              <h3>Your roadmap</h3>
              <span>
                {state.completedSteps.length} / {result.roadmap.length} complete
              </span>
            </div>
            <div className="roadmap-progress">
              <div
                style={{
                  width: `${(state.completedSteps.length / result.roadmap.length) * 100}%`,
                }}
              />
            </div>
            <ol className="roadmap-list">
              {result.roadmap.map((step, i) => (
                <li
                  key={step.id}
                  className={
                    state.completedSteps.includes(step.id) ? "complete" : ""
                  }
                >
                  <button
                    className="step-check"
                    aria-label={`${state.completedSteps.includes(step.id) ? "Mark incomplete" : "Complete"}: ${step.title}`}
                    aria-pressed={state.completedSteps.includes(step.id)}
                    disabled={progressSaving}
                    onClick={() => complete(step.id)}
                  >
                    {state.completedSteps.includes(step.id) ? (
                      <Check size={16} />
                    ) : (
                      i + 1
                    )}
                  </button>
                  <div>
                    <div className="step-heading">
                      <h4>{step.title}</h4>
                      <span>{step.minutes} min</span>
                    </div>
                    <p>{step.goal}</p>
                    <div className="step-activity">{step.activity}</div>
                    <div className="step-checkpoint">
                      <CheckCircle2 size={14} />
                      <p>{step.checkpoint}</p>
                    </div>
                    <div className="step-topic-links">
                      {step.topics.map((t) => (
                        <a
                          key={t}
                          href={`https://www.youtube.com/results?search_query=${encodeURIComponent(t + " tutorial")}`}
                          target="_blank"
                          rel="noopener noreferrer"
                        >
                          <Search size={11} />
                          Explore {t}
                        </a>
                      ))}
                    </div>
                  </div>
                </li>
              ))}
            </ol>
          </section>
          <section className="resources-section">
            <div className="roadmap-heading">
              <h3>Learning resources</h3>
              <button className="text-button" onClick={refresh}>
                <RefreshCw size={13} />
                Refresh
              </button>
            </div>
            <p className="resource-status">{result.resourceStatus}</p>
            <div className="resource-filters">
              {[
                ["all", "All resources"],
                ["youtube", "Videos"],
                ["blog", "Articles"],
                ["reddit", "Discussions"],
              ].map(([value, label]) => (
                <button
                  key={value}
                  className={filter === value ? "active" : ""}
                  onClick={() => setFilter(value)}
                >
                  {label}
                </button>
              ))}
            </div>
            <div className="learning-resources">
              {result.resources
                .filter((r) => filter === "all" || r.type === filter)
                .map((resource) => (
                  <a
                    href={resource.url}
                    key={resource.id}
                    className="learning-resource"
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    <span className={`resource-type ${resource.type}`}>
                      {resource.type === "youtube" ? (
                        <Play size={17} />
                      ) : (
                        <BookOpen size={17} />
                      )}
                    </span>
                    <div>
                      <small>
                        {resource.type === "youtube"
                          ? "YouTube"
                          : resource.type === "reddit"
                            ? "Reddit"
                            : "Article"}
                        <span>{resource.origin}</span>
                      </small>
                      <h4>{resource.title}</h4>
                      <p>{resource.description}</p>
                      <span>{resource.topic}</span>
                    </div>
                    <ArrowUpRight size={16} />
                  </a>
                ))}
            </div>
            {result.resources.filter(
              (r) => filter === "all" || r.type === filter,
            ).length === 0 && (
              <p className="resource-empty">
                No relevant{" "}
                {filter === "all"
                  ? "resources"
                  : filter === "youtube"
                    ? "videos"
                    : filter === "reddit"
                      ? "discussions"
                      : "articles"}{" "}
                were returned. Use the topic searches below or refresh later.
              </p>
            )}
            <div className="resource-searches">
              <p>Find more resources yourself</p>
              {assessmentData.topics.slice(0, 3).map((t) => (
                <div key={t.id}>
                  <span>{t.name}</span>
                  <a
                    href={`https://www.youtube.com/results?search_query=${encodeURIComponent(t.name + " tutorial")}`}
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    Search YouTube
                    <ArrowUpRight size={11} />
                  </a>
                  <a
                    href={`https://www.reddit.com/search/?q=${encodeURIComponent(t.name)}`}
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    Search Reddit
                    <ArrowUpRight size={11} />
                  </a>
                  <a
                    href={`https://duckduckgo.com/?q=${encodeURIComponent(t.name + " guide tutorial")}`}
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    Search articles
                    <ArrowUpRight size={11} />
                  </a>
                </div>
              ))}
            </div>
          </section>
          <details className="knowledge-review">
            <summary>
              Your familiarity ratings
              <ChevronDown size={14} />
            </summary>
            {assessmentData.topics.map((t) => (
              <div key={t.id}>
                <h4>
                  {t.name}
                  <span>
                    {["New to me", "I recognize it", "Comfortable", "I could explain it"][answers.confidence[t.id] ?? 0]}
                  </span>
                </h4>
                <p>{t.why}</p>
              </div>
            ))}
          </details>
        </div>
      )}
    </div>
  );
}
