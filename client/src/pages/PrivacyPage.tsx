import React, { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { marked } from "marked";
import privacyEn from "@/content/privacy.en.md?raw";
import privacyDe from "@/content/privacy.de.md?raw";

/**
 * Privacy statement for LEARN-Hub.
 *
 * The legal text lives in versioned markdown files (one per language) under
 * src/content/. This keeps the long prose out of the component and easy to
 * edit/review. The correct variant is chosen from the active locale and
 * rendered to HTML with `marked`.
 *
 * The markdown is static, first-party content authored in this repository (no
 * user input), so rendering it via dangerouslySetInnerHTML carries no XSS risk.
 */

const SOURCES: Record<"en" | "de", string> = {
  en: privacyEn,
  de: privacyDe,
};

export const PrivacyPage: React.FC = () => {
  const { t, i18n } = useTranslation();
  const lang = i18n.language.startsWith("de") ? "de" : "en";

  const html = useMemo(
    () =>
      marked.parse(SOURCES[lang], { async: false, gfm: true, breaks: true }),
    [lang],
  );

  return (
    <div className="py-6 max-w-3xl">
      <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-foreground mb-6">
        {t("privacy.title")}
      </h1>

      <div
        className="prose prose-sm dark:prose-invert max-w-none text-foreground"
        dangerouslySetInnerHTML={{ __html: html }}
      />
    </div>
  );
};
