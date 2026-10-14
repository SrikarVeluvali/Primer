import { useEffect, useState } from "react";
import { Plus, Sparkles, Tag, X, LoaderCircle } from "lucide-react";
import { api, post } from "../../lib/api";
import { errorText } from "../../lib/research";
import type { Paper } from "../../lib/types";

export function PaperTags({ paper, model, onRefresh }: { paper: Paper; model: string; onRefresh: () => void }) {
  const [tags, setTags] = useState(paper.tags || []);
  const [input, setInput] = useState("");
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [busy, setBusy] = useState("");
  const [error, setError] = useState("");
  useEffect(() => { setTags(paper.tags || []); }, [paper.tags]);
  async function save(next: string[]) {
    setBusy("save"); setError("");
    try { const result = await api<{ tags: string[] }>(`/papers/${paper.id}/tags`, { method: "PUT", body: JSON.stringify({ tags: next }) }); setTags(result.tags); setInput(""); onRefresh(); }
    catch (e) { setError(errorText(e)); } finally { setBusy(""); }
  }
  async function suggest() {
    setBusy("suggest"); setError("");
    try { const result = await post<{ tags: string[] }>(`/papers/${paper.id}/tags/suggest`, { model }); setSuggestions(result.tags); }
    catch (e) { setError(errorText(e)); } finally { setBusy(""); }
  }
  return <section className="paper-tags-editor">
    <div className="paper-tags-heading"><h3><Tag size={14} /> Paper topics</h3><button className="text-button" onClick={suggest} disabled={!!busy}>{busy === "suggest" ? <LoaderCircle className="spin" size={13} /> : <Sparkles size={13} />} Suggest</button></div>
    <p className="tag-meta">Shared tags help everyone discover this paper.</p>
    {error && <p className="error-banner" role="alert">{error}</p>}
    <div className="topic-tags editable">{tags.map(tag => <span key={tag}>{tag}<button disabled={!!busy} onClick={() => save(tags.filter(t => t !== tag))} aria-label={`Remove paper tag ${tag}`}><X size={12} /></button></span>)}</div>
    <form className="paper-tag-add" onSubmit={e => { e.preventDefault(); if (input.trim()) void save([...tags, input.trim()]); }}><input aria-label="New paper tag" placeholder="Add a topic…" maxLength={100} value={input} onChange={e => setInput(e.target.value)} /><button aria-label="Add paper tag" disabled={!!busy || !input.trim() || tags.length >= 20}><Plus size={16} /></button></form>
    {!!suggestions.length && <div className="suggested-tags"><span className="tag-meta">Suggested by your local model. Click to add.</span><div className="topic-tags">{suggestions.filter(s => !tags.some(t => t.toLowerCase() === s.toLowerCase())).map(tag => <button key={tag} disabled={!!busy || tags.length >= 20} onClick={() => save([...tags, tag])}><Plus size={12} />{tag}</button>)}</div></div>}
  </section>;
}
