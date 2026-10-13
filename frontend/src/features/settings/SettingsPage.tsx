import { useState } from "react";
import {
  Check,
  LockKeyhole,
  RefreshCw,
  Settings2,
  ShieldCheck,
} from "lucide-react";
import type { User, Connection } from "../../lib/types";
import { ThemeSettings } from "./ThemeSettings";

export function Settings({
  user,
  connection,
  model,
  onModel,
  onRefresh,
  refreshing,
}: {
  user: User;
  connection: Connection | null;
  model: string;
  onModel: (m: string) => void;
  onRefresh: () => void;
  refreshing: boolean;
}) {
  const [manualModel, setManualModel] = useState(
    model || connection?.chatModel || "phi4-mini:latest",
  );
  const [saved, setSaved] = useState(false);
  return (
    <main className="settings-page">
      <div className="page-heading">
        <div>
          <div className="inline-label">
            <Settings2 size={16} />
            Make yourself at home
          </div>
          <h1>Your workspace, your way.</h1>
          <p>A few details that keep your research running smoothly.</p>
        </div>
      </div>
      <ThemeSettings />
      <div className="settings-section">
        <h2>Local intelligence</h2>
        <p>
          Primer uses Ollama on your machine to understand and answer questions.
        </p>
        <div className="settings-connection">
          <div>
            <span
              className={`connection-dot ${connection?.connected ? "online" : ""}`}
            />
            <strong>
              {connection?.connected
                ? "Connected to Ollama"
                : "Ollama is offline"}
            </strong>
          </div>
          <button
            className="button secondary"
            onClick={onRefresh}
            disabled={refreshing}
          >
            <RefreshCw size={14} className={refreshing ? "spin" : ""} />
            Check connection
          </button>
        </div>
        {connection && !connection.connected && (
          <div className="setup-guide">
            <p>Start Ollama, then check the connection again.</p>
            <code>ollama serve</code>
          </div>
        )}
        <form
          className="model-settings"
          onSubmit={(e) => {
            e.preventDefault();
            onModel(manualModel.trim());
            setSaved(true);
            setTimeout(() => setSaved(false), 2000);
          }}
        >
          <label>
            Answer model
            <input
              list="installed-models"
              value={manualModel}
              onChange={(e) => {
                setManualModel(e.target.value);
                setSaved(false);
              }}
              placeholder="llama3.2:3b"
              required
              maxLength={100}
            />
          </label>
          <datalist id="installed-models">
            {connection?.models.map((m) => <option key={m} value={m} />)}
          </datalist>
          <button className="button primary">
            {saved ? (
              <>
                <Check size={16} />
                Saved
              </>
            ) : (
              "Save model"
            )}
          </button>
        </form>
        <p className="setting-help">
          Choose an installed chat model. This preference applies to your
          workspace.
        </p>
        <div className="embedding-setting">
          <span>Paper embedding model</span>
          <strong>{connection?.embeddingModel || "nomic-embed-text"}</strong>
          <p>
            The shared index uses the server’s embedding model. Each paper
            remembers the model used to index it.
          </p>
        </div>
        <div className="setup-guide">
          <p>If a model is missing, install it in a terminal:</p>
          <code>
            ollama pull {manualModel.trim() || "llama3.2:3b"}
            <br />
            ollama pull {connection?.embeddingModel || "nomic-embed-text"}
          </code>
        </div>
      </div>
      <div className="settings-section">
        <h2>Your account</h2>
        <div className="account-settings">
          <span className="avatar">{user.name[0].toUpperCase()}</span>
          <div>
            <strong>{user.name}</strong>
            <p>{user.email}</p>
          </div>
          <span className="private-chip">
            <LockKeyhole size={13} />
            Private workspace
          </span>
        </div>
        <div className="privacy-settings">
          <ShieldCheck size={20} />
          <div>
            <h3>Shared knowledge. Personal conversations.</h3>
            <p>
              Papers and their indexes are available to every signed-in account.
              Your questions, answers, and history are tied to your account and
              never shared with other users.
            </p>
          </div>
        </div>
      </div>
    </main>
  );
}
