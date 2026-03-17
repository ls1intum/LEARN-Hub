import React from "react";
import { Link } from "react-router-dom";
import { ExternalLink } from "lucide-react";

const EXTERNAL_LINKS = [
  {
    label: "Code of Conduct",
    href: "https://aet.cit.tum.de/code-of-conduct",
  },
  {
    label: "Applied Education Technologies",
    href: "https://aet.cit.tum.de/",
  },
  {
    label: "TUM Center for Educational Technologies",
    href: "https://www.edtech.tum.de/",
  },
] as const;

export const Footer: React.FC = () => {
  return (
    <footer className="mt-auto border-t border-border bg-card/50 px-4 py-6 text-xs text-muted-foreground">
      <div className="max-w-6xl mx-auto flex flex-wrap items-center gap-4">
        {/* Impressum internal link */}
        <Link
          to="/impressum"
          className="hover:text-foreground transition-colors"
        >
          Impressum
        </Link>

        {/* External Links */}
        {EXTERNAL_LINKS.map(({ label, href }) => (
          <a
            key={href}
            href={href}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-1 hover:text-foreground transition-colors"
          >
            {label}
            <ExternalLink className="h-3 w-3" />
          </a>
        ))}
      </div>
    </footer>
  );
};
