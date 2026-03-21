import React from "react";
import { useTranslation } from "react-i18next";

export const ImpressumPage: React.FC = () => {
  const { t } = useTranslation();
  return (
    <div className="py-6 max-w-2xl">
      <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-foreground mb-6">
        {t("impressum.title")}
      </h1>

      <div className="space-y-4 text-sm text-foreground">
        <div className="space-y-1">
          <p className="font-semibold">Technische Universität München</p>
          <p>TUM School of Computation, Information and Technology</p>
          <p>Department of Computer Science</p>
          <p>Prof. Dr. Stephan Krusche</p>
        </div>

        <div className="space-y-1">
          <p>Boltzmannstrasse 3</p>
          <p>D-85748 Garching b. München</p>
        </div>

        <div className="space-y-1">
          <p>
            <span className="font-semibold">{t("impressum.regulatingAuthority")}:</span>{" "}
            Bayerisches Staatsministerium für Wissenschaft, Forschung und Kunst
          </p>
          <p>Ust-IdNr.: DE 811193231</p>
        </div>
      </div>
    </div>
  );
};
