# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

```bash
# Build the project
mvn clean package

# Run the application (port 8080)
mvn spring-boot:run

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=ClassName

# Skip tests during build
mvn clean package -DskipTests
```

The app connects to MySQL at `localhost:3306/defense_management` with credentials `root/root` (see `application.yml`). Ensure the database is running before starting.

## Architecture Overview

**Spring Boot 2.7.5 + MyBatis + Thymeleaf MVC application.**

- Java 17, Spring Security (permissive — all auth handled by session + interceptor)
- MySQL database named `defense_management`
- Apache POI for Word (`.docx`) document generation
- Alibaba Qwen (通义千问) AI API for generating defense comments

### Package Structure

```
com.example.defensemanagement/
├── DefenseManagementApplication.java   # Entry point
├── DefenseController.java              # Root "/" routes, group management
├── config/
│   ├── SecurityConfig.java             # Spring Security (all permits, session-based auth)
│   └── WebConfig.java                  # MVC config, interceptor registration
├── interceptor/
│   └── AuthInterceptor.java            # Session-based auth + role permission checks
├── controller/                         # Feature controllers
├── service/                            # Service interfaces
├── service/impl/                       # Service implementations
├── mapper/                             # MyBatis mapper interfaces
├── entity/                             # JPA/MyBatis entity classes (Lombok)
```

MyBatis XML mappers live in `src/main/resources/mapper/`. Thymeleaf templates: `src/main/resources/templates/` (only `login.html` and `index.html` — most UI is served as static HTML/JS files from the classpath root).

### Authentication & Authorization

Spring Security is configured to **permit all requests** — actual auth is handled entirely by:
1. `AuthInterceptor` — checks `HttpSession` for `currentUser` (User entity) or `currentTeacher` (Teacher entity), redirects to `/login` if missing
2. Session attributes `currentUser` and `currentTeacher` are set on login and used throughout controllers for role checks

**Role hierarchy** (stored in `role` table, checked by name string):
- `SUPER_ADMIN` — full access, all years of data
- `DEPT_ADMIN` — department-scoped access, cannot export documents
- `DEFENSE_LEADER` — group leader, can export reports
- `TEACHER` — scoring and document export
- `STUDENT` — limited portal access

Data is **year-scoped**: all queries for non-`SUPER_ADMIN` users filter by `CURRENT_DEFENSE_YEAR` from `system_config` table.

### Key Domain Concepts

- **Defense Group** (`defense_group`): A committee group of teachers. Students are assigned to groups via `t_student.defense_group_id`.
- **Large Group Defense** (大组答辩): Top-ranked student from each small group goes to a cross-group evaluation; score stored in `large_group_score`.
- **Final Score Formula**: `total_grade = advisor_score*0.3 + reviewer_score*0.3 + final_defense_score*0.4`
- **Adjustment Factor**: `adjustment_factor = large_group_score / group_avg_score` — applied only to the top student from each group.
- **Volunteer Matching** (志愿互选): Students rank teacher preferences; managed through `student_preference` table with round-based matching.

### Teacher–User Duality

Teachers exist in two tables: `teacher` (with `teacher_no`) and `user` (with `username`). **`teacher.teacher_no` must equal `user.username`** for the same person. Both `currentTeacher` and `currentUser` session attributes may be set simultaneously — many permission checks handle both paths.

### AI Comment Generation

`AiCommentServiceImpl` calls Alibaba DashScope API (`qwen-turbo` model) using OpenAI-compatible endpoint. API key stored in `system_config` table under key `QWEN_API_KEY`. Prompt templates stored as `PAPER_PROMPT_TEMPLATE` and `DESIGN_PROMPT_TEMPLATE`.

### Scoring Items

- **PAPER** type: 3 scoring items (`item1_score`–`item3_score` in `teacher_score_record`)
- **DESIGN** type: 6 scoring items (`item1_score`–`item6_score`)
- Items and weights configured in `evaluation_item` table (global, not year-specific)

### Document Export

`DocTemplateService` uses Apache POI to generate `.docx` files. Teacher signatures stored as files named `teacher_{id}.*` and retrieved by `FileStorageService`.

### System Config Keys

All runtime configuration lives in the `system_config` table. Key constants are defined in `ConfigServiceImpl`. Important keys: `CURRENT_DEFENSE_YEAR`, `QWEN_API_KEY`, `VOLUNTEER_DEADLINE`, `VOLUNTEER_CURRENT_ROUND`, `LARGE_GROUP_DEADLINE`, `LARGE_GROUP_ARCHIVED`, `GROUP_MAX_STUDENTS`, `TEACHER_MAX_STUDENTS`.
