# agent.md — Pizza Configurator Deployable Implementation Guide

## 0. Purpose

This file is the implementation contract for building a an entterprise-grade, production-oriented, deployable version of the **Pizza Configurator** architecture.

The target is a **Spring Boot Modular Monolith**, not a microservice system.

The solution must support:

- customer pizza configuration,
- standard pizzas and customization,
- deterministic rule validation,
- dynamic pricing,
- optional free-text comments,
- AI-assisted parsing of comments (Deepseek primary, OpenAI fallback),
- kitchen review of unresolved requests, with an explicit Accept / Recommend / Reject decision and a customer Accept/Reject response to any recommendation,
- customer confirmation of the final price before order creation,
- order creation and lifecycle management,
- Kitchen Display System (KDS), available as a web view and as a native Android app,
- a native Android app for customers, alongside the customer web/PWA,
- admin-managed download links/QR codes for the native apps,
- administration of catalog, rules, prices, staff users, and app download links,
- notifications: Gmail SMTP email, and Firebase Cloud Messaging (FCM) push to the native customer Android app,
- secure role-based access, with both guest checkout and authenticated customer accounts,
- PostgreSQL persistence,
- observable and testable production deployment to a Hetzner VM via a GitHub Actions CI/CD pipeline.

The implementation must preserve the central architectural principle:

> **AI may interpret. The Rule Module decides.**

The Deepseek/OpenAI integration must never become the source of truth for validity, pricing, authorization, or order state.

---

# 1. Non-Negotiable Architecture Decisions

## 1.1 Target architecture

Use a **Spring Boot Modular Monolith** with explicit business modules.

The main backend modules are:

1. `security`
2. `configuration`
3. `catalog`
4. `rules`
5. `pricing`
6. `basket`
7. `orders`
8. `kitchen`
9. `notification`
10. `admin`
11. `aiadapter`
12. `recommendation`
13. `shared` only for truly cross-cutting technical abstractions

Do not implement these as independently deployed services in the first production version.

Internal module-to-module calls are normal Java calls through published interfaces.

Use application events where a synchronous response is not required.

Examples:

- `OrderPlaced`
- `OrderStatusChanged`
- `OrderReady`
- `ReviewRequested`
- `ReviewResolved`

Do not introduce Kafka, RabbitMQ, or another distributed broker unless a later requirement explicitly justifies it.

## 1.2 Spring Modulith

Use **Spring Modulith** to enforce modular-monolith boundaries.

The package structure must reflect the domain modules directly beneath the root package.

```text
com.example.pizzaconfigurator
├── PizzaConfiguratorApplication.java
├── security
├── configuration
├── catalog
├── rules
├── pricing
├── basket
├── orders
├── kitchen
├── notification
├── admin
├── aiadapter
├── recommendation
└── shared
```

Required Modulith checks:

- module cycles must fail tests,
- modules must not access another module's internal package,
- module integration tests should use `@ApplicationModuleTests`,
- generated module documentation may be produced during CI.

Avoid a generic layered structure such as `controller/service/repository/entity` for the entire application. Each domain module should own its controllers, application services, domain objects, persistence adapters, and DTOs.

---

# 2. Recommended Technology Baseline

## Backend

- Java 21 LTS or the organization's approved Java baseline
- Spring Boot 4.x
- Spring Modulith
- Spring MVC / REST
- Spring Data JPA
- Spring Security
- Jakarta Bean Validation
- PostgreSQL
- Flyway
- Jackson
- Maven
- Spring Boot Actuator
- Micrometer
- OpenTelemetry/OTLP where an observability backend is available
- Testcontainers
- JUnit 5
- AssertJ
- Mockito
- Spring Modulith test support
- MockMvc and/or REST Assured
- Docker

Use versions managed by the selected Spring Boot BOM whenever possible. Do not manually pin transitive Spring Framework, Jackson, Hibernate, Tomcat, or Micrometer versions unless there is a documented reason.

## Frontend

Use TypeScript for all browser frontends.

Recommended stack:

- React
- TypeScript
- Vite
- React Router
- TanStack Query
- Zod
- Material UI or another accessible component library
- Playwright for E2E tests
- Vitest + Testing Library for component tests

Deploy the customer experience as a **responsive web/PWA** usable on desktop and mobile, and additionally as a native Android app (see "Mobile" below).

Create exactly two web UI entry points, each on its own domain/link:

- **Customer web Configurator** — public, own domain (e.g. `app.<domain>`), supports both guest checkout and authenticated customer login.
- **Staff web app** — kitchen + admin combined into one app, own domain (e.g. `staff.<domain>`), one shared login screen; post-login routing shows Kitchen KDS views for `ROLE_KITCHEN` and Admin Portal views for `ROLE_ADMIN` (admins can also reach the KDS views). The KDS-vs-admin UI distinction (touch-first board vs. dense forms) is implemented as two route trees inside this one app, not two deployables.

Do not build separate `kitchen` and `admin` web apps — they are merged into the single `staff` app above.

## Mobile

Native Android apps are **in scope for v1**, for both customer and kitchen, distributed as directly downloadable APKs (not via Play Store) for now:

- **Customer Android app** — fully native (Kotlin + Jetpack Compose), hitting the same `/api/v1/**` REST endpoints as the customer web app. Coexists with the customer web app; it does not replace it.
- **Kitchen Android app** — fully native (Kotlin + Jetpack Compose), hitting the same `/api/v1/kitchen/**` REST endpoints and the SSE stream (§17) as the staff web app's KDS view. Coexists with the staff web app's KDS route tree; it does not replace it.

No native Admin app. No native iOS app for now — optional after the Android apps and web/PWA are complete.

Both native apps are additional REST/SSE clients only — they never change a backend module boundary or API contract; DTOs already used for the web frontends are reused as-is.

## Infrastructure

Local/development deployment:

- Docker Compose
- PostgreSQL container
- backend container
- frontend container(s): `customer-web`, `staff-web`
- no Keycloak container (§14.1 — app-managed JWT is the chosen auth strategy for v1, not OIDC)
- optional Prometheus + Grafana profile

Production deployment target: a **single Hetzner VM**, reached via a **GitHub Actions CI/CD pipeline**:

- containerized backend and both web frontends, deployed via `docker compose pull && docker compose up -d` over SSH from GitHub Actions (deploy-only SSH key held as a GitHub Actions secret)
- images built and pushed to GHCR, tagged with the Git SHA
- Postgres runs as a container on the same VM — **no managed database service on Hetzner** — so automated backups must be implemented explicitly (scheduled `pg_dump` uploaded off-VM, e.g. to a Hetzner Storage Box, plus a documented/tested restore procedure); see §27 and §31
- HTTPS reverse proxy via nginx (`infrastructure/nginx/`) with certbot/Let's Encrypt, one server block per frontend domain
- secret manager/environment secrets: production secrets live only in `.env` on the VM, never in git
- centralized logs
- health checks
- metrics
- automated database migrations

---

# 3. Scope

## 3.1 In scope

### Customer

- checkout as a guest, or register/log in to a customer account,
- list available pizzas,
- choose standard pizza,
- choose size,
- choose dough,
- remove allowed base ingredients,
- add allowed extras,
- set allowed quantities,
- add optional free-text comment,
- receive immediate validation,
- receive valid alternatives where possible,
- accept or reject a kitchen recommendation on an unresolved/invalid request,
- see final price,
- confirm checkout,
- create order,
- receive order display number and pickup token,
- view order status,
- receive ready/update notification,
- see a link/QR to download the native Android app (shown regardless of guest/login state).

### Kitchen Staff

- authenticate (via the shared staff web app, or the native kitchen Android app),
- access KDS,
- see manual-review queue,
- inspect unresolved configuration requests,
- accept a request as-is,
- recommend an alternative configuration to the customer,
- reject a request,
- see confirmed orders,
- move orders through allowed production states,
- mark order ready,
- see the Android app QR code on the KDS screen.

### Administrator

- authenticate,
- manage pizzas,
- manage ingredients,
- manage sizes and doughs,
- manage compatibility/removal/quantity rules,
- manage prices,
- activate/deactivate rules,
- manage kitchen/admin users,
- manage the Android app download links (customer app, kitchen app) and their QR codes,
- inspect operational audit information.

## 3.2 Out of scope for first deployable version

Unless explicitly requested later, do not implement:

