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
}

export const BadgeSelector: React.FC<BadgeSelectorProps> = ({
  label,
  options,
  selectedValues,
  onToggle,
  disabled = false,
  className = "",
}) => {
  const isBadgeDisabled = (value: string) => {
    if (disabled) return true;
    // Disable if this is the last selected item
    return selectedValues.includes(value) && selectedValues.length === 1;
  };

  return (
    <div className={`space-y-3 ${className}`}>
      <Label className="text-base font-medium">{label}</Label>
      <div className="flex flex-wrap gap-2">
        {options.map((option) => {
          const isDisabled = isBadgeDisabled(option);
          return (
            <Badge
              key={option}
              variant={selectedValues.includes(option) ? "default" : "outline"}
              className={`px-3 py-1 ${
                isDisabled ? "cursor-not-allowed opacity-50" : "cursor-pointer"
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
