import React from "react";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { X, Plus } from "lucide-react";

interface TagListProps {
  items: string[];
  onRemove: (index: number) => void;
  placeholder: string;
  value: string;
  onChange: (value: string) => void;
  onAdd: () => void;
  className?: string;
}

export const TagList: React.FC<TagListProps> = ({
  items,
  onRemove,
  placeholder,
  value,
  onChange,
  onAdd,
  className = "",
}) => {
  return (
    <div className={className}>
      <div className="flex gap-2 mb-2">
        <Input
          value={value}
          onChange={(e) => onChange(e.target.value)}
          placeholder={placeholder}
          onKeyDown={(e) => e.key === "Enter" && (e.preventDefault(), onAdd())}
        />
        <Button type="button" onClick={onAdd} size="sm">
          <Plus className="h-4 w-4" />
        </Button>
      </div>
      <div className="flex flex-wrap gap-2">
        {items.map((item, index) => (
          <Badge
            key={index}
            variant="secondary"
            className="flex items-center gap-1"
          >
            {item}
            <button
              type="button"
              onClick={() => onRemove(index)}
              className="ml-1 hover:text-destructive"
            >
              <X className="h-3 w-3" />
            </button>
          </Badge>
        ))}
      </div>
    </div>
  );
};
