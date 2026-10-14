export function Brand({ light = false }: { light?: boolean }) {
  return (
    <div className={`brand ${light ? "brand-light" : ""}`}>
      <span className="brand-mark">
        <i />
        <i />
        <i />
      </span>
      <span>
        Primer<span className="brand-dot">.</span>
      </span>
    </div>
  );
}
