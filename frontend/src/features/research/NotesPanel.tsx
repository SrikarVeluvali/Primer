import { useCallback, useEffect, useRef, useState } from "react";
import {
  Check,
  Copy,
  FileDown,
  LoaderCircle,
  LockKeyhole,
  Save,
  StickyNote,
} from "lucide-react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { api } from "../../lib/api";
import type { Note } from "../../lib/types";

export function appendToNotes(paperId: string, text: string) {
  window.dispatchEvent(
    new CustomEvent("primer-note-append", { detail: { paperId, text } }),
  );
}
export function NotesPanel({
  paperId,
  userId,
}: {
  paperId: string;
  userId: string;
}) {
  const [text, setText] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [status, setStatus] = useState("Loading notes…");
  const [error, setError] = useState("");
  const [preview, setPreview] = useState(false);
  const draftKey = `primer-note-draft-${userId}-${paperId}`;
  const latest = useRef("");
  const saved = useRef("");
  const writing = useRef(false);
  const alive = useRef(true);
  const change = useCallback(
    (value: string) => {
      latest.current = value;
      setText(value);
      setStatus("Unsaved changes");
      try {
        localStorage.setItem(
          draftKey,
          JSON.stringify({ text: value, modifiedAt: Date.now() }),
        );
      } catch {
        setError(
          "Your browser cannot keep a draft. Save your notes before leaving this paper.",
        );
      }
    },
    [draftKey],
  );
  const save = useCallback(async () => {
    if (writing.current || latest.current === saved.current) return;
    const value = latest.current;
    writing.current = true;
    setSaving(true);
    setError("");
    setStatus("Saving…");
    try {
      await api<Note>(`/papers/${paperId}/notes`, {
        method: "PUT",
        body: JSON.stringify({ text: value }),
      });
      saved.current = value;
      if (alive.current) {
        setStatus(
          latest.current === value
            ? "Saved to your account"
            : "Unsaved changes",
        );
        if (latest.current === value) localStorage.removeItem(draftKey);
      }
    } catch (e) {
      if (alive.current) {
        setError((e as Error).message);
        setStatus("Not saved. Your local draft is kept.");
      }
    } finally {
      writing.current = false;
      if (alive.current) setSaving(false);
    }
  }, [paperId, draftKey]);
  useEffect(() => {
    alive.current = true;
    let cancelled = false;
    api<Note>(`/papers/${paperId}/notes`)
      .then((note) => {
        if (cancelled) return;
        saved.current = note.text;
        let value = note.text;
        try {
          const cached = localStorage.getItem(draftKey);
          if (cached) {
            const draft = JSON.parse(cached);
            if (draft.modifiedAt > new Date(note.updatedAt || 0).getTime())
              value = draft.text;
          }
        } catch {}
        latest.current = value;
        setText(value);
        setStatus(
          value === note.text
            ? "Saved to your account"
            : "Local draft restored",
        );
      })
      .catch((e) => {
        if (!cancelled) setError((e as Error).message);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
      alive.current = false;
    };
  }, [paperId, draftKey]);
  useEffect(() => {
    if (loading || saving || error || text === saved.current) return;
    const timer = setTimeout(() => {
      void save();
    }, 1200);
    return () => clearTimeout(timer);
  }, [text, loading, saving, error, save]);
  useEffect(() => {
    const append = (event: Event) => {
      const detail = (event as CustomEvent<{ paperId: string; text: string }>)
        .detail;
      if (detail.paperId === paperId)
        change(
          `${latest.current}${latest.current ? "\n\n---\n\n" : ""}${detail.text}`,
        );
    };
    window.addEventListener("primer-note-append", append);
    return () => window.removeEventListener("primer-note-append", append);
  }, [paperId, change]);
  function download() {
    const blob = new Blob([latest.current], {
      type: "text/markdown;charset=utf-8",
    });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = "primer-paper-notes.md";
    anchor.click();
    URL.revokeObjectURL(url);
  }
  return (
    <section className="notes-panel">
      <div className="notes-intro">
        <StickyNote size={20} />
        <div>
          <h3>Your margin, your thoughts.</h3>
          <p>Private notes for this paper. Markdown is welcome.</p>
        </div>
      </div>
      <div className="notes-controls">
        <div>
          <button
            className={!preview ? "active" : ""}
            onClick={() => setPreview(false)}
          >
            Write
          </button>
          <button
            className={preview ? "active" : ""}
            onClick={() => setPreview(true)}
          >
            Preview
          </button>
        </div>
        <button
          className="icon-button"
          aria-label="Download notes as Markdown"
          disabled={!text}
          onClick={download}
        >
          <FileDown size={16} />
        </button>
      </div>
      {loading ? (
        <div className="notes-loading">
          <LoaderCircle size={20} className="spin" />
        </div>
      ) : preview ? (
        <div className="markdown notes-preview">
          {text ? (
            <ReactMarkdown remarkPlugins={[remarkGfm]}>{text}</ReactMarkdown>
          ) : (
            <p>Your notes will appear here.</p>
          )}
        </div>
      ) : (
        <textarea
          className="notes-editor"
          aria-label="Your private paper notes"
          placeholder={
            "What clicked? What needs another look?\n\n# My notes\n- A key idea\n- A question to come back to"
          }
          value={text}
          onChange={(e) => change(e.target.value)}
          maxLength={100000}
        />
      )}
      <div className="notes-save-row">
        <span role="status">
          {saving ? (
            <LoaderCircle className="spin" size={12} />
          ) : status.startsWith("Saved") ? (
            <Check size={12} />
          ) : null}
          {status}
        </span>
        <button
          className="button secondary"
          disabled={loading || saving || text === saved.current}
          onClick={() => save()}
        >
          <Save size={13} />
          Save
        </button>
      </div>
      {error && (
        <div className="error-banner" role="alert">
          {error}
        </div>
      )}
      <div className="notes-privacy">
        <LockKeyhole size={12} />
        Only your account can read these notes.
      </div>
    </section>
  );
}
