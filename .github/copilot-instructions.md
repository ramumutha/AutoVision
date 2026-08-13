# AutoVision Engineering Instructions

AutoVision is an Adaptive Vehicle Service Intelligence Platform.

Core lifecycle:
Observe -> Predict -> Decide -> Execute -> Verify -> Learn.

Architecture:
- Frontend: Next.js + TypeScript
- Backend: FastAPI + Python
- Database: PostgreSQL
- Media: object storage abstraction
- Async processing: queue + worker abstraction
- AI: provider-independent AI Gateway
- Safety: deterministic safety-policy engine
- Integration: DMS-neutral canonical adapter

Engineering principles:
1. Use a modular monolith for the POC. Do not introduce microservices unless explicitly requested.
2. Never couple domain objects to a specific DMS, OEM or AI provider.
3. Preserve tenant isolation and server-side authorization.
4. Never place secrets in source code.
5. AI outputs are untrusted until schema validated.
6. Generative AI must never be the sole authority for safety decisions.
7. Safety-blocked actions cannot be re-enabled by normal user override.
8. Preserve recommendation and actual repair as separate historical records.
9. Prefer simple, maintainable implementations over unnecessary frameworks.
10. Add tests for authorization, validation and negative paths.
11. Do not invent new product requirements. Ask before changing the frozen architecture.
12. All POC simulated predictive outputs must be clearly identifiable as simulated.
13. Follow the canonical API/data model.
14. Produce accessible responsive UI.
15. Do not change architecture merely because another pattern is more fashionable.

Sprint 0 scope:
- Repository foundation
- Development environment
- Authentication/demo roles foundation
- PostgreSQL
- Vehicle canonical entity
- Vehicle Search
- Vehicle 360 foundation screen
- CI/CD
- Correlation IDs / structured logging

Do not implement yet:
- vehicle inspection AI
- predictive maintenance
- NBSA
- safety rules
- customer approval
- DMS synchronization
- SOE