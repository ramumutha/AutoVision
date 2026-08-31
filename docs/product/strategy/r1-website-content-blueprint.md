# R1 Website Content Blueprint

**Record:** R1-WEBSITE-CONTENT-BLUEPRINT-7  
**Status:** FROZEN — ENGLISH CONTENT BASELINE FOR IMPLEMENTATION  
**Work mode:** PRODUCT / DISCOVERY  
**Scope:** English-first public SPENMER website content and information architecture

This is the frozen English content baseline for the next website implementation
phase. Content freeze does not mean visual approval, public launch approval,
legal approval, product commercial-availability approval, or proof approval.
Public launch, legal content, final brand approval, approved media, secure lead
intake, and production Product Demo configuration remain **DEFERRED** or
**RESEARCH REQUIRED** as stated in the internal notes below.

The earlier website records remain useful for technical and visual constraints:
[commercial website strategy](commercial-website-strategy.md), [UX foundation](commercial-website-ux-foundation.md),
and [public website foundation](../../architecture/public-website-foundation.md).
This record supersedes their provisional corporate storytelling where this
blueprint names SPENMER, AutoVision, Service Profit, navigation, or CTA copy.

## 1. Content Principles

SPENMER is the corporate/master brand with two pillars:

1. **Technology Products**
2. **Professional Services**

AutoVision is SPENMER's automotive service-intelligence product platform.
Service Profit is a commercially consumable capability under AutoVision. It is
not a separate corporate product and it must not be presented as a DMS.

The public narrative follows this logic:

> Problem -> Value -> How SPENMER helps -> Evidence and trust -> Next action

Use short paragraphs, meaningful headings, neutral international English, and
plain explanations before technical terms. Do not lead with AI, acronyms,
architecture, or unsupported outcomes. Keep people in control wherever the
product story concerns a customer or operational decision.

### Public copy and internal notes

Every content block in this record is explicitly classified. Only text marked
**PUBLIC COPY** is eligible to render on the website. **INTERNAL NOTE**,
**STATUS**, and **PROOF REQUIREMENT** text is editorial governance for the
implementation and review teams; it must not be rendered as customer-facing
copy.

### Truth and status rules

- **IMPLEMENTED:** supported by the current repository product or website
  evidence.
- **PLANNED:** approved direction for a future implementation or content pass,
  not proof that the capability exists today.
- **DEFERRED:** intentionally outside this content/implementation pass.
- **RESEARCH REQUIRED:** evidence or a product decision is needed before a
  stronger public claim.
- Mark any material future credibility gap with **[PROOF REQUIRED]**.
- The current Service Profit dataset is `SYNTHETIC_DEMO_ONLY`, uses India/INR
  demo context, and does not prove production outcomes.

## 2. Brand and Language Freeze

### Corporate proposition

**SPENMER builds domain-focused products and brings practical technology
expertise to complex operational challenges.**

**INTERNAL NOTE:** The corporate hierarchy and two-pillar proposition remain
the source of the homepage narrative. Do not add a second hero proposition.

### Brand hierarchy

```text
SPENMER
  Products
    AutoVision
      Service Profit
  Professional Services
```

Use **SPENMER** in the corporate header and homepage hero. Use **AutoVision**
as the platform name. Use **Service Profit** as the public capability name.
Keep `/service-profit-ai` as a technical compatibility route only; do not put
`AI` in the public product name merely for emphasis.

### Frozen CTA vocabulary

Use only the following baseline labels unless a page has a clear semantic need:

- **Contact Us**: corporate conversation
- **Request a Demo**: product evaluation
- **Explore AutoVision**: product discovery
- **Explore Service Profit**: capability discovery
- **Discuss Your Requirement**: professional-services conversation
- **Register Interest**: planned portfolio item
- **Return to SPENMER**: recovery/navigation

`Product Demo` is an authorized product-access hub, not a sales-request label.

## 3. Final Site Map

### Primary navigation

```text
SPENMER
Products ▾
Services ▾
How We Work
Trust & Security
About Us
Product Demo ▾
Contact Us →
```

### Products menu

| Route | Label | Status | Role |
| --- | --- | --- | --- |
| `/products` | Products | KEEP | Portfolio overview |
| `/autovision` | AutoVision | KEEP | Platform story |
| `/service-profit-ai` | Service Profit | KEEP | Compatibility route for current capability |
| Future approved product route | Not named yet | DEFER | Do not invent a product |

### Services menu

| Route | Label | Status | Role |
| --- | --- | --- | --- |
| `/services` | Technology Advisory & Engineering Services | KEEP | Practices and engagement boundaries |
| `/how-we-work` | How We Work | KEEP | Shared delivery model |

### Product Demo menu

| Route | Label | Status | Role |
| --- | --- | --- | --- |
| `/product-demo` | Request a Demo | KEEP | Commercial evaluation entry point |
| `/product-demo` | Authorized Product Access | KEEP | Access explanation and configured handoff |

The implementation may retain one access-hub route while showing these as two
clearly separated journeys. Anonymous visitors must not be implied to have
product access.

### Footer

```text
Products: AutoVision, Service Profit
Services: Technology Advisory & Engineering Services, How We Work
Company: About Us, Trust & Security, Contact Us
Evaluation: Request a Demo, Product Demo
Legal: Privacy, Terms
```

