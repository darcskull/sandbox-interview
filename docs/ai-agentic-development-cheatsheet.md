# AI and agentic software development cheat sheet

Generative AI features combine probabilistic model output with ordinary deterministic software. An agent is a system that can choose and invoke tools toward a goal, usually over multiple steps. Use the least autonomous design that meets the requirement: retrieval plus one model call is often simpler and safer than an open-ended agent loop.

## Core vocabulary

- **Model:** computes output from input and learned parameters. Output can be plausible and wrong.
- **Prompt:** instructions/context supplied to a model. Prompts are versioned application behavior, not a security boundary.
- **Tokens/context window:** model input/output capacity and cost unit; truncate or summarize deliberately.
- **Embedding:** vector representation used for similarity retrieval, not proof of truth or authorization.
- **RAG:** retrieve source passages, then generate an answer grounded in them.
- **Tool/function call:** structured request for the application to invoke a capability. The model proposes arguments; trusted application code validates and executes.
- **Agent loop:** observe request/state, ask model for next action, validate action, execute bounded tool, return result, repeat or finish.
- **Evaluation:** measure task quality, safety, latency, cost, and reliability on representative cases over time.

## A controlled agent loop

```text
user request
  -> authenticate / authorize
  -> load scoped context and policy
  -> model proposes answer or typed tool call
  -> validate schema, authorization, budget, and business invariants
  -> execute tool with timeout and idempotency key
  -> record redacted trace; return bounded result to model
  -> stop on answer, deadline, step/tool budget, or human escalation
```

An LLM must not be the authorization engine. Enforce access in ordinary service code on every tool call. Validate types and ranges, allowlist tools, limit tool iterations and payload size, use deadlines, and reject unknown arguments. Treat user text, retrieved documents, tool output, and model output as untrusted data. Prompt injection can arrive from data being summarized or retrieved, not just from the user.

Prefer typed request/response contracts. Use structured output or JSON schema where supported, but still validate output server-side. Keep side effects explicit and auditable. Require human confirmation for high-impact or irreversible actions unless a carefully reviewed policy permits automation.

## Retrieval-augmented generation

1. Ingest authorized source documents; preserve owner, tenant, source URI, version, and timestamps.
2. Chunk by semantic boundaries with overlap chosen through evaluation; preserve enough metadata to cite the source.
3. Embed and index chunks. Version embedding model and chunking strategy.
4. At query time authenticate first, apply tenant/ACL filters in trusted code, retrieve lexical/vector/hybrid results, rerank if justified.
5. Ask the model to answer from retrieved evidence, cite sources, and say when evidence is insufficient.
6. Evaluate retrieval recall and answer faithfulness separately; maintain deletion and reindex workflows.

Vector similarity is not access control. Embeddings can leak information and can be difficult to delete/trace; maintain source-to-chunk lineage. Avoid sending secrets or unnecessary personal data to model providers.

## Architecture and reliability

- Keep model/provider clients behind an application port so provider changes and local fakes are testable.
- For long tasks, accept a request, persist a task/outbox record, enqueue work, and return a task ID. Workers enforce tenant quotas, deadlines, retries, and idempotency.
- Classify failures: validation/authorization errors are not retried; timeouts/rate limits may be retried with bounded exponential backoff and jitter; malformed model output can use one constrained repair attempt then fail safely.
- Apply circuit breakers/bulkheads and provider timeouts. Provide a deterministic fallback or clear unavailable response; do not silently return fabricated success.
- Record model name/version, prompt/template version, retrieval IDs, tool names, latency, token usage, retries, and outcome. Redact secrets and set a retention policy.
- Cache only when inputs, tenant, model/prompt version, and freshness semantics are part of the cache key. Avoid caching sensitive cross-user answers.
- Use human feedback and offline evals to detect regressions; online A/B tests should have privacy and rollback plans.

## Data and messaging roles for AI workloads

- SQL or MongoDB stores canonical task state, authorization metadata, and durable results.
- Kafka carries replayable lifecycle events for independent projections/analytics; key by task or tenant where ordering is needed.
- RabbitMQ can distribute discrete inference/tool jobs with acknowledgements and dead-lettering.
- Redis can provide short-lived rate limits, deduplication, and session/cache data; expiry/eviction must match the role.
- Object storage can hold large source artifacts with database metadata and explicit retention/access controls.

Do not dual-write task state and broker messages without an outbox. Use an idempotency key to make retried jobs safe. A model call cannot be rolled back; record the request and result according to privacy requirements.

## Testing and evaluation

- Unit test policy, validators, tool adapters, and failure handling with deterministic model fakes.
- Contract test provider request/response mapping and tool schemas.
- Integration test the queue, persistence, retrieval filter, and timeout behavior.
- Keep a versioned evaluation set containing normal, ambiguous, adversarial, and refusal cases. Use human review for nuanced quality.
- Track task success, groundedness, correct citations, unsafe tool attempts, abstention quality, p50/p95 latency, cost per successful task, retry rate, and user corrections.
- Never make exact wording the sole correctness metric. Establish acceptance thresholds and compare by slice (language, user group, document type).

## Security checklist

1. Authenticate and authorize before retrieval and before every tool execution.
2. Separate instructions from data; delimit retrieved/user/tool content and assume prompt injection is possible.
3. Allowlist tools and operations; use least-privilege service identities.
4. Validate tool arguments and outputs; enforce tenant/object-level authorization in code.
5. Set time, token, step, request-size, and spend budgets.
6. Redact and encrypt sensitive data; define provider retention and audit policies.
7. Add rate limits, abuse detection, and a kill switch for autonomous side effects.

## Senior interview questions, answers, and reasoning

1. **How would you decide between RAG and fine-tuning?** RAG supplies changing/private evidence at query time and supports citations/deletion; fine-tuning changes model behavior/style and is not a dependable knowledge database. Start with retrieval/prompting, evaluate failure modes, and fine-tune only for a measured behavior gap with safe training data.
2. **How do you prevent a retrieved prompt injection from invoking a tool?** Treat retrieved text as untrusted data, keep policy in trusted system/application code, validate every proposed tool call, authorize against the current principal, and limit tools to a narrow allowlist. Model instructions alone cannot guarantee this.
3. **What does “agentic” add, and what does it cost?** It lets a model choose sequential actions based on observations, helping with open-ended workflows. It adds nondeterminism, latency/cost, failure/retry complexity, and side-effect risk. Bound steps, tools, deadlines, and permissions; use a fixed workflow when possible.
4. **How do you evaluate an AI feature before deployment?** Define task-specific quality and safety outcomes; maintain representative labeled cases; evaluate retrieval and generation separately; include adversarial and abstention cases; human-review ambiguous outcomes; then canary with cost/latency/error guardrails and rollback.
5. **How do you make an asynchronous AI task safe to retry?** Persist a stable task/idempotency ID, model/prompt version and status; make state transitions conditional; deduplicate tool side effects; persist outputs before acknowledging; classify permanent versus transient errors; cap retries and send exhausted tasks to review/DLQ.
6. **How do you explain a wrong answer to an operator?** Capture versioned prompt/model, scoped retrieved document IDs and versions, tool inputs/outputs (redacted), trace/correlation ID, and validation decisions. Separate source/retrieval errors from generation errors while respecting retention and privacy constraints.
