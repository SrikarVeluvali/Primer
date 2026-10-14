import { useState, useEffect, useRef, useCallback } from "react";
import {
  ArrowRight,
  ArrowUpRight,
  BookOpen,
  ChevronRight,
  FileText,
  FolderOpen,
  LoaderCircle,
  LockKeyhole,
  LogOut,
  Menu,
  MessageSquare,
  Plus,
  RefreshCw,
  Search,
  Settings2,
  X,
} from "lucide-react";
import { Brand } from "./Brand";
import { StatusBadge } from "./StatusBadge";
import { ImportDialog } from "../features/library/ImportPaper";
import { Home } from "../features/research/Home";
import { PaperWorkspace } from "../features/research/PaperWorkspace";
import { Settings } from "../features/settings/SettingsPage";
import { api, getSession, post } from "../lib/api";
import { errorText } from "../lib/research";
import type { User, Paper, Connection } from "../lib/types";
import { ThemeToggle } from "../features/settings/ThemeProvider";
import { KnowledgePage, Recommendations } from "../features/knowledge/KnowledgePage";
import { Compass } from "lucide-react";

type View = "workspace" | "library" | "settings" | "knowledge";

export function Workspace({
  user,
  onSignOut,
}: {
  user: User;
  onSignOut: () => void;
}) {
  const [view, setView] = useState<View>("workspace");
  const [papers, setPapers] = useState<Paper[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [connection, setConnection] = useState<Connection | null>(null);
  const [model, setModel] = useState(
    localStorage.getItem(`folio-model-${user.id}`) || "",
  );
  const [importOpen, setImportOpen] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [error, setError] = useState("");
  const [refreshing, setRefreshing] = useState(false);
  const [libraryLoaded, setLibraryLoaded] = useState(false);
  const searchRef = useRef<HTMLInputElement>(null);
  const paper = papers.find((p) => p.id === selectedId) || null;
  const refreshPapers = useCallback(async () => {
    try {
      setPapers(await api<Paper[]>("/papers"));
      setLibraryLoaded(true);
    } catch (e) {
      setError(errorText(e));
    }
  }, []);
  const refreshConnection = useCallback(async () => {
    setRefreshing(true);
    try {
      setConnection(await api<Connection>("/status"));
    } catch (e) {
      setError(errorText(e));
    } finally {
      setRefreshing(false);
    }
  }, []);
  useEffect(() => {
    refreshPapers();
    refreshConnection();
  }, [refreshPapers, refreshConnection]);
  useEffect(() => {
    const interval = window.setInterval(
      () => {
        refreshPapers();
      },
      papers.some((p) => p.status === "INDEXING" || p.status === "QUEUED")
        ? 2000
        : 15000,
    );
    return () => clearInterval(interval);
  }, [papers, refreshPapers]);
  useEffect(() => {
    const key = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key === "k") {
        e.preventDefault();
        setView("library");
        setTimeout(() => searchRef.current?.focus(), 0);
      }
    };
    window.addEventListener("keydown", key);
    return () => window.removeEventListener("keydown", key);
  }, []);
  function select(p: Paper) {
    setSelectedId(p.id);
    setView("workspace");
    setMenuOpen(false);
  }
  function chooseModel(value: string) {
    setModel(value);
    localStorage.setItem(`folio-model-${user.id}`, value);
  }
  async function signOut() {
    try {
      await post("/auth/logout");
      await getSession();
      onSignOut();
    } catch (e) {
      setError(errorText(e));
    }
  }
  function imported(p: Paper) {
    setPapers((previous) => [
      p,
      ...previous.filter((other) => other.id !== p.id),
    ]);
    select(p);
    setImportOpen(false);
  }
  return (
    <div className="app-shell">
      {menuOpen && (
        <button
          className="sidebar-scrim"
          aria-label="Close navigation"
          onClick={() => setMenuOpen(false)}
        />
      )}
      <aside className={`sidebar ${menuOpen ? "sidebar-open" : ""}`}>
        <button
          className="brand-button"
          onClick={() => {
            setSelectedId(null);
            setView("workspace");
          }}
        >
          <Brand />
        </button>
        <button
          className="new-paper-button"
          onClick={() => setImportOpen(true)}
        >
          <Plus size={18} />
          Add a paper
        </button>
        <nav className="main-nav" aria-label="Main navigation">
          <button
            className={view === "workspace" ? "active" : ""}
            onClick={() => {
              setView("workspace");
              setMenuOpen(false);
            }}
          >
            <MessageSquare size={18} />
            Workspace
          </button>
          <button
            className={view === "library" ? "active" : ""}
            onClick={() => {
              setView("library");
              setMenuOpen(false);
            }}
          >
            <BookOpen size={18} />
            Paper library<span className="nav-count">{papers.length}</span>
          </button>
        </nav>
        <nav className="main-nav" aria-label="Personal learning"><button className={view === "knowledge" ? "active" : ""} onClick={() => { setView("knowledge"); setMenuOpen(false); }}><Compass size={18} /> Knowledge & interests</button></nav>
        <div className="sidebar-section-title">
          <span>Recently added</span>
          <span className="shared-icon" title="Shared across accounts">
            <FolderOpen size={14} />
          </span>
        </div>
        <div className="recent-papers">
          {papers.length ? (
            papers.slice(0, 8).map((p) => (
              <button
                key={p.id}
                onClick={() => select(p)}
                className={
                  p.id === selectedId && view === "workspace" ? "selected" : ""
                }
              >
                <FileText size={16} />
                <span>
                  {p.title}
                  <small>
                    {p.status === "READY" ? `arXiv ${p.arxivId}` : p.stage}
                  </small>
                </span>
                {p.status === "INDEXING" || p.status === "QUEUED" ? (
                  <LoaderCircle size={13} className="spin" />
                ) : null}
              </button>
            ))
          ) : (
            <div className="sidebar-empty">
              Your next discovery
              <br />
              starts with a paper.
            </div>
          )}
        </div>
        <div className="sidebar-bottom">
          <div className="local-card">
            <div>
              <span
                className={`connection-dot ${connection?.connected ? "online" : ""}`}
              />
              <strong>
                {connection?.connected
                  ? "Ollama connected"
                  : connection
                    ? "Ollama offline"
                    : "Checking Ollama"}
              </strong>
              <button
                aria-label="Refresh Ollama connection"
                onClick={refreshConnection}
              >
                <RefreshCw size={13} className={refreshing ? "spin" : ""} />
              </button>
            </div>
            <small>
              {connection?.connected
                ? "Intelligence, right on your machine."
                : "Start Ollama to explore your papers."}
            </small>
          </div>
          <button
            className={`settings-nav ${view === "settings" ? "active" : ""}`}
            onClick={() => {
              setView("settings");
              setMenuOpen(false);
            }}
          >
            <Settings2 size={17} />
            Settings
          </button>
          <div className="user-row">
            <span className="avatar">
              {user.name.slice(0, 1).toUpperCase()}
            </span>
            <div>
              <strong>{user.name}</strong>
              <small>Personal workspace</small>
            </div>
            <button aria-label="Sign out" title="Sign out" onClick={signOut}>
              <LogOut size={17} />
            </button>
          </div>
        </div>
      </aside>
      <div className="app-body">
        <header className="topbar">
          <div className="breadcrumb">
            <button
              className="mobile-menu icon-button"
              aria-label="Open navigation"
              onClick={() => setMenuOpen(true)}
            >
              <Menu size={20} />
            </button>
            <span>
              {view === "library"
                ? "Paper library"
                : view === "knowledge" ? "Knowledge & interests"
                : view === "settings"
                  ? "Settings"
                  : "Workspace"}
            </span>
            {paper && view === "workspace" && (
              <>
                <ChevronRight size={14} />
                <span className="breadcrumb-paper">{paper.title}</span>
              </>
            )}
          </div>
          <div className="topbar-actions">
            <ThemeToggle />
            <span className="privacy-label">
              <LockKeyhole size={13} />
              Private conversations
            </span>
            <button
              className="icon-button search-button"
              aria-label="Search papers"
              onClick={() => {
                setView("library");
                setTimeout(() => searchRef.current?.focus(), 0);
              }}
            >
              <Search size={18} />
              <kbd>⌘ K</kbd>
            </button>
          </div>
        </header>
        {error && (
          <div className="global-error error-banner" role="alert">
            <span>{error}</span>
            <button aria-label="Dismiss error" onClick={() => setError("")}>
              <X size={16} />
            </button>
          </div>
        )}
        {view === "workspace" && !paper && (
          <Home
            user={user}
            papers={papers}
            onImport={imported}
            onSelect={select}
            onLibrary={() => setView("library")}
          />
        )}
        {view === "workspace" && paper && (
          <PaperWorkspace
            key={paper.id}
            paper={paper}
            model={model || connection?.chatModel || "phi4-mini:latest"}
            connection={connection}
            onModel={chooseModel}
            onRefresh={refreshPapers}
            userId={user.id}
          />
        )}
        {view === "library" && (
          <div className="library-page">
            <div className="page-heading">
              <div>
                <div className="inline-label">
                  <FolderOpen size={16} />A shared space for research
                </div>
                <h1>The paper library.</h1>
                <p>
                  Shared papers. Fresh perspectives. Your own conversations.
                </p>
              </div>
              <button
                className="button primary"
                onClick={() => setImportOpen(true)}
              >
                <Plus size={17} />
                Add a paper
              </button>
            </div>
            <div className="library-tools">
              <div className="search-input">
                <Search size={18} />
                <input
                  ref={searchRef}
                  placeholder="Search by title, author, tag, or arXiv ID"
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                />
              </div>
              <span>
                {papers.length} {papers.length === 1 ? "paper" : "papers"}
              </span>
            </div>
            {papers.filter((p) =>
              `${p.title} ${p.authors} ${p.arxivId} ${(p.tags || []).join(" ")}`
                .toLowerCase()
                .includes(query.toLowerCase()),
            ).length ? (
              <div className="library-grid">
                {papers
                  .filter((p) =>
                    `${p.title} ${p.authors} ${p.arxivId} ${(p.tags || []).join(" ")}`
                      .toLowerCase()
                      .includes(query.toLowerCase()),
                  )
                  .map((p) => (
                    <button
                      className="library-paper"
                      key={p.id}
                      onClick={() => select(p)}
                    >
                      <div className="library-paper-top">
                        <span className="paper-type-icon">
                          <FileText size={22} />
                        </span>
                        <StatusBadge paper={p} />
                      </div>
                      <h3>{p.title}</h3>
                      <p>{p.authors || "Fetching author details…"}</p>
                      <div className="topic-tags">{(p.tags || []).slice(0, 4).map(tag => <span key={tag}>{tag}</span>)}</div>
                      <div className="library-paper-bottom">
                        <span>arXiv {p.arxivId}</span>
                        <ArrowUpRight size={17} />
                      </div>
                    </button>
                  ))}
              </div>
            ) : (
              <div className="library-empty">
                <BookOpen size={38} />
                <h3>
                  {query
                    ? "No papers found."
                    : libraryLoaded
                      ? "A library of possibilities."
                      : "Opening the library…"}
                </h3>
                <p>
                  {query
                    ? "Try a different title or author."
                    : "Add an arXiv paper to start exploring. Everyone can read it; your questions stay private."}
                </p>
                {!query && (
                  <button
                    className="button primary"
                    onClick={() => setImportOpen(true)}
                  >
                    <Plus size={17} />
                    Add your first paper
                  </button>
                )}
              </div>
            )}
            <Recommendations onSelect={select} />
          </div>
        )}
        {view === "knowledge" && <KnowledgePage onSelect={select} />}
        {view === "settings" && (
          <Settings
            user={user}
            connection={connection}
            model={model}
            onModel={chooseModel}
            onRefresh={refreshConnection}
            refreshing={refreshing}
          />
        )}
      </div>
      {importOpen && (
        <ImportDialog
          onClose={() => setImportOpen(false)}
          onImport={imported}
        />
      )}
    </div>
  );
}