Privacy and Terms are **LEGAL CONTENT REQUIRED BEFORE PUBLIC LAUNCH**. Do not
fill those routes with invented legal text.

### System and utility routes

| Route/surface | Status | Direction |
| --- | --- | --- |
| `/404` | SYSTEM | Clear recovery with **Return to SPENMER** |
| `/privacy` | SYSTEM | Legal content required before launch |
| `/terms` | SYSTEM | Legal content required before launch |
| Consent/preferences | SYSTEM / DEFER | Add only with approved data-flow and legal decision |
| Authorized product handoff | SYSTEM | Existing AutoVision authentication boundary; no local auth |

## 4. Complete Homepage Copy

The following is the frozen rendered order. Presentation labels are content
guidance, not CSS instructions.

### Section 1: Corporate hero

**Presentation:** EDITORIAL TEXT with restrained corporate/product media.  
**PUBLIC COPY:**  
**Brand promise eyebrow:** TECHNOLOGY ENGINEERED AROUND REAL DECISIONS.  
**H1:** Technology solutions built around real business problems.  
**Lead:** SPENMER builds domain-focused technology products and brings product,
engineering and domain expertise to complex operational challenges.

**Brand promise:** Technology engineered around real decisions. This does not
belong in the persistent header.

**Brand philosophy:** Different by strength. United by purpose. This is reserved
for About Us and appropriate corporate context; it is not homepage hero copy.

**Discovery links:** Explore Products; Professional Services. These are
editorial discovery links, while Contact Us remains the strong global header
action.

### Section 2: Value strip and What We Do

**Presentation:** SPLIT CONTENT, with two editorial columns rather than giant
cards.  
**PUBLIC COPY:**  
**Value principles:** Purpose-built Products; Domain + Engineering; Trust by
Design; Outcome Focused. These describe SPENMER principles and capabilities,
not measured customer achievements.  
**Heading:** Two ways to create meaningful change.  
**Supporting line:** Products where recurring problems benefit from a repeatable
solution. Expertise where the challenge needs a tailored response.  
**PUBLIC COPY:** Some challenges need a product that makes important work more
repeatable. Others need experienced people to clarify a unique problem, shape
the solution, and deliver change. SPENMER works through both pillars.

**Technology Products**  
SPENMER builds domain-focused products that turn complex operational signals
into clearer decisions and repeatable workflows.

CTA: **Explore AutoVision**.

**Professional Services**  
SPENMER brings product thinking, domain knowledge, engineering and delivery
discipline to complex business and technology challenges.

CTA: **Explore Services**. The governed service-conversion CTA remains
**Discuss Your Requirement** elsewhere on the homepage.

### Section 3: Featured product

**Presentation:** PRODUCT REPRESENTATION plus editorial explanation.  
**Eyebrow:** SPENMER Product · AutoVision  
**PUBLIC COPY:**  
**Heading:** Turn service signals into decisions teams can act on.  
**PUBLIC COPY:** Vehicle-service operations generate recommendations, history,
timing and commercial signals across different systems and workflows.

AutoVision brings those signals into a clearer decision context, helping teams
understand what deserves attention, why it matters and what action should be
considered next. Evidence remains visible and people remain in control.

**AutoVision**  
Adaptive Vehicle Service Intelligence Platform

The homepage product representation uses synthetic-safe opportunity examples
and communicates the decision story as: see the signal, understand the context,
and decide the next action. It includes the integration message that AutoVision
works with existing service and dealer technology environments through defined
integration boundaries, and notes vehicle/powertrain breadth without presenting
unsupported customer metrics.

**INTERNAL NOTE:** Do not lead this homepage section with ADI, NBSA, or SOE.
Those supporting concepts belong below the initial business explanation on the
dedicated AutoVision page.

CTA: **Explore AutoVision**.

### Section 4: Service Profit

**Presentation:** WORKFLOW, using the five textual stages below as the content
structure; do not repeat this as a second decorative process diagram.  
**Eyebrow:** A current AutoVision capability  
**PUBLIC COPY:**  
**Heading:** Turn overlooked service opportunity into actionable work.  
**PUBLIC COPY:** Service Profit brings declined, deferred, due, and other
service signals into one reviewable view, helping service leaders prioritise
opportunities, understand the supporting evidence, and decide what deserves
follow-up.

**PUBLIC COPY:** Service Profit supports review and prioritisation. Human review
remains important, and identifying an opportunity does not guarantee
conversion.

**PUBLIC COPY:** The team can distinguish identified opportunity, reviewed
opportunity, follow-up activity, and eventual service outcome.

**INTERNAL NOTE:** Current R1 demonstrates manager-side identification and
review with synthetic data. Customer outreach, consent, follow-up, and
disposition are not implemented and must not be presented as live functionality.
ROI, revenue, margin, conversion, and customer outcomes require evidence.

**PROOF REQUIREMENT:** Customer workflow evidence and agreed measurement.

```text
Service data -> Opportunity identified -> Review -> Follow-up -> Service outcome
```

**INTERNAL NOTE:** The current product demonstrates identification,
prioritisation, evidence, suppression, and review-required safeguards. Keep
this implementation detail in product notes, not primary public copy.

CTA: **Request a Demo**. Secondary contextual link: **Explore Service Profit**.

### Section 5: Professional Services

