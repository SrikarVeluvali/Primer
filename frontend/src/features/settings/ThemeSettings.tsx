import { useState } from "react";
import { Check, Palette, Plus, Trash2, Pencil, X } from "lucide-react";
import { BUILTIN_THEMES, useTheme } from "./ThemeProvider";
import type { Theme } from "../../lib/types";

const fields: { key: keyof Theme; label: string }[] = [
  { key: "background", label: "Background" },
  { key: "surface", label: "Workspace" },
  { key: "panel", label: "Side panels" },
  { key: "text", label: "Text" },
  { key: "muted", label: "Secondary text" },
  { key: "accent", label: "Accent" },
  { key: "border", label: "Borders" },
];
function contrast(a: string, b: string) {
  const luminance = (hex: string) => {
    const c = [1, 3, 5]
      .map((i) => parseInt(hex.slice(i, i + 2), 16) / 255)
      .map((v) => (v <= 0.04045 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4));
    return c[0] * 0.2126 + c[1] * 0.7152 + c[2] * 0.0722;
  };
  const x = luminance(a),
    y = luminance(b);
  return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05);
}
export function ThemeSettings() {
  const { preferences, active, select, save, saving, loaded, error } =
    useTheme();
  const [draft, setDraft] = useState<Theme | null>(null);
  const [saved, setSaved] = useState(false);
  function create() {
    setSaved(false);
    setDraft({ ...active, id: crypto.randomUUID(), name: "My theme" });
  }
  async function persist() {
    if (!draft || !draft.name.trim()) return;
    const theme = { ...draft, name: draft.name.trim() };
    const themes = [
      ...preferences.themes.filter((t) => t.id !== theme.id),
      theme,
    ];
    if (await save({ activeTheme: theme.id, themes })) {
      setDraft(null);
      setSaved(true);
    }
  }
  async function remove(id: string) {
    if (
      await save({
        activeTheme:
          preferences.activeTheme === id ? "dark" : preferences.activeTheme,
        themes: preferences.themes.filter((t) => t.id !== id),
      })
    )
      setDraft(null);
  }
  return (
    <section className="settings-section appearance-settings">
      <h2>Appearance</h2>
      <p>
        Choose Light or Dark, or make a workspace that feels like yours. Custom
        themes are saved to your account.
      </p>
      <div className="theme-grid">
        {[...BUILTIN_THEMES, ...preferences.themes].map((theme) => (
          <div
            className={`theme-option ${active.id === theme.id ? "selected" : ""}`}
            key={theme.id}
          >
            <button
              className="theme-select"
              disabled={saving || !loaded}
              onClick={() => select(theme.id)}
              aria-pressed={active.id === theme.id}
            >
              <div
                className="theme-thumbnail"
                style={{
                  background: theme.background,
                  borderColor: theme.border,
                }}
              >
                <span style={{ background: theme.panel }} />
                <div style={{ background: theme.surface }}>
                  <i style={{ background: theme.accent }} />
                  <b style={{ background: theme.text }} />
                  <b style={{ background: theme.muted }} />
                </div>
              </div>
              <span>
                {theme.name}
                {active.id === theme.id && <Check size={14} />}
              </span>
            </button>
            {!BUILTIN_THEMES.some((t) => t.id === theme.id) && (
              <div className="theme-option-actions">
                <button
                  aria-label={`Edit ${theme.name} theme`}
                  onClick={() => {
                    setDraft({ ...theme });
                    setSaved(false);
                  }}
                >
                  <Pencil size={13} />
                </button>
                <button
                  aria-label={`Delete ${theme.name} theme`}
                  disabled={saving}
                  onClick={() => remove(theme.id)}
                >
                  <Trash2 size={13} />
                </button>
              </div>
            )}
          </div>
        ))}
        <button
          className="theme-create"
          onClick={create}
          disabled={!loaded || preferences.themes.length >= 20}
        >
          <Plus size={20} />
          <span>Create theme</span>
        </button>
      </div>
      {draft && (
        <div className="theme-editor">
          <div className="theme-editor-heading">
            <span>
              <Palette size={16} />
              Your custom theme
            </span>
            <button
              className="icon-button"
              aria-label="Cancel theme edit"
              onClick={() => setDraft(null)}
            >
              <X size={17} />
            </button>
          </div>
          <div className="theme-editor-fields">
            <label>
              Theme name
              <input
                value={draft.name}
                maxLength={60}
                onChange={(e) => setDraft({ ...draft, name: e.target.value })}
              />
            </label>
            <label>
              Base mode
              <select
                value={draft.mode}
                onChange={(e) =>
                  setDraft({
                    ...draft,
                    mode: e.target.value as "light" | "dark",
                  })
                }
              >
                <option value="dark">Dark</option>
                <option value="light">Light</option>
              </select>
            </label>
          </div>
          <div className="color-fields">
            {fields.map((f) => (
              <label key={f.key}>
                <input
                  type="color"
                  aria-label={f.label}
                  value={draft[f.key]}
                  onChange={(e) =>
                    setDraft({ ...draft, [f.key]: e.target.value })
                  }
                />
                <span>
                  {f.label}
                  <small>{draft[f.key]}</small>
                </span>
              </label>
            ))}
          </div>
          <div
            className="theme-live-preview"
            style={{
              background: draft.background,
              color: draft.text,
              borderColor: draft.border,
            }}
          >
            <div style={{ background: draft.panel, color: draft.muted }}>
              Your research desk
            </div>
            <section style={{ background: draft.surface }}>
              <h3>A little closer to understanding.</h3>
              <p style={{ color: draft.muted }}>
                A space for your papers, questions, and notes.
              </p>
              <span
                style={{
                  background: draft.accent,
                  color: draft.mode === "dark" ? "#141925" : "#ffffff",
                }}
              >
                Explore a paper
              </span>
            </section>
          </div>
          {contrast(draft.text, draft.surface) < 4.5 && (
            <p className="theme-contrast-note">
              Text contrast is low (
              {contrast(draft.text, draft.surface).toFixed(1)}:1). A lighter
              text or darker workspace color will improve readability.
            </p>
          )}
          <div className="theme-editor-footer">
            <button className="button secondary" onClick={() => setDraft(null)}>
              Cancel
            </button>
            <button
              className="button primary"
              disabled={saving || !draft.name.trim()}
              onClick={persist}
            >
              {saving ? "Saving…" : "Save and apply theme"}
            </button>
          </div>
        </div>
      )}
      {saved && (
        <p className="save-confirmation" role="status">
          <Check size={14} />
          Theme saved to your account.
        </p>
      )}
      {error && (
        <div className="error-banner" role="alert">
          {error}
        </div>
      )}
    </section>
  );
}
