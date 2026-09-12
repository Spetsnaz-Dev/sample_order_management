# AI Coding Agent Log

## Candidate input

I need to review and fix an order management system. Please preserve the public API where possible. Focus on invalid state transitions, duplicate operations, thread safety, visibility of status reads, and unique IDs under concurrent creation.

## Agent output

I inspected the state machine before changing the implementation. The important invariant is that every mutation must validate and commit the transition while holding the same lock used by readers. Otherwise cancel and ship can each observe PAID and both appear successful.

I used an enum for the lifecycle, a per-system lock for the shared map and ID allocation, and immutable Order snapshots for reads. `cancelOrder` and `shipOrder` serialize their decision and mutation, so only one can win from PAID.

## Candidate input

What tradeoff does the single lock create, and what would you improve for a high-throughput production service?

## Agent output

The single lock is simple and gives linearizable behavior, but unrelated orders also contend. A production design could use a concurrent map plus per-order locks, or a database transaction with a conditional update such as `WHERE id = ? AND status = ?`. The transition rules must remain atomic at the storage boundary.

## Candidate note

I chose the simple lock for this exercise because correctness and explainability are more important than premature throughput optimization.
