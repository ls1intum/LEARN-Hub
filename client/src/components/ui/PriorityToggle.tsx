import React from "react";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";

interface PriorityToggleProps {
  category: string;
  label: string;
  isPriority: boolean;
  onToggle: (category: string) => void;
  className?: string;
}

export const PriorityToggle: React.FC<PriorityToggleProps> = ({
  category,
  label,
  isPriority,
  onToggle,
  className = "",
}) => {
  return (
    <div className={`flex items-center gap-2 ${className}`}>
      <input
        type="checkbox"
        id={`priority-${category}`}
        checked={isPriority}
        onChange={() => onToggle(category)}
        className="rounded"
      />
      <Label
        htmlFor={`priority-${category}`}
        className="text-sm font-medium cursor-pointer flex items-center gap-2"
      >
        {label}
        {isPriority && (
          <Badge variant="default" className="text-xs">
            Priority
          </Badge>
        )}
      </Label>
    </div>
  );
};
