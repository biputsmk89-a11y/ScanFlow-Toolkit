# SCANFLOW ARCHITECTURE GLOSSARY

Domain terminology and architecture design language for the ScanFlow project.

---

### Core Architecture Vocabulary

- **Module**: A bounded unit of software with an interface and an implementation.
- **Interface**: The exposed, public test surface of a module.
- **Implementation**: The hidden internal logic, invariants, and dependencies behind an interface.
- **Depth**: The ratio between an interface's simplicity and its internal capability.
  - **Deep Module**: An interface that is small and simple, hiding substantial complexity.
  - **Shallow Module**: An interface nearly as wide as its implementation with minimal internal value.
- **Seam**: The isolated boundary line between two modules where behavior can be sliced or substituted.
- **Adapter**: A translation layer that bridges two distinct interfaces.
- **Leverage**: The power of an interface to handle many operations with minimal caller friction.
- **Locality**: Co-location of related logic such that single changes don't scatter across multiple layers.

---

### Domain Concepts

- **Document**: An immutable file entity managed within ScanFlow (PDF, Image, Text, CSV) possessing metadata (id, path, size, page count, content hash).
- **Operation**: A discrete document transformation or analysis process (e.g. Merge, Split, Compress, Watermark, OCR, Redact, Sign).
- **OperationResult**: The standard execution outcome reporting success, output file path, byte size, duration, pages processed, and diagnostic metadata.
- **DocumentOperationDispatcher**: The deep execution module responsible for validating inputs, routing commands to appropriate engines, computing integrity hashes, recording audit trails, and registering output documents.
- **ToolExecutionRequest**: The immutable parameter payload supplied by presentation ViewModels to request an operation without binding to concrete engines or storage mechanics.
- **EngineOutput<T>**: The homogeneous outcome contract across all engine seams encapsulating typed payload, execution duration, standardized error codes, and telemetry metadata.

