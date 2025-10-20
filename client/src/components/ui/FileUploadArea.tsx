import React from "react";
import { FileText } from "lucide-react";

interface FileUploadAreaProps {
  onFileSelect: (file: File) => void;
  dragActive: boolean;
  onDrag: (e: React.DragEvent) => void;
  onDrop: (e: React.DragEvent) => void;
  className?: string;
}

export const FileUploadArea: React.FC<FileUploadAreaProps> = ({
  onFileSelect,
  dragActive,
  onDrag,
  onDrop,
  className = "",
}) => {
  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      onFileSelect(file);
    }
  };

  return (
    <div className={`space-y-4 ${className}`}>
      {/* File Input */}
      <div className="space-y-2">
        <label htmlFor="pdf-file" className="text-sm font-medium">
          Select PDF File
        </label>
        <input
          id="pdf-file"
          type="file"
          accept=".pdf"
          onChange={handleFileInputChange}
          className="w-full px-3 py-2 border border-input bg-background rounded-md text-sm file:mr-4 file:py-2 file:px-4 file:rounded-md file:border-0 file:text-sm file:font-medium file:bg-primary file:text-primary-foreground hover:file:bg-primary/90 cursor-pointer"
        />
      </div>

      {/* Drag and Drop Area */}
      <div
        className={`border-2 border-dashed rounded-lg p-8 text-center transition-colors ${
          dragActive
            ? "border-primary bg-primary/5"
            : "border-border hover:border-primary/50"
        }`}
        onDragEnter={onDrag}
        onDragLeave={onDrag}
        onDragOver={onDrag}
        onDrop={onDrop}
      >
        <FileText className="h-12 w-12 mx-auto mb-4 text-muted-foreground" />
        <p className="text-lg font-medium mb-2">Drag and drop your PDF here</p>
        <p className="text-sm text-muted-foreground mb-4">
          or click the file input above
        </p>
        <p className="text-xs text-muted-foreground">Maximum file size: 10MB</p>
      </div>
    </div>
  );
};
