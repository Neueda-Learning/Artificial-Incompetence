import { APP_NAME } from "./constants";

describe("constants", () => {
  describe("APP_NAME", () => {
    it("equals 'Portfolio Manager'", () => {
      expect(APP_NAME).toBe("Portfolio Manager");
    });
  });
});
