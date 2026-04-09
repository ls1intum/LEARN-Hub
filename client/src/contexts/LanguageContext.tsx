import React, { useState } from "react";
import i18n from "@/i18n";
import {
  LanguageContext,
  type Language,
  type LanguageContextType,
} from "@/contexts/language-context";

interface LanguageProviderProps {
  children: React.ReactNode;
}

export const LanguageProvider: React.FC<LanguageProviderProps> = ({
  children,
}) => {
  const [language, setLanguageState] = useState<Language>(() => {
    const saved = localStorage.getItem("language") as Language;
    return saved === "en" ? "en" : "de";
  });

  const setLanguage = (lang: Language) => {
    setLanguageState(lang);
    localStorage.setItem("language", lang);
    i18n.changeLanguage(lang);
  };

  const toggleLanguage = () => {
    setLanguage(language === "de" ? "en" : "de");
  };

  const value: LanguageContextType = {
    language,
    setLanguage,
    toggleLanguage,
  };

  return (
    <LanguageContext.Provider value={value}>
      {children}
    </LanguageContext.Provider>
  );
};
