# Focus Classes Android App

Internal student management and performance evaluation system for **Focus Classes** coaching institute (https://www.focusclasses.co.in/).

> **Tagline:** Learn • Grow • Excel  
> **Slogan:** Track Progress. Build Leaders.

---

## Features

1. **Batch Management (Class 1 – 12 + Custom Batches)**
   - Pre-loaded with empty batches for Class 1 through Class 12.
   - Support for creating additional custom batches (e.g. Class 6 Morning, Class 6 Evening).
   - Edit, rename, archive, and delete batches with safety confirmations.

2. **Student Management**
   - Clean, production-ready Room SQLite database.
   - Android Photo Picker integration for uploading profile pictures.
   - Profile management with student names, roll numbers, unique student codes, and notes.
   - Multi-select management for batch deletions and operations.

3. **Weekly Assessment System**
   - Configurable weekly assessment periods (e.g. "Week 1 (Apr 21 – Apr 27)").
   - Configurable Test Maximum Marks (e.g. 20, 25, 50, 100).
   - Fast data entry workflow with direct number selector pills and student switcher.
   - Assessment categories:
     - **Attendance**: Marks obtained out of 6
     - **Test Marks**: Marks obtained out of configured maximum
     - **Classroom Activities**: Discipline (out of 4) and Project (out of 10)
   - Missing marks are preserved as unassessed/pending rather than silently zeroed.

4. **Scoring & Leaderboard Engine**
   - Normalized category percentages.
   - Weighted overall performance calculation (default 25% for each category, customizable in Settings).
   - Standard competition ranking with automatic tie-handling (Rank 1, Rank 2, Rank 2, Rank 4...).
   - Gold (🥇), Silver (🥈), and Bronze (🥉) medals for top 3 positions.
   - Filter rankings by Batch, Assessment Week, or specific category (Overall, Attendance, Test, Discipline, Project).

5. **Data Export & Backup**
   - Export leaderboard and student performance summaries to CSV format.
   - Share reports directly via Android Intent.

---

## Visual Design

- **Primary Blue:** `#0B2D5C` / `#0A4DA2`
- **Accent Yellow:** `#FFC928` / `#FFC107`
- **Background:** `#F6F8FC`
- **Attendance Green:** `#22C55E`
- **Test Purple:** `#8B5CF6`
- **Activities Amber:** `#F59E0B`

---

## Building and Running the App

```bash
# Compile and build debug APK
gradle assembleDebug

# Run automated tests
gradle :app:testDebugUnitTest
```
