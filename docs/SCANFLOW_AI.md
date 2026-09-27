# SCANFLOW — AI Architecture, RAG Pipeline & Privacy Guidelines

## 1. Overview & Principles

ScanFlow provides document intelligence capabilities designed around **Local-First, Privacy-First Architecture**:
- Zero cloud dependency for core heuristic extraction, summarization, entity recognition, and document question-answering.
- Document text is **never transmitted to external servers** unless the user explicitly opts into a remote provider.
- All AI responses are prominently labeled as **AI-Generated**.
- Answers generated from documents include **exact page citations** linking back to the verified source text.

---

## 2. On-Device RAG (Retrieval-Augmented Generation) Pipeline

```
  ┌──────────────────┐
  │ PDF Document     │
  └────────┬─────────┘
           │
           ▼
  ┌────────────────────────────────────────────────────────┐
  │ Text Extraction (PdfTextStripper / OCR Engine fallback)│
  └────────┬───────────────────────────────────────────────┘
           │
           ▼
  ┌────────────────────────────────────────────────────────┐
  │ Semantic Chunking (~500 tokens with 50-token overlap)  │
  └────────┬───────────────────────────────────────────────┘
           │
           ▼
  ┌────────────────────────────────────────────────────────┐
  │ Term Frequency & Query Keyword Similarity Scoring      │
  └────────┬───────────────────────────────────────────────┘
           │
           ▼
  ┌────────────────────────────────────────────────────────┐
  │ Top-K Relevant Document Chunks with Page References    │
  └────────┬───────────────────────────────────────────────┘
           │
           ▼
  ┌────────────────────────────────────────────────────────┐
  │ Synthesizer (Local Heuristic Engine / Optional Cloud)  │
  └────────┬───────────────────────────────────────────────┘
           │
           ▼
  ┌────────────────────────────────────────────────────────┐
  │ Structured Answer with Page References (e.g. [Page 3]) │
  └────────────────────────────────────────────────────────┘
```

### 2.1 Chunking Strategy
- Documents are partitioned into contextual passages of 300–600 words with boundary preservation (splitting on sentence or paragraph boundaries).
- Each chunk preserves metadata: `chunkId`, `pageIndex`, `charOffset`, and `textSnippet`.

### 2.2 Relevance Retrieval
- Queries are normalized and tokenized.
- Keyword frequency and contextual BM25/TF-IDF scoring identify top matching chunks.
- Only the most relevant snippets are evaluated to synthesize answers, preventing hallucination and preserving memory efficiency.

---

## 3. Supported AI Capabilities

| Capability ID | Name | Engine Execution | Description |
|---|---|---|---|
| SF-107 | AI Summary | Local / Optional Remote | Condenses key points into an executive overview. |
| SF-108 | Ask PDF | Local RAG | Answers user questions with verified page citations. |
| SF-109 | Chat with PDF | Local RAG + Room DB | Multi-turn conversational session with document context. |
| SF-110 | Translate PDF | Remote Gateway | Multi-lingual translation with explicit consent. |
| SF-111 | Extract Table | Local CV / Heuristics | Tabular data extraction into structured CSV/JSON. |
| SF-112 | Extract Entities | Local Regex / NLP | Identifies dates, email addresses, phone numbers, currencies. |
| SF-113 | Smart Rename | Local Text Engine | Generates a clean, descriptive document filename from headers. |
| SF-114 | Classification | Local Heuristics | Categorizes document into Invoice, Contract, Receipt, Form, Letter. |
| SF-115 | PDF to Markdown | Local Parser | Converts headings, paragraphs, and lists into structured Markdown. |
| SF-116 | Document Insights | Local Analytics | Computes reading time, word count, character density, and vocabulary size. |

---

## 4. Privacy & Data Handling Guarantees

1. **Zero Silent Cloud Uploads**: The app will never initiate an HTTP network call containing document text without displaying an explicit confirmation dialog.
2. **Zero Storage of Secrets**: No OpenAI, Anthropic, or Gemini API keys are bundled inside APK bytecode or assets.
3. **Local Conversation Storage**: Chat histories are stored strictly on-device in Room (`AiConversationEntity` and `AiMessageEntity`) and can be cleared instantly in the Settings screen.
