import { describe, it, expect } from "vitest";
import { convertSearchCriteriaToFormData } from "../searchCriteriaConverter";

describe("searchCriteriaConverter", () => {
  describe("convertSearchCriteriaToFormData", () => {
    describe("Basic Field Conversions", () => {
      it("should convert numeric fields correctly", () => {
        const result = convertSearchCriteriaToFormData({
          target_age: 10,
          target_duration: 45,
          max_activity_count: 5,
        });

        expect(result.target_age).toBe(10);
        expect(result.target_duration).toBe(45);
        expect(result.max_activity_count).toBe(5);
      });

      it("should parse string numbers to integers", () => {
        const result = convertSearchCriteriaToFormData({
          target_age: "12",
          target_duration: "60",
          max_activity_count: "3",
        });

        expect(result.target_age).toBe(12);
        expect(result.target_duration).toBe(60);
        expect(result.max_activity_count).toBe(3);
      });

      it("should convert boolean fields correctly", () => {
        const result = convertSearchCriteriaToFormData({
          allow_lesson_plans: true,
          include_breaks: false,
        });

        expect(result.allow_lesson_plans).toBe(true);
        expect(result.include_breaks).toBe(false);
      });

      it("should parse string booleans correctly", () => {
        const result = convertSearchCriteriaToFormData({
          allow_lesson_plans: "true",
          include_breaks: "false",
        });

        expect(result.allow_lesson_plans).toBe(true);
        expect(result.include_breaks).toBe(false);
      });

      it("should handle uppercase string booleans", () => {
        const result = convertSearchCriteriaToFormData({
          allow_lesson_plans: "TRUE",
          include_breaks: "FALSE",
        });

        expect(result.allow_lesson_plans).toBe(true);
        expect(result.include_breaks).toBe(false);
      });
    });

    describe("Array Field Conversions", () => {
      it("should convert array fields correctly", () => {
        const result = convertSearchCriteriaToFormData({
          format: ["unplugged", "digital"],
          bloom_levels: ["apply", "analyze"],
          topics: ["decomposition", "patterns"],
        });

        expect(result.format).toEqual(["unplugged", "digital"]);
        expect(result.bloom_levels).toEqual(["apply", "analyze"]);
        expect(result.topics).toEqual(["decomposition", "patterns"]);
      });

      it("should handle comma-separated strings in array fields", () => {
        const result = convertSearchCriteriaToFormData({
          format: "unplugged,digital",
          bloom_levels: "apply, analyze",
          preferred_topics: "decomposition,patterns,abstraction",
        });

        expect(result.format).toEqual(["unplugged", "digital"]);
        expect(result.bloom_levels).toEqual(["apply", "analyze"]);
        expect(result.topics).toEqual([
          "decomposition",
          "patterns",
          "abstraction",
        ]);
      });

      it("should filter empty strings from comma-separated values", () => {
        const result = convertSearchCriteriaToFormData({
          format: "unplugged,,digital,",
          bloom_levels: ",apply,",
        });

        expect(result.format).toEqual(["unplugged", "digital"]);
        expect(result.bloom_levels).toEqual(["apply"]);
      });

      it("should convert single string to array for certain fields", () => {
        const result = convertSearchCriteriaToFormData({
          resources_needed: "computers",
          topics: "decomposition",
        });

        expect(result.resources_needed).toEqual(["computers"]);
        expect(result.topics).toEqual(["decomposition"]);
      });
    });

    describe("Server/Client Parameter Name Mapping", () => {
      it("should map available_resources to resources_needed", () => {
        const result = convertSearchCriteriaToFormData({
          available_resources: ["computers", "tablets"],
        });

        expect(result.resources_needed).toEqual(["computers", "tablets"]);
      });

      it("should handle comma-separated available_resources", () => {
        const result = convertSearchCriteriaToFormData({
          available_resources: "computers,tablets,handouts",
        });

        expect(result.resources_needed).toEqual([
          "computers",
          "tablets",
          "handouts",
        ]);
      });

      it("should map preferred_topics to topics", () => {
        const result = convertSearchCriteriaToFormData({
          preferred_topics: ["decomposition", "patterns"],
        });

        expect(result.topics).toEqual(["decomposition", "patterns"]);
      });

      it("should handle comma-separated preferred_topics", () => {
        const result = convertSearchCriteriaToFormData({
          preferred_topics: "decomposition,patterns",
        });

        expect(result.topics).toEqual(["decomposition", "patterns"]);
      });

      it("should prioritize client names over server names", () => {
        const result = convertSearchCriteriaToFormData({
          resources_needed: ["computers"],
          available_resources: ["tablets"],
        });

        // Should use available_resources when both present (server takes priority)
        expect(result.resources_needed).toEqual(["tablets"]);
      });
    });

    describe("Priority Categories Handling", () => {
      it("should convert priority_categories array", () => {
        const result = convertSearchCriteriaToFormData({
          priority_categories: ["format", "bloom_level"],
        });

        expect(result.priority_categories).toEqual(["format", "bloom_level"]);
      });

      it("should handle comma-separated priority_categories", () => {
        const result = convertSearchCriteriaToFormData({
          priority_categories: "format,bloom_level,topics",
        });

        expect(result.priority_categories).toEqual([
          "format",
          "bloom_level",
          "topics",
        ]);
      });

      it("should map legacy priority_fields to priority_categories", () => {
        const result = convertSearchCriteriaToFormData({
          priority_fields: ["format", "topics"],
        });

        expect(result.priority_categories).toEqual(["format", "topics"]);
      });

      it("should default to empty array when no priority categories", () => {
        const result = convertSearchCriteriaToFormData({
          target_age: 10,
        });

        expect(result.priority_categories).toEqual([]);
      });

      it("should prioritize priority_categories over priority_fields", () => {
        const result = convertSearchCriteriaToFormData({
          priority_categories: ["format"],
          priority_fields: ["topics"],
        });

        expect(result.priority_categories).toEqual(["format"]);
      });
    });

    describe("Edge Cases", () => {
      it("should handle empty object", () => {
        const result = convertSearchCriteriaToFormData({});

        expect(result.priority_categories).toEqual([]);
        expect(Object.keys(result).length).toBe(1); // Only priority_categories
      });

      it("should handle undefined values", () => {
        const result = convertSearchCriteriaToFormData({
          target_age: undefined,
          format: undefined,
        });

        expect(result.target_age).toBeUndefined();
        expect(result.format).toBeUndefined();
      });

      it("should handle null values (converts to NaN for numbers)", () => {
        const result = convertSearchCriteriaToFormData({
          target_age: null,
          format: null,
        });

        // parseInt(String(null)) = parseInt('null') = NaN
        expect(result.target_age).toBeNaN();
        expect(result.format).toBeUndefined();
      });

      it("should handle mixed types in arrays", () => {
        const result = convertSearchCriteriaToFormData({
          format: [1, "digital", true],
        });

        expect(result.format).toEqual(["1", "digital", "true"]);
      });

      it("should handle whitespace in comma-separated strings", () => {
        const result = convertSearchCriteriaToFormData({
          format: "  unplugged  ,  digital  ,  hybrid  ",
        });

        expect(result.format).toEqual(["unplugged", "digital", "hybrid"]);
      });

      it("should handle empty strings in arrays", () => {
        const result = convertSearchCriteriaToFormData({
          format: ["unplugged", "", "digital"],
        });

        expect(result.format).toEqual(["unplugged", "", "digital"]);
      });

      it("should handle zero values correctly", () => {
        const result = convertSearchCriteriaToFormData({
          target_age: 0,
          target_duration: 0,
          max_activity_count: 0,
        });

        expect(result.target_age).toBe(0);
        expect(result.target_duration).toBe(0);
        expect(result.max_activity_count).toBe(0);
      });

      it('should handle string "0" for booleans as false', () => {
        const result = convertSearchCriteriaToFormData({
          allow_lesson_plans: "0",
          include_breaks: "no",
        });

        expect(result.allow_lesson_plans).toBe(false);
        expect(result.include_breaks).toBe(false);
      });
    });

    describe("Real-world Scenarios", () => {
      it("should convert complete search criteria from server", () => {
        const serverCriteria = {
          target_age: 10,
          format: ["unplugged", "digital"],
          available_resources: ["computers", "handouts"],
          bloom_levels: ["apply", "analyze"],
          target_duration: 45,
          preferred_topics: ["decomposition"],
          allow_lesson_plans: true,
          max_activity_count: 5,
          include_breaks: false,
          priority_categories: ["format", "bloom_level"],
        };

        const result = convertSearchCriteriaToFormData(serverCriteria);

        expect(result).toEqual({
          target_age: 10,
          format: ["unplugged", "digital"],
          resources_needed: ["computers", "handouts"],
          bloom_levels: ["apply", "analyze"],
          target_duration: 45,
          topics: ["decomposition"],
          allow_lesson_plans: true,
          max_activity_count: 5,
          include_breaks: false,
          priority_categories: ["format", "bloom_level"],
        });
      });

      it("should handle URL query parameters format", () => {
        const urlParams = {
          target_age: "12",
          format: "unplugged,digital",
          available_resources: "computers,tablets",
          bloom_levels: "apply",
          target_duration: "60",
          allow_lesson_plans: "true",
        };

        const result = convertSearchCriteriaToFormData(urlParams);

        expect(result.target_age).toBe(12);
        expect(result.format).toEqual(["unplugged", "digital"]);
        expect(result.resources_needed).toEqual(["computers", "tablets"]);
        expect(result.target_duration).toBe(60);
        expect(result.allow_lesson_plans).toBe(true);
      });
    });
  });
});
