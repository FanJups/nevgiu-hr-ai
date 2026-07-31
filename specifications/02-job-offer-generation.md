# AI-Generated Job Offers

## Objective

Generate a complete, editable job-offer draft from a short description while maintaining a consistent organizational tone and inclusive language.

## Inputs

Required:

- Brief free-text role description.

Optional:

- Department
- Location
- Employment type
- Salary range
- Tone: formal, friendly, inclusive, or another approved option

## Expected output

A structured job offer containing:

- Inferred title and seniority level
- Role summary
- Key responsibilities
- Required qualifications
- Preferred qualifications
- Soft skills and cultural expectations
- Benefits and perks, when supplied or applicable
- Missing or ambiguous information requiring review

The user must be able to edit, regenerate, approve, and save the draft. Export and direct job-board publication are future extensions.

## Implementation steps

1. Define and validate the generation request contract.
2. Define a versioned structured response schema; do not depend on unstructured model prose.
3. Create a prompt template that separates user input from system instructions.
4. Require the model to identify assumptions and missing information.
5. Add inclusive-language and clarity checks.
6. Validate AI output server-side before returning it.
7. Persist the original input, generated draft, approved version, prompt version, model, and timestamps.
8. Support edit, regenerate, approve, and list operations in the API.
9. Connect the Angular generator, preview, approval, and listing screens to those operations.
10. Add loading, timeout, retry, validation, and error states.
11. Add unit tests for schema validation and service behavior.
12. Add integration tests with a stubbed AI model and end-to-end tests for the main user flow.

## Acceptance criteria

- A valid short description produces a structured draft in under 30 seconds.
- Missing or ambiguous fields are clearly identified.
- A user can edit and approve the result.
- Approved content is persisted and can be reopened.
- Regeneration does not overwrite an approved version without confirmation.
- Model failures return an actionable error and do not create partial records.
- Generated language passes the agreed inclusivity and clarity checks.
- Target: at least 90% of users accept the draft with no or minimal edits; define how this is measured before release.