**Presentation:** LIST with a strong introduction and six concise practice
rows, not six equal promotional cards.  
**PUBLIC COPY:**  
**Heading:** Practical expertise for difficult technology decisions  
**PUBLIC COPY:** SPENMER's services extend the product mindset to challenges
that need discovery, engineering, quality, delivery, or specialist domain
judgement. We work with the people who own the outcome and help create clearer
decisions, useful artefacts, and a path to delivery.

Practice labels: **Automotive & Dealer Technology**, **Product & Solution
Engineering**, **Quality Engineering & Test Automation**, **Product Definition &
Requirements Engineering**, **AI & Digital Transformation**, and **FinTech
Solution Advisory**.

CTA: **Discuss Your Requirement**.

### Section 6: Business value

**Presentation:** METRIC/PROOF AREA without invented numbers.  
**PUBLIC COPY:**  
**Heading:** Measure the change, not just the delivery  
**Copy:** A credible business case begins with a baseline and a result worth
measuring. SPENMER helps make the intended outcome explicit, identify the
indicators that can show progress, and learn from what implementation reveals.

**Homepage refinement:** Measure from a clear baseline. Define what should
change, track meaningful indicators and use the result to guide what happens
next.

**Measurement sequence:** Baseline -> Desired outcome -> Measurable indicators
-> Implementation result -> Learning and improvement.

**INTERNAL NOTE:** Do not publish a recovery percentage, ROI figure, or customer
outcome until it is supported by an approved evidence record.
**PROOF REQUIREMENT:** Approved evidence record.

### Section 7: Trust and integration

**Presentation:** EDITORIAL TEXT with a compact proof list.  
**PUBLIC COPY:**  
**Heading:** Confidence comes from clear boundaries  
**Copy:** SPENMER works with existing technology environments through explicit
integration boundaries. Access is controlled, evidence remains reviewable, and
customer or dealer environments remain separated. The aim is useful change
with accountable people and understandable system responsibilities, not
unnecessary replacement.

Proof list: Existing-environment integration; controlled identity and access;
clear data-handling boundaries; tenant/customer separation where applicable;
auditability; deployment boundaries; human accountability.

**INTERNAL NOTE:** No certification claim is made.
**PROOF REQUIREMENT:** Security review, integration examples, and deployment
evidence.

### Section 8: How we work

**Presentation:** WORKFLOW, concise on the homepage.  
**PUBLIC COPY:**  
**Heading:** A practical path from question to improvement  
**Copy:** We begin with the problem and stay close to the outcome. The work
moves through five connected stages:

1. **Understand** Listen, clarify the problem, understand the context, and define the outcome that matters.
2. **Design** Shape the right product, solution and delivery approach.
3. **Deliver** Turn the design into usable technology and operational change.
4. **Measure** Evaluate what changed against the intended outcome.
5. **Improve** Use what we learn to strengthen what comes next.

**Messaging decisions:** The operating-model alternatives Discover -> Build ->
Launch -> Scale -> Evolve; Listen -> Solve -> Deploy -> Quantify -> Refine;
Define -> Craft -> Ship -> Track -> Iterate; and Audit -> Prototype -> Release
-> Analyze -> Optimize were rejected. The SPENMER model remains Understand ->
Design -> Deliver -> Measure -> Improve. The hero visual identity refinement,
expressing complexity -> interpretation -> clear direction, is deferred.

**INTERNAL NOTE:** The full operating model belongs on the How We Work page;
do not add a competing homepage CTA here.

### Section 9: Final conversion

**Presentation:** CTA BAND.  
**PUBLIC COPY:**  
**Heading:** Have a product, technology, or operational challenge worth solving?
  
**Copy:** Tell us what matters, where the difficulty sits, and what a useful
outcome would look like. We will route the conversation to the right SPENMER
product or practice.

CTA: **Contact Us**.

## 5. Products Page

**Route:** `/products`  
**STATUS:** KEEP  
**Presentation:** EDITORIAL TEXT followed by a restrained portfolio list and
proof/status notes.

**PUBLIC COPY:**  
**Eyebrow:** Technology Products  
**H1:** Products for clearer operational decisions  
**Lead:** SPENMER builds domain-focused products for problems where better
visibility, accountable decisions, and practical workflow matter. AutoVision is
the current primary platform; Service Profit is its current commercially
consumable capability.

### AutoVision

**INTERNAL NOTE:** Role: Primary technology product platform.  
**PUBLIC COPY:** Vehicle-service teams need to interpret fragmented evidence and
decide what deserves attention without losing human accountability.  
**STATUS:** **IMPLEMENTED** platform and R1 product direction. Commercial
availability remains a product-owner decision.  
**PUBLIC COPY:** Connects service intelligence with operational workflow, evidence,
review, and next-action support.  
**Action:** **Explore AutoVision**.

### Service Profit

**INTERNAL NOTE:** Role: Current capability under AutoVision.  
**PUBLIC COPY:** Potential service work can remain hidden across declined,
deferred, due/overdue, and inactive-customer signals.  
**STATUS:** **IMPLEMENTED** R1 authenticated manager capability with synthetic
demo evidence. Commercial availability remains a product-owner decision.  
  
**Value:** Identifies and prioritises reviewable service opportunities with
evidence, commercial potential, suppression, and review-required safeguards.
  
**Action:** **Explore Service Profit** or **Request a Demo**.

### Future portfolio

