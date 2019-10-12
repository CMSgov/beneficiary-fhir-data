# Rust?

## Goals

Need a working proof of concept that:

1. Is reasonably maintainable.
2. Reduces max latency by at least 30%.
    * A smashing success would cut it in half or better.
3. Can be deployed side-by-side with the existing code.
4. Can be activated via a feature flag.
5. Could gradually replace the entire BFD Server.

Additional constraints:

* Can't talk about this with anyone else until I can demonstrate that it's a **solid** win.
* Can't deploy it anywhere other than `test` until I've talked about it.

## Integration

How do I accomplish the integration goals above:
  a gradual, gentle replacement, bit-by-bit.

Go one query type at a time.
  Don't bother with intermediate steps like interprocess communication or Java interop;
    build a webapp that can support an entire query type end-to-end.