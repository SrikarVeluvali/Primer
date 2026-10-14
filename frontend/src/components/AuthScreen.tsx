import { useState } from "react";
import {
  ArrowUpRight,
  Eye,
  EyeOff,
  LockKeyhole,
  LoaderCircle,
  ArrowRight,
} from "lucide-react";
import { Brand } from "./Brand";
import { PaperArt } from "./PaperArt";
import { getSession, post } from "../lib/api";
import type { User } from "../lib/types";

export function AuthScreen({
  onSignIn,
  serverError,
}: {
  onSignIn: (user: User) => void;
  serverError: string;
}) {
  const [register, setRegister] = useState(false);
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [visible, setVisible] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      await getSession();
      const user = await post<User>(
        `/auth/${register ? "register" : "login"}`,
        register ? { name, email, password } : { email, password },
      );
      await getSession();
      onSignIn(user);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="auth-page">
      <section className="auth-story">
        <Brand />
        <div className="auth-story-content">
          <div className="story-tag">
            <span /> Your local research companion
          </div>
          <h1>
            A little closer
            <br />
            to understanding.
          </h1>
          <p>
            Good research starts with good questions.
            <br />
            Give your papers a space to answer.
          </p>
          <PaperArt />
        </div>
        <div className="auth-story-footer">
          <LockKeyhole size={14} />
          <span>Local intelligence. Your conversations stay yours.</span>
        </div>
      </section>
      <section className="auth-form-panel">
        <div className="auth-top-link">
          Made for curious minds <ArrowUpRight size={16} />
        </div>
        <div className="auth-form-wrap">
          <div className="auth-small-symbol">
            <span className="brand-mark">
              <i />
              <i />
              <i />
            </span>
          </div>
          <h2>
            {register
              ? "Make room for discovery."
              : "Welcome to your workspace."}
          </h2>
          <p>
            {register
              ? "Create your account. Start a conversation with research."
              : "Sign in to pick up where your curiosity left off."}
          </p>
          <div className="auth-tabs">
            <button
              onClick={() => {
                setRegister(false);
                setError("");
              }}
              className={!register ? "active" : ""}
            >
              Sign in
            </button>
            <button
              onClick={() => {
                setRegister(true);
                setError("");
              }}
              className={register ? "active" : ""}
            >
              Create account
            </button>
          </div>
          <form onSubmit={submit}>
            {register && (
              <label>
                Your name
                <input
                  autoComplete="name"
                  placeholder="Alex Chen"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  required
                  maxLength={80}
                />
              </label>
            )}
            <label>
              Email address
              <input
                type="email"
                autoComplete="email"
                placeholder="you@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                maxLength={254}
              />
            </label>
            <label>
              Password
              <div className="password-input">
                <input
                  type={visible ? "text" : "password"}
                  autoComplete={register ? "new-password" : "current-password"}
                  placeholder={
                    register ? "At least 10 characters" : "Enter your password"
                  }
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  minLength={register ? 10 : 1}
                  maxLength={72}
                />
                <button
                  type="button"
                  aria-label={visible ? "Hide password" : "Show password"}
                  onClick={() => setVisible(!visible)}
                >
                  {visible ? <EyeOff size={17} /> : <Eye size={17} />}
                </button>
              </div>
            </label>
            {(error || serverError) && (
              <div className="error-banner" role="alert">
                {error || serverError}
              </div>
            )}
            <button className="button primary auth-submit" disabled={busy}>
              {busy ? (
                <LoaderCircle size={18} className="spin" />
              ) : (
                <>
                  {register ? "Create your workspace" : "Enter your workspace"}
                  <ArrowRight size={18} />
                </>
              )}
            </button>
          </form>
          <div className="auth-privacy">
            <LockKeyhole size={16} />
            <p>
              A shared paper library.
              <br />
              <strong>A conversation that’s only yours.</strong>
            </p>
          </div>
        </div>
        <footer className="auth-footer">
          A space for papers. A place for perspective.
        </footer>
      </section>
    </div>
  );
}
