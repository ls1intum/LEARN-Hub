import React from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Clock, Coffee } from "lucide-react";

interface BreakData {
  description: string;
  duration: number;
  reasons: string[];
}

interface BreakCardProps {
  breakData: BreakData;
}

export const BreakCard: React.FC<BreakCardProps> = ({ breakData }) => {
  const formatDuration = (minutes: number) => {
    if (minutes < 60) {
      return `${minutes}m`;
    }
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    return mins > 0 ? `${hours}h ${mins}m` : `${hours}h`;
  };

  return (
    <Card className="relative border-dashed border-2 border-muted-foreground/30 bg-muted/20">
      <CardContent className="p-4">
        <div className="flex items-start gap-4">
          {/* Break Icon */}
          <div className="flex-shrink-0 w-8 h-8 bg-muted-foreground/20 text-muted-foreground rounded-full flex items-center justify-center">
            <Coffee className="h-4 w-4" />
          </div>

          {/* Break Content */}
          <div className="flex-1 space-y-3">
            {/* Title and Description */}
            <div>
              <h4 className="text-lg font-semibold text-muted-foreground">
                Break
              </h4>
              <p className="text-muted-foreground mt-1">
                {breakData.description}
              </p>
            </div>

            {/* Duration */}
            <div className="flex items-center gap-1 text-muted-foreground">
              <Clock className="h-4 w-4" />
              <span className="text-sm">
                {formatDuration(breakData.duration)}
              </span>
            </div>

            {/* Break Reasons */}
            {breakData.reasons && breakData.reasons.length > 0 && (
              <div className="space-y-2">
                <span className="text-sm font-medium text-muted-foreground">
                  Why this break:
                </span>
                <div className="flex flex-wrap gap-1">
                  {breakData.reasons.map((reason, index) => (
                    <Badge key={index} variant="outline" className="text-xs">
                      {reason}
                    </Badge>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>
      </CardContent>
    </Card>
  );
};
