import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { Trash2, Clock, Search, Play } from "lucide-react";
import { apiService } from "@/services/apiService";
import type { SearchHistoryEntry } from "@/types/activity";
import { logger } from "@/services/logger";
import { useTranslation } from "react-i18next";
import { PaginationBar } from "@/components/ui/PaginationBar";

export const SearchHistoryPage: React.FC = () => {
  const navigate = useNavigate();
  const { t } = useTranslation();

  const translateEnum = (category: string, value: string): string => {
    const key = `enums.${category}.${value}`;
    const translated = t(key);
    return translated === key ? value : translated;
  };

  const [error, setError] = useState<string | null>(null);
  const [searchHistory, setSearchHistory] = useState<SearchHistoryEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalCount, setTotalCount] = useState(0);

  const ITEMS_PER_PAGE = 20;
  const totalPages = Math.ceil(totalCount / ITEMS_PER_PAGE);

  useEffect(() => {
    const fetchSearchHistory = async () => {
      try {
        setLoading(true);
        setError(null);
        const offset = currentPage * ITEMS_PER_PAGE;
        const response = await apiService.getSearchHistory(
          ITEMS_PER_PAGE,
          offset,
        );
        if (response) {
          setSearchHistory(response.searchHistory);
          setTotalCount(response.pagination.count);
        }
      } catch (err) {
        logger.error("Error fetching search history", err, "SearchHistoryPage");
        setError(t("history.failedLoad"));
      } finally {
        setLoading(false);
      }
    };

    fetchSearchHistory();
  }, [currentPage, t]);

  const handleDeleteEntry = async (historyId: number) => {
    try {
      await apiService.deleteSearchHistoryEntry(historyId);
      setSearchHistory((prev) => prev.filter((entry) => entry.id !== historyId));
      setTotalCount((prev) => prev - 1);
    } catch (err) {
      logger.error(
        "Error deleting search history entry",
        err,
        "SearchHistoryPage",
      );
      setError(t("history.failedDelete"));
    }
  };

  const handleRerunSearch = (entry: SearchHistoryEntry) => {
    navigate("/recommendations", {
      state: { searchCriteria: entry.searchCriteria },
    });
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
    });
  };

  const formatSearchCriteria = (criteria: Record<string, unknown>): string[] => {
    const formatted: string[] = [];

    if (criteria.target_age) {
      formatted.push(t("history.age", { value: criteria.target_age }));
    }
    if (criteria.target_duration) {
      formatted.push(t("history.duration", { value: criteria.target_duration }));
    }
    if (criteria.bloomLevel && criteria.bloomLevel !== "any") {
      formatted.push(
        t("history.bloomLevel", {
          value: translateEnum("bloomLevel", criteria.bloomLevel as string),
        }),
      );
    }
    if (
      criteria.bloom_levels &&
      Array.isArray(criteria.bloom_levels) &&
      criteria.bloom_levels.length > 0
    ) {
      formatted.push(
        t("history.bloomLevel", {
          value: (criteria.bloom_levels as string[])
            .map((v) => translateEnum("bloomLevel", v))
            .join(", "),
        }),
      );
    }
    if (
      criteria.format &&
      Array.isArray(criteria.format) &&
      criteria.format.length > 0
    ) {
      formatted.push(
        t("history.format", {
          value: (criteria.format as string[])
            .map((v) => translateEnum("format", v))
            .join(", "),
        }),
      );
    }
    if (
      criteria.topics &&
      Array.isArray(criteria.topics) &&
      criteria.topics.length > 0
    ) {
      formatted.push(
        t("history.topics", {
          value: (criteria.topics as string[])
            .map((v) => translateEnum("topics", v))
            .join(", "),
        }),
      );
    }
    if (
      criteria.resourcesNeeded &&
      Array.isArray(criteria.resourcesNeeded) &&
      criteria.resourcesNeeded.length > 0
    ) {
      formatted.push(
        t("history.resources", {
          value: (criteria.resourcesNeeded as string[])
            .map((v) => translateEnum("resources", v))
            .join(", "),
        }),
      );
    }

    return formatted.length > 0 ? formatted : [t("history.noSpecificCriteria")];
  };

  return (
    <div className="w-full py-6">
      <div className="mb-6">
        <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-foreground mb-1.5">
          {t("history.title")}
        </h1>
        <p className="text-muted-foreground text-sm sm:text-base">
          {t("history.subtitle")}
          {totalCount > 0 && (
            <span className="ml-2 text-sm">
              ({totalCount} {t("history.entries")})
            </span>
          )}
        </p>
      </div>

      {error && (
        <div className="text-center py-8">
          <p className="text-destructive mb-4">{error}</p>
          <Button onClick={() => window.location.reload()}>
            {t("history.tryAgain")}
          </Button>
        </div>
      )}

      {!error && (
        <div className="border border-border rounded-lg overflow-hidden">
          {/* Column header */}
          <div className="flex items-center gap-3 px-3 py-2 bg-muted/40 border-b border-border text-xs font-medium text-muted-foreground uppercase tracking-wide">
            <span className="w-[110px] shrink-0">{t("history.dateHeader")}</span>
            <span className="flex-1">{t("history.criteriaHeader")}</span>
            <span className="w-[64px] shrink-0" />
          </div>

          {loading ? (
            <div className="divide-y divide-border">
              {[...Array(6)].map((_, i) => (
                <div key={i} className="flex items-center gap-3 px-3 py-2.5">
                  <Skeleton className="h-4 w-[110px] shrink-0" />
                  <div className="flex-1 flex gap-1.5">
                    <Skeleton className="h-5 w-20" />
                    <Skeleton className="h-5 w-24" />
                    <Skeleton className="h-5 w-16" />
                  </div>
                  <Skeleton className="h-7 w-[64px] shrink-0" />
                </div>
              ))}
            </div>
          ) : searchHistory.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-12 text-center">
              <Search className="h-10 w-10 text-muted-foreground mb-3" />
              <p className="text-sm font-medium text-foreground">
                {t("history.empty")}
              </p>
              <p className="text-xs text-muted-foreground mt-1">
                {t("history.emptyDesc")}
              </p>
            </div>
          ) : (
            <div className="divide-y divide-border">
              {searchHistory.map((entry) => {
                const criteria = formatSearchCriteria(entry.searchCriteria);
                return (
                  <div
                    key={entry.id}
                    className="flex items-center gap-3 px-3 py-2.5 hover:bg-muted/30 transition-colors cursor-pointer group"
                    onClick={() => handleRerunSearch(entry)}
                  >
                    <div className="flex items-center gap-1 text-xs text-muted-foreground tabular-nums w-[110px] shrink-0">
                      <Clock className="h-3 w-3 shrink-0" />
                      {formatDate(entry.createdAt)}
                    </div>
                    <div className="flex-1 flex flex-wrap gap-1 min-w-0">
                      {criteria.slice(0, 3).map((criterion, index) => (
                        <Badge
                          key={index}
                          variant="secondary"
                          className="text-xs px-2 py-0.5 font-normal"
                        >
                          {criterion}
                        </Badge>
                      ))}
                      {criteria.length > 3 && (
                        <Badge variant="outline" className="text-xs px-2 py-0.5">
                          +{criteria.length - 3}
                        </Badge>
                      )}
                    </div>
                    <div className="flex gap-1 shrink-0 w-[64px] justify-end">
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-7 w-7 text-primary opacity-0 group-hover:opacity-100 transition-opacity"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleRerunSearch(entry);
                        }}
                        aria-label="Re-run this search"
                      >
                        <Play className="h-3.5 w-3.5" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-7 w-7 text-destructive opacity-0 group-hover:opacity-100 transition-opacity"
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDeleteEntry(entry.id);
                        }}
                        aria-label="Delete search history entry"
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                      </Button>
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {!loading && searchHistory.length > 0 && (
            <div className="px-3">
              <PaginationBar
                currentPage={currentPage + 1}
                totalPages={totalPages}
                totalItems={totalCount}
                itemsPerPage={ITEMS_PER_PAGE}
                onPageChange={(page) => setCurrentPage(page - 1)}
              />
            </div>
          )}
        </div>
      )}
    </div>
  );
};
