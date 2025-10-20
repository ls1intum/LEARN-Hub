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
}) => {
  return (
    <div className={`space-y-3 ${className}`}>
      <Label className="text-base font-medium">
        {label}: {value}
        {unit}
      </Label>
      <input
        type="range"
        min={min}
        max={max}
        step={step}
        value={value}
        onChange={(e) => onChange(parseInt(e.target.value))}
        className="w-full"
      />
    </div>
  );
};
