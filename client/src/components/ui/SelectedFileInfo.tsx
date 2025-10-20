import React from "react";
import { FileText } from "lucide-react";

interface SelectedFileInfoProps {
  file: File;
  className?: string;
}

export const SelectedFileInfo: React.FC<SelectedFileInfoProps> = ({
  file,
  className = "",
}) => {
  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return "0 Bytes";
    const k = 1024;
    const sizes = ["Bytes", "KB", "MB", "GB"];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i];
  };

  return (
    <div className={`p-4 bg-muted/50 rounded-lg ${className}`}>
      <div className="flex items-center gap-2 mb-2">
        <FileText className="h-4 w-4 text-primary" />
        <span className="font-medium">{file.name}</span>
      </div>
      <p className="text-sm text-muted-foreground">
        Size: {formatFileSize(file.size)}
      </p>
    </div>
  );
};
