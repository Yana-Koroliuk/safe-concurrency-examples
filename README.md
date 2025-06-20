# Safe-concurrency code samples
(Java / C# / Kotlin)

These three mini-projects all solve the same problem:  
**simulate 100 000 concurrent money transfers across 1 000 accounts while guaranteeing that the final sum of balances remains unchanged.**  
Each language uses an idiomatic mechanism to prevent race conditions and state corruption in a parallel or fully-asynchronous environment.

---

## 1 . Concept and purpose of each example

* **Java**  
  A miniature “bank” is created in which every transfer acquires `StampedLock` write-locks on exactly the two accounts involved.  A fixed lock-ordering eliminates dead-locks, while optimistic reads allow the total balance to be computed without blocking writers.  Virtual threads enable the execution of one hundred thousand blocking tasks without saturating the thread pool.

* **C#**  
  Locks are replaced with an **actor** running behind a single-reader `Channel`.  Producer tasks enqueue `(TransferMessage, TaskCompletionSource<bool>)` tuples; the actor is the only code that touches the shared `int[]` balances.  Each producer awaits a Boolean acknowledgment and, if necessary, retries.

* **Kotlin**  
  The same actor pattern is implemented with **coroutines**.  A coroutine-based actor receives transfer messages; producers are lightweight coroutines that suspend on a `Deferred<Boolean>` until the actor replies.  Because suspension parks the coroutine instead of the OS thread, hundreds of thousands of producers can run with modest memory use.

All three demos end by printing`Σ = 1 000 000`, proving that the total balance remains intact under high concurrency.

---

## 2 . Effectiveness of the approaches and languages

**Java with `StampedLock`** provides the lowest end-to-end latency: each transfer updates memory directly under two fine-grained locks, and virtual threads eliminate classic thread-explosion problems.  The price is manual lock management—lock ordering, unlock ordering, and validation of optimistic reads must all be handled explicitly.

**C# with a channel-based actor** offers straightforward conceptual model: one task owns the state, other tasks merely send messages.  No explicit locks are written, so dead-locks and write races cannot occur.  Throughput rivals or exceeds the Java version, though each message allocates an additional `TaskCompletionSource`, and queueing introduces a small extra latency.

**Kotlin coroutines** reproduce the actor concept with suspension rather than blocking.  Latency is slightly higher—each park/unpark adds a few hundred nanoseconds—but the model excels when massive fan-out is required, because millions of coroutines can reside in memory that would hold only thousands of threads.

### Conclusion

* The **StampedLock-based approach in Java** is most appropriate when a project demands the lowest possible latency and highest throughput, and when the development team is comfortable managing explicit, fine-grained locking discipline.

* The **channel-driven actor model in C#** is the preferred choice when design clarity is paramount and the environment intends to leverage .NET’s mature async/await infrastructure together with its first-class `Channel` abstraction.

* The **coroutine actor pattern in Kotlin** becomes the method of choice when the workload requires extreme logical concurrency—hundreds of thousands of lightweight tasks—while still benefitting from the ergonomic suspension model that coroutines provide.

Ultimately, the final selection should be guided by the characteristics of the domain problem, the required concurrency profile, and the surrounding ecosystem and tooling in which the system will be developed and maintained.
