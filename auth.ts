import type { NextAuthOptions } from "next-auth";
import GoogleProvider from "next-auth/providers/google";

export const authOptions: NextAuthOptions = {
  providers: [
    GoogleProvider({
      clientId: process.env.GOOGLE_CLIENT_ID ?? "",
      clientSecret: process.env.GOOGLE_CLIENT_SECRET ?? "",
    }),
  ],
  pages: { signIn: "/signin" },
  session: { strategy: "jwt" },
  // A stable local key keeps /api/auth/session usable during first-run development.
  // Production intentionally still requires NEXTAUTH_SECRET from the environment.
  secret: process.env.NEXTAUTH_SECRET ?? (process.env.NODE_ENV === "development" ? "primer-local-development-secret" : undefined),
};
