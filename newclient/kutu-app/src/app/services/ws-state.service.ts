import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, Subject, Subscription, interval } from 'rxjs';
import { takeWhile } from 'rxjs/operators';
import {
  MessageAck,
  DurchgangStarted,
  DurchgangFinished,
  DurchgangResetted,
  AthletWertungUpdated,
  BulkEvent,
  PlaybookStateUpdated,
  RiegenEinteilungStateUpdated,
  RegistrationSyncUpdated,
  NewLastResults,
  AthletMediaIsAtStart,
  AthletMediaIsRunning,
  AthletMediaIsPaused,
  AthletMediaIsFree
} from '../backend-types';
import { clientID, formatCurrentMoment, encodeURIComponent2 } from '../utils';
import { SecretService } from './secret.service';

/**
 * Identifies a websocket channel. One physical websocket is shared per channel key.
 */
export type CompetitionChannel = { kind: 'competition'; competitionId: string };
export type RegistrationChannel = { kind: 'registration'; wettkampfUUID: string };
export type WsChannelKey = CompetitionChannel | RegistrationChannel;

export function channelKeyId(key: WsChannelKey): string {
  return key.kind === 'competition'
    ? 'competition:' + key.competitionId
    : 'registration:' + key.wettkampfUUID;
}

const RECONNECT_INTERVAL = 30000; // pause between connections
const RECONNECT_ATTEMPTS = 480;   // number of connection attempts

/**
 * Manages a single websocket connection including reconnection and keep-alive watchdog.
 * Ported from the former abstract WebsocketService base class.
 */
class WsConnection {
  private websocket?: WebSocket;
  private reconnectionSubscription?: Subscription;
  private explicitClosed = true;
  private lstKeepAliveReceived = 0;
  private watchdogRunning = false;

  readonly connected = new BehaviorSubject<boolean>(false);

  constructor(
    private readonly urlProvider: () => string,
    private readonly handleMessage: (jsonMessage: any) => void,
    private readonly log: (msg: string) => void
  ) {}

  get stopped(): boolean {
    return this.explicitClosed;
  }

  open() {
    this.explicitClosed = false;
    this.lstKeepAliveReceived = new Date().getTime();
    this.connect();
    this.startKeepAliveObservation();
  }

  /** force teardown + redial with fresh url (e.g. after focus/privilege change) */
  resync() {
    this.open();
  }

  ensureConnected() {
    if (this.shouldConnectAgain()) {
      this.open();
    }
  }

  disconnect(explicit = true) {
    this.explicitClosed = explicit;
    this.lstKeepAliveReceived = 0;
    this.teardown();
    if (!explicit) {
      // implicit close (dead connection) - reconnect loop takes over
      this.reconnect();
    }
  }

  isConnected(): boolean {
    return !!this.websocket && this.websocket.readyState === this.websocket.OPEN;
  }

  isConnecting(): boolean {
    return !!this.websocket && this.websocket.readyState === this.websocket.CONNECTING;
  }

  shouldConnectAgain(): boolean {
    return !(this.isConnected() || this.isConnecting());
  }

  private reconnecting(): boolean {
    return !!this.reconnectionSubscription;
  }

  private teardown() {
    if (this.reconnectionSubscription) {
      this.reconnectionSubscription.unsubscribe();
      this.reconnectionSubscription = undefined;
    }
    if (this.websocket) {
      this.websocket.onopen = undefined;
      this.websocket.onclose = undefined;
      this.websocket.onmessage = undefined;
      this.websocket.onerror = undefined;
      this.websocket.close();
    }
    this.websocket = undefined;
    this.connected.next(false);
  }