No additional products are named in this release. Future approved products may
be labelled **Planned** or **Future** only after a product decision and evidence
record exist. Use **Register Interest** for a genuinely approved planned item;
do not create a placeholder product merely to make the portfolio look larger.

## 6. AutoVision Page

**Route:** `/autovision`  
**STATUS:** KEEP  
**Presentation:** PRODUCT MEDIA, SPLIT CONTENT, WORKFLOW, and proof list.

**PUBLIC COPY:**  
**Eyebrow:** AutoVision  
**H1:** Turn service signals into decisions teams can act on.  
**Lead:** AutoVision helps vehicle-service organisations bring fragmented
service evidence into a clearer decision flow so teams can understand what
matters, why it matters and what action should be considered next. Evidence
remains visible. People remain in control.

### The service problem

**PUBLIC COPY:** Service records contain recommendations, customer history, vehicle context,
timing, and commercial signals, but those signals do not automatically become
a shared operational priority. Important work can be deferred, declined,
overdue, or simply difficult to follow through.

**PROOF REQUIREMENT:** Customer research and workflow evidence.

### What AutoVision is

**PUBLIC COPY:** AutoVision is an adaptive vehicle service intelligence platform. It connects
service intelligence to operational workflow while preserving evidence,
explanation, authorization, and review boundaries. It is not a DMS and does not
require a claim that every existing system should be replaced.

### Who it helps

**PUBLIC COPY:** The current product helps dealership managers. AutoVision is
relevant to aftersales leadership, service managers, automotive technology
leaders, dealer groups, enterprise product leaders, and integration partners.

**INTERNAL NOTE:** The broader role-specific buyer story beyond the current
manager workflow is **RESEARCH REQUIRED**.

### From signal to decision

**PUBLIC COPY:** AutoVision helps teams observe service signals,
understand their meaning, decide what merits attention, act through an
appropriate workflow, verify what happened, and improve the next decision.

The product concepts may then be named:

- **Adaptive Decision Intelligence (ADI):** intelligence that adapts to the
  available evidence and supports a bounded operational decision.
- **Next Best Service Action (NBSA):** a recommended next step grounded in the
  available service context, not an autonomous instruction.
- **Service Outcome Effectiveness (SOE):** a way to connect reviewed action to
  the service outcome and learning that follows.

**INTERNAL NOTE:** These acronyms are supporting terminology, not hero copy. Product-owner review
is required before public acronym expansion is frozen.

### Evidence and human control

**PUBLIC COPY:** AutoVision keeps the reason for an opportunity or recommendation visible. It can
show evidence strength, required review, and suppression so that teams do not
act on an ambiguous or explicitly protected case. Automated tests establish
product behaviour; they do not establish production accuracy, customer value,
or market fit.

**PROOF REQUIREMENT:** Approved external product captures and workflow evidence.

### Integration and breadth

**PUBLIC COPY:** AutoVision works across dealer and enterprise environments
through portable, DMS-neutral boundaries. Vehicle and powertrain
breadth, provider coverage, market support, and integration effort require
approved evidence.

**PROOF REQUIREMENT:** Vehicle/powertrain applicability, provider coverage,
market support, and integration effort.

### Current capabilities

**PUBLIC COPY:** AutoVision includes opportunity detection, a manager summary
and queue, grouping and review, contextual explanation, suppression safeguards,
review-required safeguards, and responsive manager access. Service Profit is
the current capability.

**INTERNAL NOTE:** Do not present customer outreach, follow-up, rich
longitudinal analytics, search across the complete opportunity dataset, or
production KPI results as implemented. Their inclusion as future public
capability requires separate product and evidence decisions.

CTA: **Explore Service Profit**. Secondary: **Request a Demo**.

## 7. Service Profit Page

**Route:** `/service-profit-ai` for compatibility; public label **Service Profit**  
**STATUS:** KEEP  
**Presentation:** SPLIT CONTENT, WORKFLOW, PRODUCT MEDIA, METRIC/PROOF AREA,
and CTA BAND.

**PUBLIC COPY:**  
**Eyebrow:** A current AutoVision capability  
**H1:** Turn overlooked service opportunity into actionable work.  
**Lead:** Service Profit brings declined, deferred, due and other service
signals into one reviewable view, helping service leaders prioritise
opportunities, understand the supporting evidence and decide what deserves
follow-up.

### 1. The missed-service problem

**PUBLIC COPY:** Declined work, deferred recommendations, due and overdue service, and inactive
customer signals can be difficult to connect into one reviewable picture.
Service Profit addresses the visibility and prioritisation problem; it does
not claim that every identified opportunity will convert.

### 2. Why existing service data matters

**PUBLIC COPY:** The useful signal often already exists in service activity and related records.
The value begins by making that signal understandable and separating supported
evidence from missing or partial context.

### 3. What Service Profit surfaces

**PUBLIC COPY:** The current manager experience surfaces opportunity type, commercial potential,
supporting context, recommended action, suppression, and review-required
states. Currency and data-capability boundaries remain visible where the
current evidence is partial.

### 4. Human review

**PUBLIC COPY:** A manager reviews why an opportunity was identified and whether it is
appropriate to act. Suppressed and review-required cases are safeguards, not
obstacles to hide. Service Profit supports the decision; it does not make an
autonomous customer-contact decision.

### 5. Follow-up and service outcome

