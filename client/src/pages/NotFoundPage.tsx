import React from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { FileQuestion, ArrowLeft, Home } from "lucide-react";
import { Button } from "@/components/ui/button";

export const NotFoundPage: React.FC = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();

  return (
    <div className="flex flex-col items-center justify-center py-24 text-center px-4">
      <FileQuestion className="h-16 w-16 text-muted-foreground mb-6" />
      <p className="text-7xl font-bold tracking-tight text-foreground mb-2">
        404
      </p>
      <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-foreground mb-3">
        {t("notFound.title")}
      </h1>
      <p className="text-sm sm:text-base text-muted-foreground max-w-sm mb-8">
        {t("notFound.description")}
      </p>
      <div className="flex gap-3">
        <Button variant="outline" onClick={() => navigate(-1)}>
          <ArrowLeft className="h-4 w-4 mr-2" />
          {t("notFound.goBack")}
        </Button>
        <Button onClick={() => navigate("/library")}>
          <Home className="h-4 w-4 mr-2" />
          {t("notFound.goHome")}
        </Button>
      </div>
    </div>
  );
};
