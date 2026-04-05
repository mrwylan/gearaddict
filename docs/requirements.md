# Requirements — GearAddict

## Functional Requirements

| ID     | Title                        | User Story                                                                                                                                      | Priority | Status |
|--------|------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|----------|--------|
| FR-001 | Add Gear Item                | As a home studio owner, I want to add a gear item to my inventory so that I can keep track of my equipment.                                     | High     | Open   |
| FR-002 | Edit Gear Item               | As a home studio owner, I want to edit the details of a gear item so that I can keep my inventory accurate and up to date.                      | High     | Open   |
| FR-003 | Remove Gear Item             | As a home studio owner, I want to remove a gear item from my inventory so that I can reflect equipment I no longer own.                         | High     | Open   |
| FR-004 | View Own Inventory           | As a home studio owner, I want to view my complete gear inventory so that I can see an overview of all my equipment at a glance.                | High     | Open   |
| FR-005 | Categorize Gear              | As a home studio owner, I want to assign a category (e.g. synth, effect, keyboard, interface) to a gear item so that I can organize my inventory by type. | Medium   | Open   |
| FR-006 | Search Equipment Catalog     | As a home studio owner, I want to search the equipment catalog when adding gear so that I can quickly find and select the correct device.        | High     | Open   |
| FR-007 | View Equipment Page          | As a visitor, I want to view a dedicated page for a piece of equipment so that I can read its specifications and aggregated community experience. | High     | Open   |
| FR-008 | Browse Equipment Catalog     | As a visitor, I want to browse the equipment catalog so that I can discover gear used by the community.                                         | Medium   | Open   |
| FR-009 | Start Discussion Thread      | As a home studio owner, I want to start a discussion thread on an equipment page so that I can share my experience or ask a question about the device. | High     | Open   |
| FR-010 | Reply to Discussion Thread   | As a home studio owner, I want to reply to an existing discussion thread so that I can engage with other users about a piece of gear.           | High     | Open   |
| FR-011 | View Discussion Threads      | As a visitor, I want to view all discussion threads for a piece of equipment so that I can read community insights and known issues.             | High     | Open   |
| FR-012 | Discover Gear Connections    | As a home studio owner, I want to see other users who own the same gear as me so that I can connect with like-minded musicians.                 | Medium   | Open   |
| FR-013 | Browse User Inventory        | As a home studio owner, I want to browse another user's public gear inventory so that I can discover what equipment they own.                   | Medium   | Open   |
| FR-014 | Register Account             | As a visitor, I want to register an account so that I can manage my gear inventory and participate in community discussions.                    | High     | Open   |
| FR-015 | Log In and Out               | As a home studio owner, I want to log in and out of my account so that my account and data remain secure.                                      | High     | Open   |
| FR-016 | Edit Profile                 | As a home studio owner, I want to update my profile information so that I can personalize my presence on the platform.                         | Low      | Open   |
| FR-017 | Collapsible Left Navigation  | As a home studio owner, I want a vertical navigation bar on the left side of the screen that I can collapse to icons or expand to show icons with labels, so that I can navigate efficiently regardless of screen space. | High     | Open   |
| FR-018 | Navigation Structure         | As a home studio owner, I want the navigation bar to show a collapse/expand toggle at the top, the main navigation items in usage order below it, and my user avatar at the bottom, so that I can reach frequently used sections quickly. | High     | Open   |
| FR-019 | Authentication Gate          | As a visitor, when I open the application without an active session, I want to see a login screen that lets me log in, register a new account, or continue as a guest, so that I can choose how to engage with the platform. | High     | Open   |
| FR-020 | Dashboard                    | As a user, I want to see a dashboard as my welcome screen after entering the application, so that I get an immediate overview of recent activity across all areas of the platform. | High     | Open   |
| FR-021 | Dashboard Summary Cards      | As a user, I want the dashboard to show summary cards for my personal inventory, the global equipment catalog, latest discussions, and suggested gear connections — each displaying the three most recent entries — so that I can spot what is new at a glance. | High     | Open   |

---

## Non-Functional Requirements

| ID      | Title                  | Requirement                                                                                           | Category        | Priority | Status |
|---------|------------------------|-------------------------------------------------------------------------------------------------------|-----------------|----------|--------|
| NFR-001 | Page Load Time         | All page loads must complete within 2 seconds under normal load (up to 100 concurrent users).         | Performance     | High     | Open   |
| NFR-002 | Availability           | The system must maintain 99.5% uptime measured over any 30-day rolling window.                        | Availability    | High     | Open   |
| NFR-003 | Concurrent Users       | The system must support 500 concurrent users without response time exceeding 3 seconds.               | Scalability     | Medium   | Open   |
| NFR-004 | Transport Security     | All data in transit must be encrypted using TLS 1.3 or higher.                                        | Security        | High     | Open   |
| NFR-005 | Password Storage       | User passwords must be hashed using bcrypt with a minimum cost factor of 12.                          | Security        | High     | Open   |
| NFR-006 | Session Expiry         | Authenticated sessions must expire after 30 minutes of inactivity.                                    | Security        | High     | Open   |
| NFR-007 | Accessibility          | All UI views must conform to WCAG 2.1 Level AA.                                                       | Usability       | High     | Open   |
| NFR-008 | Test Coverage          | Automated test coverage for the service and data access layers must be at least 80%.                  | Maintainability | Medium   | Open   |

