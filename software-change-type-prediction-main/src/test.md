brief overview of the two-pass structure of a macro processor:

### Pass 1

- **Purpose**: 
  - **Definition**: Processes macro definitions.
  - **Macro Table Creation**: Stores macro names and definitions in a table.
  - **Expansion**: Does not expand macros yet; focuses on identifying and storing them.
  
- **Key Steps**:
  1. **Scan Source Code**: Identify macro definitions.
  2. **Store Macros**: Save the names and bodies of macros in a Macro Definition Table (MDT) and a Macro Name Table (MNT).
  3. **Record Parameters**: Handle formal parameters and store them for future substitution.

### Pass 2

- **Purpose**: 
  - **Expansion**: Expand macro calls using the tables created in Pass 1.
  
- **Key Steps**:
  1. **Scan Source Code Again**: Locate macro calls in the code.
  2. **Expand Macros**: Substitute macro bodies for macro calls using MDT and MNT.
  3. **Generate Output**: Produce the expanded source code with all macros replaced by their definitions.

This two-pass approach ensures efficient handling of macro definitions and their expansions, separating the tasks of definition collection and code generation.