import React from "react";
import { Label } from "@/components/ui/label";

interface RangeSliderProps {
  label: string;
  value: number;
  min: number;
  max: number;
  step: number;
  onChange: (value: number) => void;
  unit?: string;
  className?: string;
  priorityToggle?: React.ReactNode;
}

export const RangeSlider: React.FC<RangeSliderProps> = ({
  label,
  value,
  min,
  max,
  step,
  onChange,
  unit = "",
  className = "",
  priorityToggle,
}) => {
  return (
    <div className={`space-y-3 ${className}`}>
      <div className="flex items-center justify-between gap-2">
        <Label className="text-base font-medium">
          {label}: {value}
          {unit}
        </Label>
        {priorityToggle && <div>{priorityToggle}</div>}
      </div>
      <input
        type="range"
        min={min}
        max={max}
        step={step}
        value={value}
        onChange={(e) => onChange(parseInt(e.target.value))}
        className="w-full h-2 bg-gradient-to-r from-muted to-muted rounded-lg appearance-none cursor-pointer accent-primary"
        style={{
          background: `linear-gradient(to right, hsl(var(--primary)) 0%, hsl(var(--primary)) ${((value - min) / (max - min)) * 100}%, hsl(var(--muted)) ${((value - min) / (max - min)) * 100}%, hsl(var(--muted)) 100%)`,
        }}
      />
    </div>
  );
};
