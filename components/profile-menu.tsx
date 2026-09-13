"use client";

import { useEffect, useRef, useState } from "react";
import { useSession } from "next-auth/react";

export function ProfileMenu() {
  const { data: session } = useSession();
  const [open, setOpen] = useState(false);
  const [dark, setDark] = useState(false);
  const menu = useRef<HTMLDivElement>(null);
  const initial = session?.user?.name?.slice(0, 1).toUpperCase() ?? "P";

  useEffect(() => {
    const close = (event: MouseEvent) => { if (!menu.current?.contains(event.target as Node)) setOpen(false); };
    document.addEventListener("mousedown", close);
    return () => document.removeEventListener("mousedown", close);
  }, []);

  useEffect(() => {
    const savedAppearance = window.localStorage.getItem("primer-appearance");
    const shouldUseDark = savedAppearance === "dark";
    setDark(shouldUseDark);
    document.documentElement.classList.toggle("dark", shouldUseDark);
  }, []);

  function toggleAppearance() {
    const next = !dark;
    setDark(next);
    document.documentElement.classList.toggle("dark", next);
    window.localStorage.setItem("primer-appearance", next ? "dark" : "light");
  }

  return <div className="profile-menu" ref={menu}>
    <button className="profile-trigger" onClick={() => setOpen((current) => !current)} aria-expanded={open} aria-label="Open profile menu"><span>{initial}</span><i>⌄</i></button>
    {open && <div className="profile-popover" role="menu"><a href="/settings" role="menuitem"><b>◉</b><span>Profile<small>Account & settings</small></span></a><button onClick={toggleAppearance} role="menuitem"><b>{dark ? "☾" : "☀"}</b><span>Appearance<small>{dark ? "Dark mode" : "Light mode"}</small></span><em className={dark ? "is-dark" : ""} /></button><a href="/dashboard" role="menuitem"><b>⌂</b><span>Home<small>Your library</small></span></a><a href="/dashboard/folders" role="menuitem"><b>▱</b><span>Folders<small>Browse collections</small></span></a></div>}
  </div>;
}
