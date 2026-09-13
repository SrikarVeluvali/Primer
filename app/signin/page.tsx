"use client";

import { signIn } from "next-auth/react";

export default function SignInPage() {
  return <main className="signin-page"><section className="signin-card"><div className="brand"><span>✦</span> Primer</div><p className="eyebrow">WELCOME TO PRIMER</p><h1>Make research<br />click for you.</h1><p>Sign in to save your papers, reading paths, and notes across devices.</p><button className="google-button" onClick={() => signIn("google", { callbackUrl: "/dashboard" })}><svg viewBox="0 0 24 24" aria-hidden="true"><path fill="#4285F4" d="M21.8 12.2c0-.7-.1-1.4-.2-2H12v3.8h5.5a4.7 4.7 0 0 1-2 3.1v2.5h3.2c1.9-1.8 3.1-4.3 3.1-7.4Z"/><path fill="#34A853" d="M12 22c2.7 0 5-.9 6.7-2.4l-3.2-2.5c-.9.6-2 .9-3.5.9-2.6 0-4.8-1.7-5.6-4.1H3.1v2.6A10 10 0 0 0 12 22Z"/><path fill="#FBBC05" d="M6.4 13.9a6 6 0 0 1 0-3.8V7.5H3.1a10 10 0 0 0 0 9l3.3 2.6Z"/><path fill="#EA4335" d="M12 6c1.6 0 3 .5 4.1 1.6l3-3A10 10 0 0 0 3.1 7.5l3.3 2.6C7.2 7.7 9.4 6 12 6Z"/></svg>Continue with Google</button><p className="signin-note">New here? Your Google account creates a Primer profile automatically.</p><a className="back-home" href="/">← Continue without an account</a></section></main>;
}
