import React from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Filter, Search } from "lucide-react";
import { useTranslation } from "react-i18next";

interface ListFilterToolbarProps {
  searchValue: string;
  onSearchChange: (value: string) => void;
  searchPlaceholder: string;
  showFilters: boolean;
  onToggleFilters: () => void;
  activeFilterCount: number;
  onClearFilters: () => void;
}

export const ListFilterToolbar: React.FC<ListFilterToolbarProps> = ({
  searchValue,
  onSearchChange,
  searchPlaceholder,
  showFilters,
  onToggleFilters,
  activeFilterCount,
  onClearFilters,
}) => {
  const { t } = useTranslation();

  return (
    <div className="flex items-center gap-2">
      <div className="relative flex-1 min-w-0">
        <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-muted-foreground pointer-events-none" />
        <Input
          value={searchValue}
          onChange={(e) => onSearchChange(e.target.value)}
          placeholder={searchPlaceholder}
          className="pl-8 h-8 text-sm w-full"
        />
      </div>
      <Button
        variant="outline"
        size="sm"
        className="h-8 gap-1.5 shrink-0"
        onClick={onToggleFilters}
      >
        <Filter className="h-3.5 w-3.5" />
        {showFilters
          ? t("resultsDisplay.hideFilters")
          : t("resultsDisplay.showFilters")}
        {activeFilterCount > 0 && (
          <span className="inline-flex items-center justify-center rounded-full bg-primary text-primary-foreground text-[10px] font-semibold w-4 h-4 leading-none">
            {activeFilterCount}
          </span>
        )}
      </Button>
      <Button
        variant="ghost"
        size="sm"
        className="h-8 text-muted-foreground shrink-0"
        onClick={onClearFilters}
        disabled={activeFilterCount === 0}
      >
        {t("resultsDisplay.clearFilters")}
      </Button>
    </div>
  );
};
