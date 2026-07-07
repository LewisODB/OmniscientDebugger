# General Help

If you are calling the ODB from the command line, there are three primary options which are defined as aliases (UNIX) or `.bat` files (WINDOWS):

*   **`debug <program> [args...]`**: Run the specified `<program>` with `<args...>` and bring up the debugger when `main()` exits.
*   **`debugp <program> [args...]`**: Bring up the command window to select options, then run the specified `<program>` with `<args...>`.
*   **`debugn <program> [args...]`**: Run the specified `<program>` with `<args...>` but don't bring up the debugger until STOP RECORDING is pressed. (for multithreaded programs where `main()` exits early)

The arrow buttons all the execute First/Previous/Next/Last on the selected element. They all have tool-tip popups.

*   Selecting a line in the **Trace** pane reverts to the first line in that method.
*   Selecting a line in the **Threads** pane reverts to the nearest previous event in that thread.
*   Selecting a line in the **Code** pane reverts to *AN* event on that line (if any). It attempts to revert the most 'reasonable' event, but this is not always clear.

*   Double-clicking on an object anywhere will copy that object into the **Objects** pane and open it.
*   Double-clicking on an object in the **Objects** pane will show/not-show the instance variables it.
*   Double-clicking on an instance variable in the **Objects** pane will show its IVs recursively.

The minibuffer at the bottom of the debugger window will display details about the most recent command. This is also where extended commands such as Incremental String Search and Evaluate Expression are run. For details, see the manual in jar file.

-- Bil Lewis (Bil.Lewis@LambdaCS.com)