**PUBLIC COPY:** The intended business journey is review, authorised follow-up, and an observed
service outcome.

**INTERNAL NOTE:** The current R1 product does not implement customer outreach,
consent, follow-up, or disposition. Present these as a future workflow to
evaluate, not as shipped capability.

### 6. Measurement

**PUBLIC COPY:** Agree a baseline, define what counts as reviewed opportunity and service
outcome, and track the indicators that matter for the customer environment.

**PROOF REQUIREMENT:** Recovery rate, revenue, margin, conversion, and ROI.

### 7. Evidence boundary

**INTERNAL NOTE:** The current demonstration uses synthetic data and is classified
`SYNTHETIC_DEMO_ONLY`. It demonstrates product behaviour and safety states, not
production dealer results. Cost coverage is partial, gross-profit attribution
is partial, and the opportunity context is a Service Profit projection rather
than canonical Customer or Vehicle master data.

### 8. Demo action

**Request a Demo** is the primary action. Explain that a demo can show the
manager review flow and synthetic scenarios; it does not grant anonymous product
access or promise a production result.

## 8. Professional Services Page

**Route:** `/services`  
**STATUS:** KEEP  
**PUBLIC COPY:**  
**Eyebrow:** Technology Advisory & Engineering Services  
**H1:** Technology expertise focused on the outcome.  
**Lead:** SPENMER combines domain knowledge, product thinking, engineering and
delivery discipline to help organisations solve complex business and technology
problems.

**INTERNAL NOTE:** Each practice retains the structured definition of customer
problem, SPENMER contribution, likely outcome, audience, boundary, and action
for editorial governance. The website should primarily answer what problem
SPENMER can bring expertise to and what it can help accomplish; do not render
the structure mechanically for all six practices.

**PUBLIC COPY:** The six practice descriptions below provide examples of the
problems SPENMER can help solve and the outcomes a conversation can clarify.
**INTERNAL NOTE:** They are service positioning statements, not promises of
staffing capacity or a fixed delivery method.

### Automotive & Dealer Technology

**PUBLIC COPY - Problem:** Automotive and dealer organisations need technology decisions that
respect service operations, evidence quality, integration realities, and the
people who run the work.  
**PUBLIC COPY - SPENMER brings:** Automotive domain orientation, service-process thinking,
and practical translation between business and technology teams.  
**PUBLIC COPY - Likely outcome:** A clearer operating problem, integration context, and
prioritised path for improvement.  
**PROOF REQUIREMENT:** Evidence of the resulting improvement.
**PUBLIC COPY - For:** Dealer/aftersales leaders, automotive technology leaders, and partners.
  
**INTERNAL NOTE - Boundary:** We do not imply OEM endorsement, DMS ownership, market coverage,
or a production result without evidence.  
**Action:** **Discuss Your Requirement**.

### Product & Solution Engineering

**PUBLIC COPY - Problem:** Important product ideas can remain disconnected from user need,
technical constraints, and a deliverable path.  
**PUBLIC COPY - SPENMER brings:** Product framing, solution design, engineering,
and delivery discipline.  
**PUBLIC COPY - Likely outcome:** A clearer solution direction, usable increments, and clearer
ownership.  
**PROOF REQUIREMENT:** Evidence of the resulting solution outcome.
**PUBLIC COPY - For:** Product, transformation, and engineering leaders.  
**INTERNAL NOTE - Boundary:** Scope, technology, and delivery commitments require discovery.
  
**Action:** **Discuss Your Requirement**.

### Quality Engineering & Test Automation

**PUBLIC COPY - Problem:** Teams need confidence that important workflows behave correctly as
systems change.  
**PUBLIC COPY - SPENMER brings:** Quality thinking, risk-based test design, automation, and
evidence-oriented validation.  
**PUBLIC COPY - Likely outcome:** Better visibility of release risk and repeatable validation
evidence.  
**PROOF REQUIREMENT:** Evidence of the resulting validation improvement.
**PUBLIC COPY - For:** Quality, engineering, and delivery leaders.  
**INTERNAL NOTE - Boundary:** Automation does not replace product judgement, exploratory work,
or customer acceptance.  
**Action:** **Discuss Your Requirement**.

### Product Definition & Requirements Engineering

**PUBLIC COPY - Problem:** Ambiguous requirements create rework, conflicting expectations,
and weak acceptance decisions.  
**PUBLIC COPY - SPENMER brings:** Structured problem definition, requirements,
traceability, and practical acceptance thinking.  
**PUBLIC COPY - Likely outcome:** A shared problem statement, decision record, and testable
outcome definition.  
**PROOF REQUIREMENT:** Evidence of the resulting definition and acceptance quality.
**PUBLIC COPY - For:** Product owners, business analysts, transformation leaders, and
engineering teams.  
**INTERNAL NOTE - Boundary:** Discovery outputs are not a guarantee of implementation funding
or business value.  
**Action:** **Discuss Your Requirement**.

### AI & Digital Transformation

**PUBLIC COPY - Problem:** AI and digital initiatives can start with technology novelty rather
than a safe, measurable business use.  
**PUBLIC COPY - SPENMER brings:** Outcome-led discovery, domain context, evidence boundaries,
human accountability, and disciplined delivery.  
**PUBLIC COPY - Likely outcome:** A clearer use case, evaluation approach, and next decision.  
**PUBLIC COPY - For:** Transformation, product, technology, and quality leaders.  
**INTERNAL NOTE - Boundary:** No autonomous-decision, accuracy, or transformation-scale claim
is made without evidence.  
**Action:** **Discuss Your Requirement**.

