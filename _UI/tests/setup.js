import { vi } from 'vitest'
globalThis.ResizeObserver = class {
  observe() {}
  unobserve() {}
  disconnect() {}
}
globalThis.matchMedia = vi.fn(() => ({
  matches: false,
  addListener() {},
  removeListener() {},
  addEventListener() {},
  removeEventListener() {},
  dispatchEvent() {},
}))
Element.prototype.scrollIntoView = vi.fn()
window.scrollTo = vi.fn()
