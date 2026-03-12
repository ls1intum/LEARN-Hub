import React from "react";
import { CheckCircle } from "lucide-react";
import { cn } from "@/lib/utils";

export interface StepDefinition {
  key: string;
  label: string;
}

interface StepIndicatorProps {
  steps: StepDefinition[];
  currentStepIndex: number;
  className?: string;
}

export const StepIndicator: React.FC<StepIndicatorProps> = ({
  steps,
  currentStepIndex,
  className,
}) => {
  return (
    <div className={cn("w-full", className)}>
      <div className="flex items-center justify-center gap-0">
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
    </div>
  );
};
