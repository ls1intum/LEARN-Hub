import { useTranslation } from "react-i18next";
import { useCallback } from "react";

export function useTranslateEnum() {
  const { t } = useTranslation();

  return useCallback(
    (category: string, value: string): string => {
      const key = `enums.${category}.${value}`;
      const translated = t(key);
      return translated === key ? value : translated;
    },
    [t],
  );
}
