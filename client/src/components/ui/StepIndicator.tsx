import React from "react";
import { CheckCircle, ArrowLeft, ArrowRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

export interface StepDefinition {
  key: string;
  label: string;
}

interface StepIndicatorProps {
  steps: StepDefinition[];
  currentStepIndex: number;
  /** Renders an arrow-left button on the left side */
  onBack?: () => void;
  /** Renders a forward button on the right side */
  onForward?: {
    label: string;
    onClick?: () => void;
    icon?: React.ReactNode;
    disabled?: boolean;
    loading?: boolean;
    loadingLabel?: string;
    /** When set, renders the button as type="submit" targeting the given form id */
    formId?: string;
  };
  className?: string;
}

export const StepIndicator: React.FC<StepIndicatorProps> = ({
  steps,
  currentStepIndex,
  onBack,
  onForward,
  className,
}) => {
  return (
    <div className={cn("w-full", className)}>
      <div className="flex items-center gap-3">
        {/* Left nav button */}
        {onBack ? (
          <Button
            variant="outline"
            size="icon"
            onClick={onBack}
            aria-label="Previous step"
            className="h-9 w-9 flex-shrink-0"
          >
            <ArrowLeft className="h-4 w-4" />
          </Button>
        ) : (
          <div className="w-9 flex-shrink-0" />
        )}

        {/* Step circles */}
        <div className="flex-1 flex items-center justify-center gap-0">
          {steps.map((step, idx) => (
            <React.Fragment key={step.key}>
              <div className="flex items-center gap-2.5">
                {/* Step circle */}
                <div
                  className={cn(
                    "relative flex items-center justify-center w-9 h-9 rounded-full text-sm font-semibold transition-all duration-300",
                    idx < currentStepIndex &&
                      "bg-primary text-primary-foreground shadow-md shadow-primary/20",
                    idx === currentStepIndex &&
                      "bg-primary text-primary-foreground ring-[3px] ring-primary/20 shadow-md shadow-primary/20",
                    idx > currentStepIndex &&
                      "bg-muted text-muted-foreground border border-border",
                  )}
                >
                  {idx < currentStepIndex ? (
                    <CheckCircle className="h-4.5 w-4.5" />
                  ) : (
                    idx + 1
                  )}
                </div>
                {/* Step label */}
                <span
                  className={cn(
                    "text-sm font-medium transition-colors duration-300 hidden sm:inline",
                    idx === currentStepIndex
                      ? "text-foreground"
                      : idx < currentStepIndex
                        ? "text-primary"
                        : "text-muted-foreground",
                  )}
                >
                  {step.label}
                </span>
              </div>
              {/* Connector line */}
              {idx < steps.length - 1 && (
                <div
                  className={cn(
                    "w-8 sm:w-14 h-0.5 mx-2 sm:mx-3 rounded-full transition-colors duration-300",
                    idx < currentStepIndex ? "bg-primary" : "bg-border",
                  )}
                />
              )}
            </React.Fragment>
          ))}
        </div>

        {/* Right nav button */}
        {onForward ? (
          <Button
            type={onForward.formId ? "submit" : "button"}
            form={onForward.formId}
            onClick={onForward.onClick}
            disabled={onForward.disabled || onForward.loading}
            className="flex-shrink-0 gap-2"
          >
            {onForward.loading
              ? (onForward.loadingLabel || "Loading...")
              : onForward.label}
            {!onForward.loading &&
              (onForward.icon || <ArrowRight className="h-4 w-4" />)}
          </Button>
        ) : (
          <div className="w-9 flex-shrink-0" />
        )}
      </div>
    </div>
  );
};
