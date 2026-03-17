import { describe, it, expect, afterEach } from "vitest";
import { render, screen, cleanup } from "@testing-library/react";
import { Footer } from "../Footer";

afterEach(cleanup);

describe("Footer", () => {
  it("renders the Impressum heading", () => {
    render(<Footer />);
    expect(screen.getByText("Impressum")).toBeInTheDocument();
  });

  it("renders TUM institution details", () => {
    render(<Footer />);
    expect(screen.getByText("Technische Universität München")).toBeInTheDocument();
    expect(
      screen.getByText("TUM School of Computation, Information and Technology"),
    ).toBeInTheDocument();
    expect(screen.getByText("Department of Computer Science")).toBeInTheDocument();
    expect(screen.getByText("Prof. Dr. Stephan Krusche")).toBeInTheDocument();
  });

  it("renders address information", () => {
    render(<Footer />);
    expect(screen.getByText("Boltzmannstrasse 3")).toBeInTheDocument();
    expect(screen.getByText("D-85748 Garching b. München")).toBeInTheDocument();
  });

  it("renders regulating authority and VAT number", () => {
    render(<Footer />);
    expect(
      screen.getByText(/Bayerisches Staatsministerium für Wissenschaft, Forschung und Kunst/),
    ).toBeInTheDocument();
    expect(screen.getByText(/DE 811193231/)).toBeInTheDocument();
  });

  it("renders external links with correct hrefs", () => {
    render(<Footer />);

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
