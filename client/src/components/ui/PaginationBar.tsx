import React from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

interface PaginationBarProps {
  /** 1-indexed current page */
  currentPage: number;
  totalPages: number;
  totalItems: number;
  itemsPerPage: number;
  onPageChange: (page: number) => void;
  className?: string;
}

export const PaginationBar: React.FC<PaginationBarProps> = ({
  currentPage,
  totalPages,
  totalItems,
  itemsPerPage,
  onPageChange,
  className,
}) => {
  if (totalItems <= 0) return null;

  const safeTotalPages = Math.max(totalPages, 1);
  const safeCurrentPage = Math.min(Math.max(currentPage, 1), safeTotalPages);
  const from = (safeCurrentPage - 1) * itemsPerPage + 1;
  const to = Math.min(safeCurrentPage * itemsPerPage, totalItems);

  const isPreviousDisabled = safeCurrentPage <= 1;
  const isNextDisabled = safeCurrentPage >= safeTotalPages;

  return (
    <div
      className={cn(
        "flex items-center justify-between pt-3 border-t border-border",
        className,
      )}
    >
      <p className="text-xs text-muted-foreground tabular-nums">
        {from}–{to} / {totalItems}
      </p>
      <div className="flex items-center gap-1">
        <Button
          variant="ghost"
          size="icon"
          className="h-7 w-7"
          onClick={() => onPageChange(safeCurrentPage - 1)}
          disabled={isPreviousDisabled}
        >
          <ChevronLeft className="h-3.5 w-3.5" />
        </Button>
        <span className="text-xs text-muted-foreground tabular-nums px-2 select-none">
          {safeCurrentPage} / {safeTotalPages}
        </span>
        <Button
          variant="ghost"
          size="icon"
          className="h-7 w-7"
          onClick={() => onPageChange(safeCurrentPage + 1)}
          disabled={isNextDisabled}
        >
          <ChevronRight className="h-3.5 w-3.5" />
        </Button>
      </div>
    </div>
  );
};
