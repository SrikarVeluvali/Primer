# Primer design direction

Primer is a local reading workspace for moving between a paper, a question, and the evidence without losing your place.

Dark is the default: desk `#141925`, surface `#1C2331`, panel `#171E2B`, text `#E3E8F2`, accent `#A79CF1`, and border `#30394B`. A softer Light palette is available alongside account-specific custom palettes. Semantic color variables keep every feature aligned with the active theme. Manrope provides the heading voice; DM Sans handles reading, navigation, and controls.

```text
Library rail | Workspace breadcrumb                         |
             | Chat or personal learning plan  | The source |
             |                                 | Overview   |
             |                                 | Passages   |
             | Model + question composer       | PDF / Notes|
```

The visual signature is a stacked research sheet drawn with CSS and SVG. The arrival screen makes importing a paper obvious. Reading replaces the illustration with useful content. Source numbers identify actual retrieved passages, and page links connect them to the original PDF.

The interface uses a compact reading desk with a quiet accent and clear separation between navigation, conversation, and paper tools. Shared papers and topic tags are identified separately from private notes, highlights, ratings, and learning plans. Familiarity questions use the same four labels throughout the app. Recommendations explain their topic matches, and the topic picker shows real paper counts.

The conversation and paper panels can be resized or expanded. PDF text selection reveals a compact highlight/explanation toolbar. On small screens, navigation and paper details become drawers. Keyboard focus, semantic forms, password autocomplete, keyboard topic selection, reduced motion, and visible loading/error states are built in.
