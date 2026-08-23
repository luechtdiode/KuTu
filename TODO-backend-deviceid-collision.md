# TODO: Backend fix — deviceId collision on parallel websocket connections

**Status:** deferred (frontend refactor in `newclient/kutu-app` mitigates the main scenario)

## Problem

`CompetitionCoordinatorClientActor` tracks live websocket connections per device in a
single-valued map:

- `src/main/scala/ch/seidel/kutu/actors/CompetitionCoordinatorClientActor.scala`
  - `Subscribe` handler (~line 439):
    ```scala
    deviceWebsocketRefs = deviceWebsocketRefs + (deviceId -> ref)
    ```
    A second connection using the **same `clientid`** overwrites the first
    connection's entry.
  - `StopDevice(deviceId)` (~line 497): looks the deviceId up again and calls
    `cleanupWebsocketRefs(...)` with whatever ref is currently stored there.

### Consequence

When two websocket connections from the same browser (same `clientid`, derived by
`CIDSupport.handleCID` from IP + query/header clientid) are open at once:

1. Opening B overwrites the registry entry of A.
2. Closing either A or B removes **B's** registration from all subscription lists
   (`wsSend`, `adminClients`, ...) because that is what the map now holds.
3. The surviving connection silently stops receiving events.

**Live-verified (2026-08-23, dev server):** while both connections are open, the
harm is *not* visible — keepAlive fan-out iterates the `wsSend` /
`registrationSyncClients` collections, which still contain both refs. Both test
sockets received every 10s keepAlive. The failure therefore manifests exactly at
**disconnect time of either socket**, when `StopDevice` evicts the mapped
(*newest*) ref from the subscription collections and orphans the survivor.

This was hit regularly before the frontend unification, where
`BackendService` and `AdminWebsocketService` both dialed
`/api/durchgang/{comp}/all/ws` with identical `clientid`s and killed each other.
It still applies today for:

- two browser tabs/windows of the app (shared `localStorage.clientid`),
- any flow holding a competition channel and a registration sync channel at the
  same time (the frontend scopes its clientid by channel kind as a workaround,
  but desktop clients via `WebSocketClient.scala` and multi-tab users remain exposed).

## Suggested fix

Make the server-side device registry tolerate multiple concurrent connections:

- Option A: change `deviceWebsocketRefs: Map[String, ActorRef]` to
  `Map[String, Set[ActorRef]]` (or `Map[(String, ActorRef), ...]`) and adapt
  `Subscribe`, `StopDevice`, `cleanupWebsocketRefs` and every lookup
  (`actorWithSameDeviceIdOfSender()`, media player ownership checks) accordingly.
- Option B: key subscriptions by a unique subscription id instead of deviceId;
  keep deviceId only as metadata/logging context.

Also verify `CompetitionRegistrationClientActor` (same pattern,
~lines 57/82/122) when fixing.

## Acceptance check

- Open two tabs of the app on the same competition, start/finish a durchgang in
  one tab → the other tab must keep receiving `DurchgangStarted` /
  `AthletWertungUpdated` events.
- Close one tab → the other tab must continue receiving events.

## Smoke test protocol (2026-08-23)

Script: `/var/folders/.../opencode/wssmoke.mjs` (Node ≥21, native WebSocket) against
the local dev server (`http://localhost:5757`, competition GeTuTest
`2634ac89-b026-4d4b-b0da-671291000923`):

| Scenario | Endpoint | Result |
|---|---|---|
| Competition channel | `/api/durchgang/{uuid}/all/ws` | PASS — welcome, 4× keepAlive, data events, open after 38s |
| Registration channel | `/api/registrations/{uuid}/sync-ws` | PASS — coexists with competition channel |
| Second device, same channel | same URL, different clientid | PASS |
| Legacy duplicate clientid ×2 | same URL, identical clientid | both alive while connected (see Consequence note) |

All URLs match what the new `WsStateService` builds (`chunk-LPAO4KYJ.js`).
