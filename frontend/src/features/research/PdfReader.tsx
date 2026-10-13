import { useEffect, useRef, useState } from "react";
import {
  ArrowUpRight,
  ChevronLeft,
  ChevronRight,
  Download,
  LoaderCircle,
  Maximize2,
  Minimize2,
  Minus,
  Plus,
  RefreshCw,
  Highlighter,
  Sparkles,
  Trash2,
  X,
} from "lucide-react";
import {
  getDocument,
  GlobalWorkerOptions,
  TextLayer,
  type PDFDocumentProxy,
  type RenderTask,
} from "pdfjs-dist";
import workerUrl from "pdfjs-dist/build/pdf.worker.min.mjs?url";
import { api, post } from "../../lib/api";
import { errorText } from "../../lib/research";
import type { PdfHighlight, HighlightRect } from "../../lib/types";

GlobalWorkerOptions.workerSrc = workerUrl;

export default function PdfReader({
  paperId,
  initialPage = 1,
  onExplain,
  explainBusy,
}: {
  paperId: string;
  initialPage?: number;
  onExplain: (highlight: PdfHighlight) => Promise<void>;
  explainBusy: boolean;
}) {
  const [pdf, setPdf] = useState<PDFDocumentProxy | null>(null);
  const [page, setPage] = useState(initialPage);
  const [zoom, setZoom] = useState(1);
  const [expanded, setExpanded] = useState(false);
  const [loading, setLoading] = useState(true);
  const [rendering, setRendering] = useState(false);
  const [error, setError] = useState("");
  const [retry, setRetry] = useState(0);
  const [width, setWidth] = useState(280);
  const scrollRef = useRef<HTMLDivElement>(null);
  const canvasHost = useRef<HTMLDivElement>(null);
  const textHost = useRef<HTMLDivElement>(null);
  const frameRef = useRef<HTMLDivElement>(null);
  const [dimensions, setDimensions] = useState({ width: 280, height: 360, scale: 1 });
  const [highlights, setHighlights] = useState<PdfHighlight[]>([]);
  const [selection, setSelection] = useState<{ text: string; rectangles: HighlightRect[] } | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [color, setColor] = useState<PdfHighlight["color"]>("amber");
  const [saving, setSaving] = useState(false);
  const [annotationError, setAnnotationError] = useState("");
  const [annotationsLoading, setAnnotationsLoading] = useState(true);
  const [textAvailable, setTextAvailable] = useState(true);
  const [showHighlights, setShowHighlights] = useState(false);
  const selectedHighlight = highlights.find(h => h.id === selectedId && h.page === page);
  const anchor = (selection || selectedHighlight)?.rectangles[0];
  useEffect(() => {
    let active = true;
    setAnnotationsLoading(true);
    api<PdfHighlight[]>(`/papers/${paperId}/annotations`).then(value => { if (active) setHighlights(value); }).catch(e => { if (active) setAnnotationError(errorText(e)); }).finally(() => { if (active) setAnnotationsLoading(false); });
    return () => { active = false; };
  }, [paperId]);
  function captureSelection() {
    const selected = window.getSelection();
    const frame = frameRef.current;
    if (!frame || !selected || selected.isCollapsed || !selected.rangeCount) { setSelection(null); return; }
    const range = selected.getRangeAt(0);
    if (!textHost.current?.contains(range.startContainer) || !textHost.current?.contains(range.endContainer)) return;
    const text = selected.toString().trim();
    if (text.length < 2) { setSelection(null); return; }
    if (text.length > 4000) { setSelection(null); setAnnotationError("Select a shorter passage (up to 4,000 characters)."); return; }
    const bounds = frame.getBoundingClientRect();
    const rectangles = Array.from(range.getClientRects()).filter(r => r.width > 1 && r.height > 1).map(r => {
      const x = Math.max(0, (r.left - bounds.left) / bounds.width);
      const y = Math.max(0, (r.top - bounds.top) / bounds.height);
      return { x, y, width: Math.max(0, Math.min(r.width / bounds.width, 1 - x)), height: Math.max(0, Math.min(r.height / bounds.height, 1 - y)) };
    }).filter(r => r.width > 0 && r.height > 0)
      .filter((r, i, all) => !all.slice(0, i).some(p => Math.abs(p.x - r.x) < 0.0001 && Math.abs(p.y - r.y) < 0.0001 && Math.abs(p.width - r.width) < 0.0001 && Math.abs(p.height - r.height) < 0.0001));
    if (!rectangles.length || rectangles.length > 120) return;
    setSelectedId(null); setSelection({ text, rectangles }); setAnnotationError("");
  }
  useEffect(() => {
    let timer: number;
    const changed = () => { window.clearTimeout(timer); timer = window.setTimeout(captureSelection, 120); };
    document.addEventListener("selectionchange", changed);
    return () => { window.clearTimeout(timer); document.removeEventListener("selectionchange", changed); };
  }, []);
  async function saveSelection(explain = false) {
    if (!selection || saving) return;
    setSaving(true); setAnnotationError("");
    try {
      const highlight = await post<PdfHighlight>(`/papers/${paperId}/annotations`, { page, ...selection, color });
      setHighlights(old => [...old, highlight]); setSelectedId(highlight.id); setSelection(null);
      window.getSelection()?.removeAllRanges();
      if (explain) { setExpanded(false); await onExplain(highlight); }
    } catch (e) { setAnnotationError(errorText(e)); } finally { setSaving(false); }
  }
  async function removeHighlight(highlight: PdfHighlight) {
    setSaving(true); setAnnotationError("");
    try { await api(`/papers/${paperId}/annotations/${highlight.id}`, { method: "DELETE" }); setHighlights(old => old.filter(h => h.id !== highlight.id)); setSelectedId(null); }
    catch (e) { setAnnotationError(errorText(e)); } finally { setSaving(false); }
  }
  const url = `/api/papers/${paperId}/pdf`;
  useEffect(() => {
    setPage(initialPage);
  }, [initialPage]);
  useEffect(() => {
    let active = true;
    setLoading(true);
    setPdf(null);
    setError("");
    const task = getDocument({ url, withCredentials: true });
    task.promise
      .then((document) => {
        if (!active) return;
        setPdf(document);
        setPage((p) => Math.max(1, Math.min(p, document.numPages)));
        setLoading(false);
      })
      .catch(() => {
        if (active) {
          setError(
            "The paper could not be loaded. Check your connection or sign in again, then retry.",
          );
          setLoading(false);
        }
      });
    return () => {
      active = false;
      void task.destroy();
    };
  }, [url, retry]);
  useEffect(() => {
    const element = scrollRef.current;
    if (!element) return;
    const observer = new ResizeObserver((entries) =>
      setWidth(entries[0].contentRect.width),
    );
    observer.observe(element);
    return () => observer.disconnect();
  }, []);
  useEffect(() => {
    if (!pdf || !canvasHost.current) return;
    let active = true;
    let renderTask: RenderTask | undefined;
    let textLayer: TextLayer | undefined;
    setRendering(true);
    setSelection(null);
    window.getSelection()?.removeAllRanges();
    setError("");
    pdf
      .getPage(page)
      .then(async (pdfPage) => {
        if (!active || !canvasHost.current || !textHost.current) return;
        const base = pdfPage.getViewport({ scale: 1 });
        const viewport = pdfPage.getViewport({
          scale: Math.max(0.15, (width - 32) / base.width) * zoom,
        });
        const density = Math.min(window.devicePixelRatio || 1, 2);
        setDimensions({ width: viewport.width, height: viewport.height, scale: viewport.scale });
        // Separate canvases prevent resize and page changes from racing on one canvas.
        const canvas = document.createElement("canvas");
        canvas.width = Math.floor(viewport.width * density);
        canvas.height = Math.floor(viewport.height * density);
        canvas.style.width = `${viewport.width}px`;
        canvas.style.height = `${viewport.height}px`;
        canvas.setAttribute("role", "img");
        canvas.setAttribute(
          "aria-label",
          `Research paper, page ${page} of ${pdf.numPages}`,
        );
        canvasHost.current.replaceChildren(canvas);
        const container = document.createElement("div");
        container.className = "textLayer";
        textHost.current.replaceChildren(container);
        textLayer = new TextLayer({ textContentSource: pdfPage.streamTextContent(), container, viewport });
        scrollRef.current?.scrollTo({ top: 0, left: 0 });
        renderTask = pdfPage.render({
          canvas,
          canvasContext: canvas.getContext("2d")!,
          viewport,
          transform: density === 1 ? undefined : [density, 0, 0, density, 0, 0],
        });
        await Promise.all([renderTask.promise, textLayer.render()]);
        if (active) setTextAvailable(textLayer.textContentItemsStr.some(text => text.trim()));
      })
      .then(() => {
        if (active) setRendering(false);
      })
      .catch((e) => {
        if (active && e?.name !== "RenderingCancelledException") {
          setRendering(false);
          setError("This page could not be rendered. Retry loading the paper.");
        }
      });
    return () => {
      active = false;
      renderTask?.cancel();
      textLayer?.cancel();
    };
  }, [pdf, page, width, zoom]);
  useEffect(() => {
    if (!expanded) return;
    const escape = (e: KeyboardEvent) => {
      if (e.key === "Escape") setExpanded(false);
    };
    window.addEventListener("keydown", escape);
    return () => window.removeEventListener("keydown", escape);
  }, [expanded]);
  return (
    <div
      className={`pdf-reader ${expanded ? "pdf-reader-expanded" : ""}`}
      role={expanded ? "dialog" : undefined}
      aria-modal={expanded || undefined}
      aria-label="Research paper reader"
    >
      <div className="pdf-reader-toolbar">
        <div className="pdf-page-controls">
          <button
            aria-label="Previous PDF page"
            disabled={!pdf || page <= 1}
            onClick={() => setPage((p) => p - 1)}
          >
            <ChevronLeft size={16} />
          </button>
          <label>
            <span className="sr-only">PDF page number</span>
            <input
              type="number"
              min={1}
              max={pdf?.numPages || 1}
              value={page}
              disabled={!pdf}
              onChange={(e) => {
                const n = Number(e.target.value);
                if (Number.isInteger(n) && n >= 1 && n <= (pdf?.numPages || 1))
                  setPage(n);
              }}
            />
          </label>
          <span>/ {pdf?.numPages || "–"}</span>
          <button
            aria-label="Next PDF page"
            disabled={!pdf || page >= pdf.numPages}
            onClick={() => setPage((p) => p + 1)}
          >
            <ChevronRight size={16} />
          </button>
        </div>
        <div className="pdf-zoom-controls">
          <button
            aria-label="Zoom out"
            disabled={zoom <= 0.5}
            onClick={() => setZoom((z) => Math.max(0.5, z - 0.25))}
          >
            <Minus size={14} />
          </button>
          <button
            className="pdf-fit"
            onClick={() => setZoom(1)}
            title="Fit page width"
          >
            {Math.round(zoom * 100)}%
          </button>
          <button
            aria-label="Zoom in"
            disabled={zoom >= 3}
            onClick={() => setZoom((z) => Math.min(3, z + 0.25))}
          >
            <Plus size={14} />
          </button>
        </div>
        <button
          aria-label={expanded ? "Collapse PDF reader" : "Expand PDF reader"}
          onClick={() => setExpanded(!expanded)}
        >
          {expanded ? <Minimize2 size={16} /> : <Maximize2 size={16} />}
        </button>
      </div>
      <div className="pdf-annotation-tools"><span><Highlighter size={13} /> {textAvailable ? "Select text to highlight or explain" : "This page has no selectable text"}</span><div className="highlight-colors">{(["amber", "green", "blue", "pink"] as const).map(value => <button key={value} className={`highlight-color ${value}`} aria-label={`${value} highlight color`} aria-pressed={color === value} onClick={() => setColor(value)} />)}</div></div>
      {annotationError && <div className="error-banner annotation-error" role="alert">{annotationError}</div>}
      <div ref={scrollRef} className="pdf-reader-scroll">
        {(loading || rendering) && (
          <div className="pdf-loading" role="status">
            <LoaderCircle size={19} className="spin" />
            <span>
              {loading ? "Loading paper…" : `Rendering page ${page}…`}
            </span>
          </div>
        )}
        {error && (
          <div className="pdf-reader-error" role="alert">
            <p>{error}</p>
            <button
              className="button secondary"
              onClick={() => setRetry((r) => r + 1)}
            >
              <RefreshCw size={14} />
              Retry loading
            </button>
          </div>
        )}
        <div className="pdf-canvas-host" hidden={loading || !!error}>
          <div ref={frameRef} className="pdf-page-frame" style={{ width: dimensions.width, height: dimensions.height, "--total-scale-factor": dimensions.scale } as React.CSSProperties}>
            <div ref={canvasHost} />
            <div ref={textHost} className="pdf-text-host" onPointerUp={() => window.setTimeout(captureSelection, 0)} onKeyUp={captureSelection} />
            <div className="pdf-highlight-layer">{highlights.filter(h => h.page === page).flatMap(h => h.rectangles.map((r, i) => <button key={`${h.id}-${i}`} tabIndex={i === 0 ? 0 : -1} className={`pdf-highlight ${h.color} ${selectedId === h.id ? "active" : ""}`} style={{ left: `${r.x * 100}%`, top: `${r.y * 100}%`, width: `${r.width * 100}%`, height: `${r.height * 100}%` }} aria-label={`Highlight: ${h.text.slice(0, 80)}`} title={h.text} onClick={() => { setSelection(null); setSelectedId(selectedId === h.id ? null : h.id); window.getSelection()?.removeAllRanges(); }} />))}</div>
            {anchor && !rendering && <div className="selection-actions" role="toolbar" aria-label="Highlight actions" style={{ left: Math.max(4, Math.min(dimensions.width - 228, anchor.x * dimensions.width)), top: Math.max(4, Math.min(dimensions.height - 54, (anchor.y + anchor.height) * dimensions.height + 6)) }} onPointerDown={e => e.preventDefault()}>
              {selection ? <><button disabled={saving || annotationsLoading} title="Save highlight" aria-label="Save highlight" onClick={() => saveSelection()}><Highlighter size={15} /></button><button disabled={saving || explainBusy || annotationsLoading} onClick={() => saveSelection(true)}><Sparkles size={15} /> Explain this</button></> : selectedHighlight && <><button disabled={saving || explainBusy} onClick={() => { setExpanded(false); void onExplain(selectedHighlight); }}><Sparkles size={15} /> Explain this</button><button aria-label="Delete highlight" disabled={saving} onClick={() => removeHighlight(selectedHighlight)}><Trash2 size={14} /></button></>}
              <button aria-label="Dismiss highlight actions" onClick={() => { setSelectedId(null); setSelection(null); window.getSelection()?.removeAllRanges(); }}><X size={13} /></button>
            </div>}
          </div>
        </div>
      </div>
      <div className="pdf-reader-footer">
        <button className="text-button" onClick={() => setShowHighlights(!showHighlights)} aria-expanded={showHighlights}><Highlighter size={13} /> {highlights.length} highlights</button>
        <a href={url} download="research-paper.pdf">
          <Download size={13} />
          Download PDF
        </a>
        <a
          href={`${url}#page=${page}`}
          target="_blank"
          rel="noopener noreferrer"
        >
          Open original
          <ArrowUpRight size={13} />
        </a>
        <span className="sr-only" role="status">
          {!loading && !rendering && !error ? `Page ${page} rendered` : ""}
        </span>
      </div>
      {showHighlights && <div className="saved-highlights"><span className="tag-meta">Your private highlights</span>{highlights.length ? highlights.map(h => <div className="saved-highlight" key={h.id}><button onClick={() => { setPage(h.page); setSelection(null); setSelectedId(h.id); }}><span className={`highlight-color ${h.color}`} /><span><small>Page {h.page}</small>{h.text}</span></button><button aria-label={`Explain highlight on page ${h.page}`} disabled={saving || explainBusy} onClick={() => { setExpanded(false); void onExplain(h); }}><Sparkles size={15} /></button><button aria-label={`Delete highlight on page ${h.page}`} disabled={saving} onClick={() => removeHighlight(h)}><Trash2 size={14} /></button></div>) : <p>Select text on any page to save your first highlight.</p>}</div>}
    </div>
  );
}