### FinTech Solution Advisory

**PUBLIC COPY - Problem:** Financial technology decisions must balance customer value,
controls, integration, operational risk, and delivery reality.  
**PUBLIC COPY - SPENMER brings:** Solution advisory, product and engineering perspective, and
attention to evidence, quality, and accountable operations.  
**PUBLIC COPY - Likely outcome:** A better-defined solution path and decision boundary.  
**PUBLIC COPY - For:** FinTech and technology decision-makers.  
**INTERNAL NOTE - Boundary:** No regulatory, compliance, certification, or financial outcome
claim is implied.  
**Action:** **Discuss Your Requirement**.

## 9. How We Work Page

**Route:** `/how-we-work`  
**STATUS:** KEEP  
**Presentation:** WORKFLOW with one explanatory section per stage.

**PUBLIC COPY:**  
**Eyebrow:** How We Work  
**H1:** Understand the problem. Deliver the change. Learn from the result.  
**Lead:** SPENMER's model keeps business context, product thinking, engineering
discipline, and measurement connected from the first conversation to the next
improvement.

### Understand

We clarify the context, people, evidence, constraints, and decision that
matters. This prevents activity from being mistaken for progress. The customer
brings domain context and access to the right decision-makers. **Output:** a
shared problem statement, baseline, and initial success indicators.

### Design

We shape a practical response that fits the problem and its boundaries. This
may be a product slice, service engagement, workflow, or integration approach.
The customer reviews trade-offs and chooses the useful scope. **Output:** an
agreed direction, assumptions, risks, and acceptance view.

### Deliver

We turn the agreed direction into usable change through disciplined engineering,
quality, communication, and review. The customer provides timely feedback and
acceptance decisions. **Output:** a delivered increment or service result with
the evidence needed to review it.

### Measure

We compare what happened with the baseline and desired outcome using indicators
that are meaningful in the customer's environment. The customer validates the
interpretation and business relevance. **Output:** an implementation result and
an explicit view of what is known, partial, or unproven. **[PROOF REQUIRED]**

### Improve

We use operation, feedback, and evidence to decide what should change next.
Improvement may mean extending the solution, adjusting the workflow, stopping
an ineffective path, or researching an unanswered question. **Output:** a
prioritised learning and improvement decision.

CTA: **Discuss Your Requirement**.

## 10. Trust & Security Page

**Route:** `/trust-security`  
**STATUS:** KEEP  
**Presentation:** EDITORIAL TEXT, boundary list, and proof register links.

**PUBLIC COPY:**  
**Eyebrow:** Trust & Security  
**H1:** Built for trust. Clear about responsibility.  
**Lead:** Trust is practical: know what enters a system, who can access it,
where responsibilities sit, what evidence is retained, and how people remain
accountable for important decisions.

### Practical integration

**PUBLIC COPY:** SPENMER works with existing technology environments through
explicit, manageable integration boundaries. Specific integrations, provider
coverage, and production deployment examples require approved evidence.

**PROOF REQUIREMENT:** Integration proof.

### Secure access

**PUBLIC COPY:** Appropriate users should have controlled access to the right
environment and information. Product access hands off to the existing
AutoVision boundary.

**INTERNAL NOTE:** The public website does not authenticate users or expose
protected product data.

### Clear data boundaries

**PUBLIC COPY:** Make responsibilities around customer and dealer information
understandable without exposing sensitive architecture. Customer and dealer
environments remain separated where the product contract requires it.

**INTERNAL NOTE:** Exact retention, residency, and processing commitments are
**LEGAL CONTENT REQUIRED BEFORE PUBLIC LAUNCH**.

### Human accountability

**PUBLIC COPY:** Important operational decisions remain understandable and
accountable. Evidence, explanations, review-required states, suppression, and
responsible human action support that principle.

**INTERNAL NOTE:** Do not claim autonomous decisions or a certification that is
not recorded.

CTA: **Discuss Your Requirement**.

## 11. About Us Page

**Route:** `/about-us`  
**STATUS:** KEEP  
**Presentation:** EDITORIAL TEXT with a concise principles list.

**PUBLIC COPY:**  
**Eyebrow:** About SPENMER  
**H1:** Different by strength. United by purpose.  
**Lead:** SPENMER brings different minds and different strengths to meaningful
business and operational problems. We build domain-aware products and provide
the practical expertise needed to make technology useful, understandable, and
accountable.

### Products and professional services

Products make valuable decisions and workflows more repeatable. Professional
services help organisations understand difficult problems, shape solutions,
and deliver change. Together they express the same purpose: solve the problem
behind the feature.

### Operating principles

- Start with the outcome that matters.
- Keep evidence visible.
- Respect domain context.
- Make boundaries and trade-offs clear.
- Keep people accountable for important decisions.
- Learn from what happens in operation.

The supporting idea may appear selectively as: **Different minds. Different
strengths. One purpose.** The SPENMER acronym interpretation
(Strength, Purpose, Expertise, Network, Minds, Evolution, Results) is optional
brand material, not required primary narrative copy.

