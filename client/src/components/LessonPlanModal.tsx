import React, { useState } from "react";
import { Button } from "@/components/ui/button";
import { apiService } from "@/services/apiService";
import type { Activity } from "@/types/activity";
import { logger } from "@/services/logger";
import { Download, Heart } from "lucide-react";
import { TimelineItem } from "@/components/ui/TimelineItem";
import { BreakCard } from "@/components/ui/BreakCard";
import { TimelineContainer } from "@/components/ui/TimelineContainer";

interface LessonPlanData {
  activities: Activity[];
  total_duration_minutes: number;
  breaks?: Array<{
    description: string;
    duration: number;
    reasons: string[];
  }>;
  ordering_strategy?: string;
  created_at?: string;
  title?: string;
  search_criteria?: Record<string, unknown>;
}

interface LessonPlanModalProps {
  lessonPlanData: LessonPlanData;
  onClose: () => void;
  onSave?: () => void;
  isFromFavorites?: boolean;
}

export const LessonPlanModal: React.FC<LessonPlanModalProps> = ({
  lessonPlanData,
  onClose,
  onSave,
  isFromFavorites = false,
}) => {
  const [isSaving, setIsSaving] = useState(false);
  const [isDownloading, setIsDownloading] = useState(false);

  const handleDownload = async () => {
    setIsDownloading(true);
    try {
      // Extract breaks from activities' break_after field for PDF generation
      const inlineBreaks = lessonPlanData.activities
        .filter((activity) => activity.break_after)
        .map((activity, index) => ({
          position: (index + 1) / lessonPlanData.activities.length, // Normalize position
          duration: activity.break_after?.duration || 0,
          description: activity.break_after?.description || "Take a break",
          reasons: activity.break_after?.reasons || [],
        }));

      // Use inline breaks if available, otherwise fall back to lessonPlanData.breaks
      const breaksForPdf =
        inlineBreaks.length > 0
          ? inlineBreaks
          : (lessonPlanData.breaks || []).map((breakItem, index) => ({
              ...breakItem,
              position: index + 1,
            }));

      // Generate lesson plan PDF
      const blob = await apiService.generateLessonPlan({
        activities: lessonPlanData.activities,
        search_criteria: (lessonPlanData.search_criteria || {}) as Record<
          string,
          string | number | boolean | string[]
        >,
        total_duration: lessonPlanData.total_duration_minutes,
        breaks: breaksForPdf,
      });

      // Download the PDF
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = `${lessonPlanData.title || "lesson_plan"}.pdf`;

      try {
        document.body.appendChild(a);
        a.click();
      } finally {
        window.URL.revokeObjectURL(url);
        if (document.body.contains(a)) {
          document.body.removeChild(a);
        }
      }
    } catch (error) {
      logger.error("Error downloading lesson plan", error, "LessonPlanModal");
      alert("Failed to download lesson plan. Please try again.");
    } finally {
      setIsDownloading(false);
    }
  };

  const handleSave = async () => {
    if (!onSave) return;

    setIsSaving(true);
    try {
      await onSave();
    } catch (error) {
      logger.error("Error saving lesson plan", error, "LessonPlanModal");
      alert("Failed to save lesson plan to favorites. Please try again.");
    } finally {
      setIsSaving(false);
    }
  };

  const formatDuration = (minutes: number) => {
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    if (hours > 0) {
      return `${hours}h ${mins}m`;
    }
    return `${mins}m`;
  };

  return (
    <div className="max-w-4xl mx-auto p-6">
      <div className="mb-6">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="text-2xl font-bold text-foreground">
              {lessonPlanData.title || "Lesson Plan"}
            </h2>
            <p className="text-muted-foreground">
              {lessonPlanData.activities.length} activities •{" "}
              {formatDuration(lessonPlanData.total_duration_minutes)} total
              duration
            </p>
          </div>
          <div className="flex gap-2">
            {!isFromFavorites && onSave && (
              <Button
                onClick={handleSave}
                disabled={isSaving}
                variant="outline"
              >
                <Heart className="h-4 w-4 mr-2" />
                {isSaving ? "Saving..." : "Save Plan"}
              </Button>
            )}
            <Button onClick={handleDownload} disabled={isDownloading}>
              <Download className="h-4 w-4 mr-2" />
              {isDownloading ? "Generating..." : "View PDF"}
            </Button>
          </div>
        </div>
      </div>

      {/* Timeline View - Activities and Breaks in Sequence */}
      <div className="space-y-4">
        <h3 className="text-xl font-semibold text-foreground">
          Lesson Timeline
        </h3>

        {/* Timeline Container */}
        <TimelineContainer>
          {lessonPlanData.activities.map((activity, index) => (
            <React.Fragment key={`timeline-${activity.id || index}`}>
              {/* Activity Item */}
              <TimelineItem activity={activity} stepNumber={index + 1} />

              {/* Break After Activity */}
              {activity.break_after && (
                <BreakCard breakData={activity.break_after} />
              )}
            </React.Fragment>
          ))}
        </TimelineContainer>
      </div>

      {/* Footer Actions */}
      <div className="mt-8 flex justify-end gap-3">
        <Button variant="outline" onClick={onClose}>
          Close
        </Button>
      </div>
    </div>
  );
};
