import { ChangeDetectionStrategy, Component, inject } from "@angular/core";
import { ActivatedRoute, RouterLink } from "@angular/router";
import { MetadataService } from "./metadata.service";

type PageKey =
  | "home"
  | "products"
  | "autovision"
  | "service-profit-ai"
  | "services"
  | "for-dealers"
  | "how-it-works"
  | "integration-security"
  | "about"
  | "business-value"
  | "legal"
  | "not-found";

@Component({
  selector: "site-page-shell",
  imports: [RouterLink],
  template: `
    @if (pageKey === "home") {
      <main class="home-page">
        <section class="home-hero" aria-labelledby="home-title">
          <div class="site-container home-hero-inner">
            <div class="home-hero-copy">
              <p class="eyebrow">Technology engineered around real decisions.</p>
              <h1 id="home-title">Technology solutions built around real business problems.</h1>
              <p class="home-hero-lead">
                SPENMER builds domain-focused technology products and brings product,
                engineering and domain expertise to complex operational challenges.
              </p>
              <nav class="home-hero-links" aria-label="Explore SPENMER">
                <a class="text-link" routerLink="/autovision">Explore Products <span aria-hidden="true">→</span></a>
                <a class="text-link" routerLink="/services">Professional Services <span aria-hidden="true">→</span></a>
              </nav>
            </div>
            <div class="home-hero-visual" aria-hidden="true">
              <svg viewBox="0 0 640 420" role="presentation" aria-hidden="true" focusable="false">
                <path class="signal-path signal-path-one" d="M24 92C146 92 170 26 292 26s142 72 324 72" />
                <path class="signal-path signal-path-two" d="M24 210h132c70 0 88-88 164-88h60c76 0 94 88 236 88" />
                <path class="signal-path signal-path-three" d="M24 328c132 0 154 66 270 66 108 0 120-98 294-98" />
                <path class="signal-rail" d="M360 72v276" />
                <circle class="signal-node" cx="360" cy="210" r="18" />
                <circle class="signal-node-inner" cx="360" cy="210" r="6" />
              </svg>
            </div>
          </div>
        </section>

        <section class="home-value-strip" aria-label="SPENMER operating principles">
          <div class="site-container">
            <ol class="home-value-list">
              <li><span>01</span><div><h2>Purpose-built Products</h2><p>Built around defined operational problems.</p></div></li>
              <li><span>02</span><div><h2>Domain + Engineering</h2><p>Business context translated into usable technology.</p></div></li>
              <li><span>03</span><div><h2>Trust by Design</h2><p>Clear responsibilities, controlled access and human accountability.</p></div></li>
              <li><span>04</span><div><h2>Outcome Focused</h2><p>Success begins with what should measurably change.</p></div></li>
            </ol>
          </div>
        </section>

        <section class="home-pillars site-section" aria-labelledby="pillars-title">
          <div class="site-container">
            <div class="home-section-heading">
              <p class="eyebrow">What We Do</p>
              <h2 id="pillars-title">Two ways to create meaningful change.</h2>
              <p>Products where recurring problems benefit from a repeatable solution. Expertise where the challenge needs a tailored response.</p>
            </div>
            <div class="pillar-layout">
              <article class="pillar pillar-product">
                <span class="home-index">01</span>
                <h3>Technology Products</h3>
                <p>SPENMER builds domain-focused products that turn complex operational signals into clearer decisions and repeatable workflows.</p>
                <a class="text-link" routerLink="/autovision"
                  >Explore AutoVision <span aria-hidden="true">→</span></a
                >
              </article>
              <article class="pillar pillar-services">
                <span class="home-index">02</span>
                <h3>Professional Services</h3>
                <p>
                  Some challenges need more than a product. SPENMER brings product
                  thinking, domain knowledge, engineering, and delivery discipline
                  to complex business and technology problems. The engagement begins
                  with the outcome that matters, not with a request for additional
                  staffing.
                </p>
                <a class="text-link" routerLink="/services"
                  >Explore Services <span aria-hidden="true">→</span></a
                >
              </article>
            </div>
          </div>
        </section>

        <section class="home-autovision site-section" aria-labelledby="autovision-home-title">
          <div class="site-container product-feature-layout">
            <div class="product-story">
              <p class="eyebrow">SPENMER Product · AutoVision</p>
              <h2 id="autovision-home-title">Turn service signals into decisions teams can act on.</h2>
              <p>
                Vehicle-service operations generate recommendations, history, timing
                and commercial signals across different systems and workflows.
              </p>
              <p>AutoVision brings those signals into a clearer decision context, helping teams understand what deserves attention, why it matters and what action should be considered next.</p>
              <p class="product-name"><strong>AutoVision</strong><span>Adaptive Vehicle Service Intelligence Platform</span></p>
              <p class="product-scope">Built for service intelligence across vehicle and powertrain contexts. <span>ICE · Hybrid · EV</span></p>
              <p class="product-integration">Works with existing service and dealer technology environments through defined integration boundaries.</p>
              <div class="decision-story" aria-label="AutoVision decision story">
                <div><strong>See the signal</strong><span>Bring relevant service information into view.</span></div>
                <div><strong>Understand the context</strong><span>See why the service need deserves attention.</span></div>
                <div><strong>Decide the next action</strong><span>Support a clear human-reviewed service decision.</span></div>
              </div>
              <a class="text-link" routerLink="/autovision"
                >Explore AutoVision <span aria-hidden="true">→</span></a
              >
            </div>
            <div class="product-ui" aria-label="Illustrative AutoVision opportunity review using synthetic demo data">
              <div class="product-ui-header">
                <span><b>AutoVision</b><small>Service intelligence</small></span><small>Illustrative review</small>
              </div>
              <div class="product-ui-summary">
                <span><small>Opportunity queue</small><strong>Service signals</strong></span>
                <span class="ui-status">Human review</span>
              </div>
              <ul class="opportunity-list">
                <li><span><b>Deferred recommendation</b><small>Vehicle 01 · Evidence context</small></span><strong>Review</strong></li>
                <li><span><b>Due service</b><small>Vehicle 02 · Timing signal</small></span><strong>Review</strong></li>
                <li><span><b>Inactive customer</b><small>Vehicle 03 · Supporting context</small></span><strong>Review</strong></li>
              </ul>
              <div class="product-ui-context"><small>Selected context</small><strong>Deferred recommendation</strong><dl><div><dt>Signal</dt><dd>Recommendation / service context</dd></div><div><dt>Evidence</dt><dd>Supporting service record and history</dd></div><div><dt>Next action</dt><dd>Review opportunity</dd></div></dl></div>
              <small class="synthetic-note">Illustrative product view using synthetic demo data.</small>
            </div>
          </div>
        </section>

        <section class="home-service-profit site-section" aria-labelledby="service-profit-home-title">
          <div class="site-container service-profit-layout">
            <div>
              <p class="eyebrow">A current AutoVision capability</p>
              <h2 id="service-profit-home-title">Turn overlooked service opportunity into actionable work.</h2>
              <p>
                Service Profit brings declined, deferred, due, and other service
                signals into one reviewable view, helping service leaders prioritise
                opportunities, understand the supporting evidence, and decide what
                deserves follow-up.
              </p>
              <p>
                Service Profit supports review and prioritisation. Human review
                remains important, and identifying an opportunity does not guarantee
                conversion.
              </p>
              <a class="button button-primary" routerLink="/request-demo"
                >Request a Demo</a
              >
              <a class="text-link secondary-link" routerLink="/service-profit-ai"
                >Explore Service Profit <span aria-hidden="true">→</span></a
              >
            </div>
            <div class="service-profit-evidence">
              <div class="evidence-label">Service Profit / review flow</div>
              <ol class="business-journey">
                @for (step of profitSteps; track step; let i = $index) {
                  <li><span>0{{ i + 1 }}</span><b>{{ step }}</b></li>
                }
              </ol>
              <p class="synthetic-note">Current demonstrations use synthetic scenarios. Customer follow-up is not automated here.</p>
            </div>
          </div>
        </section>

        <section class="home-services site-section" aria-labelledby="services-home-title">
          <div class="site-container">
            <div class="home-section-heading services-heading">
              <p class="eyebrow">Professional Services</p>
              <h2 id="services-home-title">Technology expertise focused on the outcome.</h2>
              <p>SPENMER combines domain knowledge, product thinking, engineering and delivery discipline to help organisations solve complex business and technology problems.</p>
            </div>
            <ol class="practice-index">
              @for (practice of homePractices; track practice.title; let i = $index) {
                <li><span>0{{ i + 1 }}</span><div><h3>{{ practice.title }}</h3><p>{{ practice.copy }}</p></div></li>
              }
            </ol>
            <a class="text-link" routerLink="/services"
              >Discuss Your Requirement <span aria-hidden="true">→</span></a
            >
          </div>
        </section>

        <section class="home-measurement site-section" aria-labelledby="measurement-title">
          <div class="site-container measurement-layout">
            <div>
              <p class="eyebrow quiet-eyebrow">Business value</p>
              <h2 id="measurement-title">Measure the change, not just the delivery.</h2>
            </div>
            <div>
              <p>A credible business case begins with a baseline and a result worth measuring. SPENMER helps make the intended outcome explicit, identify useful indicators, and learn from what implementation reveals.</p>
              <ol class="measurement-sequence">
                <li>Baseline</li><li>Desired outcome</li><li>Measurable indicators</li><li>Implementation result</li><li>Learning and improvement</li>
              </ol>
              <p class="quiet-proof">Measure from a clear baseline. Define what should change, track meaningful indicators and use the result to guide what happens next.</p>
            </div>
          </div>
        </section>

        <section class="home-trust site-section" aria-labelledby="trust-title">
          <div class="site-container">
            <div class="home-section-heading">
              <p class="eyebrow quiet-eyebrow">Trust &amp; Integration</p>
              <h2 id="trust-title">Built for trust. Clear about responsibility.</h2>
            </div>
            <ul class="trust-index">
              <li><h3>Secure Access</h3><p>Appropriate users should have controlled access to the right environment and information.</p></li>
              <li><h3>Clear Data Boundaries</h3><p>Responsibilities around customer and dealer information should remain understandable.</p></li>
              <li><h3>Practical Integration</h3><p>Work with existing technology environments through explicit, manageable integration boundaries.</p></li>
              <li><h3>Human Accountability</h3><p>Important operational decisions remain understandable and accountable.</p></li>
            </ul>
          </div>
        </section>

        <section class="home-how site-section" aria-labelledby="how-title">
          <div class="site-container">
            <div class="home-section-heading">
              <p class="eyebrow quiet-eyebrow">How We Work</p>
              <h2 id="how-title">A practical path from question to improvement.</h2>
              <p>We begin with the problem and stay close to the outcome.</p>
            </div>
            <ol class="operating-sequence" aria-label="SPENMER operating model">
              @for (step of homeOperatingSteps; track step.name; let i = $index) {
                <li><span>0{{ i + 1 }}</span><div><b>{{ step.name }}</b><p>{{ step.copy }}</p></div></li>
              }
            </ol>
            <a class="text-link" routerLink="/how-it-works"
              >How We Work <span aria-hidden="true">→</span></a
            >
          </div>
        </section>

        <section class="home-contact" aria-labelledby="contact-title">
          <div class="site-container contact-cta">
            <p class="eyebrow">Start with the challenge worth solving</p>
            <h2 id="contact-title">Have a product, technology, or operational challenge worth solving?</h2>
            <p>Tell us what matters, where the difficulty sits, and what a useful outcome would look like. We will route the conversation to the right SPENMER product or practice.</p>
            <a class="button button-light" routerLink="/contact">Contact Us</a>
          </div>
        </section>
      </main>
    }
    @if (pageKey !== "home") {
      @switch (pageKey) {
        @case ("home") {
          <section class="hero">
            <div class="site-container hero-grid">
              <div>
                <p class="kicker">
                  Technology products and professional services
                </p>
                <h1>Practical technology for complex business decisions.</h1>
                <p class="hero-copy">
                  We build domain-aware products and provide advisory and
                  engineering support for organisations solving real operational
                  problems.
                </p>
                <div class="actions">
                  <a class="button button-primary" routerLink="/products"
                    >Explore products</a
                  ><a class="button button-secondary" routerLink="/services"
                    >Explore services</a
                  >
                </div>
              </div>
              <div
                class="company-loop"
                aria-label="How we approach business problems"
              >
                <span class="board-label">How we work</span>
                <div class="company-steps">
                  @for (step of companySteps; track step) {
                    <b>{{ step }}</b>
                  }
                </div>
                <p>
                  Understand the problem, deliver useful capability, and learn
                  from outcomes.
                </p>
              </div>
            </div>
          </section>
          <section class="site-section band">
            <div class="site-container">
              <p class="kicker">Two commercial directions</p>
              <div class="split-cards">
                <article>
                  <span class="index">01</span>
                  <h2>Products</h2>
                  <p>
                    Technology products designed around defined operational
                    problems, beginning with AutoVision for vehicle service
                    organisations.
                  </p>
                  <a class="text-link" routerLink="/products"
                    >Explore products <span aria-hidden="true">→</span></a
                  >
                </article>
                <article>
                  <span class="index">02</span>
                  <h2>Professional services</h2>
                  <p>
                    Technology advisory and engineering across product
                    definition, architecture, integration, quality, and
                    responsible transformation.
                  </p>
                  <a class="text-link" routerLink="/services"
                    >Explore services <span aria-hidden="true">→</span></a
                  >
                </article>
              </div>
            </div>
          </section>
          <section class="site-section">
            <div class="site-container feature-grid">
              <div>
                <p class="kicker">Featured product</p>
                <h2>AutoVision makes service evidence easier to use.</h2>
                <p>
                  AutoVision is an Adaptive Vehicle Service Intelligence
                  Platform. Service Profit is its current commercially
                  consumable capability.
                </p>
                <a class="button button-primary" routerLink="/autovision"
                  >Explore AutoVision</a
                >
              </div>
              <div class="quote-panel">
                <strong>Built around real decisions</strong>
                <p>
                  Evidence remains visible, human judgement remains in control
                  where required, and outcomes can close the loop.
                </p>
              </div>
            </div>
          </section>
          <section class="site-section dark-band">
            <div class="site-container">
              <p class="kicker">Start with the right path</p>
              <h2>
                Bring us a product question, service challenge, or integration
                boundary.
              </h2>
              <a class="button button-light" routerLink="/contact"
                >Contact Us</a
              >
            </div>
          </section>
        }
        @case ("products") {
          <section class="page-intro">
            <div class="site-container">
              <p class="kicker">Products</p>
              <h1>Products shaped around operational work.</h1>
              <p>
                Our product direction starts with domain context, evidence, and
                the decisions people need to make.
              </p>
            </div>
          </section>
          <section class="site-section">
            <div class="site-container portfolio-grid">
              <article>
                <span class="status status-early">Early access</span>
                <h2>AutoVision</h2>
                <p>
                  Adaptive Vehicle Service Intelligence Platform for
                  evidence-led service decisions.
                </p>
                <a class="text-link" routerLink="/autovision"
                  >Explore AutoVision →</a
                >
              </article>
              <article>
                <span class="status status-available">Current capability</span>
                <h2>Service Profit</h2>
                <p>
                  A commercially consumable AutoVision capability for
                  identifying and reviewing service opportunities from available
                  evidence.
                </p>
                <a class="text-link" routerLink="/service-profit-ai"
                  >Explore Service Profit →</a
                >
              </article>
              <article>
                <span class="status status-planned">Planned</span>
                <h2>Future product directions</h2>
                <p>
                  Additional product opportunities require validation before
                  they become commitments or demonstrable offerings.
                </p>
                <a class="text-link" routerLink="/contact"
                  >Register interest →</a
                >
              </article>
            </div>
          </section>
        }
        @case ("autovision") {
          <section class="page-intro tinted">
            <div class="site-container">
              <p class="kicker">AutoVision platform</p>
              <h1>Adaptive Vehicle Service Intelligence Platform.</h1>
              <p>
                AutoVision helps vehicle-service organisations interpret
                available evidence and progress reviewable decisions without
                presenting itself as a DMS.
              </p>
            </div>
          </section>
          <section class="site-section">
            <div class="site-container">
              <div class="content-narrow">
                <p class="kicker">A closed loop for service intelligence</p>
                <h2>Evidence remains visible. Outcomes close the loop.</h2>
                <p>
                  AutoVision supports human judgement where required and does
                  not imply that every vehicle or source system is automatically
                  monitored.
                </p>
              </div>
              <div class="autovision-loop" aria-label="AutoVision lifecycle">
                <div class="loop-rail" aria-hidden="true"></div>
                @for (
                  step of autoVisionSteps;
                  track step.name;
                  let i = $index
                ) {
                  <article>
                    <span class="index">0{{ i + 1 }}</span>
                    <h3>{{ step.name }}</h3>
                    <p>{{ step.copy }}</p>
                  </article>
                }
              </div>
            </div>
          </section>
          <section class="site-section band">
            <div class="site-container feature-grid">
              <div>
                <p class="kicker">Current boundary</p>
                <h2>Service decisions stay explainable.</h2>
                <p>
                  Available vehicle, service, and operational evidence is
                  interpreted in context. Rules, configured policy, and human
                  review remain part of the decision boundary.
                </p>
              </div>
              <div class="plain-list">
                <p>AutoVision is not a DMS.</p>
                <p>
                  Prediction is a specific future or configured capability, not
                  a mandatory lifecycle stage.
                </p>
                <p>
                  Automatic model retraining and autonomous outreach are not
                  claimed here.
                </p>
              </div>
            </div>
          </section>
        }
        @case ("service-profit-ai") {
          <section class="page-intro dark-intro">
            <div class="site-container">
              <p class="kicker">AutoVision / Service Profit</p>
              <h1>Service Profit</h1>
              <p>
                Make service opportunity visible, reviewable, and ready for
                human follow-through.
              </p>
              <a
                class="button button-light"
                routerLink="/contact"
                [queryParams]="{
                  purpose: 'product-demo',
                  product: 'service-profit-ai',
                }"
                >Request a demo</a
              >
            </div>
          </section>
          <section class="site-section">
            <div class="site-container feature-grid">
              <div>
                <p class="kicker">The dealer problem</p>
                <h2>Useful service work can become difficult to follow.</h2>
                <p>
                  Declined, deferred, due, overdue, and inactive-customer
                  signals may be difficult to review consistently. Service
                  Profit creates a common evidence-backed view for the next
                  conversation.
                </p>
              </div>
              <div class="quote-panel">
                <p>
                  Opportunities are grounded in available service evidence and
                  governed workflow.
                </p>
                <small
                  >The Work Queue is an execution surface, not the source of
                  opportunity creation. Synthetic demo data only.</small
                >
              </div>
            </div>
          </section>
          <section class="site-section band">
            <div class="site-container">
              <p class="kicker">The business flow</p>
              <div class="process-line">
                @for (step of profitSteps; track step; let i = $index) {
                  <div>
                    <span>0{{ i + 1 }}</span
                    ><b>{{ step }}</b>
                  </div>
                }
              </div>
              <p class="process-note">
                The workflow connects service data to review, follow-up, and a
                service outcome without claiming guaranteed revenue or
                autonomous outreach.
              </p>
            </div>
          </section>
        }
        @case ("services") {
          <section class="page-intro">
            <div class="site-container">
              <p class="kicker">Professional services</p>
              <h1>Technology Advisory &amp; Engineering Services.</h1>
              <p>
                Shape a product question, quality challenge, or integration
                boundary into a clear, buildable path.
              </p>
              <a
                class="button button-primary"
                routerLink="/contact"
                [queryParams]="{ purpose: 'advisory-implementation' }"
                >Discuss your requirement</a
              >
            </div>
          </section>
          <section class="site-section">
            <div class="site-container service-grid">
              @for (
                service of servicesList;
                track service.slug;
                let i = $index
              ) {
                <article [id]="service.slug">
                  <span class="index">0{{ i + 1 }}</span>
                  <h2>{{ service.title }}</h2>
                  <p>{{ service.copy }}</p>
                  <a
                    class="text-link"
                    routerLink="/contact"
                    [queryParams]="{ purpose: 'advisory-implementation' }"
                    >Discuss this capability →</a
                  >
                </article>
              }
            </div>
          </section>
          <section class="site-section dark-band">
            <div class="site-container">
              <p class="kicker">How we engage</p>
              <h2>Start at the level of clarity you need.</h2>
              <div class="engagements">
                @for (item of engagements; track item) {
                  <span>{{ item }}</span>
                }
              </div>
            </div>
          </section>
        }
        @case ("for-dealers") {
          <section class="page-intro tinted">
            <div class="site-container">
              <p class="kicker">AutoVision audience</p>
              <h1>See more of the service work your team can act on.</h1>
              <p>
                AutoVision is intended to work alongside existing dealer
                systems. It is not a claim to replace your DMS.
              </p>
              <a
                class="button button-primary"
                routerLink="/contact"
                [queryParams]="{
                  purpose: 'product-demo',
                  product: 'service-profit-ai',
                }"
                >Request a demo</a
              >
            </div>
          </section>
          <section class="site-section">
            <div class="site-container role-grid">
              <article>
                <h2>Dealer leaders</h2>
                <p>
                  Make opportunity, follow-up, and management visibility easier
                  to discuss without losing the evidence underneath.
                </p>
              </article>
              <article>
                <h2>Aftersales teams</h2>
                <p>
                  Bring declined, deferred, due, and overdue patterns into a
                  reviewable workflow with clear ownership and safeguards.
                </p>
              </article>
              <article>
                <h2>Technology teams</h2>
                <p>
                  Explore a DMS-neutral integration boundary that isolates
                  source-system differences from product logic.
                </p>
              </article>
            </div>
          </section>
        }
        @case ("how-it-works") {
          <section class="page-intro">
            <div class="site-container">
              <p class="kicker">How we work</p>
              <h1>Understand, design, deliver, measure, improve.</h1>
              <p>
                This is a practical representation of how we approach business
                problems, not a proprietary methodology claim.
              </p>
            </div>
          </section>
          <section class="site-section">
            <div class="site-container architecture-steps">
              @for (step of companyStepCopy; track step.name; let i = $index) {
                <div>
                  <span>0{{ i + 1 }}</span>
                  <h2>{{ step.name }}</h2>
                  <p>{{ step.copy }}</p>
                </div>
              }
            </div>
          </section>
        }
        @case ("integration-security") {
          <section class="page-intro dark-intro">
            <div class="site-container">
              <p class="kicker">Trust &amp; security</p>
              <h1>Trust belongs at the product boundary.</h1>
              <p>
                Integration readiness, tenant isolation, least privilege,
                evidence traceability, and auditability are design requirements.
              </p>
            </div>
          </section>
          <section class="site-section">
            <div class="site-container principle-grid">
              @for (item of securityPrinciples; track item.title) {
                <article>
                  <h2>{{ item.title }}</h2>
                  <p>{{ item.copy }}</p>
                </article>
              }
            </div>
          </section>
        }
        @case ("about") {
          <section class="page-intro">
            <div class="site-container">
              <p class="kicker">Company</p>
              <h1>
                Products and useful engineering for real operating contexts.
              </h1>
              <p>
                SPENMER is a provisional public placeholder for a company
                direction grounded in domain-led technology products and
                professional services.
              </p>
            </div>
          </section>
          <section class="site-section">
            <div class="site-container two-up">
              <div>
                <p class="kicker">What we bring</p>
                <h2>Business meaning close to technical decisions.</h2>
              </div>
              <div>
                <p>
                  We work across product definition, architecture,
                  implementation, integration, quality, and transformation. We
                  keep claims close to evidence and treat maintainability as a
                  commercial concern.
                </p>
                <p class="review-note">
                  Legal identity, public launch, and company particulars require
                  separate review.
                </p>
              </div>
            </div>
          </section>
        }
        @case ("business-value") {
          <section class="page-intro">
            <div class="site-container">
              <p class="kicker">Business value</p>
              <h1>Measure the work before promising the outcome.</h1>
              <p>
                Define a baseline around visibility, review quality, follow-up,
                and agreed service outcomes. There are no invented ROI
                percentages here.
              </p>
            </div>
          </section>
          <section class="site-section">
            <div class="site-container value-grid">
              @for (item of valuePoints; track item.title) {
                <article>
                  <span class="index">{{ item.number }}</span>
                  <h2>{{ item.title }}</h2>
                  <p>{{ item.copy }}</p>
                </article>
              }
            </div>
          </section>
        }
        @case ("legal") {
          <section class="page-intro">
            <div class="site-container">
              <p class="kicker">Review-ready content</p>
              <h1>{{ title }}</h1>
              <p>
                This private/local review candidate does not present legal
                approval or a public launch claim.
              </p>
            </div>
          </section>
          <section class="site-section">
            <div class="site-container content-narrow">
              <h2>Privacy and terms require review.</h2>
              <p>
                Information submitted through Contact Us is used to respond to
                the enquiry and arrange the requested conversation. It is not
                used to subscribe people to marketing.
              </p>
            </div>
          </section>
        }
        @default {
          <section class="not-found site-section">
            <div class="site-container">
              <p class="kicker">404</p>
              <h1>That page is not here.</h1>
              <a class="button button-primary" routerLink="/">Return home</a>
            </div>
          </section>
        }
      }
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PageShellComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly metadata = inject(MetadataService);
  readonly title = String(this.route.snapshot.data["title"] ?? "SPENMER");
  readonly pageKey: string = this.resolvePageKey();
  readonly companySteps = [
    "Understand",
    "Design",
    "Deliver",
    "Measure",
    "Improve",
  ];
  readonly homeOperatingSteps = [
    { name: "Understand", copy: "Listen, clarify the problem, understand the context, and define the outcome that matters." },
    { name: "Design", copy: "Shape the right product, solution and delivery approach." },
    { name: "Deliver", copy: "Turn the design into usable technology and operational change." },
    { name: "Measure", copy: "Evaluate what changed against the intended outcome." },
    { name: "Improve", copy: "Use what we learn to strengthen what comes next." },
  ];
  readonly autoVisionSteps = [
    {
      name: "Observe",
      copy: "Establish what is known from available vehicle, service, and operational evidence.",
    },
    {
      name: "Understand",
      copy: "Interpret available evidence in its service and business context.",
    },
    {
      name: "Decide",
      copy: "Determine the appropriate next action using evidence, policy, rules, and human judgement where required.",
    },
    {
      name: "Act",
      copy: "Progress the decision through the appropriate operational workflow.",
    },
    {
      name: "Verify",
      copy: "Determine the actual service or business outcome.",
    },
    {
      name: "Improve",
      copy: "Use verified outcomes to improve processes, rules, and future decisions.",
    },
  ];
  readonly profitSteps = [
    "Service data",
    "Opportunity identified",
    "Review",
    "Follow-up",
    "Service outcome",
  ];
  readonly homePractices = [
    {
      title: "Automotive & Dealer Technology",
      copy: "Clarify service, dealer, and automotive technology challenges and identify a practical path forward.",
    },
    {
      title: "Product & Solution Engineering",
      copy: "Shape complex product ideas into clearer solutions, useful increments, and deliverable outcomes.",
    },
    {
      title: "Quality Engineering & Test Automation",
      copy: "Build confidence in important workflows through risk-based quality thinking and repeatable validation.",
    },
    {
      title: "Product Definition & Requirements Engineering",
      copy: "Turn ambiguity into shared problem statements, decisions, and outcomes teams can test.",
    },
    {
      title: "AI & Digital Transformation",
      copy: "Connect technology opportunity to a measurable business use with clear accountability and boundaries.",
    },
    {
      title: "FinTech Solution Advisory",
      copy: "Bring product, solution, integration, and engineering perspective to financial technology decisions.",
    },
  ];
  readonly servicesList = [
    {
      slug: "automotive-dealer-technology",
      title: "Automotive & Dealer Technology",
      copy: "Aftersales process, DMS integration architecture, dealer modernisation, and automotive intelligence advisory.",
    },
    {
      slug: "product-solution-engineering",
      title: "Product & Solution Engineering",
      copy: "Product and solution architecture, API development, integration, modernisation, and cloud-ready engineering.",
    },
    {
      slug: "quality-engineering-test-automation",
      title: "Quality Engineering & Test Automation",
      copy: "Quality strategy, API and UI automation, integration testing, framework design, and delivery quality gates.",
    },
    {
      slug: "product-definition-requirements",
      title: "Product Definition & Requirements Engineering",
      copy: "Discovery, product definition, domain modelling, requirements engineering, and delivery planning.",
    },
    {
      slug: "ai-digital-transformation",
      title: "AI & Digital Transformation",
      copy: "AI opportunity discovery, product architecture, workflow automation, responsible implementation, and transformation advisory.",
    },
    {
      slug: "fintech-solutions-advisory",
      title: "FinTech Solution Advisory",
      copy: "Careful product, process, integration, and engineering advisory for financial technology contexts.",
    },
  ];
  readonly engagements = [
    "Advisory / assessment",
    "Architecture & solution design",
    "Implementation / development",
    "Integration",
    "Testing & quality engineering",
    "Product definition",
    "Modernisation",
    "Ongoing engineering support",
  ];
  readonly companyStepCopy = [
    {
      name: "Understand",
      copy: "Understand the customer problem, operating context, constraints, and desired outcome.",
    },
    {
      name: "Design",
      copy: "Shape the appropriate product, solution, architecture, engagement, or approach.",
    },
    {
      name: "Deliver",
      copy: "Turn the agreed solution into practical technology, implementation, advisory, or operational capability.",
    },
    {
      name: "Measure",
      copy: "Evaluate whether the delivered capability produced the intended result.",
    },
    {
      name: "Improve",
      copy: "Use evidence and outcomes to refine the solution, process, or future decisions.",
    },
  ];
  readonly securityPrinciples = [
    {
      title: "DMS-neutral by design",
      copy: "Integration and mapping boundaries are intended to isolate provider-specific details.",
    },
    {
      title: "Least privilege",
      copy: "Access should be explicit, scoped, and enforced by the server-side product boundary.",
    },
    {
      title: "Evidence traceability",
      copy: "Decisions should retain enough context for a person to review and understand them.",
    },
    {
      title: "Maintainable foundations",
      copy: "Portable contracts, clear ownership, and testable services matter as much as the first demo.",
    },
  ];
  readonly valuePoints = [
    {
      number: "01",
      title: "Visibility",
      copy: "Agree where operational opportunity is currently hard to see.",
    },
    {
      number: "02",
      title: "Review quality",
      copy: "Make evidence, confidence, and required review part of the conversation.",
    },
    {
      number: "03",
      title: "Follow-through",
      copy: "Define the workflow and measures that connect a decision to an outcome.",
    },
  ];

  constructor() {
    this.metadata.update(
      this.title,
      String(
        this.route.snapshot.data["description"] ??
          "SPENMER technology products and services.",
      ),
    );
  }

  private resolvePageKey(): PageKey {
    const path = this.route.snapshot.url?.[0]?.path ?? "";
    if (path === "") return "home";
    if (
      [
        "products",
        "autovision",
        "service-profit-ai",
        "services",
        "for-dealers",
        "how-it-works",
        "integration-security",
        "about",
        "business-value",
      ].includes(path)
    )
      return path as PageKey;
    if (path === "privacy" || path === "terms") return "legal";
    return "not-found";
  }
}
