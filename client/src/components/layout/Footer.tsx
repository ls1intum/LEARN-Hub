import React from "react";
import { Link } from "react-router-dom";
import { ExternalLink } from "lucide-react";
import { useTranslation } from "react-i18next";

const EXTERNAL_LINKS = [
  {
    labelKey: "footer.codeOfConduct",
    href: "https://aet.cit.tum.de/code-of-conduct",
  },
  {
    labelKey: "footer.appliedEducationTechnologies",
    href: "https://aet.cit.tum.de/",
  },
  {
    labelKey: "footer.tumCenterForEducationalTechnologies",
    href: "https://www.edtech.tum.de/",
  },
] as const;

export const Footer: React.FC = () => {
  const { t } = useTranslation();
  return (
    <footer className="mt-auto border-t border-border bg-card/50 px-4 py-3 text-xs text-muted-foreground">
      <div className="max-w-6xl mx-auto flex flex-wrap items-center justify-center gap-4">
        {/* Impressum internal link */}
        <Link
          to="/impressum"
          className="hover:text-foreground transition-colors"
        >
          {t("footer.impressum")}
        </Link>

        {/* External Links */}
        {EXTERNAL_LINKS.map(({ labelKey, href }) => (
          <a
            key={href}
            href={href}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-1 hover:text-foreground transition-colors"
          >
            {t(labelKey)}
            <ExternalLink className="h-3 w-3" />
          </a>
        ))}
      </div>
    </footer>
  );
};
