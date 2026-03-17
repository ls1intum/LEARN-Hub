import React from "react";
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
      <div className="max-w-6xl mx-auto space-y-4">
        {/* Impressum */}
        <div className="space-y-1">
          <p className="font-semibold text-foreground">Impressum</p>
          <p>Technische Universität München</p>
          <p>TUM School of Computation, Information and Technology</p>
          <p>Department of Computer Science</p>
          <p>Prof. Dr. Stephan Krusche</p>
          <p className="pt-1">Boltzmannstrasse 3</p>
          <p>D-85748 Garching b. München</p>
          <p className="pt-1">
            <span className="font-medium">Regulating Authority:</span>{" "}
            Bayerisches Staatsministerium für Wissenschaft, Forschung und Kunst
          </p>
          <p>Ust-IdNr.: DE 811193231</p>
        </div>

        {/* External Links */}
        <div className="flex flex-wrap gap-4 pt-1">
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
      </div>
    </footer>
  );
};
