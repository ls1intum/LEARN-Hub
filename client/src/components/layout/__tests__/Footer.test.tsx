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
    renderFooter();

    const codeOfConductLink = screen.getByRole("link", {
      name: /Code of Conduct/i,
    });
    expect(codeOfConductLink).toHaveAttribute(
      "href",
      "https://aet.cit.tum.de/code-of-conduct",
    );
    expect(codeOfConductLink).toHaveAttribute("target", "_blank");
    expect(codeOfConductLink).toHaveAttribute("rel", "noopener noreferrer");

    const aetLink = screen.getByRole("link", {
      name: /Applied Education Technologies/i,
    });
    expect(aetLink).toHaveAttribute("href", "https://aet.cit.tum.de/");

    const edtechLink = screen.getByRole("link", {
      name: /TUM Center for Educational Technologies/i,
    });
    expect(edtechLink).toHaveAttribute("href", "https://www.edtech.tum.de/");
  });
});
