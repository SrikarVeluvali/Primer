"use client";

import { useParams } from "next/navigation";
import { ProfileMenu } from "@/components/profile-menu";

export default function FolderPage() {
    const params = useParams<{ slug: string }>(); const title = params.slug.split("-").map((word) => word[0].toUpperCase() + word.slice(1)).join(" "); return <main className="dashboard"><nav className="dashboard-nav"><div className="brand"><span>✦</span> Primer</div><a href="/dashboard">Library</a><a className="nav-active" href="/dashboard/folders">Folders</a><span className="nav-spacer" /><ProfileMenu /></nav><section className="dashboard-content folder-page"><a className="back" href="/dashboard/folders">← All folders</a><header className="folder-title"><span>▱</span><div><p className="eyebrow">FOLDER</p><h1>{title}</h1><p>Three papers in this collection</p></div><button className="secondary">Manage folder</button></header><div className="folder-paper-list">{["Attention Is All You Need", "BERT: Pre-training of Deep Bidirectional Transformers", "A Survey of Transformers"].map((paper, index) => <a href="/reader/demo" key={paper}><span className={`list-art list-${index}`}>arXiv</span><div><h2>{paper}</h2><p>{index === 0 ? "64% read · Your reading guide is ready" : "Not started · Add a reading goal to begin"}</p></div><b>Open →</b></a>)}</div></section></main>; }
