import { useEffect, useState } from "react";
import { BookOpen, Compass, Plus, Tag, Trash2, LockKeyhole, ArrowUpRight, LoaderCircle, Search, Check } from "lucide-react";
import { api, post } from "../../lib/api";
import { errorText } from "../../lib/research";
import type { KnowledgeProfile, KnowledgeTag, Paper, Recommendation, CatalogTag } from "../../lib/types";

export const familiarity = ["New to me", "I recognize it", "Comfortable", "I could explain it"];

export function Recommendations({ onSelect, refresh = 0 }: { onSelect: (paper: Paper) => void; refresh?: number }) {
  const [items, setItems] = useState<Recommendation[]>([]);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    let active = true;
    setLoading(true);
    api<Recommendation[]>("/recommendations").then(value => { if (active) { setItems(value); setError(""); } })
      .catch(e => { if (active) setError(errorText(e)); }).finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [refresh]);
  return <section className="recommendations-section">
    <div className="recommendation-heading"><div><span className="inline-label"><Compass size={16} /> Selected for your next chapter</span><h2>For you.</h2></div><span className="tag-meta">From the shared library</span></div>
    {error && <p className="error-banner" role="alert">{error}</p>}
    {loading ? <p className="tag-empty"><LoaderCircle size={15} className="spin" /> Finding matches…</p> : items.length ? <div className="recommendation-grid">
      {items.map(item => <button key={item.paper.id} className="recommendation-card" onClick={() => onSelect(item.paper)}>
        <div className="recommendation-card-top"><BookOpen size={18} /><ArrowUpRight size={18} /></div>
        <h3>{item.paper.title}</h3>
        <div className="topic-tags">{item.matchedTags.map(tag => <span key={tag}>{tag}</span>)}</div>
        <ul>{item.reasons.map(reason => <li key={reason}>{reason}</li>)}</ul>
      </button>)}
    </div> : <div className="recommendation-empty"><Compass size={25} /><div><strong>Your next paper starts with a topic.</strong><p>Add your knowledge and interests, then tag papers in Overview. Matching papers will appear here with a reason.</p></div></div>}
  </section>;
}

