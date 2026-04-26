# Dual-Audience Notes Reference

## Purpose

Split topic records into a human-facing view and an AI-facing view.

## Human-Facing Files

### `human-guide.md`

Use as the first file a human should reopen.

Include:

- why the topic matters
- current stage and momentum
- this week's focus
- the exact next step
- the most valuable recap points

Write in plain language. Optimize for fast re-entry and motivation.

### `README.md`

Use as the stable topic index.

Include:

- goal
- scope
- resources
- status
- next 3 actions

### `outline.md`

Use as the canonical roadmap from foundations to advanced work.

Include phased progression instead of disconnected questions.

### `review.md`

Use to capture what is solid, what is fuzzy, and what should change next.

## AI-Facing Files

### `ai-context.md`

Use for structured state that helps Codex continue work quickly.

Include:

- current phase
- dependency map
- knowledge gaps
- extraction backlog from journal
- source map
- next best edits

Keep it dense, factual, and action-oriented.

## Journal Routing

Use daily journals for raw progress.

From a session, promote content as follows:

- stable explanation -> `notes.md`
- unresolved question -> `qa.md`
- experiment or exercise -> `projects.md`
- next-step and motivation cues -> `human-guide.md`
- state and extraction hints -> `ai-context.md`

## Priority Rule

If time is limited, update human-facing files first. A learner who can resume quickly is more valuable than a perfectly instrumented AI state file.
