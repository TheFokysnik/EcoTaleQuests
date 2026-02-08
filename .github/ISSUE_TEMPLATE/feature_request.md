---
name: Feature request
about: Suggest a new quest type, mechanic, or improvement for EcoTaleQuests
title: ''
labels: enhancement
assignees: TheFokysnik

---

name: 💡 Feature Request
description: Suggest a new quest type, mechanic, or improvement for EcoTaleQuests
title: "[FEATURE] "
labels: ["enhancement"]
assignees: []
body:
  - type: markdown
    attributes:
      value: |
        ✨ Thank you for your suggestion!
        Please describe your idea clearly and consider its impact on gameplay balance and progression.

  - type: textarea
    id: feature_description
    attributes:
      label: Feature Description
      description: What would you like to add or improve?
      placeholder: |
        Add elite boss quests with higher rewards...
    validations:
      required: true

  - type: textarea
    id: problem
    attributes:
      label: Problem or Motivation
      placeholder: |
        Currently there is no long-term or high-risk quest content...
    validations:
      required: true

  - type: textarea
    id: solution
    attributes:
      label: Proposed Solution
      placeholder: |
        Introduce weekly elite quests with scaling difficulty...
    validations:
      required: true

  - type: textarea
    id: alternatives
    attributes:
      label: Alternative Solutions
      placeholder: |
        This could also be handled through server events...
    validations:
      required: false

  - type: dropdown
    id: impact
    attributes:
      label: Impact on Gameplay & Economy
      options:
        - Low (QoL / cosmetic)
        - Medium
        - High (affects balance or progression)
    validations:
      required: true

  - type: dropdown
    id: complexity
    attributes:
      label: Estimated Implementation Complexity
      options:
        - Low
        - Medium
        - High
        - Not sure
    validations:
      required: true

  - type: checkboxes
    id: confirmation
    attributes:
      label: Confirmation
      options:
        - label: I understand that this feature may be rejected
          required: true
        - label: I understand that implementation may take time
          required: true
