import { useState, useEffect, useRef } from "react";
import {
  ArrowRight,
  FileText,
  Link2,
  LoaderCircle,
  ShieldCheck,
  X,
} from "lucide-react";
import { post } from "../../lib/api";
import { errorText } from "../../lib/research";
import type { Paper } from "../../lib/types";

export function ImportForm({
  onImport,
  dialog = false,
}: {
  onImport: (p: Paper) => void;
  dialog?: boolean;
}) {
  const [url, setUrl] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const input = useRef<HTMLInputElement>(null);
  useEffect(() => {
    const example = (e: Event) => {
      setUrl((e as CustomEvent<string>).detail);
      input.current?.focus();
    };
    window.addEventListener("example-paper", example);
    return () => window.removeEventListener("example-paper", example);
  }, []);
  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!url.trim()) return;
    setBusy(true);
    setError("");
    try {
      onImport(await post<Paper>("/papers", { url: url.trim() }));
      setUrl("");
    } catch (e) {
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <form
      onSubmit={submit}
      className={dialog ? "import-form in-dialog" : "import-form"}
    >
      <label htmlFor={dialog ? "dialog-url" : "paper-url"}>
        Bring a paper to the conversation
      </label>
      <div className="import-input">
        <Link2 size={19} />
        <input
          id={dialog ? "dialog-url" : "paper-url"}
          ref={input}
          autoFocus={dialog}
          value={url}
          onChange={(e) => setUrl(e.target.value)}
          placeholder="Paste an arXiv link or paper ID"
          required
          maxLength={500}
        />
        <button className="button primary" disabled={busy || !url.trim()}>
          {busy ? (
            <LoaderCircle size={18} className="spin" />
          ) : (
            <>
              Explore paper
              <ArrowRight size={17} />
            </>
          )}
        </button>
      </div>
      {error && (
        <div role="alert" className="error-banner">
          {error}
        </div>
      )}
    </form>
  );
}

export function ImportDialog({
  onClose,
  onImport,
}: {
  onClose: () => void;
  onImport: (p: Paper) => void;
}) {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const key = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
      if (e.key === "Tab") {
        const nodes = ref.current?.querySelectorAll<HTMLElement>(
          "button:not(:disabled), input",
        );
        if (!nodes?.length) return;
        if (e.shiftKey && document.activeElement === nodes[0]) {
          e.preventDefault();
          nodes[nodes.length - 1].focus();
        } else if (
          !e.shiftKey &&
          document.activeElement === nodes[nodes.length - 1]
        ) {
          e.preventDefault();
          nodes[0].focus();
        }
      }
    };
    document.addEventListener("keydown", key);
    return () => document.removeEventListener("keydown", key);
  }, [onClose]);
  return (
    <div
      className="modal-backdrop"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div
        ref={ref}
        className="import-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="import-title"
      >
        <button
          className="dialog-close icon-button"
          aria-label="Close import"
          onClick={onClose}
        >
          <X size={20} />
        </button>
        <span className="dialog-symbol">
          <FileText size={27} />
        </span>
        <h2 id="import-title">Make space for a new idea.</h2>
        <p>
          Add a paper to the shared library, then explore it
          <br />
          in your own private conversation.
        </p>
        <ImportForm onImport={onImport} dialog />
        <div className="dialog-foot">
          <ShieldCheck size={16} />
          <span>
            Only arXiv links are supported. Abstract and PDF links both work.
          </span>
        </div>
      </div>
    </div>
  );
}
