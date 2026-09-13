"use client";

import { FormEvent, useState } from "react";
import { useSession } from "next-auth/react";
import { ProfileMenu } from "@/components/profile-menu";

const initialFolders = ["Machine learning", "Distributed systems", "Design research"];
const recentPapers = [
  { title: "Attention Is All You Need", tag: "Machine learning", progress: 64, shade: "lime" },
  { title: "The Annotated Transformer", tag: "Reading next", progress: 18, shade: "peach" },
  { title: "BERT: Pre-training of Deep Bidirectional Transformers", tag: "Machine learning", progress: 0, shade: "blue" },
];

export default function DashboardPage() {
  const { data: session } = useSession();
  const [folders, setFolders] = useState(initialFolders);
  const [showAdd, setShowAdd] = useState(false);
  const [url, setUrl] = useState("");
  const [folderName, setFolderName] = useState("");
  const [message, setMessage] = useState("");
  const greeting = session?.user?.name?.split(" ")[0] ?? "there";
  function addFolder(event: FormEvent) { event.preventDefault(); const name = folderName.trim(); if (name && !folders.includes(name)) setFolders((current) => [...current, name]); setFolderName(""); }
  function addPaper(event: FormEvent) { event.preventDefault(); if (!url.trim()) return; setShowAdd(false); setMessage("Your paper was added to Recents. Open the demo reader to continue the study flow."); setUrl(""); }
  return <main className="dashboard"><DashboardNav name={greeting} /><section className="dashboard-content"><header className="dashboard-header"><div><p className="eyebrow">YOUR LIBRARY</p><h1>Good to see you, {greeting}.</h1><p>Pick up where you left off, or begin with something new.</p></div><button className="primary add-paper" onClick={() => setShowAdd(true)}>＋ Add a paper</button></header>{message && <p className="dashboard-message">✦ {message}</p>}<section><div className="section-heading"><h2>Recents</h2><a href="/reader/demo">View all →</a></div><div className="paper-grid">{recentPapers.map((paper) => <a className="paper-card" href="/reader/demo" key={paper.title}><div className={`paper-art ${paper.shade}`}><span>arXiv</span><strong>{paper.title.split(" ").slice(0, 3).join(" ")}</strong></div><div className="paper-card-info"><p>{paper.tag}</p><h3>{paper.title}</h3>{paper.progress > 0 ? <div className="card-progress"><i style={{ width: `${paper.progress}%` }} /><span>{paper.progress}% read</span></div> : <span className="not-started">Not started</span>}</div></a>)}<button className="new-paper-card" onClick={() => setShowAdd(true)}><span>＋</span><b>Bring a new paper</b><small>Paste an arXiv link</small></button></div></section><section className="folder-section"><div className="section-heading"><h2>Folders</h2></div><div className="folder-grid">{folders.map((folder) => <a className="folder-card" href={`/dashboard/folders/${encodeURIComponent(folder.toLowerCase().replaceAll(" ", "-"))}`} key={folder}><span>▱</span><h3>{folder}</h3><p>3 papers</p></a>)}<form className="folder-add" onSubmit={addFolder}><input aria-label="Folder name" value={folderName} onChange={(event) => setFolderName(event.target.value)} placeholder="New folder name" /><button>Add folder</button></form></div></section></section>{showAdd && <div className="modal-backdrop" role="presentation"><form className="add-paper-modal" onSubmit={addPaper}><button type="button" className="modal-close" onClick={() => setShowAdd(false)}>×</button><p className="eyebrow">ADD TO YOUR LIBRARY</p><h2>What are you reading?</h2><p>Paste an arXiv URL or reference and Primer will prepare a guided reading path.</p><input autoFocus value={url} onChange={(event) => setUrl(event.target.value)} placeholder="https://arxiv.org/abs/..." /><button className="primary wide">Add paper →</button></form></div>}</main>;
}

function DashboardNav({ name: _name }: { name: string }) { return <nav className="dashboard-nav"><div className="brand"><span>✦</span> Primer</div><a className="nav-active" href="/dashboard">Library</a><a href="/dashboard/folders">Folders</a><span className="nav-spacer" /><ProfileMenu /></nav>; }
