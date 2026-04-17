import React, { createContext, useContext, useState } from "react";

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

  return (
    <SidebarContext.Provider
      value={{
        isMobileOpen,
        isCollapsed,
        hasSidebar,
        toggleMobile: () => setIsMobileOpen((v) => !v),
        closeMobile: () => setIsMobileOpen(false),
        toggleCollapsed: () => setIsCollapsed((v) => !v),
        setHasSidebar,
      }}
    >
      {children}
    </SidebarContext.Provider>
  );
};

export const useSidebar = () => useContext(SidebarContext);
