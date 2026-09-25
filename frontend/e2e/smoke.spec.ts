import { expect, test, type Page } from "@playwright/test"
import commodities from "./fixtures/commodities.json"
import commodity1 from "./fixtures/commodity-1.json"
import cities from "./fixtures/cities.json"
import materials from "./fixtures/materials.json"
import chips from "./fixtures/recommendation-chips.json"

const REAL = !!process.env.E2E_REAL_API

async function mockApi(page: Page) {
  if (REAL) return
  const json = (body: unknown) => ({ status: 200, contentType: "application/json", body: JSON.stringify(body) })
  await page.route("**/api/**", async (route) => {
    const url = new URL(route.request().url())
    const p = url.pathname
    if (p === "/api/commodities") return route.fulfill(json(commodities))
    if (/^\/api\/commodities\/\d+$/.test(p)) return route.fulfill(json(commodity1))
    if (p === "/api/cities") return route.fulfill(json(cities))
    if (p === "/api/materials") return route.fulfill(json(materials))
    if (p === "/api/recommend") return route.fulfill(json(chips))
    if (/^\/api\/recommendations\/\d+$/.test(p)) return route.fulfill(json(chips))
    if (p === "/api/ai/explain") return route.fulfill(json({ text: "Use PET/AL/PE.", language: "en", aiUsed: false }))
    return route.fulfill(json({}))
  })
}

test("home → recommend Chips via wizard → results show an option card", async ({ page }) => {
  await mockApi(page)
  await page.goto("/")
  await expect(page.getByRole("heading", { level: 1 })).toContainText("Right pack")
  await page.getByRole("link", { name: /Find my packaging/ }).first().click()
  await expect(page).toHaveURL(/\/recommend/)

  await page.getByTestId("food-combobox").click()
  await page.getByTestId("food-search").fill("chips")
  await page.getByTestId("food-option-Chips").click()
  await page.getByTestId("wizard-next").click()
  await expect(page.getByTestId("pack-weight")).toBeVisible()
  await page.getByTestId("wizard-next").click()
  await page.getByTestId("wizard-submit").click()

  await expect(page).toHaveURL(/\/results\/\d+/)
  await expect(page.getByTestId("option-card").first()).toBeVisible()
  await expect(page.getByTestId("option-card").first()).toContainText("PET/AL/PE")
  await expect(page.getByTestId("decision-trace")).toContainText("oxygen barrier HIGH")
})
