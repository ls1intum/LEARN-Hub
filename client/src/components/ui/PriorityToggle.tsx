import React from "react";
import { Star } from "lucide-react";
import {
  Tooltip,
  TooltipTrigger,
  TooltipContent,
} from "@/components/ui/tooltip";

interface PriorityToggleProps {
  category: string;
  isPriority: boolean;
  onToggle: (category: string) => void;
  className?: string;
}

export const PriorityToggle: React.FC<PriorityToggleProps> = ({
  category,
  isPriority,
  onToggle,
  className = "",
}) => {
  return (
    <Tooltip>
      <TooltipTrigger asChild>
        <button
          type="button"
          onClick={() => onToggle(category)}
          className={`inline-flex items-center justify-center transition-all duration-200 hover:scale-110 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 rounded ${className}`}
          aria-label={`Toggle priority for ${category}`}
        >
          {isPriority ? (
            <Star className="h-5 w-5 fill-yellow-400 text-yellow-400" />
          ) : (
            <Star className="h-5 w-5 text-muted-foreground hover:text-yellow-400" />
          )}
        </button>
      </TooltipTrigger>
      <TooltipContent side="top" className="text-xs">
        Click to prioritize this field in recommendations
      </TooltipContent>
    </Tooltip>
  );
};
