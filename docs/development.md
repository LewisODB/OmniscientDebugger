# Development

## Build the debugger

```bash
./gradlew jar
```

The main fat JAR is written to `build/libs/LewisOmniscientDebugger.jar`.

## Build the documentation

Create a virtual environment:

```bash
python3 -m venv .venv-docs
source .venv-docs/bin/activate
pip install -r requirements-docs.txt
```

Preview locally:

```bash
mkdocs serve
```

Build static HTML:

```bash
mkdocs build --strict
```

Generated files are written to `site/`.
