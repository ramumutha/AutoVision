import { Routes } from '@angular/router';
import { PageShellComponent } from './page-shell.component';
import { RequestDemoComponent } from './request-demo.component';
import { ContactUsComponent } from './contact-us.component';
import { VerifyContactComponent } from './verify-contact.component';

export const routes: Routes = [
  { path: '', component: PageShellComponent, data: { title: 'Service intelligence for dealerships', description: 'AutoVision helps teams identify and review evidence-supported service opportunities.' } },
  { path: 'autovision', component: PageShellComponent, data: { title: 'AutoVision', description: 'Adaptive vehicle service intelligence for evidence-led decisions.' } },
  { path: 'service-profit-ai', component: PageShellComponent, data: { title: 'Service Profit AI', description: 'Identify and prioritize evidence-supported service opportunities.' } },
  { path: 'for-dealers', component: PageShellComponent, data: { title: 'For Dealers', description: 'A clearer view for dealership leadership and aftersales teams.' } },
  { path: 'business-value', component: PageShellComponent, data: { title: 'Business Value', description: 'Discuss measurable service opportunity and operational visibility.' } },
  { path: 'how-it-works', component: PageShellComponent, data: { title: 'How It Works', description: 'From service signals to evidence-backed decisions.' } },
  { path: 'integration-security', component: PageShellComponent, data: { title: 'Integration and Security', description: 'DMS-neutral integration and trustworthy product boundaries.' } },
  { path: 'about', component: PageShellComponent, data: { title: 'About VERSPEN', description: 'The provisional VERSPEN and AutoVision product relationship.' } },
  { path: 'contact', component: ContactUsComponent, data: { title: 'Contact Us', description: 'Start a conversation about AutoVision.' } },
  { path: 'contact/verify', component: VerifyContactComponent, data: { title: 'Verify your email', description: 'Verify your commercial enquiry email.' } },
  { path: 'privacy', component: PageShellComponent, data: { title: 'Privacy', description: 'Privacy information will be published after legal review.' } },
  { path: 'terms', component: PageShellComponent, data: { title: 'Terms', description: 'Terms information will be published after legal review.' } },
  { path: 'request-demo', component: RequestDemoComponent, data: { title: 'Request a Demo', description: 'Request a guided AutoVision demonstration.' } },
  { path: '**', component: PageShellComponent, data: { title: 'Page not found', description: 'The requested page could not be found.', notFound: true } },
];