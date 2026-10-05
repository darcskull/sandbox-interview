# C# interview cheat sheet (.NET 8+ syntax)

## Types, variables, nulls

```csharp
int retries = 3;                 // value type
string name = "Ada";             // reference type, non-null by default with nullable enabled
string? middleName = null;        // nullable reference annotation
var labels = new List<string>(); // inferred static type
const int MaxRetries = 5;         // compile-time constant
readonly Guid id = Guid.NewGuid();
```

Value types (structs, enums, numeric primitives) hold/copy values; reference variables refer to objects. `string` is immutable. Nullable reference types are compiler analysis, not a runtime wrapper. `Nullable<T>`/`T?` for value types represents a value or no value. `==` may be overloaded; `Equals` expresses equality by type contract. `is`, `as`, and pattern matching safely test types. `dynamic` defers binding to runtime; avoid it unless needed.

## Classes, records, interfaces, properties

```csharp
public interface IPayable { decimal Amount { get; } }
public sealed record Invoice(string Number, decimal Amount) : IPayable;

public class Customer(string id) // primary constructor (C# 12)
{
    public string Id { get; } = id;
    public string Name { get; private set; } = "";
    public void Rename(string name) => Name = name;
}
```

A `class` is a reference type; a `struct` is a value type (keep small and immutable); `record` adds value equality and concise data syntax. Interfaces define contracts; abstract classes can share state and implementation. Properties wrap accessors (`get`, `set`, `init`). `init` permits assignment during initialization. `static` belongs to the type. `sealed` prevents inheritance/override; `virtual` enables override. `ref`, `out`, and `in` are explicit parameter passing modes; normal parameters pass value (reference values are copied too).

## Branching, loops, and pattern matching

```csharp
foreach (var item in items) Process(item);
for (var i = 0; i < items.Count; i++) Process(items[i]);
while (TryRead(out var line)) Console.WriteLine(line);

var label = status switch
{
    Status.New => "pending",
    Status.Done or Status.Archived => "complete",
    _ => "unknown"
};
if (value is string { Length: > 0 } text) Use(text);
```

`break` exits a loop/switch; `continue` advances. `switch` expressions return a value and should cover expected cases (use `_` as fallback). `&&`/`||` short-circuit; `&`/`|` can evaluate both operands.

## Collections and LINQ

```csharp
List<string> names = ["Ada", "Grace"];
HashSet<string> unique = ["api", "worker"];
Dictionary<string, int> counts = new();
Queue<string> queue = new();
Stack<string> stack = new();

var activeEmails = users.Where(u => u.Active)
    .Select(u => u.Email).Distinct().Order().ToList();
var byTeam = users.GroupBy(u => u.Team).ToDictionary(g => g.Key, g => g.Count());
```

`List<T>`: indexed access O(1), append amortized O(1), interior insert/remove O(n). `Dictionary<TKey,TValue>` and `HashSet<T>` are average O(1), rely on correct stable equality/hash behavior. `SortedDictionary` is ordered with O(log n) lookup. `Queue<T>` is FIFO; `Stack<T>` LIFO. LINQ `IEnumerable<T>` operators are commonly deferred; materialize with `ToList`/`ToArray` when needed. `IQueryable<T>` builds provider-translated queries (e.g., SQL); inspect generated query and beware client-side evaluation and N+1 queries.

## Async, exceptions, disposal

```csharp
public async Task<Order?> FindAsync(Guid id, CancellationToken ct)
{
    return await repository.FindAsync(id, ct);
}
using var stream = File.OpenRead(path);
await using var connection = new AsyncConnection();
```

`async`/`await` frees a thread while awaiting I/O; it does not make CPU work faster. Prefer `Task`/`Task<T>`, propagate `CancellationToken`, avoid `async void` except event handlers, and avoid `.Result`/`.Wait()` deadlocks. `IDisposable`/`IAsyncDisposable` releases resources deterministically with `using`. Catch expected exceptions at a boundary, preserve inner exceptions, and don't use exceptions for routine branching. `finally` runs during unwinding, but process termination is exceptional.

## Generics, delegates, and memory

`Func<T,..., TResult>` represents a function; `Action<T...>` returns void; `Predicate<T>` returns bool. Events use delegates for notifications. Generic variance: `out T` is covariant (producer), `in T` contravariant (consumer), subject to interface/delegate rules. Garbage collection handles managed memory, not deterministic native/file/DB resource cleanup. `Span<T>`/`ReadOnlySpan<T>` can represent slices without allocations and cannot generally escape to the heap. `lock` protects a critical section; it is not distributed coordination. Prefer immutable data and concurrent collections when suitable.

## AI and agentic .NET services

Hide provider SDKs behind an injected interface; use `HttpClient` from `IHttpClientFactory`, cancellation tokens, and typed request/response models. Validate model-produced function arguments and authorize each tool call in ordinary application code. Run long workflows through a durable background queue and scoped services; avoid capturing request-scoped services in singleton workers. Redact traces and bound token, time, and spend budgets.

## Senior interview questions, answers, and reasoning

1. **Why does `IQueryable<T>` need different care from `IEnumerable<T>`?** `IQueryable` builds an expression tree a provider translates (often to SQL); unsupported calls may fail or execute client-side. Inspect generated queries and keep database-side filtering/projection before materialization.
2. **Why avoid `.Result` in async code?** It blocks a thread and can deadlock in contexts with synchronization; it also harms scalability. Use `await`, propagate `CancellationToken`, and keep async all the way through I/O boundaries.
3. **When use a `struct` versus a class?** A struct is copied by value and suits small value-like data with clear equality and limited mutation. Large/mutable structs create copying surprises; a class has identity/reference semantics.
4. **How do you choose DI lifetimes?** Singleton for thread-safe app-wide state, scoped per request/unit of work, transient for lightweight stateless construction. Avoid capturing scoped dependencies in singletons; workers should create scopes per job.
5. **How would you diagnose EF Core N+1?** Inspect generated SQL/command counts and query plan; project only needed fields, use explicit eager loading/joins or batching, and avoid indiscriminate `Include` that explodes row counts. Measure round trips and memory.
6. **How do you expose an AI tool safely in .NET?** Define a typed operation, validate arguments, authorize the current principal and resource on every invocation, propagate cancellation/deadlines, and cap calls. Keep provider configuration and secrets in managed configuration, never prompt text.