---

## Constraints

| ID    | Title                   | Constraint                                                                                   | Category   | Priority | Status |
|-------|-------------------------|----------------------------------------------------------------------------------------------|------------|----------|--------|
| C-001 | Java Runtime            | The backend must run on JDK 25 (LTS).                                                        | Technical  | High     | Open   |
| C-002 | Application Framework   | The application must use Spring Boot 4.0.5.                                                  | Technical  | High     | Open   |
| C-003 | Database                | The system must use PostgreSQL 18.3 as its database engine.                                  | Technical  | High     | Open   |
| C-004 | UI Framework            | The UI must be built with Vaadin Flow 25.1.1.                                                | Technical  | High     | Open   |
| C-005 | Data Access Layer       | Database access must use jOOQ 3.21.1. JPA and Hibernate are not permitted.                  | Technical  | High     | Open   |
| C-006 | Schema Versioning       | All database schema changes must be managed via Flyway 12.3.0 migration scripts.             | Technical  | High     | Open   |
| C-007 | Build Tool              | The project must use Apache Maven as its build tool.                                         | Technical  | High     | Open   |
| C-008 | Browser Support         | The UI must support the latest two major versions of Chrome, Firefox, and Safari.            | Technical  | High     | Open   |
| C-009 | CI/CD Pipeline          | The build and release pipeline must be implemented as GitHub Actions workflows.              | Operational | High    | Open   |
| C-010 | Deployment Artifact     | The release artifact must be an OCI-compliant container image published to a container registry. | Operational | High | Open   |
| C-011 | Open Source License     | The project must be distributed under the MIT License.                                       | Regulatory | Medium   | Open   |

---

## UI Specification

### Navigation Layout

The application shell uses a persistent vertical navigation bar on the left side of the screen.

#### Collapsed state (default)

- Width: icon-only, narrow strip
- Each navigation item displays its icon centered, no label visible
- Tooltip on hover shows the item label

#### Expanded state

- Width: wider panel showing icon + text label side by side
- Toggled by the collapse/expand control at the top of the bar

#### Structure (top to bottom)

| Position | Element | Description |
|---|---|---|
| Top | Collapse/Expand toggle | Icon button that collapses the bar to icon-only or expands it to icon + label |
| 1 | My Inventory | Navigates to the authenticated user's gear inventory (FR-004) |
| 2 | Equipment Catalog | Navigates to the browsable equipment catalog (FR-008) |
| 3 | Discussions | Navigates to the aggregated discussion thread overview (FR-011) |
| 4 | Gear Connections | Navigates to the user's gear connection overview (FR-012) |
| Bottom | User Avatar | Displays the logged-in user's avatar; opens profile/settings on click (FR-016) |

Navigation items 1–4 are ordered by expected usage frequency, most frequent at the top.

For unauthenticated visitors, items requiring authentication (My Inventory, Gear Connections) are hidden or replaced by Log In / Register actions.

---

### Screen Flow

#### Entry point — Authentication gate (FR-019)

When the application is opened and no valid session exists, the authentication gate screen is shown **before** any other view.

The screen offers three actions:

| Action | Outcome |
|---|---|
| Log In | Opens the log-in form (UC-002); on success redirects to the Dashboard |
| Register | Opens the registration form (UC-001); on success redirects to the Dashboard |
| Continue as Guest | Dismisses the gate and loads the Dashboard in visitor mode |

The authentication gate is a full-screen overlay or dedicated route — the application shell (navigation bar) is not shown until the user has made a choice.

#### Default landing — Dashboard (FR-020, FR-021)

After the authentication gate is passed (whether authenticated or as a guest), the Dashboard is the first screen shown.

The Dashboard displays four summary cards arranged in a responsive grid:

| Card | Content | Source | Auth required |
|---|---|---|---|
| My Inventory | The 3 most recently added gear items from the user's own inventory | GEAR_ITEM ordered by created_at desc | Yes — hidden for guests |
| Equipment Catalog | The 3 most recently added equipment entries in the global catalog | EQUIPMENT ordered by created_at desc | No |
| Latest Discussions | The 3 most recently active discussion threads across all equipment | DISCUSSION_THREAD ordered by last_reply_at desc | No |
| Suggested Connections | The 3 other users most recently sharing gear with the logged-in user | GEAR_ITEM joined on EQUIPMENT, ordered by created_at desc | Yes — hidden for guests |

Each card item is clickable and navigates to the relevant detail view.

For guest visitors, the My Inventory and Suggested Connections cards are replaced by a prompt to register or log in.

#### Flow diagram

```
Open application
       │
       ▼
 Session valid?
   ┌───┴───┐
  No      Yes
   │       │
   ▼       ▼
Auth gate  Dashboard
   │
   ├── Log In ──────────► UC-002 ──► Dashboard (authenticated)
   ├── Register ─────────► UC-001 ──► Dashboard (authenticated)
   └── Continue as Guest ─────────► Dashboard (visitor)
```
