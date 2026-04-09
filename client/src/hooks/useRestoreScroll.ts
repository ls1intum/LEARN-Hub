import { useEffect, useRef } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { getAppScrollTarget } from "@/utils/scroll";

interface RestoreScrollLocationState {
  restoreScrollY?: number;
}

// How many animation frames to keep trying before giving up. The content
// behind the scroll container may render asynchronously (data fetch, image
// loads, lazy components), so we have to wait until the container is actually
// tall enough to honour the requested scroll position.
const MAX_RESTORE_ATTEMPTS = 60;

export function useRestoreScroll(ready = true) {
  const location = useLocation();
  const navigate = useNavigate();
  const hasRestoredRef = useRef(false);
  const restoreScrollY = (
    location.state as RestoreScrollLocationState | null
  )?.restoreScrollY;

  useEffect(() => {
    hasRestoredRef.current = false;
  }, [location.key]);

  useEffect(() => {
    if (!ready || hasRestoredRef.current || typeof restoreScrollY !== "number") {
      return;
    }

    hasRestoredRef.current = true;

    let cancelled = false;
    let attempts = 0;

    const tryRestore = () => {
      if (cancelled) return;

      const target = getAppScrollTarget();

      if (target.maxScroll >= restoreScrollY || attempts >= MAX_RESTORE_ATTEMPTS) {
        target.scrollTo(restoreScrollY);
        navigate(
          {
            pathname: location.pathname,
            search: location.search,
            hash: location.hash,
          },
          {
            replace: true,
            state: null,
          },
        );
        return;
      }

      attempts++;
      requestAnimationFrame(tryRestore);
    };

    requestAnimationFrame(tryRestore);

    return () => {
      cancelled = true;
    };
  }, [
    location.hash,
    location.key,
    location.pathname,
    location.search,
    navigate,
    ready,
    restoreScrollY,
  ]);
}
