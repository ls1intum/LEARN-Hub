import React from "react";
import { Badge } from "@/components/ui/badge";
import { Label } from "@/components/ui/label";

interface BadgeSelectorProps {
  label: string;
  options: string[];
  selectedValues: string[];
  onToggle: (value: string) => void;
  disabled?: boolean;
  className?: string;
  priorityToggle?: React.ReactNode;
}

export const BadgeSelector: React.FC<BadgeSelectorProps> = ({
  label,
  options,
  selectedValues,
  onToggle,
  disabled = false,
  className = "",
  priorityToggle,
}) => {
  const isBadgeDisabled = (value: string) => {
    if (disabled) return true;
    // Disable if this is the last selected item
    return selectedValues.includes(value) && selectedValues.length === 1;
  };

  return (
    <div className={`space-y-3 ${className}`}>
      <div className="flex items-center justify-between gap-2">
        <Label className="text-base font-medium">{label}</Label>
        {priorityToggle && <div>{priorityToggle}</div>}
      </div>
      <div className="flex flex-wrap gap-2">
        {options.map((option) => {
          const isDisabled = isBadgeDisabled(option);
          return (
            <Badge
              key={option}
              variant={selectedValues.includes(option) ? "default" : "outline"}
              className={`px-3 py-1 transition-all duration-200 ${
                isDisabled
                  ? "cursor-not-allowed opacity-50"
                  : "cursor-pointer hover:scale-105"
              }`}
              onClick={() => !isDisabled && onToggle(option)}
            >
              {option}
            </Badge>
          );
        })}
      </div>
    </div>
  );
};
