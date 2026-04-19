# Handoff: Onboarding & Gear Connections redesign — GearAddict

> **Read this first.** The HTML in `wireframes/` and `reference_mockups/` are **design references**, not production code. Do not port the HTML or its CSS into the running app. Your job is to recreate the structure, behavior, and information architecture of the selected variants **inside the existing Vaadin Flow (Java) codebase**, using **Lumo** tokens and stock Vaadin components wherever possible.

---

## Tech context

The production app is built with:
- **Java ** / **Spring Boot**
- **Vaadin Flow** (server-side Java UI)
- **Lumo theme** (Vaadin's default design system)

Component naming convention used throughout this document: I write the web-component name (e.g. `vaadin-button`) but you should instantiate the Java counterpart (e.g. `com.vaadin.flow.component.button.Button`) in the codebase.

---

## Fidelity

**Low-fidelity wireframes.** Layout, hierarchy, and flow are specified. **Visual styling is NOT.** Use the existing Lumo tokens (`--lumo-primary-color`, `--lumo-space-m`, `--lumo-border-radius-l`, typography scale, etc.) exactly as the existing `reference_mockups/` do. Do not import the sketchy `Kalam` font, the brick-red `#d14628`, or the wobbly borders from `wire.css` — those exist only to signal "wireframe, not final design" during review.

Translate each wireframe frame to stock Lumo components. When the wireframe shows a custom layout (e.g. the rack, the match bar, the per-gear rail), build it as a Vaadin **composite** (`Composite<VerticalLayout>` or a `LitTemplate`-backed component) using Lumo CSS custom properties for colors and spacing.

---

## Scope of this handoff

Two feature areas, selected from a broader exploration:

1. **Onboarding & guest experience** — replace the current "auth gate → empty dashboard" dead end.
2. **Profile & Gear Connections** — replace the thin "people who own the same gear" list with something actually useful.

A refreshed **Dashboard** is also in-scope because changes to #1 and #2 imply changes to the home screen.

Before implementing, **the product owner must pick one variant per section.** The wireframes show 3–4 directions each; `wireframes/Wireframes.html` is the canonical reference. See *"Variants — pick before you start"* below.

---

## Design principles (hard constraints on implementation)

These must hold regardless of which variant is chosen:

1. **The collection is the identity.** Every user-facing screen leads with a user's gear, not with their text bio or avatar. In Java terms: the hero region of `UserProfileView` must be driven by `user.getInventory()`, not `user.getBio()`.
2. **Connections = knowledge transfer.** Never display raw "N users also own this" as a terminal fact. Always one click away: what did they say, write, or share about it? Surfaces: threads authored, patches posted, replies liked.
3. **Empty state is the marketing page.** A guest visiting the home URL, or a logged-in user with zero gear, must never see a blank panel with "No items" text. Populate with public/sample content + a single clear CTA.
4. **Lumo is the skeleton, not the skin.** Stock Vaadin components first. Custom layouts only when no Lumo primitive fits. Never re-theme a Lumo component to something that no longer looks like itself.

---

## Variants — pick before you start

### Dashboard (pick one)

| # | Name | Summary | Implementation cost |
|---|---|---|---|
| D1 | Activity-first | Chronological feed of events on gear the user owns | Medium — needs an `ActivityEvent` domain model + Kafka-style feed query |
| D2 | Studio at a glance | Inventory hero + mini grid + side rail of signals | **Low** — composes existing inventory + discussions data |
| D3 | Per-gear "rooms" | Each owned gear becomes a channel with unread count | High — needs per-gear subscription + read state per user |
| D4 | Guest dashboard | Public curated content; shown to guests + zero-gear users | Low — combines catalog + public threads |

**Recommendation:** Ship **D2 (logged-in) + D4 (guest / empty)** first. D1 and D3 are good later iterations but require data model work.

### Profile + Connections (pick one)

| # | Name | Summary | Implementation cost |
|---|---|---|---|
| P1 | "The Rack" | Literal vertical rack layout of the user's gear | Medium — custom layout component |
| P2 | Knowledge-first | Lists threads/patches the user has authored, filtered to shared gear | **Low** — a reordered profile with an extra tab filter |
| P3 | Taste match | % overlap score + "you both own" / "she has, you might like" columns | Medium — needs a taste-similarity query |
| P4 | Connections discover | Grid of people with human-readable "why" reasons instead of % | Medium — needs reason-generator logic |

**Recommendation:** Ship **P2 + P4**. P2 is the profile; P4 replaces the `profile.html > Gear Connections` tab with a discover-style list.

---

## Screens to build

### 1. `AuthGateView` (updated)

**Route:** `/login` (existing)
**File today:** `reference_mockups/auth-gate.html`
**Wireframe:** `Wireframes.html` → Onboarding tab → "Lean gate" variant

**Changes from today:**
- Replace the tab-switched Login / Register with three **equally weighted** paths:
  - `Button("Continue with Google")` — `theme="primary"`, full width
  - `Button("Continue with email")` — full width
  - `Button("Just browse as guest")` — `theme="tertiary"`, full width, below a divider
- Above the buttons, show a **rotating stat line** (e.g. `"847 racks logged · 12,400 threads"`). Can be a `Span` with plain text; update via `@PostConstruct` from a `StatsService`.
- The email path expands inline (same view) into `EmailField` + `PasswordField` + primary `Button`.
- The "Just browse as guest" path routes to `/` and starts a **guest session** (anonymous `UserDetails`, `ROLE_GUEST`).

**Layout:**
- `VerticalLayout`, centered, `max-width: 420px`
- `Lumo` tokens: `--lumo-space-l` for vertical rhythm, `--lumo-border-radius-l` for the container card
- Logo area: `H1("GearAddict")` + small `Span` with stats

**Spring Security note:** guest needs to be a real authenticated principal with a `ROLE_GUEST` authority. Use `AnonymousAuthenticationFilter` with a custom key, or a `RememberMeAuthenticationProvider` that grants `ROLE_GUEST` on the guest button click.

---

### 2. `OnboardingView` — "Pick your rack" flow

**Route:** `/onboarding` (new)
**Wireframe:** `Wireframes.html` → Onboarding tab → Variant 01

**Triggered when:**
- A newly registered user hits `/` for the first time AND `user.inventory.isEmpty()`
- OR: A guest clicks "Start my rack" from the guest dashboard

**Three steps (use `Stepper` — or if not available, a custom `HorizontalLayout` of 3 numbered dots):**

**Step 1 — "What's in your room?"**
- Grid of catalog items, 3 columns on desktop, `VirtualList` or `Grid` with `ComponentRenderer`.
- Each tile: `Image` (catalog photo or placeholder) + `Span` (model name). Click toggles a `"chosen"` state.
- Live counter at the bottom: `"{n} picked · unlocks {threadCount} threads, {connectionCount} people"` — driven by a reactive query that runs on selection change. Debounce by 300ms.
- "Skip" link routes to step 3 (guest) or marks onboarding dismissed (logged-in).
- Primary `Button("Continue →")` enabled when `n >= 1`.

**Step 2 — "Pick your vibe"**
- Three to five `Card`-style tiles: Ambient analog / Hip-hop bedroom / Dawless techno / etc.
- Single-select. Sets `user.preferredGenre` (or similar) on save.

**Step 3 — "Claim your rack"** (account gate)
- Shown only if user arrived as guest. For logged-in users, go directly to `/` (dashboard).
- `EmailField` + `PasswordField` + `Button("Create account")`
- Copy: "We'll restore your picks in one click."
- On submit: promote the guest session to a full account via `UserService.promoteGuest(guestId, email, password)`. Persist the selections.

**State model:**
```java
@Component
@VaadinSessionScope
public class OnboardingState {
    private Set<Long> pickedGearIds = new LinkedHashSet<>();
    private String pickedGenre;
    // getters, setters
}
```

---

### 3. `GuestDashboardView` — D4

**Route:** `/` for guests, and for logged-in users with empty inventory
**Wireframe:** `Wireframes.html` → Dashboard tab → Variant 04

**Sections (top to bottom):**

1. **Hero band** — full-width, `--lumo-contrast-5pct` background
   - `H2`: "The studio you build is the community you find."
   - `Paragraph`: "Log the gear in your room → meet the people using it like you."
   - Two buttons: `"Start my rack"` (primary) → `/onboarding`, `"Browse as guest"` (tertiary) → `/catalog`
2. **"Most-loved right now" gear cloud**
   - 10 tiles, `Grid` with `ComponentRenderer` or a `FlexLayout`
   - Each tile shows catalog image + model name
   - Click → catalog detail
3. **"Real threads today"**
   - 3–4 thread preview items, same layout as existing `discussions.html` thread list
   - Click requires login (soft wall — see P4/onboarding)

**Data sources:**
- `CatalogService.findMostLoved(limit=10)` — order by `ownerCount DESC` or a rolling 7-day signal
- `DiscussionService.findRecentPublic(limit=4)` — newest public threads

**No empty state is allowed.** If any query returns fewer than N items, the query itself is buggy — not a UI concern.

---

### 4. `DashboardView` — D2, Studio at a glance

**Route:** `/`
**Wireframe:** `Wireframes.html` → Dashboard tab → Variant 02

**Top-level layout:**
- `SplitLayout(horizontal)` — 60/40 split, min-width 720px; stacks vertically below that

**Left 60%:**
1. **Studio hero card** — `Div` with `--lumo-contrast-5pct` background, `--lumo-border-radius-l`
   - Eyebrow: `"Jakob's studio"` (small, secondary text)
   - Headline: `"{gearCount} pieces · {synthCount} synths · {effectCount} effects"` — typeset at `--lumo-font-size-xl`
   - Stat row: three `Stat` sub-components — **new threads**, **patches shared**, **connections**. Each is `{bigNumber}` over `{label}`.
2. **Mini gear grid** — 4-column `FlexLayout`, wraps to 3/2 at narrow widths
   - Each tile: 80px thumbnail + model name. No category chip at this size.
   - Last tile is always a dashed `"+ add"` tile that routes to `/inventory?add=true`.

**Right 40% (side rail):**
- `H4("Signal")`
- Vertical list of 4–6 compact activity items (same schema as D1 feed items). Each is one row: avatar + one line.
- Footer link: `"See all activity →"` → routes to a full activity page (future work; OK to disable for now).

**Data sources:**
- `InventoryService.summaryFor(userId)` → `InventorySummary(totalCount, byCategory, topItems)`
- `SignalService.recentForUser(userId, limit=6)` → mixed stream of thread replies, new gear matches, patch shares

---

### 5. `UserProfileView` — P2, Knowledge-first

**Route:** `/u/{username}` (existing)
**Wireframe:** `Wireframes.html` → Profile tab → Variant 02

**Structure:**
- **Header band** (unchanged from today's `profile.html` visually, but simplified):
  - `Avatar` (64px) + `H2(displayName)` + `Span("@username · city")` + bio
  - Right side: `Button("Follow")` primary, `Button("Message")` tertiary
  - Below, a thin row: **"3 shared pieces of gear"** — clickable → filters the tabs below
- **Tabs** (`Tabs` with 3 `Tab`s):
  - **"From shared gear (N)"** — default. Posts by this user, filtered to gear the viewer also owns.
  - **"All posts"**
  - **"Rack"** — the user's full public inventory (the existing "My Gear" tab content)
- **Post cards** (below tabs): a list of `PostCard` composites. Each post card:
  - Left: 48px thumbnail of the gear the post is about
  - Right: thread title (`H4`), 2-line excerpt (`Paragraph` with `text-overflow: ellipsis`), meta row ("on {gear} · {likes} likes · {relTime}")
  - Click → thread detail

**Critical constraint for P2:** the *default* tab shows posts filtered to shared gear. This is how Principle #2 ("knowledge transfer") shows up. If the viewer shares zero gear with this user, default to "All posts" and hide the "From shared gear" tab.

**Data:**
- `PostService.findByAuthorFilteredToGearOwnedBy(authorId, viewerId)` — new query
- `InventoryService.sharedGearCount(viewerId, otherId)` — new query

---

### 6. `ConnectionsView` — P4, Discover

**Route:** `/connections` (new top-level) OR `/u/{username}/connections` (sub-tab of profile — your call; see IA Q1 below)
**Wireframe:** `Wireframes.html` → Profile tab → Variant 04

**Layout:**
- Header row: `H2("People worth following")` + filter `ChipGroup`: **For you** (default) / **Prolific** / **Nearby**
- 2-column grid of `PersonCard` composites (1 column on narrow)

**PersonCard:**
- Row 1: avatar + display name + small muted "{score}% match"
- Row 2: **because-line** (italic, primary text color): a human-readable reason
  - Examples: `"writes a lot about your Minimoog"`, `"Berlin local, same 3 pedals"`, `"deep on Model D patches"`, `"owns gear you've viewed 3×"`
  - Generated server-side by `MatchReasonService.describe(viewerId, otherId)`. See algorithm sketch below.
- Row 3: `ChipGroup` of shared gear. Shared chips rendered with `theme="primary"`, non-shared (wishlist hints) with `theme="contrast"`.

**`MatchReasonService` algorithm (sketch):**
```java
public String describe(long viewerId, long otherId) {
    var shared = gearRepo.sharedBetween(viewerId, otherId); // list of gear both own
    var otherPosts = postRepo.countByAuthor(otherId);
    var otherCity = userRepo.cityOf(otherId);
    var viewerCity = userRepo.cityOf(viewerId);

    // rank in priority order, return first that applies:
    if (shared.size() >= 1) {
        var top = shared.stream().max(comparing(gear ->
            postRepo.countByAuthorAndGear(otherId, gear.getId()))
        ).orElseThrow();
        long n = postRepo.countByAuthorAndGear(otherId, top.getId());
        if (n >= 5) return "writes a lot about your " + top.getName();
    }
    if (viewerCity != null && viewerCity.equals(otherCity)
        && shared.size() >= 2) {
        return otherCity + " local, same " + shared.size() + " pieces";
    }
    if (otherPosts >= 20) return "prolific poster in your feed";
    // ... more rules
    return shared.size() + " shared pieces"; // fallback
}
```

Why this matters: the reason is the value prop. Without it, this view reduces to today's underwhelming "gear connections" list.

---

## Component mapping (wireframe → Vaadin)

| Wireframe element | Vaadin / Lumo implementation |
|---|---|
| Sketchy bordered frame | `Div` with `--lumo-border-radius-l` + `--lumo-box-shadow-xs` |
| Handwritten annotation | **Do not port.** Annotations are review-only. |
| `chip` | `Badge` (i.e. `Span` with `theme="badge"`) |
| `chip--accent` | `theme="badge primary"` |
| `chip--ink` | `theme="badge contrast"` |
| `btn btn--primary` | `Button` with `theme="primary"` |
| `btn btn--accent` | `Button` with `theme="primary success"` or just `primary` (accent is wireframe-only) |
| `btn btn--ghost` | `Button` with `theme="tertiary"` |
| `ph` (hatched placeholder) | `Image` with a fallback `<div>` showing initials or an `Icon` |
| `ph-round` | `Avatar` component |
| Segmented control in Tweaks | **Do not port.** Wireframe-only affordance. |
| Rack unit row | Custom `Composite<HorizontalLayout>` — see P1 |
| Feed item | Custom `Composite` — `Avatar` + vertical layout of `Span`s |
| Gear mini-tile | Custom `Composite` — 80px `Image` + `Span` |
| Person card (P4) | Custom `Composite<VerticalLayout>` |

---

## Routes summary

| Route | View | Access |
|---|---|---|
| `/` (guest or empty inventory) | `GuestDashboardView` | public |
| `/` (logged-in, has gear) | `DashboardView` | `ROLE_USER` |
| `/login` | `AuthGateView` | public |
| `/onboarding` | `OnboardingView` | `ROLE_USER` or `ROLE_GUEST` |
| `/inventory` | `InventoryView` | `ROLE_USER` (unchanged) |
| `/catalog` | `CatalogView` | public |
| `/catalog/{id}` | `CatalogDetailView` | public |
| `/discussions` | `DiscussionsView` | public read, auth-walled write |
| `/u/{username}` | `UserProfileView` | public |
| `/connections` | `ConnectionsView` | `ROLE_USER` |

Dispatch between `GuestDashboardView` and `DashboardView` happens in the `/` route — either via a `BeforeEnterListener` on a shared `HomeView` or by two views with conflicting routes resolved in a `RouteRegistry` hook.

---

## Domain additions required

These aren't in the existing mockups. Before you code the views, confirm the domain supports them.

| New concept | Why | Minimum fields |
|---|---|---|
| `ActivityEvent` | D1 feed, D2 signal rail, guest threads preview | `id`, `type` (enum), `actorId`, `subjectGearId`, `subjectThreadId`, `timestamp` |
| `Patch` | P2 knowledge view references patches | `id`, `authorId`, `gearId`, `title`, `bodyMarkdown`, `createdAt`, `likeCount` |
| `MatchReason` (transient, not stored) | P4 because-line | computed per (viewerId, otherId) pair |
| `user.preferredGenre` | Onboarding step 2 | enum or string; can be null |
| `user.city` | P4 "Berlin local" reason | string; nullable |
| Guest session | Onboarding + soft walls | Spring Security anonymous with `ROLE_GUEST` |

---

## Interactions & behavior

**Onboarding step 1 counter:**
- On every tile click, run `PreviewService.forPicks(Set<Long> gearIds)` → `{threadCount, personCount}`
- Debounce 300ms (`UI.getCurrent().access(...)` with a scheduled executor) to avoid DB thrash on rapid clicking

**Soft auth walls** (guest tries to reply, follow, add gear):
- Replace the bottom of the form/page with an inline sign-up block: `EmailField` + `Button("Claim my rack")`
- Copy: `"Takes 10 seconds. We'll drop you right back into this thread."`
- Do NOT redirect to `/login`. The inline block on the current page preserves context.
- Impl: a reusable `SoftAuthWall` composite injected by every write-action handler when `auth.hasRole("GUEST")`.

**Dashboard signal rail:**
- Poll with `@Push` (WebSocket) if available; otherwise 60s client-side poll. Each new event fades in at the top.

**Follow button (P2):**
- Optimistic toggle: immediately swap label to "Following" on click, rollback on server error via `Notification`.

---

## Design tokens (use existing Lumo, do not invent)

| Purpose | Lumo token |
|---|---|
| Primary surfaces | `--lumo-base-color` |
| Page background | `--lumo-contrast-5pct` |
| Body text | `--lumo-body-text-color` |
| Secondary text (meta, labels) | `--lumo-secondary-text-color` |
| Muted text | `--lumo-tertiary-text-color` |
| Primary brand | `--lumo-primary-color` |
| Tinted primary (hero bands, selected states) | `--lumo-primary-color-10pct` |
| Borders | `--lumo-contrast-10pct` |
| Soft borders | `--lumo-contrast-5pct` |
| Spacing | `--lumo-space-xs` / `-s` / `-m` / `-l` / `-xl` |
| Radius | `--lumo-border-radius-s` / `-m` / `-l` |
| Shadow | `--lumo-box-shadow-xs` / `-s` / `-m` |
| Font size | `--lumo-font-size-xs` through `-xxl` |

**Do not use** any value from `wire.css`. That file is intentionally decorative and disposable.

---

## Files in this bundle

```
design_handoff_onboarding_and_connections/
├── README.md                         ← this file
├── wireframes/
│   ├── Wireframes.html               ← canonical design reference (4 tabs, 12 variants)
│   └── wire.css                      ← wireframe styling only — DO NOT port
└── reference_mockups/                ← the existing Vaadin prototypes
    ├── index.html
    ├── auth-gate.html                ← baseline for AuthGateView
    ├── dashboard.html                ← baseline for DashboardView
    ├── inventory.html                ← unchanged, reference for layout patterns
    ├── catalog.html                  ← unchanged
    ├── discussions.html              ← unchanged
    └── profile.html                  ← baseline for UserProfileView
```

**Read order:**
1. This README
2. `wireframes/Wireframes.html` — open in a browser, click through all four tabs
3. `reference_mockups/*.html` — confirm existing Vaadin component patterns (layouts, use of tokens, `theme` attribute usage)

---

## Open IA questions — decide before implementing

Surfaced during the exploration. They aren't blockers but they'll shape the build.

1. **Is `/connections` its own top-level route, or a sub-tab of `/u/{me}`?** If profile-as-identity (P2) is strong enough, the dedicated page may be redundant. Recommendation: keep `/connections` as a top-level *discover* surface, and have the profile tab link to it with a pre-applied "same gear as me" filter.
2. **Inventory vs. public gear list.** Today they're two pages (`inventory.html` edits; `profile.html > My Gear` views). Unify: one `InventoryView` with a "What others see" preview toggle.
3. **Are patches a first-class object?** D1 and P2 assume yes. If no, those surfaces collapse to just threads — which is fine, but say so explicitly.
4. **Gear photo sourcing.** Catalog-level photos (curated, one per model) + user photos as secondary strikes the right balance. Dashboard D2 and guest D4 rely on at least catalog-level photos being present.

---

## Done criteria

A review passes when:
- All routes in the table above resolve and render without errors
- Guest user flow: land on `/` → see `GuestDashboardView` → click "Start my rack" → complete onboarding → land on populated `DashboardView`
- Logged-in user with gear: land on `/` → see D2 studio-at-a-glance with their real data
- Visit another user's profile → default tab shows posts about gear both users own
- `/connections` shows at least one person card with a non-trivial "because" line
- No Lumo tokens have been overridden; no handwriting fonts; no colors from `wire.css` present anywhere
- Soft auth walls replace hard redirects for guests at every write action
