---
name: Bug report
about: Report a bug or unexpected behavior in EcoTaleQuests
title: ''
labels: bug
assignees: TheFokysnik

---

name: 🐞 Bug Report
description: Report a bug or unexpected behavior in EcoTaleQuests
title: "[BUG] "
labels: ["bug"]
assignees: []
body:
  - type: markdown
    attributes:
      value: |
        ⚠️ Before submitting a bug report, please ensure that:
        - You are using the latest version of EcoTaleQuests
        - The issue is reproducible
        - This is not caused by a misconfiguration or conflicting plugin

  - type: input
    id: version
    attributes:
      label: EcoTaleQuests Version
      description: Specify the plugin version
      placeholder: "e.g. 1.0.0"
    validations:
      required: true

  - type: input
    id: hytale_version
    attributes:
      label: Hytale Server Version
      placeholder: "e.g. 0.9.x"
    validations:
      required: true

  - type: textarea
    id: description
    attributes:
      label: Issue Description
      description: Clearly describe what is not working correctly
      placeholder: |
        Quest progress is not counted when killing mobs...
    validations:
      required: true

  - type: textarea
    id: steps
    attributes:
      label: Steps to Reproduce
      description: Provide detailed steps to reproduce the issue
      placeholder: |
        1. Accept a daily kill quest
        2. Kill mobs within the required level range
        3. Check quest progress
    validations:
      required: true

  - type: textarea
    id: expected
    attributes:
      label: Expected Behavior
      placeholder: "Quest progress should increase correctly"
    validations:
      required: true

  - type: textarea
    id: actual
    attributes:
      label: Actual Behavior
      placeholder: "Quest progress does not change"
    validations:
      required: true

  - type: textarea
    id: logs
    attributes:
      label: Logs / Stacktrace
      description: Paste relevant logs if available
      render: shell

  - type: textarea
    id: config
    attributes:
      label: Configuration (config.yml)
      description: Remove any sensitive information
      render: yaml

  - type: checkboxes
    id: confirmation
    attributes:
      label: Confirmation
      options:
        - label: I am using the latest version of EcoTaleQuests
          required: true
        - label: I have checked that this is not a configuration issue
          required: true
        - label: I have provided all relevant information
          required: true
