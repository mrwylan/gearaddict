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
| C-009 | Open Source License     | The project must be distributed under the MIT License.                                       | Regulatory | Medium   | Open   |