export function KnowledgePage({ onSelect }: { onSelect: (paper: Paper) => void }) {
  const [profile, setProfile] = useState<KnowledgeProfile>({ tags: [] });
  const [name, setName] = useState("");
  const [catalog, setCatalog] = useState<CatalogTag[]>([]);
  const [selectedTopic, setSelectedTopic] = useState<CatalogTag | null>(null);
  const [pickerOpen, setPickerOpen] = useState(false);
  const [activeOption, setActiveOption] = useState(0);
  const [level, setLevel] = useState(0);
  const [interested, setInterested] = useState(true);
  const [busy, setBusy] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [refresh, setRefresh] = useState(0);
  useEffect(() => { let active = true; Promise.all([api<KnowledgeProfile>("/knowledge"), api<CatalogTag[]>("/topics")]).then(([p, topics]) => { if (active) { setProfile(p); setCatalog(topics); } }).catch(e => { if (active) setError(errorText(e)); }).finally(() => { if (active) setLoading(false); }); return () => { active = false; }; }, []);
  const options = catalog.filter(topic => topic.name.toLowerCase().includes(name.toLowerCase().trim())).slice(0, 10);
  function choose(topic: CatalogTag) { setName(topic.name); setSelectedTopic(topic); setPickerOpen(false); }
  async function save(tag: { name: string; level: number; interested: boolean }) {
    setBusy(true); setError("");
    try { setProfile(await post<KnowledgeProfile>("/knowledge/tags", tag)); setRefresh(n => n + 1); return true; }
    catch (e) { setError(errorText(e)); return false; } finally { setBusy(false); }
  }
  async function remove(tag: KnowledgeTag) {
    setBusy(true); setError("");
    try { setProfile(await api<KnowledgeProfile>(`/knowledge/tags/${tag.id}`, { method: "DELETE" })); setRefresh(n => n + 1); }
    catch (e) { setError(errorText(e)); } finally { setBusy(false); }
  }
  return <main className="knowledge-page">
    <div className="page-heading"><div><div className="inline-label"><Tag size={16} /> A map of what you know</div><h1>Your knowledge, growing.</h1><p>Keep track of familiar concepts and the ideas you want to explore.</p></div><span className="private-chip"><LockKeyhole size={13} /> Only you</span></div>
    {error && <div className="error-banner" role="alert">{error}</div>}
    <section className="knowledge-editor">
      <div className="knowledge-intro"><h2>Knowledge & interests</h2><p>Learn saves your familiarity ratings here. Explore topics already tagged in the library, see how many papers cover each one, and choose what you want to learn next.</p></div>
      <form className="knowledge-add" onSubmit={async e => { e.preventDefault(); if (!selectedTopic) return; if (await save({ name: selectedTopic.name, level, interested })) { setName(""); setSelectedTopic(null); setPickerOpen(false); } }}>
        <div className="topic-picker" onBlur={e => { if (!e.currentTarget.contains(e.relatedTarget as Node | null)) setPickerOpen(false); }}>
          <label htmlFor="knowledge-topic">Find a library topic</label>
          <div className="topic-picker-input"><Search size={15} /><input id="knowledge-topic" role="combobox" aria-autocomplete="list" aria-expanded={pickerOpen} aria-controls="topic-options" aria-activedescendant={pickerOpen && options[activeOption] ? `topic-option-${options[activeOption].id}` : undefined} autoComplete="off" maxLength={100} value={name} onFocus={() => setPickerOpen(true)} onChange={e => { setName(e.target.value); setSelectedTopic(null); setPickerOpen(true); setActiveOption(0); }} placeholder="Search existing paper tags…" onKeyDown={e => {
            if (e.key === "ArrowDown") { e.preventDefault(); setPickerOpen(true); setActiveOption(i => Math.min(i + 1, options.length - 1)); }
            if (e.key === "ArrowUp") { e.preventDefault(); setPickerOpen(true); setActiveOption(i => Math.max(0, i - 1)); }
            if (e.key === "Enter" && pickerOpen) { e.preventDefault(); if (options[activeOption]) choose(options[activeOption]); }
            if (e.key === "Escape") setPickerOpen(false);
          }} />{selectedTopic && <Check size={15} />}</div>
          {pickerOpen && <div className="topic-options" id="topic-options" role="listbox" aria-label="Existing paper topics">{loading ? <p>Loading library topics…</p> : options.length ? options.map((topic, i) => <button type="button" id={`topic-option-${topic.id}`} role="option" aria-selected={activeOption === i} className={activeOption === i ? "active" : ""} key={topic.id} onPointerDown={e => e.preventDefault()} onClick={() => choose(topic)}><span>{topic.name}</span><small>{topic.paperCount} {topic.paperCount === 1 ? "paper" : "papers"}</small></button>) : <p>{catalog.length ? "No tagged topics match. Try another search." : "No paper topics yet. Add tags in a paper’s Overview first."}</p>}</div>}
          <small className="topic-picker-help">{selectedTopic ? `${selectedTopic.paperCount} ${selectedTopic.paperCount === 1 ? "paper covers" : "papers cover"} this topic` : "Choose an existing tag to get paper recommendations."}</small>
        </div>
        <label>Your familiarity<select value={level} onChange={e => setLevel(Number(e.target.value))}>{familiarity.map((text, i) => <option key={text} value={i}>{text}</option>)}</select></label>
        <label className="interest-checkbox"><input type="checkbox" checked={interested} onChange={e => setInterested(e.target.checked)} /> Interested in learning</label>
        <button className="button primary" disabled={busy || loading || !selectedTopic}><Plus size={15} /> Add topic</button>
      </form>
      {loading ? <p className="tag-empty">Opening your knowledge map…</p> : !profile.tags.length ? <p className="tag-empty">No tags yet. Add a topic above, or use Learn on a paper.</p> : <div className="knowledge-tags">
        {[...profile.tags].sort((a, b) => a.name.localeCompare(b.name)).map(tag => <div className="knowledge-tag" key={tag.id}>
          <div className="knowledge-tag-name"><Tag size={15} /><strong>{tag.name}<small className="knowledge-tag-count">{catalog.find(t => t.id === tag.id)?.paperCount || 0} {(catalog.find(t => t.id === tag.id)?.paperCount || 0) === 1 ? "paper" : "papers"} in the library</small></strong></div>
          <select aria-label={`Familiarity with ${tag.name}`} value={tag.level} disabled={busy} onChange={e => save({ ...tag, level: Number(e.target.value) })}>{familiarity.map((text, i) => <option key={text} value={i}>{text}</option>)}</select>
          <button className={`interest-toggle ${tag.interested ? "active" : ""}`} aria-pressed={tag.interested} disabled={busy} onClick={() => save({ ...tag, interested: !tag.interested })}><Compass size={14} /> {tag.interested ? "Interested" : "Add interest"}</button>
          <button className="icon-button" aria-label={`Remove knowledge tag ${tag.name}`} disabled={busy} onClick={() => remove(tag)}><Trash2 size={15} /></button>
        </div>)}
      </div>}
      <p className="tag-meta">Self-reported familiarity. Completing a roadmap does not automatically change your rating.</p>
    </section>
    <Recommendations onSelect={onSelect} refresh={refresh} />
  </main>;
}
