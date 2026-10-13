import { Sparkles } from "lucide-react";

export function PaperArt() {
  return (
    <div className="paper-art" aria-hidden="true">
      <div className="orbit orbit-one" />
      <div className="orbit orbit-two" />
      <div className="art-page page-back" />
      <div className="art-page page-middle" />
      <div className="art-page page-front">
        <div className="art-page-top">
          <span>Research, connected.</span>
          <span>↗</span>
        </div>
        <div className="art-title">
          From a paper.
          <br />
          To a possibility.
        </div>
        <div className="art-authors">A new perspective starts here</div>
        <div className="art-divider" />
        <div className="art-columns">
          <div>
            <b />
            <i />
            <i />
            <i />
            <i />
            <i />
            <i />
          </div>
          <div>
            <b />
            <i />
            <i />
            <i />
            <i />
            <i />
            <i />
          </div>
        </div>
        <div className="art-graph">
          <span />
          <span />
          <span />
          <span />
          <span />
          <span />
          <svg viewBox="0 0 200 70">
            <path d="M0 62 C25 62 32 48 50 48 S76 50 91 33 S110 38 126 21 S153 25 167 11 S180 16 200 3" />
          </svg>
        </div>
        <div className="art-columns art-bottom">
          <div>
            <i />
            <i />
            <i />
          </div>
          <div>
            <i />
            <i />
            <i />
          </div>
        </div>
      </div>
      <div className="art-note">
        <span className="note-icon">
          <Sparkles size={17} />
        </span>
        <span>
          A little more clarity.<small>Grounded in the source.</small>
        </span>
      </div>
      <div className="art-citation">
        <span>[1]</span> Every answer has a starting point.
      </div>
    </div>
  );
}
