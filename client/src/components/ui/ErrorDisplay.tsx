import React from "react";
import { Alert, AlertDescription } from "./alert";
import { Button } from "./button";
import { AlertCircle, RefreshCw } from "lucide-react";
import { useTranslation } from "react-i18next";

interface ErrorDisplayProps {
  error: string | null;
  onRetry?: () => void;
  className?: string;
  title?: string;
}

export const ErrorDisplay: React.FC<ErrorDisplayProps> = ({
  error,
  onRetry,
  className = "",
  title,
}) => {
  const { t } = useTranslation();

  if (!error) return null;

  return (
    <Alert variant="destructive" className={className}>
      <AlertCircle className="h-4 w-4" />
      <AlertDescription className="flex items-center justify-between">
        <div>
          <p className="font-medium">
            {title ?? t("errorDisplay.defaultTitle")}
          </p>
          <p className="text-sm">{error}</p>
        </div>
        {onRetry && (
          <Button
            variant="outline"
            size="sm"
            onClick={onRetry}
            className="ml-4"
          >
            <RefreshCw className="h-3 w-3 mr-1" />
            {t("errorDisplay.retry")}
          </Button>
        )}
      </AlertDescription>
    </Alert>
  );
};
