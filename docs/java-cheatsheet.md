# Java interview cheat sheet (Java 17/21/27)

## Types, variables, and nullability

```java
int retries = 3;                 // primitive, stored by value
Integer maybeRetries = null;    // wrapper/reference, nullable
long count = 4_000_000_000L;
double ratio = 0.25;
char initial = 'A';
String name = "Ada";            // immutable reference type
var labels = List.of("api");   // local inference; still statically typed
final int timeout = 30;         // cannot reassign this variable
```

Primitives include `byte`, `short`, `int`, `long`, `float`, `double`, `char`, `boolean`. Widening numeric conversions are generally safe; narrowing conversions need a cast and may lose data. `==` compares primitive values and object identity. Use `.equals` for value equality; use `Objects.equals(a,b)` when either side may be null. Autoboxing converts primitives and wrappers but can allocate and can throw on null unboxing.

## Classes, objects, and interfaces

```java
public interface Payable { BigDecimal amount(); }
public record Invoice(String number, BigDecimal amount) implements Payable { }

public final class Customer {
    private final String id;
    public Customer(String id) { this.id = Objects.requireNonNull(id); }
    public String id() { return id; }
}
```

A class describes state and behavior; `new` creates an instance. Encapsulate state behind methods. Prefer composition and small interfaces to deep inheritance. `record` is concise for transparent data carriers (final, component accessors, value equality); it is shallowly immutable. Override `equals`/`hashCode` together when defining value equality. An abstract class can hold shared implementation/state; an interface defines a contract and may have default/static methods. `static` belongs to the class; instance members belong to an object. `this` is the current instance; `super` accesses a parent implementation.

Access: `private` class-only; package-private (no keyword) same package; `protected` same package/subclasses; `public` everywhere. `final` prevents reassignment/overriding/inheritance depending on where used. `sealed`/`permits` restrict subtype lists.

## Control flow and methods

```java
for (int i = 0; i < items.size(); i++) { use(items.get(i)); }
for (var item : items) { use(item); }
while (ready()) { poll(); }
if (value instanceof String text) { use(text); }
String label = switch (status) {
    case NEW -> "pending";
    case DONE, ARCHIVED -> "complete";
};
```

`break` exits a loop/switch; `continue` advances the loop. Methods are overloaded by parameter list, not return type. Java passes arguments by value (object references are values too); a method can mutate the referenced object but cannot replace the caller's variable. `static` methods have no `this`.

## Collections and data structures

```java
List<String> names = new ArrayList<>();             // ordered, duplicates
Set<String> unique = new HashSet<>();               // uniqueness, no order guarantee
Set<String> sorted = new TreeSet<>();               // sorted, O(log n) typical
Map<String, Integer> counts = new HashMap<>();      // key/value lookup, average O(1)
Deque<String> queue = new ArrayDeque<>();            // FIFO/LIFO operations
Queue<String> jobs = new ArrayDeque<>();
Map<String, Integer> immutable = Map.of("a", 1);   // unmodifiable
```

`ArrayList`: fast indexed reads, append amortized O(1), middle insert/remove O(n). `LinkedList`: O(1) at ends via deque API, but indexed lookup O(n); rarely the default choice. `HashMap`/`HashSet`: average O(1), requires stable `equals`/`hashCode`; tree variants keep sort order at O(log n). `PriorityQueue` exposes the least element by default; peek O(1), add/remove O(log n). `ArrayDeque` is usually preferable to legacy `Stack` or `LinkedList` for stack/queue use. Complexity is typical, not an unconditional guarantee.

Generics provide compile-time type safety: `List<String>`. They are invariant (`List<Integer>` is not a `List<Number>`). PECS: producer `? extends T`, consumer `? super T`; wildcard reads are safe, writes are restricted. Prefer interface declarations (`List`) and immutable factory methods when mutation is unnecessary.

## Streams, lambdas, Optional

```java
List<String> result = users.stream()
    .filter(User::active)
    .map(User::email)
    .distinct()
    .sorted()
    .toList();
long total = orders.stream().mapToLong(Order::cents).sum();
Optional<User> user = repository.findById(id);
String display = user.map(User::name).orElse("unknown");
```

Intermediate stream stages are lazy; a terminal operation (`toList`, `collect`, `count`, `forEach`) triggers evaluation. Streams are one-use. Avoid side effects in pipelines and parallel streams unless work is independent and measured. `Optional` is useful primarily as a return type for possibly absent values, not as a field/parameter by default; avoid `get()` without presence checks.

## Exceptions, resources, and concurrency

```java
try (var reader = Files.newBufferedReader(path)) { return reader.readLine(); }
try { parse(input); } catch (NumberFormatException ex) { log.warn("Bad input", ex); }
```

Checked exceptions must be caught or declared; runtime exceptions need not. Catch the narrowest useful type, preserve causes, and do not swallow errors. Try-with-resources closes `AutoCloseable` resources. Use `ExecutorService`/`CompletableFuture` for asynchronous work and define ownership/shutdown; shared mutable state needs synchronization/locks/atomics or confinement. `volatile` guarantees visibility, not compound-operation atomicity. Virtual threads, introduced in Java 21 and available in Java 27, make blocking I/O concurrency cheaper, not CPU work faster.

## AI and agentic Java services

Wrap provider SDKs behind an interface, pass request deadlines/cancellation through to network calls, and deserialize structured output into validated records. Use bounded executors or virtual threads for blocking I/O, not unbounded task creation. Put long inference work on a durable queue; persist idempotency/task state and avoid holding a JPA transaction while calling a model. Keep secrets and provider clients out of prompts and logs. Use fakes for deterministic unit tests and separate provider contract/integration tests.

## Senior interview questions, answers, and reasoning

1. **Why does `HashMap` require consistent `equals` and `hashCode`?** Hash selects a bucket and equality selects a matching key inside it. Equal objects must have the same hash or lookup can fail. Mutating fields used in either method after insertion can make an entry unreachable.
2. **When choose `ArrayList` over `LinkedList`?** Usually `ArrayList`: contiguous storage gives fast indexing and cache locality; append is amortized O(1). `LinkedList` only helps when frequent insertion/removal occurs at a known node/ends and traversal cost is acceptable.
3. **What does `volatile` guarantee?** Visibility and ordering for reads/writes of that variable under Java's memory model; it does not make compound operations such as `count++` atomic. Use synchronization, atomics, or confinement for read-modify-write invariants.
4. **How would you design an immutable value object?** Make state final, validate at construction, avoid exposing mutable internals, defensively copy mutable inputs/outputs, and define value equality. A record is shallowly immutable, so mutable components still need care.
5. **What changes when using virtual threads?** Blocking I/O can use a thread-per-task style with lower thread cost, but CPU remains bounded by cores and downstream pools still constrain capacity. Apply admission control, deadlines, resource ownership, and avoid pinning-sensitive synchronized/native paths.
6. **How should a Java service call an AI model?** Keep the client behind an interface, propagate deadline/cancellation, validate structured output, bound concurrency/cost, and keep network calls outside DB transactions. Test business policy with a fake and provider serialization with contract tests.
