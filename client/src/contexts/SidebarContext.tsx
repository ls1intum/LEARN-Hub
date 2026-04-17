import React, { createContext, useCallback, useContext, useState } from "react";

interface SidebarContextValue {
  isMobileOpen: boolean;
  isCollapsed: boolean;
  /** True while a route with a sidebar (MainLayout) is mounted */
  hasSidebar: boolean;
  toggleMobile: () => void;
  closeMobile: () => void;
  toggleCollapsed: () => void;
  setHasSidebar: React.Dispatch<React.SetStateAction<boolean>>;
}

const SidebarContext = createContext<SidebarContextValue>({
  isMobileOpen: false,
  isCollapsed: false,
  hasSidebar: false,
  toggleMobile: () => {},
  closeMobile: () => {},
  toggleCollapsed: () => {},
  setHasSidebar: () => {},
});

export const SidebarProvider: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const [isMobileOpen, setIsMobileOpen] = useState(false);
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [hasSidebar, setHasSidebar] = useState(false);

  const toggleMobile = useCallback(() => setIsMobileOpen((v) => !v), []);
  const closeMobile = useCallback(() => setIsMobileOpen(false), []);
  const toggleCollapsed = useCallback(() => setIsCollapsed((v) => !v), []);

  return (
    <SidebarContext.Provider
      value={{
        isMobileOpen,
        isCollapsed,
        hasSidebar,
        toggleMobile,
        closeMobile,
        toggleCollapsed,
        setHasSidebar,
      }}
    >
      {children}
    </SidebarContext.Provider>
  );
};

export const useSidebar = () => useContext(SidebarContext);
