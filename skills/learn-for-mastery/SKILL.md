---
name: learn-for-mastery
description: Design and maintain a complete learning system for a topic or field. Use when the user wants a full curriculum, staged roadmap, problem-solving archive, research workflow, progress tracking, or split notes for humans and AI instead of question-driven tutoring.
---

# Learn For Mastery

## Overview

Create or update a topic so learning follows a complete mastery roadmap. Route work into one of three workflows: topic mastery, problem solving, or research exploration.

## Core Workflow

1. Identify whether the user input is a topic, a problem, or a research direction.
2. If needed, create a topic folder from `01-topics/_template/`.
3. If needed, create a research folder from `06-research/_template/`.
4. Read `references/mastery-mode.md` for topic planning.
5. Read `references/problem-solving.md` for problem archiving.
6. Read `references/research-flow.md` for open research work.
7. Read `references/dual-audience-notes.md` for human vs AI routing.
8. Update human-facing files first, then AI-facing files.
9. End with the next 1 to 3 actions for the next session.

## Workflow Routing

### Topic Mastery

Use for systematic learning of a field.

Primary files:

- `README.md`
- `outline.md`
- `human-guide.md`
- `ai-context.md`
- `notes.md`
- `qa.md`
- `projects.md`
- `progress.md`
- `review.md`

### Problem Solving

Use when the learner gives a concrete problem.

Primary files:

- `solved-problems.md`
- `formula-sheet.md`
- `qa.md`
- `progress.md`

### Research Exploration

Use when the learner gives an idea, direction, or hypothesis.

Primary files:

- `README.md`
- `human-brief.md`
- `ai-context.md`
- `source-log.md`
- `working-notes.md`
- `conclusion.md`
- `review.md`

## Planning Rules

- Start from field boundaries and prerequisites before diving into tools.
- Build a staged progression from foundations to advanced judgment.
- For each phase, include:
  - goal
  - key concepts
  - practice method
  - expected artifact
  - exit criteria
  - common traps
- Prefer a long arc plus an 8 to 12 week medium arc plus a next-session plan.
- Do not structure the topic as a loose list of questions unless the user explicitly asks for that style.

## Note Routing

### Human-facing

Prioritize files that help the learner resume quickly and make decisions.

Emphasize:

- why this matters
- current stage
- recent progress
- what to do next
- what conclusion currently stands

### AI-facing

Use machine-oriented state files for structured continuation.

Emphasize:

- current phase
- dependency or evidence map
- knowledge gaps
- extraction backlog
- next best edits

## Repository Expectations

If this skill is used in this repository, prefer the established structure under:

- `01-topics/`
- `02-journal/`
- `05-meta/`
- `06-research/`

If the repository already contains templates, reuse them instead of inventing new layouts.
