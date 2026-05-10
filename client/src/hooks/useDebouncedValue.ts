import { useEffect, useMemo, useState } from "react";
import debounce from "lodash/debounce";

export function useDebouncedValue<T>(value: T, delayMs: number) {
  const [debouncedValue, setDebouncedValue] = useState(value);

  const updateDebouncedValue = useMemo(
    () => debounce((nextValue: T) => setDebouncedValue(nextValue), delayMs),
    [delayMs],
  );

  useEffect(() => {
    updateDebouncedValue(value);
    return () => updateDebouncedValue.cancel();
  }, [value, updateDebouncedValue]);

  return debouncedValue;
}