Do not claim global offices, customer counts, workforce size, awards,
partnerships, certifications, or market leadership without approved evidence.

## 12. Contact Us Page

**Route:** `/contact-us`  
**STATUS:** KEEP  
**Presentation:** EDITORIAL TEXT plus a short accessible form.

**PUBLIC COPY:**  
**Eyebrow:** Contact Us  
**H1:** Start with the challenge worth solving  
**Lead:** Tell us whether you are exploring a product, a services engagement, a
partnership, or a general enquiry. We will use the context to route the
conversation to the right SPENMER team.

### Field labels

- Name
- Work email
- Organisation
- Purpose: Product Demo; Services; Partnership; General Enquiry
- What would you like to solve?
- Optional product or service context
- Consent to be contacted about this enquiry

Primary CTA: **Contact Us** or **Send Enquiry** only after the governed form
submission contract is approved. The frozen corporate vocabulary prefers
**Contact Us** for the route and **Request a Demo** for the product path.

Confirmation direction: Thank the visitor, confirm that the enquiry was
received, restate the selected purpose without exposing sensitive details, and
explain the next human follow-up step. Failure direction: preserve entered
values where appropriate, explain that nothing was lost or submitted twice,
and offer a retry or alternate contact route.

Microcopy direction: state what the information is used for, link to approved
Privacy content, and avoid promising a response time until one is governed.
**LEGAL CONTENT REQUIRED BEFORE PUBLIC LAUNCH.** This is not legal policy text.

## 13. Product Demo Page

**Route:** `/product-demo`  
**STATUS:** KEEP  
**Presentation:** SPLIT CONTENT with two clearly separated journey choices.

**PUBLIC COPY:**  
**Eyebrow:** Product Demo  
**H1:** Choose the right way into AutoVision  
**Lead:** A product evaluation and authorised product access are different
journeys. Choose the one that matches what you need.

### Request a Demo

For a prospective customer, partner, or evaluator. A SPENMER conversation can
show the relevant product or capability, explain current boundaries, and agree
what evidence would be useful.  
CTA: **Request a Demo** -> `/contact-us?purpose=PRODUCT_DEMO`.

### Authorized Product Access

For an approved, time-bound user with a configured access destination. This
journey hands off to the existing AutoVision authentication boundary; it does
not create anonymous access, local authentication, or a public tenant.  
CTA: **Authorized Product Access** -> configured access destination when valid;
otherwise show a clear unavailable state.

## 14. Decision Blueprints

### For Dealers: REMOVE / MERGE

**STATUS:** REMOVE / MERGE  
**Decision:** Merge into AutoVision and Service Profit; do not retain a
standalone `/for-dealers` route in this release.

The route does not yet have enough materially different, dealer-specific
evidence from the AutoVision and Service Profit stories. Dealer leaders remain
the priority audience, so dealer language belongs in the product pages, demo
journey, and services page. A dedicated route may return after validated role-
specific research produces distinct content for dealer principal, aftersales
leader, service manager, and advisor workflows. **RESEARCH REQUIRED.**

### Business Value: REMOVE / MERGE

**STATUS:** REMOVE / MERGE  
**Decision:** Merge the strongest measurement content into Home, AutoVision,
Service Profit, Services, and How We Work; defer standalone `/business-value`.

The current evidence supports a measurement discipline, not a unique library
of customer results. A standalone page would repeat generic principles and
invite unsupported ROI claims. Reconsider only when approved baselines,
indicators, customer outcomes, and case studies exist. **[PROOF REQUIRED]**

### Request Demo: KEEP as journey, not duplicate route

**STATUS:** KEEP  
**Decision:** Keep **Request a Demo** as the product-evaluation intent and route
it through the shared Contact Us workflow. Do not create a second lead model or
a separate competing form.

The contextual CTA preserves the referring product/capability context while the
Contact Us workflow remains the single commercial intake boundary. Submission,
notification, anti-abuse, privacy, and CRM behavior are deferred until the
governed public intake design is approved.

## 15. CTA Matrix

| Page/context | CTA | Destination | Intent | Priority |
| --- | --- | --- | --- | --- |
| Header | Contact Us | `/contact-us` | Corporate conversation | Primary |
| Header/Product Demo menu | Product Demo | `/product-demo` | Access hub | Primary |
| Home / Products | Explore AutoVision | `/autovision` | Product discovery | Primary |
| Home / AutoVision | Explore Service Profit | `/service-profit-ai` | Capability discovery | Secondary |
| Home / Service Profit | Request a Demo | `/contact-us?purpose=PRODUCT_DEMO` | Commercial evaluation | Primary |
| Home / Services | Discuss Your Requirement | `/contact-us?purpose=SERVICES` | Services conversation | Primary |
| How We Work | Discuss Your Requirement | `/contact-us?purpose=SERVICES` | Apply delivery model | Secondary |
| Trust & Security | Discuss Your Requirement | `/contact-us?purpose=GENERAL_ENQUIRY` | Integration/security discussion | Secondary |
| About Us | Contact Us | `/contact-us` | Corporate conversation | Secondary |
| Planned portfolio item | Register Interest | Approved item route/form | Future interest | Primary when applicable |
| Error/system | Return to SPENMER | `/` | Recovery | Primary |

## 16. Proof Register

These are the places where evidence would materially improve credibility. None
may be fabricated in copy.

