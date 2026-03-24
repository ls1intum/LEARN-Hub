import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  Trash2,
  Clock,
  Search,
  Play,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import { apiService } from "@/services/apiService";
import type { SearchHistoryEntry } from "@/types/activity";
import { logger } from "@/services/logger";
import { useTranslation } from "react-i18next";

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
  const [hasMore, setHasMore] = useState(true);
  const [totalCount, setTotalCount] = useState(0);

  const ITEMS_PER_PAGE = 20;

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
          setHasMore(response.searchHistory.length === ITEMS_PER_PAGE);
        }
      } catch (err) {
        logger.error("Error fetching search history", err, "SearchHistoryPage");
        setError(t("history.failedLoad"));
      } finally {
        setLoading(false);
      }
    };

    fetchSearchHistory();
  }, [currentPage]);

  const handleDeleteEntry = async (historyId: number) => {
    try {
      await apiService.deleteSearchHistoryEntry(historyId);
      setSearchHistory((prev) =>
        prev.filter((entry) => entry.id !== historyId),
      );
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
    try {
      // Navigate to recommendations page with pre-filled search criteria
      navigate("/recommendations", {
        state: {
          searchCriteria: entry.searchCriteria,
        },
      });
    } catch (err) {
      logger.error("Error re-running search", err, "SearchHistoryPage");
      setError(t("history.failedRerun"));
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const formatSearchCriteria = (criteria: Record<string, unknown>) => {
    const formatted: string[] = [];

    if (criteria.target_age) {
      formatted.push(t("history.age", { value: criteria.target_age }));
    }
    if (criteria.target_duration) {
      formatted.push(
        t("history.duration", { value: criteria.target_duration }),
      );
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

  if (loading) {
    return (
      <div className="w-full">
        <div className="flex items-center justify-center py-12">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto mb-4"></div>
            <p className="text-muted-foreground">{t("history.loading")}</p>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="w-full py-6">
        <div className="mb-8">
          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight text-foreground mb-1.5">
            {t("history.title")}
          </h1>
          <p className="text-muted-foreground text-sm sm:text-base">
            {t("history.subtitle")}
          </p>
        </div>
        <div className="text-center py-8">
          <p className="text-destructive mb-4">{error || t("common.error")}</p>
          <Button onClick={() => window.location.reload()}>
            {t("history.tryAgain")}
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="w-full py-6">
      <div className="mb-8">
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

      {searchHistory.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <Search className="h-12 w-12 text-muted-foreground mb-4" />
            <h3 className="text-lg font-semibold text-foreground mb-2">
              {t("history.empty")}
            </h3>
            <p className="text-muted-foreground text-center">
              {t("history.emptyDesc")}
            </p>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-2">
          {searchHistory.map((entry) => (
            <Card
              key={entry.id}
              className="hover:shadow-sm transition-shadow cursor-pointer group"
              onClick={() => handleRerunSearch(entry)}
            >
              <CardContent className="p-4">
                <div className="flex items-center justify-between">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-3 mb-2">
                      <h3 className="text-sm font-semibold group-hover:text-primary transition-colors truncate">
                        {t("history.search", { id: entry.id })}
                      </h3>
                      <div className="flex items-center text-xs text-muted-foreground">
                        <Clock className="h-3 w-3 mr-1" />
                        {formatDate(entry.createdAt)}
                      </div>
                    </div>
                    <div className="flex flex-wrap gap-1">
                      {formatSearchCriteria(entry.searchCriteria)
                        .slice(0, 4)
                        .map((criterion, index) => (
                          <Badge
                            key={index}
                            variant="secondary"
                            className="text-xs px-2 py-0.5"
                          >
                            {criterion}
                          </Badge>
                        ))}
                      {formatSearchCriteria(entry.searchCriteria).length >
                        4 && (
                        <Badge
                          variant="outline"
                          className="text-xs px-2 py-0.5"
                        >
                          {t("history.more", {
                            count:
                              formatSearchCriteria(entry.searchCriteria)
                                .length - 4,
                          })}
                        </Badge>
                      )}
                    </div>
                  </div>
                  <div className="flex gap-1 ml-3">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleRerunSearch(entry);
                      }}
                      className="text-primary hover:text-primary-foreground hover:bg-primary h-8 px-3"
                      aria-label="Re-run this search"
                    >
                      <Play className="h-3 w-3 mr-1" />
                      {t("history.rerun")}
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleDeleteEntry(entry.id);
                      }}
                      className="text-destructive hover:text-destructive h-8 px-2"
                      aria-label="Delete search history entry"
                    >
                      <Trash2 className="h-3 w-3" />
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Pagination */}
      {searchHistory.length > 0 && (
        <div className="flex items-center justify-between mt-6 pt-4 border-t border-border">
          <div className="text-sm text-muted-foreground">
            {t("history.showing", {
              from: currentPage * ITEMS_PER_PAGE + 1,
              to: Math.min((currentPage + 1) * ITEMS_PER_PAGE, totalCount),
              total: totalCount,
            })}
          </div>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setCurrentPage((prev) => Math.max(0, prev - 1))}
              disabled={currentPage === 0}
              className="h-8 px-3"
            >
              <ChevronLeft className="h-4 w-4 mr-1" />
              {t("history.previous")}
            </Button>
            <span className="text-sm text-muted-foreground px-2">
              {t("history.page")} {currentPage + 1}
            </span>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setCurrentPage((prev) => prev + 1)}
              disabled={!hasMore}
              className="h-8 px-3"
            >
              {t("history.next")}
              <ChevronRight className="h-4 w-4 ml-1" />
            </Button>
          </div>
        </div>
      )}
    </div>
  );
};