  private connect() {
    const url = this.urlProvider();
    this.log('init with ' + url);
    this.teardown();
    this.explicitClosed = false;
    this.websocket = new WebSocket(url);

    this.websocket.onopen = () => {
      this.log('connection established');
      this.connected.next(true);
      // (re-)arm the keep-alive watchdog, e.g. after a successful reconnection
      this.startKeepAliveObservation();
    };

    this.websocket.onclose = (evt: CloseEvent) => {
      this.log('connection closed (' + evt.code + ')');
      this.teardown();
      switch (evt.code) {
        case 1001 /* Going Away */:
        case 1002 /* Protocol error */:
        case 1003 /* Unsupported Data */:
        case 1005 /* No Status Rcvd */:
        case 1006 /* Abnormal Closure */:
        case 1007 /* Invalid frame payload data */:
        case 1008 /* Policy Violation */:
        case 1009 /* Message Too Big */:
        case 1010 /* Mandatory Ext. */:
        case 1011 /* Internal Server Error */:
          if (!this.explicitClosed) {
            this.reconnect();
          }
          break;
        case 1015:
          this.log('TLS handshake');
          break;
        default:
      }
    };

    this.websocket.onmessage = (evt: MessageEvent) => {
      this.lstKeepAliveReceived = new Date().getTime();
      if (typeof evt.data === 'string' && evt.data.startsWith('Connection established.')) {
        return;
      }
      if (evt.data === 'keepAlive') {
        return;
      }
      try {
        const jsonMessage = JSON.parse(evt.data);
        this.handleMessage(jsonMessage);
      } catch (e) {
        this.log(e + ': ' + evt.data);
      }
    };

    this.websocket.onerror = (e: ErrorEvent) => {
      this.log(e.message + ', ' + e.type);
    };
  }

  private startKeepAliveObservation() {
    if (this.watchdogRunning) {
      return;
    }
    this.watchdogRunning = true;
    const watch = () => {
      setTimeout(() => {
        const yet = new Date().getTime();
        const lastSeenSince = yet - this.lstKeepAliveReceived;
        if (!this.explicitClosed && !this.reconnecting() && lastSeenSince > RECONNECT_INTERVAL) {
          this.log('connection verified since ' + lastSeenSince + 'ms. It seems to be dead and need to be reconnected!');
          this.disconnect(false);
          this.watchdogRunning = false;
          return;
        } else {
          this.log('connection verified since ' + lastSeenSince + 'ms');
        }
        if (!this.explicitClosed) {
          watch();
        } else {
          this.watchdogRunning = false;
        }
      }, RECONNECT_INTERVAL);
    };
    watch();
  }

  private reconnect() {
    if (this.reconnecting()) {
      return;
    }
    this.log('start try reconnection ...');
    const reconnectionObservable = interval(RECONNECT_INTERVAL).pipe(
      takeWhile((v, index) => index < RECONNECT_ATTEMPTS && !this.explicitClosed)
    );
    this.reconnectionSubscription = reconnectionObservable.subscribe(() => {
      if (this.shouldConnectAgain()) {
        this.log('continue with reconnection ...');
        this.connect();
      }
    }, null, () => {
      this.reconnectionSubscription = undefined;
      if (this.isConnected()) {
        this.log('finish with reconnection (successfull)');
      } else if (this.isConnecting()) {
        this.log('continue with reconnection (CONNECTING)');
      } else {
        this.log('finish with reconnection (unsuccessfull)');
      }
    });
  }
}

interface ChannelEntry {
  key: WsChannelKey;
  refs: number;
  focusDurchgang?: string;
  connection?: WsConnection;
  lastMessages: string[];
}

/**
 * Central websocket state service.
 *
 * Owns all websocket connections of the app. Pages request a shared, reference-counted
 * connection per channel instead of opening their own sockets - this avoids parallel
 * competing connections to the same endpoint (which the backend resolves by clientid
 * and would tear each other down).
 *
 * All server events are routed into a single set of subjects, so app state stays
 * consistent regardless of which page requested the connection.
 */
@Injectable({
  providedIn: 'root'
})
export class WsStateService {
  private secretService = inject(SecretService);

  //// unified state exposed to the app

  /** active durchgaenge across all connected competitions */
  durchgangStarted = new BehaviorSubject<DurchgangStarted[]>([]);
  durchgangStartedEvent = new Subject<DurchgangStarted>();
  durchgangFinished = new Subject<DurchgangFinished>();
  durchgangResetted = new Subject<DurchgangResetted>();

  wertungUpdated = new Subject<AthletWertungUpdated>();
  /** startlist changed (athlets moved in / removed from wettkampf) */
  athletAssignmentChanged = new Subject<void>();

  stepFinished = new Subject<void>();
  stationFinished = new Subject<void>();

