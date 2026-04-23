import React from "react";
import { LayoutList, LayoutGrid } from "lucide-react";
import { Button } from "@/components/ui/button";

export type ViewMode = "list" | "grid";

interface ViewToggleProps {
  value: ViewMode;
  onChange: (value: ViewMode) => void;
}

export const ViewToggle: React.FC<ViewToggleProps> = ({ value, onChange }) => {
  return (
    <div className="flex items-center rounded-md border border-border overflow-hidden shrink-0">
      <Button
        variant="ghost"
        size="icon"
        className={`h-7 w-7 rounded-none border-r border-border ${value === "list" ? "bg-muted text-foreground" : "text-muted-foreground"}`}
        onClick={() => onChange("list")}
        aria-label="List view"
      >
        <LayoutList className="h-3.5 w-3.5" />
      </Button>
      <Button
        variant="ghost"
        size="icon"
        className={`h-7 w-7 rounded-none ${value === "grid" ? "bg-muted text-foreground" : "text-muted-foreground"}`}
        onClick={() => onChange("grid")}
        aria-label="Grid view"
      >
        <LayoutGrid className="h-3.5 w-3.5" />
      </Button>
    </div>
  );
};
