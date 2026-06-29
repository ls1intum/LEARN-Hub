import { describe, it, expect, afterEach } from "vitest";
import { render, screen, cleanup } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { Footer } from "../Footer";

afterEach(cleanup);

describe("Footer", () => {
  const renderFooter = () =>
    render(
      <MemoryRouter>
        <Footer />
      </MemoryRouter>,
    );

  it("renders the Impressum link pointing to /impressum", () => {
    renderFooter();
    const link = screen.getByRole("link", { name: "Impressum" });
    expect(link).toBeInTheDocument();
    expect(link).toHaveAttribute("href", "/impressum");
  });

  it("renders external links with correct hrefs", () => {
    const { container } = renderFooter();

    // Query by href so the assertions stay locale-independent (labels are
    // translated; the i18n test setup renders German).
    const codeOfConductLink = container.querySelector(
      'a[href="https://aet.cit.tum.de/code-of-conduct"]',
    );
    expect(codeOfConductLink).toBeInTheDocument();
    expect(codeOfConductLink).toHaveAttribute("target", "_blank");
    expect(codeOfConductLink).toHaveAttribute("rel", "noopener noreferrer");

    expect(
      container.querySelector('a[href="https://aet.cit.tum.de/"]'),
    ).toBeInTheDocument();
    expect(
      container.querySelector('a[href="https://www.edtech.tum.de/"]'),
    ).toBeInTheDocument();
  });
});
