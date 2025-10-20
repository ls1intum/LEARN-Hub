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

export const SearchHistoryPage: React.FC = () => {
  const navigate = useNavigate();
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
          setSearchHistory(response.search_history);
          setTotalCount(response.pagination.count);
          setHasMore(response.search_history.length === ITEMS_PER_PAGE);
        }
      } catch (err) {
        logger.error("Error fetching search history", err, "SearchHistoryPage");
        setError("Failed to load search history. Please try again.");
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
      setError("Failed to delete search history entry. Please try again.");
    }
  };

  const handleRerunSearch = (entry: SearchHistoryEntry) => {
    try {
      // Navigate to recommendations page with pre-filled search criteria
      navigate("/recommendations", {
        state: {
          searchCriteria: entry.search_criteria,
        },
      });
    } catch (err) {
      logger.error("Error re-running search", err, "SearchHistoryPage");
      setError("Failed to re-run search. Please try again.");
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
      formatted.push(`Age: ${criteria.target_age}`);
    }
    if (criteria.target_duration) {
      formatted.push(`Duration: ${criteria.target_duration}min`);
    }
    if (criteria.bloom_level && criteria.bloom_level !== "any") {
      formatted.push(`Bloom Level: ${criteria.bloom_level}`);
    }
    if (
      criteria.bloom_levels &&
      Array.isArray(criteria.bloom_levels) &&
      criteria.bloom_levels.length > 0
    ) {
      formatted.push(`Bloom Level: ${criteria.bloom_levels.join(", ")}`);
    }
    if (
      criteria.format &&
      Array.isArray(criteria.format) &&
      criteria.format.length > 0
    ) {
      formatted.push(`Format: ${criteria.format.join(", ")}`);
    }
    if (
      criteria.topics &&
      Array.isArray(criteria.topics) &&
      criteria.topics.length > 0
    ) {
      formatted.push(`Topics: ${criteria.topics.join(", ")}`);
    }
    if (
      criteria.resources_needed &&
      Array.isArray(criteria.resources_needed) &&
      criteria.resources_needed.length > 0
    ) {
      formatted.push(`Resources: ${criteria.resources_needed.join(", ")}`);
    }

    return formatted.length > 0 ? formatted : ["No specific criteria"];
  };

  if (loading) {
    return (
      <div className="w-full">
        <div className="flex items-center justify-center py-12">
          <div className="text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary mx-auto mb-4"></div>
            <p className="text-muted-foreground">Loading search history...</p>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="w-full">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-foreground mb-2">
            Search History
          </h1>
          <p className="text-muted-foreground">
            View and manage your previous activity searches
          </p>
        </div>
        <div className="text-center py-8">
          <p className="text-destructive mb-4">
            {error || "An error occurred"}
          </p>
          <Button onClick={() => window.location.reload()}>Try Again</Button>
        </div>
      </div>
    );
  }

  return (
    <div className="w-full">
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-foreground mb-2">
          Search History
        </h1>
        <p className="text-muted-foreground">
          View and manage your previous activity searches
          {totalCount > 0 && (
            <span className="ml-2 text-sm">({totalCount} total entries)</span>
          )}
        </p>
      </div>

      {searchHistory.length === 0 ? (
        <Card>
          <CardContent className="flex flex-col items-center justify-center py-12">
            <Search className="h-12 w-12 text-muted-foreground mb-4" />
            <h3 className="text-lg font-semibold text-foreground mb-2">
              No search history yet
            </h3>
            <p className="text-muted-foreground text-center">
              Your activity searches will appear here once you start using the
              recommendation system.
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
                        Search #{entry.id}
                      </h3>
                      <div className="flex items-center text-xs text-muted-foreground">
                        <Clock className="h-3 w-3 mr-1" />
                        {formatDate(entry.created_at)}
                      </div>
                    </div>
                    <div className="flex flex-wrap gap-1">
                      {formatSearchCriteria(entry.search_criteria)
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
                      {formatSearchCriteria(entry.search_criteria).length >
                        4 && (
                        <Badge
                          variant="outline"
                          className="text-xs px-2 py-0.5"
                        >
                          +
                          {formatSearchCriteria(entry.search_criteria).length -
                            4}{" "}
                          more
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
                      Re-run
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
            Showing {currentPage * ITEMS_PER_PAGE + 1} to{" "}
            {Math.min((currentPage + 1) * ITEMS_PER_PAGE, totalCount)} of{" "}
            {totalCount} entries
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
              Previous
            </Button>
            <span className="text-sm text-muted-foreground px-2">
              Page {currentPage + 1}
            </span>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setCurrentPage((prev) => prev + 1)}
              disabled={!hasMore}
              className="h-8 px-3"
            >
              Next
              <ChevronRight className="h-4 w-4 ml-1" />
            </Button>
          </div>
        </div>
      )}
    </div>
  );
};