  playbookStateUpdated = new Subject<PlaybookStateUpdated>();
  riegeEinteilungStateUpdated = new Subject<RiegenEinteilungStateUpdated>();
  registrationSyncUpdated = new Subject<RegistrationSyncUpdated>();

  newLastResults = new BehaviorSubject<NewLastResults>(undefined);

  mediaStateChanged = new BehaviorSubject<AthletMediaIsAtStart | AthletMediaIsRunning | AthletMediaIsPaused | AthletMediaIsFree>({context: '', type: 'AthletMediaIsFree'} as AthletMediaIsFree);
  mediaPlayerAvailable = new BehaviorSubject<boolean>(false);

  /** global message bus for MessageAcks (websocket + http error handling) */
  showMessage = new Subject<MessageAck>();

  private channels = new Map<string, ChannelEntry>();
  private _activeDurchgangList: DurchgangStarted[] = [];

  get activeDurchgangList(): DurchgangStarted[] {
    return this._activeDurchgangList;
  }

  //// lifecycle api for pages

  acquire(key: WsChannelKey) {
    const entry = this.getOrCreateEntry(key);
    entry.refs++;
    if (entry.connection) {
      entry.connection.ensureConnected();
    } else {
      this.openConnection(entry);
    }
  }

  release(key: WsChannelKey) {
    const id = channelKeyId(key);
    const entry = this.channels.get(id);
    if (!entry) {
      return;
    }
    entry.refs--;
    if (entry.refs <= 0 && entry.connection) {
      entry.connection.disconnect(true);
      this.channels.delete(id);
    }
  }

  /** force teardown + redial of the channel (fresh squash of server state) */
  resync(key: WsChannelKey) {
    const entry = this.getOrCreateEntry(key);
    if (entry.connection) {
      entry.connection.resync();
    } else {
      this.openConnection(entry);
    }
  }

  ensureConnected(key: WsChannelKey) {
    const entry = this.getOrCreateEntry(key);
    if (entry.connection) {
      entry.connection.ensureConnected();
    } else {
      this.openConnection(entry);
    }
  }

  isConnected(key: WsChannelKey): boolean {
    return this.channels.get(channelKeyId(key))?.connection?.isConnected() || false;
  }

  /**
   * Sets the durchgang focus for a competition channel (caption mode).
   * A change triggers a redial so the server sends only events of that durchgang.
   */
  setDurchgangFocus(competitionId: string, durchgang?: string) {
    const focus = durchgang && durchgang !== 'undefined' ? durchgang : undefined;
    const id = channelKeyId({kind: 'competition', competitionId});
    let entry = this.channels.get(id);
    if (!entry) {
      if (!focus) {
        return;
      }
      entry = this.getOrCreateEntry({kind: 'competition', competitionId});
    }
    if (entry.focusDurchgang !== focus) {
      entry.focusDurchgang = focus;
      if (entry.connection) {
        entry.connection.resync();
      }
    }
  }

  //// internals

  private getOrCreateEntry(key: WsChannelKey): ChannelEntry {
    const id = channelKeyId(key);
    let entry = this.channels.get(id);
    if (!entry) {
      entry = {
        key,
        refs: 0,
        lastMessages: []
      };
      this.channels.set(id, entry);
    }
    return entry;
  }

  private openConnection(entry: ChannelEntry) {
    if (!entry.connection) {
      entry.connection = new WsConnection(
        () => this.buildWebsocketUrl(entry),
        message => this.routeMessage(message, entry),
        msg => this.pushLog(entry, msg)
      );
    }
    entry.connection.open();
  }

  private pushLog(entry: ChannelEntry, msg: string) {
    entry.lastMessages.push(formatCurrentMoment(true) + ` - ${msg}`);
    entry.lastMessages = entry.lastMessages.slice(Math.max(entry.lastMessages.length - 50, 0));
  }

