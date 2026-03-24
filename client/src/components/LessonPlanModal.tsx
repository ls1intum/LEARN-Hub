import React, { useState } from "react";
import { Button } from "@/components/ui/button";
import { apiService } from "@/services/apiService";
import type { Activity } from "@/types/activity";
import { logger } from "@/services/logger";
import { Download, Heart } from "lucide-react";
import { useTranslation } from "react-i18next";
import { TimelineItem } from "@/components/ui/TimelineItem";
import { BreakCard } from "@/components/ui/BreakCard";
import { TimelineContainer } from "@/components/ui/TimelineContainer";

interface LessonPlanData {
  activities: Activity[];
  totalDurationMinutes: number;
  breaks?: Array<{
    description: string;
    duration: number;
    reasons: string[];
  }>;
  ordering_strategy?: string;
  createdAt?: string;
  title?: string;
  searchCriteria?: Record<string, unknown>;
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
  const { t } = useTranslation();
  const [isSaving, setIsSaving] = useState(false);
  const [isDownloading, setIsDownloading] = useState(false);

  const handleDownload = async () => {
    setIsDownloading(true);
    try {
      // Extract breaks from activities' breakAfter field for PDF generation
      const inlineBreaks = lessonPlanData.activities
        .filter((activity) => activity.breakAfter)
        .map((activity, index) => ({
          position: (index + 1) / lessonPlanData.activities.length, // Normalize position
          duration: activity.breakAfter?.duration || 0,
          description: activity.breakAfter?.description || "Take a break",
          reasons: activity.breakAfter?.reasons || [],
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
        searchCriteria: (lessonPlanData.searchCriteria || {}) as Record<
          string,
          string | number | boolean | string[]
        >,
        totalDuration: lessonPlanData.totalDurationMinutes,
        breaks: breaksForPdf,
      });

      // Open the PDF in a new browser tab via a blob URL
      const url = window.URL.createObjectURL(blob);
      window.open(url, "_blank");
      // Revoke after a short delay so the new tab has time to load the resource
      setTimeout(() => window.URL.revokeObjectURL(url), 10000);
    } catch (error) {
      logger.error("Error downloading lesson plan", error, "LessonPlanModal");
      alert(t("lessonPlan.downloadFailed"));
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
      alert(t("lessonPlan.saveFailed"));
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
              {lessonPlanData.title || t("lessonPlan.title")}
            </h2>
            <p className="text-muted-foreground">
              {t("lessonPlan.activitiesCount", {
                count: lessonPlanData.activities.length,
              })}{" "}
              • {formatDuration(lessonPlanData.totalDurationMinutes)}{" "}
              {t("lessonPlan.totalDuration")}
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
                {isSaving ? t("lessonPlan.saving") : t("lessonPlan.savePlan")}
              </Button>
            )}
            <Button onClick={handleDownload} disabled={isDownloading}>
              <Download className="h-4 w-4 mr-2" />
              {isDownloading
                ? t("lessonPlan.generating")
                : t("lessonPlan.viewPdf")}
            </Button>
          </div>
        </div>
      </div>

      {/* Timeline View - Activities and Breaks in Sequence */}
      <div className="space-y-4">
        <h3 className="text-xl font-semibold text-foreground">
          {t("lessonPlan.lessonTimeline")}
        </h3>

        {/* Timeline Container */}
        <TimelineContainer>
          {lessonPlanData.activities.map((activity, index) => (
            <React.Fragment key={`timeline-${activity.id || index}`}>
              {/* Activity Item */}
              <TimelineItem activity={activity} stepNumber={index + 1} />

              {/* Break After Activity */}
              {activity.breakAfter && (
                <BreakCard breakData={activity.breakAfter} />
              )}
            </React.Fragment>
          ))}
        </TimelineContainer>
      </div>

      {/* Footer Actions */}
      <div className="mt-8 flex justify-end gap-3">
        <Button variant="outline" onClick={onClose}>
          {t("lessonPlan.close")}
        </Button>
      </div>
    </div>
  );
};
