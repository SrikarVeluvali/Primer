import { useState, useEffect } from "react";
import { LoaderCircle } from "lucide-react";
import { Brand } from "./components/Brand";
import { AuthScreen } from "./components/AuthScreen";
import { Workspace } from "./components/WorkspaceShell";
import { getSession } from "./lib/api";
import { errorText } from "./lib/research";
import type { User } from "./lib/types";
import { ThemeProvider } from "./features/settings/ThemeProvider";

export default function App() {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [serverError, setServerError] = useState("");
  useEffect(() => {
    getSession()
      .then((s) => setUser(s.user))
      .catch((e) => setServerError(errorText(e)))
      .finally(() => setLoading(false));
    const expired = () => {
      setUser(null);
      setServerError("Your session has expired. Sign in again to continue.");
    };
    window.addEventListener("session-expired", expired);
    return () => window.removeEventListener("session-expired", expired);
  }, []);
  if (loading)
    return (
      <ThemeProvider userId={null}>
        <div className="boot-screen">
          <Brand />
          <LoaderCircle className="spin" size={22} />
          <p>Opening your workspace…</p>
        </div>
      </ThemeProvider>
    );
  if (!user)
    return (
      <ThemeProvider userId={null}>
        <AuthScreen
          serverError={serverError}
          onSignIn={(u) => {
            setUser(u);
            setServerError("");
          }}
        />
      </ThemeProvider>
    );
  return (
    <ThemeProvider userId={user.id}>
      <Workspace
        key={user.id}
        user={user}
        onSignOut={() => {
          setUser(null);
          setServerError("");
        }}
      />
    </ThemeProvider>
  );
}
