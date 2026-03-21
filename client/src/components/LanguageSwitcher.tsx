import React from "react";
import { Button } from "@/components/ui/button";
import { useLanguage } from "@/hooks/useLanguage";

interface LanguageSwitcherProps {
  className?: string;
}

export const LanguageSwitcher: React.FC<LanguageSwitcherProps> = ({
  className,
}) => {
  const { language, setLanguage } = useLanguage();

  return (
    <div className={`flex items-center gap-1 ${className ?? ""}`}>
      <Button
        variant={language === "de" ? "default" : "ghost"}
        size="sm"
        onClick={() => setLanguage("de")}
        className="h-7 px-2 text-xs font-medium"
        aria-label="Switch to German"
      >
        DE
      </Button>
      <Button
        variant={language === "en" ? "default" : "ghost"}
        size="sm"
        onClick={() => setLanguage("en")}
        className="h-7 px-2 text-xs font-medium"
        aria-label="Switch to English"
      >
        EN
      </Button>
    </div>
  );
};
