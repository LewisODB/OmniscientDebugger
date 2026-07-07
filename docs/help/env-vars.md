# ENV Var Help

These environment variables control various runtime behaviors of the Omniscient Debugger:

| Environment Variable | Description |
| :--- | :--- |
| `MEMORY` | Used to determine number of Timestamps. |
| `GC_OFF` | If too many Timestamps, turn off recording. |
| `DONT_SHOW` | Don't stop recording when `main()` exits. |
| `DONT_INSTRUMENT` | Don't instrument class. (Assume files instrumented.) |
| `PAUSED` | Don't start recording. |
| `DONT_START` | Just bring up the controller. |
| `DEBUGIFY_ONLY` | Just debugify. (Not used normally.) |
| `BUG` | Use the buggy version of `revert()` for demos. |
| `VGA` | Format window for VGA screen (drops some menus). |
| `SCREEN_SHOT` | Format window for screen shots. |
| `TRACE_LOADER` | Trace all calls to the classloader. |
| `TRACE_LOADER_STACK` | Trace all calls to the classloader + print out stack. |
| `TEST` | Run recorded tests. |
| `DEBUG_DEBUGGER` | Print out data on GCs. |
| `DONT_PAUSE_ON_STOP` | Let the target program continue to run when recording stops. |

### Debugifier Options

| Option | Description |
| :--- | :--- |
| `NOTHING` | Debugifier: don't do any instrumentation. |
| `DEBUG_DEBUGIFY` | Debugifier: print out details during instrumentation. |
| `ATHROW` | Debugifier: don't instrument throws. |
| `CATCH` | Debugifier: don't instrument catch. |
| `ASTORE` | Debugifier: don't instrument astore. |
| `AASTORE` | Debugifier: don't instrument aastore. |
| `IASTORE` | Debugifier: don't instrument iastore. |
| `RETURN` | Debugifier: don't instrument return. |
| `RETURNVALUE` | Debugifier: don't instrument returnvalue. |
| `INVOKEVIRTUAL` | Debugifier: don't instrument invokevirtual. |
| `IINC` | Debugifier: don't instrument iinc. |
| `ISTORE` | Debugifier: don't instrument istore. |
| `PUTFIELD` | Debugifier: don't instrument putfield. |
| `PUTSTATIC` | Debugifier: don't instrument putstatic. |
| `INVOKESTATIC` | Debugifier: don't instrument invokestatic. |
| `ARGUMENTS` | Debugifier: don't instrument arguments. |
| `NEW` | Debugifier: don't instrument new. |
| `NO_LOCKS` | Debugifier: don't instrument locks. |
| `PUBLIC_ONLY` | Debugifier: only change all IVs to be public. |
| `DONT_REPLACE_VECTOR` | Debugifier: don't replace Vectors, etc. |
| `PUTFIELD_ONLY` | Debugifier: only instrument putfield. |
