export const APP_SCROLL_CONTAINER_ID = "app-scroll-container";

export function getAppScrollContainer(): HTMLElement | null {
  return document.getElementById(APP_SCROLL_CONTAINER_ID);
}

// The layout currently doesn't constrain the <main> scroll container to the
// viewport, so depending on the page the actual scrolling element is either
// the main element itself or the window/document. Always check at call time
// which one is actually overflowing rather than assuming the container.
function isElementScrollable(el: HTMLElement | null): el is HTMLElement {
  return !!el && el.scrollHeight - el.clientHeight > 1;
}

interface ScrollTarget {
  scrollTop: number;
  maxScroll: number;
  scrollTo: (top: number) => void;
}

export function getAppScrollTarget(): ScrollTarget {
  const container = getAppScrollContainer();
  if (isElementScrollable(container)) {
    return {
      scrollTop: container.scrollTop,
      maxScroll: container.scrollHeight - container.clientHeight,
      scrollTo: (top) => container.scrollTo({ top, behavior: "auto" }),
    };
  }

  const docEl = document.scrollingElement ?? document.documentElement;
  return {
    scrollTop: window.scrollY,
    maxScroll: docEl.scrollHeight - window.innerHeight,
    scrollTo: (top) => window.scrollTo({ top, behavior: "auto" }),
  };
}

export function getAppScrollTop(): number {
  return getAppScrollTarget().scrollTop;
}

export function setAppScrollTop(top: number) {
  getAppScrollTarget().scrollTo(top);
}