- multiple branches,
- delivery logistics,
- online payment,
- loyalty program,
- advanced analytics,
- Kafka,
- Kubernetes,
- service mesh,
- independent microservices,
- machine-learning-based validity decisions,
- autonomous AI ordering decisions,
- native iOS apps,
- Play Store distribution (APK is direct-download only for now),
- a persistent staging VM (Hetzner production VM only for now; CI's own ephemeral stack covers pre-release verification).

The design must remain extensible enough for these to be added later.

---

# 4. Core Business Flow

## 4.1 Standard order without a comment

```text
Customer
  → Customer Frontend
  → load catalog + rule-aware options
  → configure pizza
  → POST validation request
  → Rule Module
  → VALID
  → Pricing Module
  → final price
  → Customer confirms
  → POST /orders
  → Order Module creates order
  → OrderPlaced event
  → Kitchen KDS receives update
  → production
  → READY
  → Notification Module
  → customer update
```

The AI Adapter must not be called.

## 4.2 Order with comment

```text
Customer enters free-text comment
  → backend receives structured configuration + comment
  → AI Adapter parses comment
  → structured modification JSON
  → Rule Module validates complete configuration
```

Example input:

```text
"No onions and double cheese."
```

Example normalized AI output:

```json
{
  "removeIngredients": ["ONION"],
  "extras": [
    {
      "ingredientCode": "CHEESE",
      "quantity": 2
    }
  ],
  "unresolvedText": []
}
```

The AI result is not considered valid merely because JSON parsing succeeded. It must pass through the Rule Module.

## 4.3 Invalid or unresolved request

If the Rule Module returns `INVALID`:

1. attempt deterministic recommendation where possible,
2. if a valid alternative is found, return it to the customer,
3. if the request cannot be resolved automatically, create a manual `ReviewRequest` (status `OPEN`),
4. send it to Kitchen KDS review queue,
5. Kitchen Staff chooses one of three actions, owned by the `recommendation` module (§7.11):
   - **Accept** — the original request is accepted as-is → revalidate/reprice the original configuration → go to step 8.
   - **Recommend** — Kitchen proposes an alternative configuration → the customer is notified and must explicitly **Accept** or **Reject** it:
     - Customer Accepts → revalidate/reprice the recommended configuration → go to step 8.
     - Customer Rejects → the session ends (no Order); the customer may revise the configuration and resubmit.
   - **Reject** — the request cannot be fulfilled → the customer is notified; the session ends (no Order).
6. (Accept/Recommend paths only) the final valid configuration is repriced,
7. customer sees the new final price,
8. customer confirms,
9. only then create the Order.

Important:

> A kitchen review — and a customer's response to a kitchen recommendation — can happen before the final Order exists.

Use a separate `ConfigurationSession` / `ReviewRequest` entity rather than prematurely creating an Order.

---

# 5. Domain Model

Use domain terms consistently.

## 5.1 Core entities

### Customer

```text
customerId
name
email
phoneNumber
passwordHash    nullable
createdAt
updatedAt
```

Guest checkout remains supported: `passwordHash` is null for guests. Authenticated customers register/log in with email + password (hashed, per §14) and their order access is via their session; guests access their order via the opaque `accessToken` described under `Order` below and in §14.3.

### Employee

```text
employeeId
username
displayName
email
role
enabled
createdAt
updatedAt
```

Roles:

```text
ADMIN
KITCHEN
```

### Pizza

```text
pizzaId
code
name
description
basePrice
active
version
createdAt
updatedAt
```

### Ingredient

```text
ingredientId
code
name
type
active
defaultUnit
```

Possible ingredient types:

```text
BASE
EXTRA
CHEESE
MEAT
VEGETABLE
SAUCE
OTHER
```

### PizzaIngredient

Represents the base recipe.

```text
pizzaId
ingredientId
defaultQuantity
removable
```

### Size

```text
sizeId
code
displayName
priceModifier
active
```

Example: `S`, `M`, `L`.

### Dough

```text
doughId
code
displayName
priceModifier
active
```

Example: `CLASSIC`, `GLUTEN_FREE`.

### RuleDefinition

```text
ruleId
ruleCode
ruleType
scopeType
scopeId
parametersJson
message
active
version
validFrom
validTo
createdAt
updatedAt
```

Example rule types:

```text
MAX_QUANTITY
MIN_QUANTITY
REMOVAL_ALLOWED
REQUIRES
EXCLUDES
SIZE_COMPATIBILITY
DOUGH_COMPATIBILITY
INGREDIENT_COMPATIBILITY
OPTION_ALLOWED
```

The database stores rule configuration. The Java implementation contains deterministic evaluators. Do not implement arbitrary database expressions that execute code.

### PriceDefinition

```text
priceId
itemType
itemId
amount
currency
validFrom
validTo
active
version
```

Possible `itemType`:

```text
PIZZA
INGREDIENT
SIZE
DOUGH
```

### ConfigurationSession

Represents a customer configuration before order creation.

```text
configurationId
customerId nullable
pizzaId
sizeId
doughId
configurationJson
comment nullable
validationStatus
ruleVersion
priceStatus
calculatedPrice nullable
currency
expiresAt
createdAt
updatedAt
```

Possible status:

```text
DRAFT
VALID
INVALID
PENDING_REVIEW
REVIEW_APPROVED
REVIEW_REJECTED
PRICED
READY_FOR_CHECKOUT
EXPIRED
```

### ReviewRequest

```text
reviewRequestId
configurationId
status
reason
originalRequestJson
proposedModificationJson nullable
reviewedBy nullable
reviewedAt nullable
customerResponse nullable
customerRespondedAt nullable
createdAt
updatedAt
```

Owned by the `recommendation` module (§7.11). Status:

```text
OPEN
ACCEPTED_BY_KITCHEN
RECOMMENDED_BY_KITCHEN
REJECTED_BY_KITCHEN
RECOMMENDATION_ACCEPTED_BY_CUSTOMER
RECOMMENDATION_REJECTED_BY_CUSTOMER
```

`customerResponse` (`ACCEPTED` | `REJECTED`) and `customerRespondedAt` are only populated when `status` reaches `RECOMMENDED_BY_KITCHEN` and the customer responds; see §4.3 and §7.11 for the full lifecycle.

### Basket

For the first version, basket may be persisted server-side or kept as a short-lived configuration collection.

```text
basketId
customerId nullable
sessionToken
status
createdAt
updatedAt
```

### BasketItem

```text
basketItemId
basketId
configurationId
quantity
priceSnapshot
```

### Order

Required important attributes:

```text
orderId
displayNumber
status
totalPrice
customerId
customNotes
pickupToken
accessTokenHash    nullable
fcmDeviceToken     nullable
currency
createdAt
updatedAt
version
```

`displayNumber` is human-readable. `orderId` is the technical unique identifier. Use UUID for technical identifiers unless an organizational standard says otherwise.

`accessTokenHash` is populated only for guest orders (no `customerId` session) — store a hash of the opaque order-access token per §14.3, never the raw token. `pickupToken` and the access token are separate concepts (§14.3). Authenticated-customer orders are accessed via the customer's session instead, so `accessTokenHash` stays null for those.

`fcmDeviceToken` is populated when the order was placed from the native customer Android app (§8.5) — submitted alongside order creation (§9.1/§10), bound to this specific order, not to a persistent customer/device registration. Null for orders placed via the customer web app. Used by the Notification Module (§7.9) to push order status/ready updates to that device.

Order status:

```text
CONFIRMED
APPROVED
IN_PROCESSING
READY
COMPLETED
CANCELLED
REJECTED
```

### OrderItem

```text
orderItemId
orderId
pizzaId
pizzaNameSnapshot
sizeCode
doughCode
quantity
modificationsJson
unitPrice
subtotal
ruleVersion
priceVersion
```

Store snapshots needed to reconstruct the accepted order even if the catalog later changes.

### NotificationRecord

```text
notificationId
orderId
channel
recipient
type
status
providerMessageId nullable
failureReason nullable
createdAt
sentAt nullable
```

`channel` is `EMAIL` or `PUSH` (§7.9). One `NotificationRecord` row is created per channel attempted for a given order event — e.g. an order-ready event with both an email address and an `Order.fcmDeviceToken` produces two rows, one per channel, sent independently.

### AppLinkSetting

Owned by the `admin` module (§7.10). One row per distributed native app.

```text
appLinkId
platform      (ANDROID; extensible to IOS later)
audience      (CUSTOMER | KITCHEN)
url
active
updatedBy
updatedAt
```

No stored QR image — the QR code is generated on demand from `url` at request time (e.g. via a small server-side QR library such as ZXing), so it never drifts out of sync with the current URL and needs no image storage/versioning. See §7.10 and §8.4.

---

# 6. Database Design

Use one PostgreSQL database.

Logical ownership should be separated by module.

Preferred schema organization:

```text
catalog
rules
pricing
configuration
orders
kitchen
security
notification
```

Do not directly join another module's private tables from arbitrary repositories. Access another module through its published application API where practical.

Use PostgreSQL JSONB for structurally variable but validated data, for example:

- AI-parsed modification payload,
- configuration snapshot,
- order modification snapshot,
- rule parameters.

Do not use JSONB as a replacement for all relational modeling.

## 6.1 Migrations

Use Flyway.

Rules:

- never rely on Hibernate auto-DDL in production,
- `ddl-auto=validate` in non-test deployment,
- every schema change has a versioned Flyway migration,
- migrations must be forward-compatible with rolling application deployment where possible,
- seed only minimal reference data in migrations,
- development/demo seed data belongs in separate dev profile migrations or scripts.

```text
db/migration
├── V001__create_catalog_schema.sql
├── V002__create_rules_schema.sql
├── V003__create_pricing_schema.sql
├── V004__create_configuration_schema.sql
├── V005__create_orders_schema.sql
├── V006__create_security_schema.sql
└── V007__seed_reference_data.sql
```

---

# 7. Module Contracts

## 7.1 Catalog Module

Responsibilities:

- pizza definitions,
- ingredients,
- recipes,
- sizes,
- doughs,
- active/inactive availability,
- catalog queries used by frontend.

Published API example:

```java
public interface CatalogQuery {
    PizzaView getPizza(UUID pizzaId);
    List<PizzaSummary> findActivePizzas();
    ConfigurableOptions getOptions(UUID pizzaId);
}
```

Do not put pricing or validity decisions inside Catalog.

## 7.2 Rule Module

This is the business authority for configuration validity.

Responsibilities:

- load applicable active rule set,
- run deterministic rule evaluators,
- return `VALID` or `INVALID`,
- return structured violations,
- optionally return deterministic alternatives,
- expose rule-aware UI constraints,
- track applied rule version.

Published API:

```java
public interface RuleValidation {
    ValidationResult validate(ConfigurationCandidate candidate);
}
```

Example result:

```java
public record ValidationResult(
    ValidationStatus status,
    String ruleVersion,
    List<Violation> violations,
    List<ConfigurationSuggestion> suggestions
) {}
```

Example violation:

```json
{
  "code": "MAX_QUANTITY_EXCEEDED",
  "field": "extras.CHEESE",
  "ruleCode": "CHEESE_MAX_2",
  "message": "A maximum of two extra portions of cheese is allowed."
}
```

## 7.3 Pricing Module

Responsibilities:

- calculate current price of a valid configuration,
- apply pizza base price,
- size modifier,
- dough modifier,
- extra ingredient prices,
- quantity,
- return price breakdown,
- provide price version,
- create immutable price snapshot for confirmed order.

Pricing must only process a configuration that has a successful rule validation or an explicit trusted validation reference.

```java
public interface PricingService {
    PriceQuote calculate(ValidatedConfiguration configuration);
}
```

```json
{
  "currency": "EUR",
  "base": 9.00,
  "size": 1.50,
  "dough": 0.00,
  "extras": 1.30,
  "total": 11.80,
  "priceVersion": "2026-08-01"
}
```

Use `BigDecimal` for money. Never use `double` or `float` for currency.

## 7.4 AI Adapter

`CommentInterpreter` is provider-agnostic, with **Deepseek as the primary provider and OpenAI as the fallback**. It is never hardwired to a single vendor.

Responsibilities:

- receive only the free-text portion plus necessary constrained context,
- call the AI provider only when a non-blank comment exists,
- try Deepseek first; on timeout, error, or malformed response, fail over to OpenAI; if both fail, fall back to `MANUAL_REVIEW_REQUIRED`,
- request schema-constrained structured output from whichever provider is called,
- validate returned JSON,
- map result to internal DTO,
- return ambiguity metadata where useful,
- never decide validity,
- never calculate price,
- never directly create or modify an Order.

```java
public interface CommentInterpreter {
    ParsedComment interpret(CommentInterpretationRequest request);
}
```

Implementation shape: a single `CommentInterpreter` implementation delegates to an ordered list of provider clients (`DeepseekClient`, `OpenAiClient`), each behind its own timeout/circuit breaker, trying the next provider only after the current one fails or times out.

Required safeguards (apply per provider):

- strict timeout,
- limited retry,
- request size limit,
- allowlisted output schema,
- no secrets in prompt,
- no unrestricted tool execution,
- structured logging without unnecessary sensitive comment contents,
- circuit breaker/failure fallback,
- prompt-injection-resistant prompt design.

If AI fails (both providers):

```text
comment path → MANUAL_REVIEW_REQUIRED
```

The normal no-comment ordering path must remain available.

## 7.5 Configuration Module

This is the application-flow coordinator.

Responsibilities:

- combine customer selections with catalog data,
- decide whether AI parsing is needed,
- invoke Rule Module,
- obtain suggestions,
- create manual ReviewRequest when needed,
- invoke Pricing after successful validation,
- maintain configuration lifecycle,
- expose configuration APIs.

Do not duplicate Rule or Pricing logic here.

## 7.6 Basket Module

Responsibilities:

- add priced valid configuration,
- update quantity,
- remove item,
- calculate basket aggregate from immutable item price quotes,
- reject expired/stale configuration quotes if required,
- prepare checkout input.

Basket must never bypass validation/pricing.

## 7.7 Order Module

Responsibilities:

- create Order only after customer confirms final price,
- enforce idempotent order creation,
- generate display number,
- generate pickup token,
- create OrderItems with snapshots,
- manage order lifecycle transitions,
- publish domain events.

Example state flow:

```text
CONFIRMED → APPROVED → IN_PROCESSING → READY → COMPLETED
```

Do not allow invalid reverse transitions without an explicit administrative recovery mechanism.

## 7.8 Kitchen Module

Responsibilities:

- manual review queue **listing** (the accept/recommend/reject decision logic itself lives in the `recommendation` module, §7.11 — Kitchen surfaces the queue in the KDS UI and calls into `recommendation`),
- confirmed order queue,
- kitchen status updates,
- KDS read model,
- authorization for kitchen actions,
- serve the same KDS data (review queue, production board) to both the staff web app's KDS view and the native kitchen Android app — both are REST/SSE clients of this module, with no backend distinction between them.

Kitchen must not edit price definitions or business rules.

A kitchen-recommended configuration must be revalidated and repriced before customer checkout (see §4.3, §7.11).

## 7.9 Notification Module

Responsibilities:

- send order status updates,
- send ready/pickup notification,
- fan out each event to every channel with data available for that order (§5.1 `NotificationRecord.channel`): email when the customer/order has an email address, push when `Order.fcmDeviceToken` is set — one `NotificationRecord` per channel attempted,
- abstract provider (one `NotificationProvider` implementation per channel),
- store delivery status per channel,
- retry transient failures per channel independently,
- avoid blocking order transaction on slow provider, and never let one channel's failure block or delay the other.

Use asynchronous application event handling. Never roll back an Order because an email/push/SMS provider is temporarily unavailable — this applies independently per channel, so e.g. a push failure must not suppress the email attempt or vice versa.

**Providers for v1:**

- **Email — Gmail SMTP.** Implemented as `GmailSmtpNotificationProvider implements NotificationProvider`, using Spring's `JavaMailSender` against `smtp.gmail.com:587` (STARTTLS) authenticated with a Gmail App Password (see §24 for config). This is a "for now" choice: Gmail's free-tier sending cap (~500/day, ~2,000/day on Google Workspace) is not meant for production-scale transactional email — document this limitation in the README (§31) and treat swapping to a dedicated provider (SendGrid, SES, etc.) as a Phase 13 follow-up.
- **Push — Firebase Cloud Messaging.** Implemented as `FcmPushNotificationProvider implements NotificationProvider`, using the Firebase Admin SDK server-side (see §24 for config), sending to `Order.fcmDeviceToken` when present. Applies only to orders placed via the native customer Android app (§8.5) — orders from the customer web app simply have no `fcmDeviceToken`, so no push is attempted, no error, no fallback needed.

The `NotificationProvider` abstraction means adding, swapping, or removing a channel is a new implementation class only, no redesign of the module.

## 7.10 Admin Module

Responsibilities:

- admin APIs,
- manage catalog,
- manage rule configuration,
- manage price configuration,
- manage users/roles,
- manage `AppLinkSetting` rows (customer Android app link, kitchen Android app link) and expose a published `AppLinkQuery` API so other modules/frontends can read the active link/QR without reaching into admin's persistence,
- audit important changes.

Admin must call module APIs rather than directly changing another module's private JPA entities.

```java
public interface AppLinkQuery {
    Optional<AppLinkView> getActiveLink(Platform platform, Audience audience);
}
```

## 7.11 Recommendation Module

Owns the kitchen-side triage of an unresolved/invalid `ReviewRequest` and the customer's response to a kitchen recommendation. Introduced to make §4.3's review flow an explicit two-sided proposal/response protocol instead of an implicit "modify then reprice."

Responsibilities:

- receive a `ReviewRequest` (status `OPEN`) from the Configuration Module,
- expose the Kitchen decision API: **Accept**, **Recommend** (with a proposed configuration patch), **Reject**,
- on Accept: revalidate/reprice the original configuration via Rule/Pricing Modules, mark `ACCEPTED_BY_KITCHEN`,
- on Recommend: mark `RECOMMENDED_BY_KITCHEN`, store the proposal in `proposedModificationJson`, notify the customer,
- on Reject: mark `REJECTED_BY_KITCHEN`, notify the customer, terminal — no further action possible on this request,
- expose the customer decision API for a `RECOMMENDED_BY_KITCHEN` request: **Accept** (revalidate/reprice the recommended configuration via Rule/Pricing Modules, mark `RECOMMENDATION_ACCEPTED_BY_CUSTOMER`) or **Reject** (mark `RECOMMENDATION_REJECTED_BY_CUSTOMER`, terminal — customer may submit a new configuration),
- publish `RecommendationCreated`, `RecommendationAcceptedByCustomer`, `RecommendationRejectedByCustomer` events (§16).

```java
public interface RecommendationDecision {
    ReviewOutcome accept(UUID reviewRequestId, String reviewedBy);
    ReviewOutcome recommend(UUID reviewRequestId, String reviewedBy, ConfigurationPatch patch);
    ReviewOutcome reject(UUID reviewRequestId, String reviewedBy, String reason);
}

public interface RecommendationResponse {
    ReviewOutcome acceptRecommendation(UUID reviewRequestId);
    ReviewOutcome rejectRecommendation(UUID reviewRequestId);
}
```

`recommendation` depends on `configuration` (to revalidate/reprice) the same way `kitchen` already depends on it — no new dependency direction. It does not duplicate Rule or Pricing logic, matching §7.5's principle for the Configuration Module.

Regardless of which of the three kitchen paths (Accept/Recommend/Reject) or two customer paths (Accept/Reject) is taken, **no Order exists until the customer confirms a final price** (§4.3) — this module never creates an Order itself.

---

# 8. Frontend Design

Two web apps (§2): `customer` (its own domain) and `staff` (kitchen + admin, its own domain, one login). Two native Android apps (§2/§10): customer and kitchen. Web and native are independent clients of the same REST/SSE API — no backend distinction between them.

## 8.1 Customer Configurator (web + native)

Core screen (same functional set on web and the native Android app):

- standard pizza cards: Margherita, Hawaii, Napoli,
- base ingredients,
- removable ingredient toggles,
- extras with +/- controls,
- maximum quantity feedback,
- size selection,
- dough selection,
- quantity,
- price,
- optional comment,
- Add to Basket,
- guest checkout or log in/register (§14),
- a persistent "Get the app" link/banner (§8.6), shown identically to guests and logged-in customers.

Behavior:

1. Load menu.
2. Load rule-aware configurable options.
3. Disable unavailable combinations.
4. Validate server-side after relevant change.
5. Show violations in the context of the affected field.
6. Show current server-calculated price only for valid configuration.
7. On optional comment, call comment-aware validation.
8. If manual review is needed, clearly show pending state.
9. If Kitchen sends a Recommendation, present it distinctly and require an explicit Accept or Reject (§4.3, §7.11) — do not silently fold it into step 10.
10. After the request is Accepted (by kitchen or, for a recommendation, by the customer), refresh the final configuration and price.
11. Require customer confirmation before checkout.

Principle:

> **The UI is rule-aware but not rule-owning.**

Do not hardcode business constraints only in JavaScript. The native Android customer app follows the same principle — it is a REST client, not an independent source of validation truth.

## 8.2 Kitchen KDS (staff web app + native Android)

Design for touch screens:

- large touch targets,
- high contrast,
- minimal navigation,
- clear state columns,
- clear timestamps,
- fast refresh/live updates,
- no dense admin forms.

Available both as a route tree inside the `staff` web app (for `ROLE_KITCHEN`, and `ROLE_ADMIN`) and as the native kitchen Android app — same design principles, same backend calls, either can be used interchangeably.

### Review Queue

Cards show:

- review reference,
- original pizza,
- customer comment,
- AI interpretation where available,
- rule violations,
- proposed alternatives,
- **Accept**,
- **Recommend** (opens an editor for the proposed configuration patch),
- **Reject**.

If a request is `RECOMMENDED_BY_KITCHEN`, the card shows the customer's response (pending / accepted / rejected) once available.

### Production Board

Columns:

```text
CONFIRMED
APPROVED
IN PROCESSING
READY
```

Use live updates via SSE or WebSocket.

KDS sends commands back to backend via REST.

```http
POST /api/v1/kitchen/orders/{orderId}/start
POST /api/v1/kitchen/orders/{orderId}/ready
```

The customer Android app's QR code (§8.6) is displayed in a corner of the board so staff can point in-store customers at it.

## 8.3 Admin Portal (staff web app)

A route tree inside the `staff` web app (§8.4), reachable by `ROLE_ADMIN` only. Views:

- Pizzas
- Ingredients
- Recipes
- Sizes
- Doughs
- Rules
- Prices
- Staff Users
- App Links (§8.6) — edit the customer/kitchen Android app URLs, live QR preview
- Audit Log

Backend remains authoritative. Dangerous changes (catalog/rule/price edits) should require explicit confirmation; the App Links view does not need this since it carries no pricing/validity risk.

## 8.4 Staff Web App Structure

Single build (`frontend/apps/staff`), single login screen using the app-managed JWT (§14.1). After login, routing is role-based:

- `ROLE_KITCHEN` → KDS views (§8.2)
- `ROLE_ADMIN` → Admin Portal views (§8.3), and can also reach the KDS views

The KDS-vs-admin UI distinction (touch-first board vs. dense forms) is implemented as two route trees within this one app, not two deployables.

## 8.5 Native Android Apps

Fully native (Kotlin + Jetpack Compose), not a WebView wrapper:

- **Customer Android app** — same functional scope as §8.1, hitting the same `/api/v1/**` endpoints. Also integrates Firebase Cloud Messaging (FCM): requests the Android 13+ notification permission, obtains the device's FCM token, submits it with order creation (`fcmDeviceToken`, §9.1/§10), and renders incoming push notifications for order status/ready updates (§7.9). Push is an additional channel alongside email and the order status page — the app must work correctly (order creation, status polling) even if the user denies the notification permission or push delivery fails.
- **Kitchen Android app** — same functional scope as §8.2's review queue and production board, hitting the same `/api/v1/kitchen/**` endpoints and the SSE stream (§17); use an Android SSE client (e.g. OkHttp EventSource), with a WebSocket fallback if SSE proves awkward on Android. No push notifications for kitchen — it relies on the SSE stream for live updates.

Both distribute as directly downloadable APKs (not Play Store) for now — see §8.6 and §10.

## 8.6 Android App Links / QR

Admin-managed (§7.10), one entry per app (`CUSTOMER`, `KITCHEN`), each with a URL and an on-demand-generated QR code (no stored image, §5.1 `AppLinkSetting`). Surfaced:

- **Customer web + customer Android app**: "Get the app" link/banner (§8.1), guest and logged-in alike.
- **Kitchen KDS** (web and native): QR code shown on the production board/review screen (§8.2), for staff to point in-store customers at.
- **Admin Portal**: edit URL + live QR preview (§8.3).

---

# 9. REST API

Base path:

```text
/api/v1
```

Use JSON and consistent problem responses. Prefer Spring `ProblemDetail` for HTTP API errors where applicable.

## 9.1 Public/customer endpoints

```text
GET  /api/v1/catalog/pizzas
GET  /api/v1/catalog/pizzas/{pizzaId}
GET  /api/v1/catalog/pizzas/{pizzaId}/options

POST /api/v1/customers/register
POST /api/v1/customers/login

POST /api/v1/configurations
PUT  /api/v1/configurations/{configurationId}
POST /api/v1/configurations/{configurationId}/validate
POST /api/v1/configurations/{configurationId}/price
GET  /api/v1/configurations/{configurationId}
GET  /api/v1/configurations/{configurationId}/review-status

GET  /api/v1/configurations/{configurationId}/recommendation
POST /api/v1/configurations/{configurationId}/recommendation/accept
POST /api/v1/configurations/{configurationId}/recommendation/reject

POST /api/v1/baskets
POST /api/v1/baskets/{basketId}/items
DELETE /api/v1/baskets/{basketId}/items/{itemId}
GET /api/v1/baskets/{basketId}

POST /api/v1/orders
GET  /api/v1/orders/{displayNumber}/status

GET  /api/v1/app-links/android/{audience}            audience = customer | kitchen
GET  /api/v1/app-links/android/{audience}/qr.png
```

## 9.2 Kitchen endpoints

```text
POST /api/v1/staff/login

GET  /api/v1/kitchen/reviews
GET  /api/v1/kitchen/reviews/{reviewId}
POST /api/v1/kitchen/reviews/{reviewId}/accept
POST /api/v1/kitchen/reviews/{reviewId}/recommend
POST /api/v1/kitchen/reviews/{reviewId}/reject

GET  /api/v1/kitchen/orders
POST /api/v1/kitchen/orders/{orderId}/approve
POST /api/v1/kitchen/orders/{orderId}/start
POST /api/v1/kitchen/orders/{orderId}/ready
POST /api/v1/kitchen/orders/{orderId}/complete
GET  /api/v1/kitchen/stream

GET  /api/v1/kitchen/app-links/android/customer
```

`/staff/login` is unauthenticated (used by both `ROLE_KITCHEN` and `ROLE_ADMIN` to obtain a JWT, §14.1); every other endpoint above requires `ROLE_KITCHEN` or `ROLE_ADMIN`.

`/kitchen/reviews/{reviewId}/accept|recommend|reject` are owned by the `recommendation` module (§7.11); `kitchen` surfaces the queue and proxies the calls.

## 9.3 Admin endpoints

```text
/api/v1/admin/pizzas
/api/v1/admin/ingredients
/api/v1/admin/rules
/api/v1/admin/prices
/api/v1/admin/users
/api/v1/admin/audit

GET /api/v1/admin/app-links
PUT /api/v1/admin/app-links/android/{audience}        audience = customer | kitchen
```

All `/admin/**` endpoints require `ROLE_ADMIN`.

---

# 10. API Contracts

Never return JPA entities directly.

Use separate request DTOs, response DTOs, domain objects, and persistence entities where beneficial.

Example validation request:

```json
{
  "pizzaId": "9b9bf4b6-fbec-4a1e-9d69-bb793832d848",
  "sizeCode": "M",
  "doughCode": "CLASSIC",
  "removedIngredients": ["BASIL"],
  "extras": [
    {
      "ingredientCode": "CHEESE",
      "quantity": 2
    }
  ],
  "comment": "Please make it extra crispy."
}
```

Example invalid response:

```json
{
  "configurationId": "bd80ae3c-37a9-48c7-811f-2481aa95c369",
  "status": "INVALID",
  "ruleVersion": "2026.08.1",
  "violations": [
    {
      "code": "MAX_QUANTITY_EXCEEDED",
      "field": "extras.CHEESE",
      "message": "A maximum of two extra portions is allowed."
    }
  ],
  "suggestions": [
    {
      "description": "Use two portions of extra cheese.",
      "patch": {
        "extras.CHEESE": 2
      }
    }
  ]
}
```

Example order creation request (`POST /api/v1/orders`):

```json
{
  "basketId": "3f1a9e2c-6b0a-4e0f-9a9d-2a6e2ab7b5b0",
  "customNotes": "Ring the bell twice",
  "fcmDeviceToken": null
}
```

`fcmDeviceToken` is optional — set by the native customer Android app (§8.5) so the Notification Module (§7.9) can push order status/ready updates to that device; omitted or `null` for orders placed via the customer web app.

---

# 11. Rule Engine Design

Do not build a general-purpose scripting engine first. Implement deterministic typed evaluators.

```java
public interface RuleEvaluator {
    boolean supports(RuleDefinition rule);
    List<Violation> evaluate(
        RuleDefinition rule,
        ConfigurationCandidate configuration,
        RuleEvaluationContext context
    );
}
```

Implement evaluators such as:

```text
MaxQuantityEvaluator
RemovalAllowedEvaluator
RequiresOptionEvaluator
ExclusionEvaluator
SizeCompatibilityEvaluator
DoughCompatibilityEvaluator
IngredientCompatibilityEvaluator
```

The Rule Validation Service:

1. determines applicable rules,
2. orders them if necessary,
3. runs evaluators,
4. aggregates violations,
5. creates deterministic suggestions where supported,
6. returns a complete ValidationResult.

Rules must be testable without HTTP or database access.

## 11.1 Rule versioning

Every validation result must include the rule version used.

When checkout occurs, revalidate if:

- rule version changed,
- price quote expired,
- configuration changed,
- referenced catalog option became inactive.

Orders must keep the accepted snapshot. Historical orders must not silently change when catalog/rules change.

---

# 12. Pricing Rules

Price calculation must be deterministic.

```text
total =
    pizzaBasePrice
  + sizeModifier
  + doughModifier
  + sum(extraIngredientPrice × extraQuantity)
```

Apply order-item quantity afterward.

Use `BigDecimal` with explicit scale and rounding.

Initial currency: `EUR`.

Do not derive historical order totals from the latest catalog price. Store accepted price snapshot.

---

# 13. AI Integration

## 13.1 When AI is called

Only call AI if:

```java
comment != null && !comment.isBlank()
```

No comment:

```text
Frontend → Backend → Rule Module
```

Comment:

```text
Frontend → Backend → AI Adapter → Deepseek (primary) → AI Adapter → Rule Module
                                 ↳ on Deepseek failure → OpenAI (fallback) → AI Adapter → Rule Module
                                 ↳ on both failures → MANUAL_REVIEW_REQUIRED
```

## 13.2 Structured schema

Do not ask the model for unrestricted prose. Use a strict structured-output schema conceptually equivalent to:

```json
{
  "type": "object",
  "properties": {
    "removeIngredients": {
      "type": "array",
      "items": { "type": "string" }
    },
    "extras": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "ingredientCode": { "type": "string" },
          "quantity": { "type": "integer", "minimum": 0 }
        },
        "required": ["ingredientCode", "quantity"],
        "additionalProperties": false
      }
    },
    "requestedSize": { "type": ["string", "null"] },
    "requestedDough": { "type": ["string", "null"] },
    "unresolvedText": {
      "type": "array",
      "items": { "type": "string" }
    }
  },
  "required": [
    "removeIngredients",
    "extras",
    "requestedSize",
    "requestedDough",
    "unresolvedText"
  ],
  "additionalProperties": false
}
```

After the response:

1. validate schema,
2. normalize catalog codes,
3. reject unknown items as unresolved,
4. merge with structured UI configuration,
5. send merged candidate to Rule Module.

Never map unknown model output directly into trusted domain entities.

## 13.3 AI failure policy

On timeout, API error, malformed response, unknown ingredient, or unresolved language, return `PENDING_REVIEW` or ask the customer to clarify.

Normal structured ordering must continue even when AI is unavailable.

---

# 14. Security

## 14.1 Authentication

**Chosen for v1: application-managed authentication (Spring Security, self-issued JWT).** No external identity provider (no Keycloak, no OIDC resource-server config) — this keeps the Docker Compose topology and Hetzner VM deployment (§27) simple. Revisit an OIDC/Keycloak-based approach only if a real corporate IAM requirement appears later.

- Staff (`ROLE_KITCHEN`, `ROLE_ADMIN`) log in via the `staff` web app or the native kitchen Android app (§8.4/§8.5) against `POST /api/v1/staff/login` (Spring Security, hashed passwords, short-lived JWT).
- Customers optionally register/log in (`POST /api/v1/customers/register`, `POST /api/v1/customers/login`, §9.1) using the same JWT approach; guest checkout remains fully supported for customers who don't register (§5.1 `Customer.passwordHash` nullable).
- Because auth is stateless bearer-JWT (no session cookies), CSRF protection is unnecessary on `/api/**` (see §14.4).

## 14.2 Authorization

Required permissions:

```text
PUBLIC_CUSTOMER
KITCHEN
ADMIN
```

`PUBLIC_CUSTOMER` covers both guest and authenticated-customer requests — the distinction between the two is whether a customer session/JWT is present, not a separate role.

Use request-level security and method-level authorization on sensitive operations.

```java
@PreAuthorize("hasRole('ADMIN')")
public void updateRule(...) { ... }

@PreAuthorize("hasAnyRole('KITCHEN','ADMIN')")
public void markReady(...) { ... }
```

Protect with deny-by-default rules.

## 14.3 Guest order access

Do not expose orders merely by sequential display number.

- **Guest orders** (no customer session): use the unguessable `Order.accessTokenHash` (§5.1) — store a hash of the token, never the raw token.
  ```text
  /order/{displayNumber}?token={opaque-order-access-token}
  ```
- **Authenticated-customer orders**: accessed via the customer's own session/JWT instead of a token — no `accessTokenHash` is generated for these orders.

Pickup token and online access token are separate concepts in both cases.

## 14.4 General controls

Implement:

- TLS in deployed environments (Let's Encrypt via nginx on the Hetzner VM, §27),
- secure headers,
- request size limits,
- validation,
- rate limiting at reverse proxy or application edge,
- CORS allowlist,
- no CSRF middleware needed — stateless bearer-JWT auth (§14.1) carries no session cookies,
- password hashing for both staff and registered-customer credentials,
- secrets via environment/secret manager (on the Hetzner VM: `.env` file outside git, §24/§27),
- no secrets committed to Git,
- no Deepseek/OpenAI/Gmail/Firebase-Admin credentials in any frontend or native app — the Firebase Admin *service-account* key (used server-side by `FcmPushNotificationProvider`, §7.9) stays on the backend only; the customer Android app ships only the public `google-services.json` client config, which is not a secret,
- audit of admin rule/price/app-link changes.

---

# 15. Idempotency and Concurrency

## 15.1 Order creation

`POST /api/v1/orders` must accept an idempotency key.

```http
Idempotency-Key: 4a903125-1291-4fd5-8117-b02ca7fe2b18
```

Store:

```text
idempotencyKey
requestHash
orderId
createdAt
```

A repeated identical request returns the previously created Order. A repeated key with a different request returns a conflict.

## 15.2 Optimistic locking

Use JPA `@Version` for mutable aggregate roots such as:

- Order,
- ConfigurationSession,
- RuleDefinition,
- PriceDefinition where concurrent admin changes are possible.

Return HTTP `409 Conflict` for conflicting updates where appropriate.

---

# 16. Events

Use Spring application/domain events for internal asynchronous communication.

Suggested events:

```java
ConfigurationReviewRequested
ConfigurationReviewResolved
RecommendationCreated
RecommendationAcceptedByCustomer
RecommendationRejectedByCustomer
OrderPlaced
OrderApproved
OrderProcessingStarted
OrderReady
OrderCompleted
OrderCancelled
```

`ConfigurationReviewRequested`/`ConfigurationReviewResolved` are still emitted for the Accept/Reject-by-kitchen paths (§7.11); the three `Recommendation*` events cover the Recommend path and the customer's response to it.

Event handlers should be idempotent where they cause side effects.

Do not build a distributed event architecture before there is a real need.

---

# 17. Realtime KDS Updates

Preferred MVP:

- SSE from backend to KDS for server-to-client order updates,
- REST for KDS commands.

Use WebSocket only if bidirectional persistent communication becomes necessary.

SSE event examples:

```text
order.created
order.updated
order.ready
review.created
review.updated
```

Reconnect behavior:

- browser reconnects automatically,
- client refetches current KDS state after reconnect,
- events are an optimization, not the only source of truth.

---

# 18. Error Handling

Create global exception handling and stable error codes.

```text
CONFIGURATION_INVALID
CONFIGURATION_EXPIRED
PRICE_QUOTE_EXPIRED
RULE_VERSION_CHANGED
REVIEW_PENDING
ORDER_ALREADY_CREATED
ORDER_STATE_CONFLICT
UNAUTHORIZED
FORBIDDEN
AI_UNAVAILABLE
NOTIFICATION_FAILED
```

User-facing errors must be understandable. Logs may contain technical detail but must not leak secrets.

---

# 19. Observability

Use Spring Boot Actuator.

Required internal endpoints:

```text
/actuator/health
/actuator/info
/actuator/metrics
/prometheus if enabled
```

Do not expose sensitive actuator endpoints publicly.

Recommended metrics:

```text
http.server.requests
configuration.validation.duration
configuration.validation.invalid.count
ai.comment.requests
ai.comment.failures
review.open.count
order.created.count
order.state.transition.count
notification.success.count
notification.failure.count
```

Logs should include correlation identifiers.

Recommended fields:

```text
timestamp
level
service
traceId
requestId
orderId
configurationId
event
message
```

Do not log raw authorization headers, passwords, secrets, or API keys.

---

# 20. Testing Strategy

The deployable project is not complete without automated tests.

## 20.1 Unit tests

High priority:

- Rule evaluators,
- Pricing calculations,
- Order state machine,
- Configuration merging,
- AI output normalization,
- token generation,
- idempotency behavior.

Rule tests should be table-driven where possible.

## 20.2 Module tests

Use Spring Modulith module tests.

```text
CatalogModuleTests
RulesModuleTests
PricingModuleTests
OrdersModuleTests
KitchenModuleTests
```

Verify no accidental cross-module dependencies.

## 20.3 Integration tests

Use Testcontainers PostgreSQL.

Do not use H2 as the primary persistence integration-test database.

Test:

- Flyway migrations,
- JPA mappings,
- JSONB columns,
- constraints,
- repository behavior,
- transaction behavior.

Mock only true external systems such as Deepseek, OpenAI, or the Gmail SMTP notification provider.

## 20.4 API tests

Test:

- validation,
- pricing,
- order creation,
- idempotency,
- unauthorized kitchen/admin access,
- manual review,
- order state transitions,
- error contracts.

## 20.5 External integration tests

Use WireMock or equivalent for:

- AI Adapter HTTP behavior — both Deepseek (primary) and OpenAI (fallback),
- notification provider behavior: Gmail SMTP (use a fake/test SMTP server such as GreenMail, not a live Gmail account) and FCM (mock the Firebase Admin SDK's HTTP calls, not a live Firebase project).

Test success, timeout, 429, 5xx, malformed JSON, and schema violation for each provider, plus the Deepseek→OpenAI failover path itself (Deepseek fails → OpenAI succeeds → merged candidate reaches Rule Module), and the email/push fan-out independence (push fails → email still sends; email fails → push still sends).

Do not use live Deepseek/OpenAI/Gmail/Firebase calls in ordinary CI tests.

## 20.6 Frontend tests

Component tests:

- quantity controls,
- disabled rule-aware options,
- validation error presentation,
- comment field,
- final-price confirmation,
- KDS status actions.

E2E with Playwright:

1. customer creates standard pizza order,
2. customer adds valid extras,
3. max extra restriction is enforced,
4. comment path returns valid interpretation,
5. unresolved comment enters review,
6. kitchen accepts, or recommends and customer accepts the recommendation,
7. customer confirms new price,
8. order appears in KDS,
9. kitchen marks processing,
10. kitchen marks ready,
11. customer sees ready state.

---

# 21. Required Acceptance Scenarios

## Scenario A — Standard Margherita

```text
Given active Margherita
When customer selects size M, Classic dough, no changes
Then configuration is VALID
And price is returned
And no AI provider (Deepseek or OpenAI) is called
And customer can checkout
And Order is created
And KDS receives the Order
```

## Scenario B — Extra cheese within max

```text
Given cheese max quantity is 2
When customer selects extra cheese quantity 2
Then validation is VALID
And correct extra price is included
```

## Scenario C — Extra cheese above max

```text
When customer requests extra cheese quantity 3
Then validation is INVALID
And violation code is MAX_QUANTITY_EXCEEDED
And customer cannot checkout
```

## Scenario D — Comment parsed successfully

```text
When customer enters "no basil and double cheese"
Then AI Adapter is invoked
And structured changes are created
And Rule Module validates the merged configuration
And AI does not determine validity
```

## Scenario E — AI unavailable

```text
When Deepseek times out for a comment
Then OpenAI is tried as fallback
When OpenAI also fails
Then normal backend remains healthy
And request is marked for clarification/manual review (MANUAL_REVIEW_REQUIRED)
And structured no-comment orders still work
```

## Scenario F — Manual kitchen review, kitchen accepts

```text
Given an unresolved configuration
When Kitchen Staff accepts it as-is
Then the configuration is revalidated
And repriced
And customer receives final price
And no Order exists before customer confirmation
```

## Scenario F2 — Manual kitchen review, kitchen recommends

```text
Given an unresolved configuration
When Kitchen Staff recommends an alternative configuration
Then the customer is notified and must explicitly Accept or Reject the recommendation
When the customer Accepts
Then the recommended configuration is revalidated and repriced
And customer receives final price
And no Order exists before customer confirmation
When the customer Rejects instead
Then the session ends with no Order
And the customer may revise the configuration and resubmit
```

## Scenario G — Duplicate checkout

```text
When the same checkout request with same idempotency key is sent twice
Then exactly one Order exists
And both successful responses reference the same Order
```

## Scenario H — Security

```text
When unauthenticated user calls admin endpoint
Then access is denied

When Kitchen user calls price-admin endpoint
Then access is denied

When Admin calls rule-admin endpoint
Then access is allowed
```

## Scenario I — KDS lifecycle

```text
CONFIRMED
→ APPROVED
→ IN_PROCESSING
→ READY
→ COMPLETED
```

Invalid reverse transitions must fail.

## Scenario J — Multi-channel notification on order ready

```text
Given an order was placed via the native customer Android app with a valid fcmDeviceToken
And the customer also has an email address
When the order transitions to READY
Then two NotificationRecord rows are created: one channel=EMAIL, one channel=PUSH
And both are attempted independently
When the PUSH attempt fails (e.g. FCM error)
Then the EMAIL attempt still succeeds
And the Order status is unaffected by either outcome
```

---

# 22. Repository Structure

Recommended monorepo:

```text
pizza-configurator/
├── agent.md
├── README.md
├── .editorconfig
├── .gitignore
├── .env.example
├── compose.yaml
├── docs/
│   ├── architecture/
│   ├── api/
│   └── adr/
├── backend/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/
│       │   ├── java/com/example/pizzaconfigurator/
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-local.yml
│       │       └── db/migration/
│       └── test/
├── frontend/
│   ├── package.json
│   ├── pnpm-workspace.yaml
│   └── apps/
│       ├── customer/     — public web/PWA, own domain
│       └── staff/        — kitchen + admin, unified, own domain, role-routed after login
├── mobile/
│   ├── customer-android/ — Kotlin + Jetpack Compose
│   └── kitchen-android/  — Kotlin + Jetpack Compose
├── infrastructure/
│   ├── nginx/            — reverse proxy + TLS (Let's Encrypt) for the Hetzner VM
│   ├── prometheus/
│   └── grafana/
└── .github/
    └── workflows/
        ├── backend-ci.yml
        ├── frontend-ci.yml
        ├── android-ci.yml     — build/lint/test both native apps, produce APK artifacts
        ├── integration.yml
        ├── container-build.yml
        └── deploy.yml         — build+push images to GHCR, SSH to Hetzner VM, docker compose pull && up -d, smoke test
```

---

# 23. Backend Package Pattern

Each domain module should look roughly like:

```text
rules/
├── Rules.java
├── api/
│   ├── RuleValidation.java
│   ├── ValidationResult.java
│   └── Violation.java
├── application/
│   └── RuleValidationService.java
├── domain/
│   ├── RuleDefinition.java
│   ├── RuleEvaluator.java
│   └── evaluator/
├── infrastructure/
│   └── persistence/
│       ├── RuleJpaEntity.java
│       ├── RuleJpaRepository.java
│       └── RuleRepositoryAdapter.java
└── web/
    └── RuleAdminController.java
```

Use package-private visibility for internal implementation classes where practical. Expose only the minimum module API.

---

# 24. Configuration

Environment variables:

```text
SPRING_PROFILES_ACTIVE
SERVER_PORT

DB_HOST
DB_PORT
DB_NAME
DB_USER
DB_PASSWORD

DEEPSEEK_API_KEY
DEEPSEEK_MODEL
DEEPSEEK_TIMEOUT_MS

OPENAI_API_KEY
OPENAI_MODEL
OPENAI_TIMEOUT_MS

JWT_SIGNING_SECRET
JWT_ISSUER
JWT_AUDIENCE

NOTIFICATION_PROVIDER=gmail-smtp
GMAIL_SMTP_USERNAME
GMAIL_SMTP_APP_PASSWORD

FIREBASE_PROJECT_ID
FIREBASE_SERVICE_ACCOUNT_JSON

CORS_ALLOWED_ORIGINS
PUBLIC_BASE_URL
```

Deepseek is the primary AI provider, OpenAI the fallback (§7.4/§13.1) — both sets of keys are required. `JWT_SIGNING_SECRET`/`JWT_ISSUER`/`JWT_AUDIENCE` back the self-issued-JWT auth chosen in §14.1 (no OIDC provider URL, since there is no external IdP for v1). `NOTIFICATION_PROVIDER` defaults to `gmail-smtp` per §7.9; `GMAIL_SMTP_USERNAME`/`GMAIL_SMTP_APP_PASSWORD` authenticate against Gmail's SMTP. `FIREBASE_PROJECT_ID`/`FIREBASE_SERVICE_ACCOUNT_JSON` (path to, or base64-encoded, the Firebase Admin SDK service-account key — never committed) back `FcmPushNotificationProvider` (§7.9), used to push order status/ready updates to the native customer Android app.

GitHub Actions secrets (not application env vars, held in the repo/environment settings, used only by `deploy.yml`, §22/§27):

```text
HETZNER_HOST
HETZNER_SSH_USER
HETZNER_SSH_KEY        (deploy-only key)
GHCR_TOKEN
```

Do not commit `.env`. Commit `.env.example` with placeholder values. On the Hetzner VM, production secrets live only in `/opt/pizza-configurator/.env`, never in git.

---

# 25. Docker

## 25.1 Backend Dockerfile

Use a multi-stage build and a non-root runtime user.

Conceptual structure:

```dockerfile
FROM maven:... AS build
WORKDIR /workspace
COPY pom.xml .
COPY src src
RUN mvn -B clean verify

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
USER 10001
ENTRYPOINT ["java","-jar","/app/app.jar"]
```

Use the approved base image and pin tested versions.

## 25.2 Local Compose

`compose.yaml` should provide at minimum:

```text
postgres
backend
customer-web
staff-web
```

Optional profiles:

```text
observability
```

No `identity` profile — there is no external IdP for v1 (§14.1). Native Android apps (§10) are not part of Compose; they build/run via their own toolchain (`mobile/`, `android-ci.yml`).

Health dependencies should use real health checks rather than fixed sleep commands.

---

# 26. CI/CD

## Backend

```text
checkout
setup Java
Maven cache
mvn verify
Spring Modulith verification
Testcontainers integration tests
build JAR
build container
```

## Frontend

```text
checkout
setup Node
install locked dependencies
lint
typecheck
unit tests
build
```

Build matrix: `customer`, `staff` (§22).

## Mobile (`android-ci.yml`)

```text
checkout
setup JDK + Android SDK
lint
unit tests
assemble debug/release APK
upload APK as build artifact
```

Build matrix: `customer-android`, `kitchen-android` (§22).

## E2E

```text
start compose test stack
run Flyway
run Playwright tests
collect logs on failure
tear down
```

## Main/release (`deploy.yml`)

```text
build immutable images
tag with Git SHA
security scan
publish images to GHCR
SSH into the Hetzner VM (deploy-only key, HETZNER_* secrets)
on the VM: docker compose pull && docker compose up -d
run smoke test against the public URL
```

Never deploy an image tagged only `latest` without an immutable reference.

---

# 27. Deployment Profiles

## Local

- Docker Compose
- local PostgreSQL
- stub notification providers (both email and push) by default
- mock/stub Deepseek and OpenAI by default
- optional real-provider profile

## Staging

No persistent staging VM for v1 — CI's own ephemeral Compose stack (the E2E job above) serves as pre-release verification. Revisit a dedicated staging VM later if needed.

## Production — single Hetzner VM

- Postgres runs as a container on the same VM — **no managed database service on Hetzner**, so automated backups must be explicit: scheduled `pg_dump` (cron/systemd timer) uploaded off-VM (e.g. Hetzner Storage Box), plus a documented and periodically-tested restore procedure (§31),
- TLS via nginx + certbot/Let's Encrypt (`infrastructure/nginx/`), one server block per frontend domain (`app.<domain>`, `staff.<domain>`) plus the API domain,
- app-managed JWT auth (§14.1) — no external IAM,
- secret management: `.env` on the VM only, never in git,
- restrictive CORS,
- resource limits,
- health probes,
- metrics,
- centralized logs,
- rollback procedure: redeploy the previous Git-SHA-tagged image via the same `deploy.yml` workflow,
- database backup/restore test (see the Postgres-on-VM bullet above).

---

# 28. Performance and Reliability Targets

Treat the following as initial engineering targets and measure them.

- normal catalog/configuration interactions should feel immediate,
- validation/pricing target: approximately `<500 ms p95` excluding slow optional AI calls,
- standard no-comment order flow must not depend on Deepseek or OpenAI,
- duplicate Order creation must be prevented,
- database writes must be transactional,
- external notification failure must not corrupt Order state,
- AI failure (both Deepseek and OpenAI) must not take down normal configuration.

Create load tests for:

- catalog reads,
- configuration validation,
- price calculation,
- checkout,
- KDS reads.

Do not optimize prematurely at the cost of correctness.

---

# 29. Caching

Allowed initial caches:

- active catalog reference data,
- active rule definitions,
- active price definitions if invalidation is reliable.

Use an in-process cache such as Caffeine if needed.

Do not introduce Redis in the first version without a proven need.

Cache invalidation must occur after admin updates. The database remains authoritative.

---

# 30. Auditability

Audit at minimum:

- admin creates/updates/deactivates rule,
- admin changes price,
- admin changes catalog availability,
- staff manual review decision,
- staff order status change,
- exceptional administrative state correction.

Audit event:

```text
eventId
timestamp
actorId
actorRole
action
entityType
entityId
beforeJson optional
afterJson optional
correlationId
```

Do not store passwords, tokens, or secrets in audit JSON.

---

# 31. Documentation

Repository must contain a README explaining:

- architecture,
- prerequisites,
- local startup,
- test commands,
- environment variables,
- demo accounts,
- key URLs,
- how to seed demo data,
- how AI can be disabled,
- how to run without external notification provider,
- the Gmail SMTP sending-volume limitation (§7.9) and when to swap to a dedicated provider,
- how to build and install the native Android APKs locally,
- how the Hetzner deployment works (deploy.yml, SSH target, backup/restore procedure for Postgres, §27).

Create Architecture Decision Records:

```text
ADR-001 Modular Monolith instead of Microservices
ADR-002 Deterministic Rule Engine as Source of Truth
ADR-003 Conditional AI Comment Parsing
ADR-004 PostgreSQL as Primary Store
ADR-005 SSE for KDS Live Updates
ADR-006 Price Snapshot at Order Confirmation
ADR-007 Recommendation Module for Kitchen Accept/Recommend/Reject
ADR-008 Deepseek Primary with OpenAI Fallback
ADR-009 Native Android Apps Distributed as Direct APKs
ADR-010 Single Hetzner VM via GitHub Actions SSH Deploy
ADR-011 Gmail SMTP as Interim Notification Provider
```

---

# 32. Development Sequence

The coding agent should implement in this order.

## Phase 1 — Project skeleton

- Spring Boot project
- Spring Modulith
- PostgreSQL
- Flyway
- Docker Compose
- basic health endpoint
- CI
- module boundary verification

Definition of done:

```text
mvn verify succeeds
application starts
PostgreSQL migration succeeds
/actuator/health is UP
module verification passes
```

## Phase 2 — Catalog

- pizzas,
- ingredients,
- recipe,
- size,
- dough,
- read API,
- admin CRUD,
- demo data.

Definition of done: customer frontend can load Margherita, Hawaii, Napoli and configuration options.

## Phase 3 — Rule Module

Implement:

- removal,
- max quantity,
- compatibility,
- size/dough rules,
- validation API,
- structured violations,
- rule admin,
- rule versioning,
- tests.

Definition of done: valid/invalid scenarios work independently of AI.

## Phase 4 — Pricing

- price definitions,
- calculation,
- breakdown,
- version,
- admin pricing,
- tests.

Definition of done: valid configuration returns deterministic price.

## Phase 5 — Customer Frontend

- catalog UI,
- pizza selection,
- ingredients,
- extras,
- size/dough,
- rule-aware UI,
- price display,
- comment field,
- basket,
- guest checkout and customer register/login (§14.1),
- "Get the app" link placeholder (wired up fully in Phase 11's App Links feature).

Definition of done: standard no-comment configuration works end-to-end up to checkout, for both guest and authenticated customers.

## Phase 6 — Order Module

- checkout,
- idempotency,
- Order/OrderItem,
- display number,
- pickup token,
- state machine,
- OrderPlaced event.

Definition of done: confirmed order persists once and appears in kitchen query.

## Phase 7 — KDS (staff web app)

- kitchen authentication (shared staff login, §8.4),
- production board,
- state commands,
- SSE updates.

Definition of done: new order appears without manual page refresh and can reach READY.

## Phase 8 — Notifications

- provider abstraction (`NotificationProvider`),
- `GmailSmtpNotificationProvider` implementation (§7.9),
- `FcmPushNotificationProvider` implementation using the Firebase Admin SDK (§7.9), keyed off `Order.fcmDeviceToken`,
- per-channel `NotificationRecord` fan-out (one row per channel with data available, §5.1),
- order-ready notification,
- retry/failure recording, independent per channel.

Definition of done: READY event generates independent Gmail SMTP and (when `fcmDeviceToken` is set) FCM push notification attempts without blocking the order transaction, and a failure in one channel never suppresses the other.

## Phase 9 — AI Adapter

- conditional invocation,
- structured output,
- schema validation,
- mapping,
- Deepseek primary client, OpenAI fallback client, failover logic (§7.4/§13.1),
- timeout/circuit-breaker per provider,
- WireMock tests for both providers plus the failover path.

Definition of done: AI is never called for blank comments, never decides validity, and a Deepseek failure transparently falls back to OpenAI before reaching `MANUAL_REVIEW_REQUIRED`.

## Phase 10 — Recommendation Module & Manual Review

- ReviewRequest,
- `recommendation` module (§7.11): Accept / Recommend / Reject kitchen actions,
- customer Accept/Reject response to a Recommendation,
- KDS review queue UI update,
- revalidation,
- repricing,
- customer review-status refresh.

Definition of done: an unresolved request can be Accepted, Recommended-then-customer-Accepted, Recommended-then-customer-Rejected, or Rejected — all four paths tested, and no Order exists until final customer price confirmation regardless of path (§4.3).

## Phase 11 — Admin Portal & App Links

- catalog,
- rules,
- prices,
- staff,
- App Links (customer + kitchen Android app URLs, on-demand QR, §7.10/§8.6) — wires up the placeholder from Phase 5,
- audit.

Definition of done: admin can change rule/price configuration and app-link URLs, and changes are reflected safely; the customer web app's "Get the app" link/QR resolves to the admin-configured URL.

## Phase 12 — Native Android Apps

- `mobile/customer-android` (Kotlin + Jetpack Compose): standard order flow (§4.1) against the same backend,
- `mobile/kitchen-android` (Kotlin + Jetpack Compose): review queue + production board (§7.8) against the same backend and SSE stream,
- `android-ci.yml`: lint, unit tests, APK build artifacts.

Definition of done: both APKs install and can complete their respective flows end-to-end against the same backend used by the web apps; distributed via the App Links feature from Phase 11.

## Phase 13 — Production hardening & Hetzner deployment

- security review,
- rate limits,
- observability,
- logs,
- metrics,
- tracing,
- container scan,
- `deploy.yml`: build/push to GHCR, SSH to the Hetzner VM, `docker compose pull && up -d`, smoke test,
- nginx + certbot/Let's Encrypt on the VM (§27),
- Postgres backup process (scheduled `pg_dump` off-VM + tested restore, since Hetzner has no managed Postgres),
- E2E suite,
- load test,
- deployment documentation (README, §31).

---

# 33. Coding Standards

## Java

- prefer immutable records for simple DTOs,
- constructor injection,
- no field injection,
- explicit transactions,
- small application services,
- no business logic in controllers,
- no static mutable global state,
- inject `Clock` for time-dependent logic,
- `BigDecimal` for money,
- UUID for technical identifiers,
- enums for stable internal states,
- map enums to stable strings in DB/API.

## REST

- nouns for resources,
- commands only when state-transition semantics justify them,
- validate all inputs,
- correct HTTP status codes,
- `201 Created` for new Order,
- `409 Conflict` for state/idempotency conflicts,
- choose one consistent policy for semantically invalid configuration.

## TypeScript

- strict mode,
- no `any` without justification,
- API schemas validated,
- reusable typed API client,
- accessible form controls,
- loading/error/empty states required.

---

# 34. Forbidden Shortcuts

The agent must not:

- replace the modular monolith with microservices,
- let AI mark a configuration valid,
- put business rules only in the frontend,
- store money as floating point,
- create Orders before final customer price confirmation,
- create an Order merely because Kitchen review was requested,
- commit secrets,
- use H2 as the only integration-test database,
- expose JPA entities directly,
- let Kitchen users edit administrative rules/prices,
- let notification failure roll back a successful order,
- hardcode all rules into frontend JavaScript,
- depend on Deepseek or OpenAI for standard orders,
- bypass Rule Module after an AI response,
- let a kitchen Recommendation become the final configuration without an explicit customer Accept,
- let the `recommendation` module create an Order itself,
- use a distributed message broker without justification,
- introduce Kubernetes before the Compose-based deployable system is working,
- store Deepseek/OpenAI/Gmail credentials in a frontend or native app,
- assume Hetzner provides a managed Postgres service — backups must be implemented explicitly (§27).

---

# 35. Definition of Done for the Complete Deployable Version

## Architecture

- Spring Boot Modular Monolith is implemented, including the `recommendation` module.
- Spring Modulith verification passes.
- Modules have clear published interfaces.
- Deepseek (primary) and OpenAI (fallback) are both external and conditional.
- PostgreSQL is the primary store.

## Functional

- standard pizzas can be ordered, as a guest or as an authenticated customer,
- size and dough can be selected,
- allowed ingredients can be added/removed,
- quantities are constrained,
- comments are optional,
- Rule Module validates all configurations,
- alternatives can be suggested,
- invalid unresolved comments can reach Kitchen review,
- Kitchen can Accept, Recommend, or Reject a review request; a customer can Accept or Reject a Recommendation,
- reviewed/recommended configuration is revalidated/repriced,
- customer confirms final price before Order creation,
- Order appears in KDS (staff web app and native kitchen Android app),
- KDS can update lifecycle,
- customer can receive an order-ready notification via email (Gmail) and, when ordering through the native Android app, also via push (FCM) — independently of each other and of the order status page,
- Admin can manage catalog/rules/prices/staff/app-links,
- customer web app shows the Android app download link/QR; kitchen KDS shows it too,
- native customer Android app can complete the standard order flow,
- native kitchen Android app can operate the review queue and production board.

## Security

- staff and customer authentication both work (app-managed JWT),
- guest checkout still works without an account,
- RBAC is enforced,
- secrets are externalized,
- admin/kitchen APIs reject unauthorized callers.

## Reliability

- checkout is idempotent,
- invalid order transitions fail,
- AI outage (both Deepseek and OpenAI) does not stop normal ordering,
- notification outage does not corrupt orders,
- database migrations are automated.

## Quality

- unit tests pass,
- module tests pass,
- Testcontainers integration tests pass,
- frontend tests pass (both `customer` and `staff` web apps),
- native Android unit/lint checks pass for both apps,
- Playwright happy-path E2E passes,
- architecture tests pass,
- CI passes from a clean clone.

## Deployment

A new developer must be able to run:

```bash
docker compose up --build
```

and obtain a working local environment.

README must list the exact URLs, for example:

```text
Customer: http://localhost:3000
Staff:    http://localhost:3001    (kitchen + admin, role-routed after login)
Backend:  http://localhost:8080
```

Ports may differ, but they must be documented. For the production Hetzner deployment, README must additionally document the live URLs (`https://app.<domain>`, `https://staff.<domain>`, `https://api.<domain>`) and the `deploy.yml` GitHub Actions workflow.

---

# 36. Recommended Demo Data

## Pizzas

```text
Margherita
Hawaii
Napoli
```

## Sizes

```text
S
M
L
```

## Dough

```text
Classic
Gluten-Free
```

## Ingredients

At minimum:

```text
Tomato Sauce
Mozzarella
Basil
Ham
Pineapple
Olives
Cheese
Onion
Anchovy
```

## Demo rules

Examples:

```text
CHEESE extra max = 2
base Tomato Sauce not removable for selected pizzas
selected base ingredients removable where configured
Gluten-Free availability controlled by rule
specific incompatible extra combination
```

## Demo app links

Seed one `AppLinkSetting` row per audience with a placeholder URL (e.g. pointing at the CI-built APK artifact or a local file server) so the "Get the app" UI and QR codes have something to render out of the box:

```text
CUSTOMER → placeholder APK URL
KITCHEN  → placeholder APK URL
```

Never commit real staff passwords. Seed development users (including at least one demo registered customer account, alongside guest checkout) through environment-driven bootstrap or a documented local-only script.

---

# 37. Future Extensions

Keep these outside the first implementation but preserve architecture options.

## Native iOS, Play Store distribution, dedicated staging VM, dedicated email provider

Deferred per §3.2/§7.9/§27: native iOS apps, Play Store publishing for the Android apps (APK direct-download only for now), a persistent staging Hetzner VM, and swapping Gmail SMTP for a dedicated transactional email provider (SendGrid, SES, etc.). None of these change a backend module boundary when adopted later.

## Payments

Add a `payment` module. Do not place card processing inside Order Module.

## Delivery

Add delivery address and fulfillment module.

## Multi-branch

Introduce:

```text
Branch
branch-specific catalog
branch-specific rule overrides
branch-specific price overrides
branch-specific kitchen queue
```

Do not add branch columns everywhere before the requirement exists.

## Analytics

Publish/query operational data without coupling reporting directly to write models.

## ML

Potential future uses:

- next-best valid option,
- ranking valid alternatives,
- anomaly detection for unusual configurations,
- quote win prediction in a full CPQ solution,
- detecting patterns that suggest missing rules.

ML recommendations must still pass deterministic rule validation.

---

# 38. Final Architectural Principle

When making an implementation decision, optimize in this order:

1. **Correct business behavior**
2. **Deterministic and explainable validation**
3. **Clear module ownership**
4. **Security**
5. **Testability**
6. **Operational simplicity**
7. **Performance**
8. **Future extensibility**

Do not add distributed-system complexity simply to appear more scalable.

For the current project, the preferred path is:

```text
simple deployment
+ strong modular boundaries
+ deterministic rules
+ conditional AI
+ real PostgreSQL
+ production-grade tests
+ observable containers
```

That is the target deployable Pizza Configurator.
