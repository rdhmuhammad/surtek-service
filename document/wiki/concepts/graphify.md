# Graphify Knowledge Graph

**Summary**: Project knowledge graph configuration and AST indexing for codebase structure and relationship navigation.
**Sources**: Project codebase, `.agents/rules/graphify.md`, `graphify-out/`
**Last updated**: 2026-10-13

---

## Overview

Graphify analyzes the project codebase and extracts entities, modules, and relationships into a queryable knowledge graph. Outputs reside in `graphify-out/` and provide visual, structural, and GraphRAG-ready representations.

## Key Artifacts

- `graphify-out/graph.json`: Machine-readable graph structure (nodes, edges, communities).
- `graphify-out/graph.html`: Interactive visualization of the codebase graph.
- `graphify-out/GRAPH_REPORT.md`: Architectural summary detailing communities and key components.
- `.agents/rules/graphify.md`: Always-on agent rule instructing AI tools to query the graph before codebase traversal.
- `.agents/workflows/graphify.md`: Project workflow definition for running graphify commands.

## Common Operations

- Run `graphify query "<question>"` or MCP `query_graph` to inspect relevant subgraphs.
- Run `graphify path "<nodeA>" "<nodeB>"` or MCP `shortest_path` to trace paths between components.
- Run `graphify update .` to update the AST extraction incrementally after modifying code.

## Related pages

- [[index]]
- [[concepts/ponytail]]
