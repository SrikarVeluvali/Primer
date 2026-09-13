"use client";

import { FormEvent, useState } from "react";
import { ProfileMenu } from "@/components/profile-menu";

const defaultFolders = [
  { name: "Machine learning", papers: 3, color: "lime" },
  { name: "Distributed systems", papers: 4, color: "blue" },
  { name: "Design research", papers: 2, color: "peach" },
];

export default function FoldersPage() {
  const [folders, setFolders] = useState(defaultFolders);
  const [managing, setManaging] = useState(false);
  const [newFolder, setNewFolder] = useState("");
  const slug = (name: string) => name.toLowerCase().replaceAll(" ", "-");
  function addFolder(event: FormEvent) { event.preventDefault(); const name = newFolder.trim(); if (!name || folders.some((folder) => folder.name.toLowerCase() === name.toLowerCase())) return; setFolders((current) => [...current, { name, papers: 0, color: "lime" }]); setNewFolder(""); }
  function removeFolder(name: string) { setFolders((current) => current.filter((folder) => folder.name !== name)); }
  return <main className="dashboard"><nav className="dashboard-nav"><div className="brand"><span>✦</span> Primer</div><a href="/dashboard">Library</a><a className="nav-active" href="/dashboard/folders">Folders</a><span className="nav-spacer" /><ProfileMenu /></nav><section className="dashboard-content folders-overview"><header className="dashboard-header"><div><p className="eyebrow">YOUR LIBRARY</p><h1>Folders</h1><p>Organize papers into collections that match how you think.</p></div><button className="secondary manage-folders" onClick={() => setManaging((current) => !current)}>{managing ? "Done managing" : "Manage folders"}</button></header><div className="all-folders-grid">{folders.map((folder) => <article className="all-folder-card" key={folder.name}><a href={`/dashboard/folders/${slug(folder.name)}`}><div className={`folder-cover ${folder.color}`}><span>▱</span><b>{folder.papers}</b></div><h2>{folder.name}</h2><p>{folder.papers === 1 ? "1 paper" : `${folder.papers} papers`}</p></a>{managing && <button className="remove-folder" onClick={() => removeFolder(folder.name)} aria-label={`Remove ${folder.name}`}>×</button>}</article>)}<form className="all-folder-add" onSubmit={addFolder}><span>＋</span><input value={newFolder} onChange={(event) => setNewFolder(event.target.value)} placeholder="Folder name" /><button>Add folder</button></form></div>{managing && <p className="manage-hint">Remove a folder only when you no longer need its collection. Paper deletion will be connected when backend folder APIs are added.</p>}</section></main>;
}
