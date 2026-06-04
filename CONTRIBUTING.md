# Contributing to Radar Health Monitor

Thank you for your interest in contributing! This guide will help you get started.

## Quick Start

1. **Fork** this repository
2. **Clone** your fork locally
3. Create a **feature branch** from `master`
4. Make your changes
5. Submit a **Pull Request**

```bash
git clone https://github.com/<your-username>/radar-health-monitor.git
cd radar-health-monitor
git checkout -b feature/your-feature-name
```

## Branch Naming Convention

| Type | Format | Example |
|------|--------|---------|
| Feature | `feature/<name>` | `feature/add-sleep-analysis` |
| Bug fix | `fix/<name>` | `fix/mqtt-reconnect-loop` |
| Docs | `docs/<name>` | `docs/update-api-reference` |
| Refactor | `refactor/<name>` | `refactor/extract-health-utils` |

## Commit Message Convention

We use [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <description>

[optional body]
```

**Types:** `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `chore`

**Examples:**
```
feat(mqtt): add auto-reconnect with exponential backoff
fix(engine): correct heart rate scoring for bradycardia range
docs(readme): add project structure section
```

## Project Modules

| Module | Directory | Tech Stack |
|--------|-----------|------------|
| Frontend | `frontend-vue/` | Vue 3, Vite, Axios |
| Backend | `radar-mqtt2.0/` | Java 17, Spring Boot 2.7, MyBatis-Plus |
| ML Service | `health-ml-backup-v4.0-71.8/` | Python, Flask, Scikit-learn |

Choose the module you want to contribute to and follow the setup instructions in the main README.

## Pull Request Process

### Before Submitting

- [ ] Code compiles/runs without errors
- [ ] New features include reasonable test coverage
- [ ] Existing tests still pass
- [ ] Commit messages follow the convention above
- [ ] No unrelated changes in the PR

### PR Checklist (filled in PR template)

1. **Description**: What does this PR do and why?
2. **Type**: Feature / Bug fix / Refactor / Docs
3. **Module**: Frontend / Backend / ML / All
4. **Testing**: How did you verify the changes?
5. **Screenshots**: (if UI changes)

### Review Process

1. At least one maintainer review required
2. All CI checks must pass (if configured)
3. Resolve all review comments before merge
4. Squash merge is preferred for feature branches

## Code Style

### Java (Backend)
- Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- 4-space indentation
- Max line length: 120

### JavaScript/Vue (Frontend)
- Follow [Vue.js Style Guide](https://vuejs.org/style-guide/) (Priority A & B rules)
- 2-space indentation
- Single quotes for strings

### Python (ML)
- Follow [PEP 8](https://peps.python.org/pep-0008/)
- 4-space indentation
- Use type hints where possible

## Reporting Issues

- **Bug reports**: Use the Bug Report template
- **Feature requests**: Use the Feature Request template
- **Questions**: Open a Discussion (if enabled) or use Issues with the `question` label

## License

By contributing to this project, you agree that your contributions will be licensed under the [GNU GPLv3](LICENSE).
