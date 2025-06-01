# DungeonDSL - Testing Extension

This extension focuses on testing for the DungeonDSL.
It includes parser, validation, traceability, and code generation stability tests.

## Goals

- Verify correct parsing and structure of dungeon models.
- Validate semantic constraints (e.g. trap trigger chance).
- Check that traceability markers are correctly embedded in generated Python code.
- Ensure code generation is stable and consistent over multiple outputs.

---

## Test Suite Overview

### 1. DungeonDSLParsingTest.xtend
Validates that `.dung` input files can be parsed correctly.

**Features Tested:**
- Correct parsing of a full dungeon.
- Invalid trap values.
- Verification of dungeon structure (e.g. rooms, connections).

---

### 2. DungeonDSLValidationTest.xtend
Covers semantic validation rules using `ValidationTestHelper`.

**Highlights:**
- Ensures triggerChance is between 0–100.

---

### 3. DungeonDSLTraceabilityTest.xtend
Validates that the Python code generator includes **traceability markers** that map DSL model elements (like rooms) to lines in the output code.

**Two levels of traceability testing:**
- `testTraceCommentInGeneratedPython()` checks if at least one file includes valid trace blocks.
- `testTraceabilityCommentsInAllFiles()` prints per-file traceability coverage.

---

### 4. DungeonDSLStableGenerationTest.xtend
Ensures the generator produces the same output each time for the same input.

**What it does:**
- Searches for all `_v1.py` and `_v2.py` file pairs in `src-gen/`.
- Asserts that the content of the files are **byte-for-byte identical**.

**Sample output:**
```
Found 2 _v1.py files. Starting comparisons...
Comparing: Dung1_v1.py with Dung1_v2.py
Comparing: TraceTest_v1.py with TraceTest_v2.py
All stable code generation pairs matched successfully.
```

---

## File Locations

- Generated files live in:  
  `runtime-EclipseApplication/Dungeon/src-gen/`

- Test files are located in:  
  `org.xtext.dungeondsl.tests/src/org/xtext/tests/`

---

## How to Run Tests

1. Open the `org.xtext.dungeondsl` project in Eclipse.
2. Right-click any test class (e.g., `DungeonDSLValidationTest.xtend`) → Run As → JUnit Test.
3. View logs in the Eclipse console for validation or trace output.