  private buildWebsocketUrl(entry: ChannelEntry): string {
    let host = location.host;
    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
    if (!host || host === '') {
      host = 'wss://kutuapp.sharevic.net/';
    } else {
      host = (protocol + '//' + host + '/').replace('index.html', '');
    }
    // scope clientid by channel kind so parallel channels of one device
    // don't collide on the servers device registry
    const params = [`clientid=${encodeURIComponent(clientID() + ':' + entry.key.kind)}`];
    let apiPath: string;
    switch (entry.key.kind) {
      case 'competition': {
        if (entry.focusDurchgang) {
          apiPath = 'api/durchgang/' + entry.key.competitionId + '/' + encodeURIComponent2(entry.focusDurchgang) + '/ws';
        } else {
          apiPath = 'api/durchgang/' + entry.key.competitionId + '/all/ws';
        }
        const secret = this.secretService.getSecret(entry.key.competitionId)?.secret;
        if (secret) {
          params.push(`jwt=${encodeURIComponent(secret)}`);
        }
        break;
      }
      case 'registration': {
        apiPath = 'api/registrations/' + entry.key.wettkampfUUID + '/sync-ws';
        break;
      }
    }
    return host + apiPath + '?' + params.join('&');
  }

  private routeMessage(message: any, entry: ChannelEntry): void {
    const type = message.type;
    switch (type) {
      case 'BulkEvent':
        (message as BulkEvent).events.forEach(e => this.routeMessage(e, entry));
        return;

      case 'MessageAck':
        console.log((message as MessageAck).msg);
        this.showMessage.next(message as MessageAck);
        return;

      case 'DurchgangStarted': {
        const started = message as DurchgangStarted;
        this._activeDurchgangList = [...this._activeDurchgangList, started]
          .filter((value, index, self) => self.findIndex(ds => ds.durchgang === value.durchgang) === index);
        this.durchgangStarted.next(this._activeDurchgangList);
        this.durchgangStartedEvent.next(started);
        return;
      }

      case 'DurchgangFinished': {
        const finished = message as DurchgangFinished;
        this._activeDurchgangList = this._activeDurchgangList
          .filter(d => d.durchgang !== finished.durchgang || d.wettkampfUUID !== finished.wettkampfUUID);
        this.durchgangStarted.next(this._activeDurchgangList);
        this.durchgangFinished.next(finished);
        return;
      }

      case 'DurchgangResetted': {
        const resetted = message as DurchgangResetted;
        this._activeDurchgangList = this._activeDurchgangList
          .filter(d => d.durchgang !== resetted.durchgang || d.wettkampfUUID !== resetted.wettkampfUUID);
        this.durchgangStarted.next(this._activeDurchgangList);
        this.durchgangResetted.next(resetted);
        return;
      }

      case 'AthletWertungUpdatedSequenced':
      case 'AthletWertungUpdated':
        this.wertungUpdated.next(message as AthletWertungUpdated);
        return;

      case 'AthletMovedInWettkampf':
      case 'AthletRemovedFromWettkampf':
        this.athletAssignmentChanged.next();
        return;

      case 'NewLastResults':
        this.newLastResults.next(message as NewLastResults);
        return;

      case 'MediaPlayerIsReady':
        this.mediaPlayerAvailable.next(true);
        return;
      case 'MediaPlayerDisconnected':
        this.mediaPlayerAvailable.next(false);
        return;
      case 'AthletMediaIsAtStart':
        this.mediaStateChanged.next(message as AthletMediaIsAtStart);
        return;
      case 'AthletMediaIsRunning':
        this.mediaStateChanged.next(message as AthletMediaIsRunning);
        return;
      case 'AthletMediaIsPaused':
        this.mediaStateChanged.next(message as AthletMediaIsPaused);
        return;
      case 'AthletMediaIsFree':
        this.mediaStateChanged.next(message as AthletMediaIsFree);
        return;

      case 'DurchgangStepFinished':
        this.stepFinished.next();
        return;

      case 'DurchgangStationFinished':
        this.stationFinished.next();
        return;

      case 'PlaybookStateUpdated':
        this.playbookStateUpdated.next(message as PlaybookStateUpdated);
        return;

      case 'RiegenEinteilungStateUpdated':
        this.riegeEinteilungStateUpdated.next(message as RiegenEinteilungStateUpdated);
        return;

      case 'RegistrationSyncUpdated':
        this.registrationSyncUpdated.next(message as RegistrationSyncUpdated);
        return;

      default:
        console.log(message);
        this.pushLog(entry, 'unknown message: ' + JSON.stringify(message));
        return;
    }
  }
}
