# Security

- Never commit `.env`, passwords, bearer tokens, client secrets, cookies, or other credentials.
- Keep local environment values in the ignored `.env` file created from `.env.example`.
- Treat Keycloak admin and local development-user credentials as sensitive. Do not paste them into issues, logs, screenshots, or chat.
- Logs and diagnostics must not include authorization headers, tokens, passwords, cookies, or session data. Prefer sanitized error summaries and correlation IDs.
- Server-side authorization and tenant isolation are authoritative; client-side guards are not a security control.
- Report suspected security issues privately to the maintainers. No formal public disclosure channel is defined yet; maintainers must establish one before public release.
