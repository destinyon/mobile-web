# Project AGENTS.md

## Project Overview

- This repository contains a WeChat Mini Program and Spring Boot backend for "羽球在线".
- Main business domain: badminton news browsing, community interaction, content publishing, and admin moderation.
- Primary users: WeChat Mini Program users and backend administrators.
- Main modules:
  - `backend/`: Spring Boot REST API, database schema, OSS upload, and admin endpoints.
  - `app/`: native WeChat Mini Program frontend.
  - `docs/`: project documentation and API notes.

## Before Editing

- Read this file first.
- Read relevant README files and existing code before making changes.
- Search for existing implementations before creating new ones.
- Keep change scope small and avoid compatibility clutter.
- Do not modify generated files unless the task requires it.
- Do not change public API contracts without explicitly mentioning the impact.

## Coding Rules

- Match existing code style.
- Prefer clear, boring, maintainable implementation over clever abstractions.
- Keep business logic out of controllers when service layers exist.
- Reuse existing utilities, constants, validators, and error handlers.
- Add Chinese comments only for important business logic or non-obvious course/project constraints.
- Do not add comments for obvious code.

## API Rules

- Keep request and response structures consistent.
- Validate all external inputs.
- Return clear errors using the existing response envelope.
- Do not leak stack traces or sensitive information.
- New endpoints must be documented with path, method, request, response, error cases, and permission requirements.

## Security Rules

- Never hardcode secrets, tokens, credentials, or private keys.
- Never commit `.env`, local config, database dumps, or generated credentials.
- Sanitize user input where needed.
- Check permission and ownership for user-specific resources.
- Treat file uploads as untrusted input.

## Testing and Validation

- Use the repository's existing test/build commands.
- After code changes, run the smallest relevant validation command.
- If validation fails, investigate the root cause.
- Do not mark work complete until validation is run or clearly explained as unavailable.

## Completion Checklist

Before finishing, report:

- Files changed
- Main behavior changed
- Validation command run and result
- Known risks or assumptions
- Any manual steps required
