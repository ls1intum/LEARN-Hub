import React from "react";
import { Skeleton } from "./skeleton";
import { Loader2 } from "lucide-react";

interface LoadingStateProps {
  isLoading: boolean;
  children: React.ReactNode;
  fallback?: React.ReactNode;
  className?: string;
}

export const LoadingState: React.FC<LoadingStateProps> = ({
  isLoading,
  children,
  fallback,
  className = "",
}) => {
  if (isLoading) {
    return (
      fallback || (
        <div className={`flex items-center justify-center p-8 ${className}`}>
          <div className="flex items-center gap-2">
            <Loader2 className="h-4 w-4 animate-spin" />
            <span className="text-sm text-muted-foreground">Loading...</span>
          </div>
        </div>
      )
    );
  }

  return <>{children}</>;
};

interface SkeletonGridProps {
  count?: number;
  className?: string;
}

export const SkeletonGrid: React.FC<SkeletonGridProps> = ({
  count = 6,
  className = "",
}) => (
  <div
    className={`grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 ${className}`}
  >
    {Array.from({ length: count }).map((_, i) => (
      <div key={i} className="space-y-3">
        <Skeleton className="h-4 w-3/4" />
        <Skeleton className="h-4 w-1/2" />
        <Skeleton className="h-20 w-full" />
      </div>
    ))}
  </div>
);
