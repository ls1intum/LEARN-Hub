import React, { useState } from "react";
import { RecommendationRow } from "@/components/RecommendationRow";
import { LessonPlanModal } from "@/components/LessonPlanModal";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { BookOpen, AlertCircle } from "lucide-react";
import type { ResultsData, Recommendation } from "@/types/activity";
import type { LessonPlanData } from "@/types/activity";

interface ResultsDisplayProps {
  results: ResultsData;
  className?: string;
}

export const ResultsDisplay: React.FC<ResultsDisplayProps> = ({
  results,
  className = "",
}) => {
  const [isLessonPlanOpen, setIsLessonPlanOpen] = useState(false);
  const [lessonPlanData, setLessonPlanData] = useState<LessonPlanData | null>(
    null,
  );

  const handleCreateLessonPlanFromRecommendation = (
    recommendation: Recommendation,
  ) => {
    // Calculate total duration from activities
    const totalDuration = recommendation.activities.reduce(
      (total, activity) => {
        return total + (activity.duration_min_minutes || 0);
      },
      0,
    );

    const data: LessonPlanData = {
      activities: recommendation.activities,
      total_duration_minutes: totalDuration,
      breaks: [], // Breaks are now included inline with activities
      ordering_strategy: "balanced",
      title: "My Lesson Plan",
    };

    setLessonPlanData(data);
    setIsLessonPlanOpen(true);
  };

  const handleCloseLessonPlan = () => {
    setIsLessonPlanOpen(false);
    setLessonPlanData(null);
  };

  if (!results || !results.activities || results.activities.length === 0) {
    return (
      <div className="text-center py-12">
        <AlertCircle className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
        <h3 className="text-lg font-semibold text-foreground mb-2">
          No recommendations found
        </h3>
        <p className="text-muted-foreground">
          Try adjusting your search criteria to find more activities.
        </p>
      </div>
    );
  }

  return (
    <>
      <div className={`space-y-8 ${className}`}>
        {/* Header */}
        <div className="text-center space-y-2">
          <div className="flex items-center justify-center gap-3 mb-4">
            <div className="p-3 bg-primary/10 rounded-full">
              <BookOpen className="h-6 w-6 text-primary" />
            </div>
            <div className="text-left">
              <h2 className="text-2xl font-bold text-foreground">
                Recommendations
              </h2>
              <p className="text-muted-foreground">
                {results.activities.length} recommendation
                {results.activities.length !== 1 ? "s" : ""} found
              </p>
            </div>
          </div>
        </div>

        {/* Recommendations */}
        <div className="space-y-6">
          {results.activities.map((recommendation, index) => (
            <RecommendationRow
              key={`recommendation-${index}`}
              recommendation={recommendation}
              index={index}
              onSelect={() =>
                handleCreateLessonPlanFromRecommendation(recommendation)
              }
            />
          ))}
        </div>
      </div>

      {/* Lesson Plan Dialog */}
      <Dialog open={isLessonPlanOpen} onOpenChange={handleCloseLessonPlan}>
        <DialogContent className="max-w-4xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Lesson Plan</DialogTitle>
          </DialogHeader>
          {lessonPlanData && (
            <LessonPlanModal
              lessonPlanData={lessonPlanData}
              onClose={handleCloseLessonPlan}
            />
          )}
        </DialogContent>
      </Dialog>
    </>
  );
};
