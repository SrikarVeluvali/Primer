import {
  createContext,
  useContext,
  useEffect,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { Moon, Sun } from "lucide-react";
import { api } from "../../lib/api";
import type { Preferences, Theme } from "../../lib/types";

export const BUILTIN_THEMES: Theme[] = [
  {
    id: "light",
    name: "Light",
    mode: "light",
    background: "#f6f7fb",
    surface: "#ffffff",
    panel: "#f1f2f8",
    text: "#26334b",
    muted: "#6e7589",
    accent: "#6c59d9",
    border: "#e3e5ef",
  },
  {
    id: "dark",
    name: "Dark",
    mode: "dark",
    background: "#141925",
    surface: "#1c2331",
    panel: "#171e2b",
    text: "#e3e8f2",
    muted: "#a0adc1",
    accent: "#a79cf1",
    border: "#30394b",
  },
];
type Context = {
  preferences: Preferences;
  active: Theme;
  saving: boolean;
  loaded: boolean;
  error: string;
  save: (preferences: Preferences) => Promise<boolean>;
  select: (id: string) => Promise<boolean>;
};
const ThemeContext = createContext<Context | null>(null);
export const useTheme = () => {
  const context = useContext(ThemeContext);
  if (!context) throw new Error("Theme provider missing");
  return context;
};
function apply(theme: Theme) {
  const root = document.documentElement;
  root.dataset.theme = theme.mode;
  for (const [key, value] of Object.entries({
    bg: theme.background,
    surface: theme.surface,
    panel: theme.panel,
    text: theme.text,
    "text-muted": theme.muted,
    accent: theme.accent,
    border: theme.border,
  }))
    root.style.setProperty(`--${key}`, value);
  const channel = [1, 3, 5]
    .map((i) => parseInt(theme.accent.slice(i, i + 2), 16) / 255)
    .map((v) => (v <= 0.04045 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4));
  const luminance =
    channel[0] * 0.2126 + channel[1] * 0.7152 + channel[2] * 0.0722;
  root.style.setProperty(
    "--on-accent",
    luminance > 0.18 ? "#141925" : "#ffffff",
  );
  root.style.colorScheme = theme.mode;
}
export function ThemeProvider({
  userId,
  children,
}: {
  userId: string | null;
  children: ReactNode;
}) {
  const [preferences, setPreferences] = useState<Preferences>({
    activeTheme: "dark",
    themes: [],
  });
  const [saving, setSaving] = useState(false);
  const [loaded, setLoaded] = useState(false);
  const [error, setError] = useState("");
  const owner = useRef(userId);
  owner.current = userId;
  const active =
    [...BUILTIN_THEMES, ...preferences.themes].find(
      (t) => t.id === preferences.activeTheme,
    ) || BUILTIN_THEMES[1];
  useEffect(() => {
    apply(active);
  }, [active]);
  useEffect(() => {
    let cancelled = false;
    setLoaded(false);
    setError("");
    let initial: Preferences = { activeTheme: "dark", themes: [] };
    if (userId)
      try {
        const cached = localStorage.getItem(`primer-themes-${userId}`);
        if (cached) initial = JSON.parse(cached);
      } catch {
        /* Server remains authoritative. */
      }
    setPreferences(initial);
    if (!userId) {
      setLoaded(true);
      return;
    }
    api<Preferences>("/preferences")
      .then((p) => {
        if (!cancelled) {
          setPreferences(p);
          localStorage.setItem(`primer-themes-${userId}`, JSON.stringify(p));
        }
      })
      .catch((e) => {
        if (!cancelled) setError((e as Error).message);
      })
      .finally(() => {
        if (!cancelled) setLoaded(true);
      });
    return () => {
      cancelled = true;
    };
  }, [userId]);
  async function save(next: Preferences) {
    if (saving || !loaded || !userId) return false;
    const previous = preferences;
    const account = userId;
    setSaving(true);
    setError("");
    setPreferences(next);
    try {
      const result = await api<Preferences>("/preferences", {
        method: "PUT",
        body: JSON.stringify(next),
      });
      if (owner.current === account) {
        setPreferences(result);
        localStorage.setItem(
          `primer-themes-${account}`,
          JSON.stringify(result),
        );
      }
      return true;
    } catch (e) {
      if (owner.current === account) {
        setPreferences(previous);
        setError((e as Error).message);
      }
      return false;
    } finally {
      setSaving(false);
    }
  }
  return (
    <ThemeContext.Provider
      value={{
        preferences,
        active,
        saving,
        loaded,
        error,
        save,
        select: (id) => save({ ...preferences, activeTheme: id }),
      }}
    >
      {children}
    </ThemeContext.Provider>
  );
}
export function ThemeToggle() {
  const { active, select, saving, loaded } = useTheme();
  return (
    <button
      className="icon-button theme-toggle"
      aria-label={
        active.mode === "dark"
          ? "Switch to Light theme"
          : "Switch to Dark theme"
      }
      title={`Current theme: ${active.name}`}
      disabled={saving || !loaded}
      onClick={() => select(active.mode === "dark" ? "light" : "dark")}
    >
      {active.mode === "dark" ? <Sun size={18} /> : <Moon size={18} />}
    </button>
  );
}