| Content claim or surface | Proof class | Current status |
| --- | --- | --- |
| AutoVision manager decision flow | PRODUCT SCREENSHOT | Synthetic/approved external capture required |
| Service Profit opportunity review | PRODUCT SCREENSHOT | Synthetic demo capture and disclosure required |
| Service data to reviewed action | WORKFLOW EVIDENCE | Current product behavior partial; customer follow-up not implemented |
| Review, suppression, and human-control safeguards | WORKFLOW EVIDENCE | Product evidence exists; public capture approval required |
| Service Profit measurement sequence | METRIC | Baseline and customer indicator design required |
| Recovery, conversion, revenue, margin, or ROI | METRIC | **PROOF REQUIRED**; no target or result exists |
| Dealer/service leader usefulness | CUSTOMER PROOF | Research interviews or pilot evidence required |
| Production service outcome | CUSTOMER PROOF | No production outcome record |
| Customer/dealer narrative | CASE STUDY | No approved case study |
| Integration with dealer/enterprise systems | INTEGRATION PROOF | Approved interface and deployment examples required |
| DMS/provider/market breadth | INTEGRATION PROOF | Coverage and applicability research required |
| Tenant separation and access boundary | SECURITY PROOF | Public-safe architecture/security review required |
| Auditability and data handling | SECURITY PROOF | Approved public security content required |
| Certifications, awards, partnerships, scale | SECURITY PROOF / CUSTOMER PROOF | Do not claim; no evidence recorded |

## 17. Implementation Component Map

This is a recommendation for the next implementation phase. It does not refactor
`PageShellComponent` now.

### Application-level boundaries

- `PublicHeaderComponent`: brand, primary navigation, Product Demo disclosure,
  mobile keyboard behavior.
- `PublicFooterComponent`: grouped navigation, legal links, launch-stage notes,
  and removal of internal review controls before public launch.
- `PageMetadataService`: route title, description, and approved social metadata.
- `PublicContentModel`: typed page sections, statuses, CTA destinations, and
  proof labels; keep content separate from conditional shell logic.

### Page-level boundaries

- `HomePageComponent`
- `ProductsPageComponent`
- `AutoVisionPageComponent`
- `ServiceProfitPageComponent`
- `ServicesPageComponent`
- `HowWeWorkPageComponent`
- `TrustSecurityPageComponent`
- `AboutUsPageComponent`
- `ContactUsPageComponent`
- `ProductDemoPageComponent`
- `NotFoundPageComponent`

### Reusable content components

- `EditorialHeroComponent`
- `PillarIntroductionComponent`
- `ProductFeatureSectionComponent`
- `PracticeListComponent`
- `MeasurementSequenceComponent`
- `TrustBoundaryListComponent`
- `OperatingModelComponent`
- `ProofNoteComponent`
- `ContextualCtaBandComponent`
- `ContactPurposeFormComponent`
- `AuthorizedAccessPanelComponent`

Each component should own one semantic presentation responsibility and accept
typed content. Business calculations, authorization, and product access remain
outside the public content components.

## 18. Freeze Decisions

### FREEZE NOW

- SPENMER is the corporate/master brand.
- Products and Professional Services are the two corporate pillars.
- AutoVision is the platform; Service Profit is a capability under AutoVision.
- Corporate homepage hero is not automotive-only.
- Header IA and CTA vocabulary in this record.
- Homepage order and complete English baseline copy.
- `For Dealers` and `Business Value` are merged/deferred as described.
- Request a Demo and Authorized Product Access remain separate journeys.
- English-first content; no translation work in this pass.

### DEFER

- CSS, Angular implementation, PageShell refactor, and media production.
- Final logo, fonts, visual pattern, and product screenshot approval.
- Public hosting, domain, indexing, analytics, consent tooling, and CRM.
- Secure lead intake, anti-abuse, notification, and response-time commitment.
- Locale-prefixed routes, translations, and Arabic/RTL review.
- Standalone For Dealers and Business Value routes.

### REMOVE/MERGE

- Merge standalone For Dealers content into relevant product and service pages.
- Merge standalone Business Value content into measurement sections.
- Remove `AI` from the public Service Profit product name; retain the technical
  `/service-profit-ai` compatibility route.
- Remove unsupported scale, ROI, certification, partnership, and leadership
  language.
- Remove internal visual review tooling before public launch.

### PROOF REQUIRED

- Approved product media and workflow captures.
- Customer/dealer research, outcomes, and case studies.
- Production measurement, ROI, revenue, margin, and conversion evidence.
- Integration/provider/market breadth evidence.
- Public-safe security, data handling, deployment, and tenant-separation proof.
- Any claim of powertrain breadth, performance, scale, or commercial result.

### PRODUCT-OWNER DECISION REQUIRED

- Approve the SPENMER corporate proposition and final public brand treatment.
- Approve AutoVision/Service Profit naming and acronym use.
- Confirm which current capability statements may be labelled Available versus
  Early Access; this blueprint uses **IMPLEMENTED** for repository behavior and
  leaves the commercial availability label for owner approval.
- Approve the services practice promises and engagement boundaries.
- Approve the final demo evidence and synthetic-demo disclosure wording.
- Approve public launch legal, privacy, security, intake, and response language.

FROZEN — ENGLISH WEBSITE CONTENT BASELINE READY FOR HOMEPAGE IMPLEMENTATION