# Community Guide

## Contribution lanes

StudyOS intentionally separates contribution lanes so community members can help without touching the full core.

- Core maintainers: domain model, engine, workspace, CLI, web
- Pack contributors: exam packs, research packs, session templates, error taxonomies
- Provider contributors: local model and API provider adapters

## Add a new exam pack

1. Create `src/studyos/assets/packs/exams/<pack-name>/`
2. Add these files:
   - `pack.yaml`
   - `syllabus.yaml`
   - `error-taxonomy.yaml`
   - `session-templates/`
3. Keep the files as YAML-compatible JSON so the MVP stays dependency-free.
4. Required `pack.yaml` fields:
   - `name`
   - `kind`
   - `display_name`
   - `description`
   - `default_stage`
   - `review_cadence`
5. Required `syllabus.yaml` field:
   - `units`
6. Required `error-taxonomy.yaml` field:
   - `categories`

## Add a new provider

1. Implement a new provider class in `src/studyos/providers.py`
2. Support these methods:
   - `send_chat`
   - `send_structured`
   - `get_models`
   - `check_health`
3. Register the provider in `build_provider`
4. Add or extend provider contract tests

## Test a pack

Run the test suite:

```powershell
python -m unittest discover -s tests -v
```

Pack tests should validate:

- required metadata fields
- syllabus presence
- error taxonomy presence
- topic creation using the pack
- plan generation using the pack

## Submit a sample workspace

A sample workspace is useful when you add a new pack or workflow.

Recommended contents:

- one initialized workspace
- one topic using the new pack
- one generated plan
- one sample session markdown and JSON
- one short review entry
