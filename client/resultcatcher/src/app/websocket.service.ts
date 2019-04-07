import { formatCurrentMoment, clientID } from './utils';
import { MessageAck } from './backend-types';
import { Observable } from 'rxjs/Observable';
import { Subject } from 'rxjs/Subject';
import { BehaviorSubject } from 'rxjs';

export function encodeURIComponent2(uri: string): string {
  return !!uri ? encodeURIComponent(uri.replace(/[,&.*+?/^${}()|[\]\\]/g, "_")) : '';
}

export abstract class WebsocketService {

  private identifiedState = false;
  private connectedState = false;
  private websocket: WebSocket;
  private backendUrl: string;
  private reconnectionObservable: Observable<number>;
  private explicitClosed = true;
  private reconnectInterval: number = 30000; // pause between connections
  private reconnectAttempts: number = 480; // number of connection attempts
  private lstKeepAliveReceived: number = 0;

  connected = new BehaviorSubject<boolean>(false);
  identified = new BehaviorSubject<boolean>(false);
  logMessages = new BehaviorSubject<string>("");
  showMessage = new Subject<MessageAck>();
  lastMessages: string[] = [];

  get stopped(): boolean {
    return this.explicitClosed;
  }

  private startKeepAliveObservation() {
    setTimeout(() => {
      const yet = new Date().getTime();
      const lastSeenSince = yet - this.lstKeepAliveReceived;
      if (!this.explicitClosed
         && !this.reconnectionObservable 
         && lastSeenSince > this.reconnectInterval) {
          this.logMessages.next('connection verified since ' + lastSeenSince + 'ms. It seems to be dead and need to be reconnected!');
          this.disconnectWS(false);
          this.reconnect();
      } else {
        this.logMessages.next('connection verified since ' + lastSeenSince + 'ms');
      }
      this.startKeepAliveObservation();
    }, this.reconnectInterval);    
  }

  sendMessage(message) {
    if (!this.websocket) {
      this.connect(message);
    } else if (this.connectedState) {
      this.websocket.send(message);
    }
  }

  disconnectWS(explicit = true) {
    this.explicitClosed = explicit;
    if (this.websocket) {
      this.websocket.close();
      if (explicit) {
        this.close();  
      }
    } else {
      this.close();
    }
  }

  private close() {
    this.websocket = undefined;
    this.identifiedState = false;
    this.identified.next(this.identifiedState);
    this.connectedState = false;
    this.connected.next(this.connectedState);
  }

  public isWebsocketConnected(): boolean {
    return this.websocket && this.websocket.readyState === this.websocket.OPEN;
  }
  private isWebsocketConnecting(): boolean {
    return this.websocket && this.websocket.readyState === this.websocket.CONNECTING;
  }
  private shouldConnectAgain(): boolean {
    return !(this.isWebsocketConnected() || this.isWebsocketConnecting());
  }

  /// reconnection
  reconnect(): void {
    if (!this.reconnectionObservable) {
      this.logMessages.next('start try reconnection ...');
      this.reconnectionObservable = Observable.interval(this.reconnectInterval)
        .takeWhile((v, index) => {
          return index < this.reconnectAttempts && !this.explicitClosed
        });
      const subsr = this.reconnectionObservable.subscribe(
        () => {
          if (this.shouldConnectAgain()) {
            this.logMessages.next('continue with reconnection ...');
            this.connect(undefined);
          }
        },
        null,
        () => {
          /// if the reconnection attempts are failed, then we call complete of our Subject and status
          this.reconnectionObservable = null;
          subsr.unsubscribe();
          if (this.isWebsocketConnected()) {
            this.logMessages.next('finish with reconnection (successfull)');
          } else if (this.isWebsocketConnecting()) {
            this.logMessages.next('continue with reconnection (CONNECTING)');
          } else if (!this.websocket || this.websocket.CLOSING || this.websocket.CLOSED) {
            this.disconnectWS();
            this.logMessages.next('finish with reconnection (unsuccessfull)');
          }
        });
    }
  }

  protected abstract getWebsocketBackendUrl(): string;
  protected abstract handleWebsocketMessage(message: any): boolean;

  public initWebsocket() {
    this.logMessages.subscribe(msg => {
      this.lastMessages.push(formatCurrentMoment(true) + ` - ${msg}`);
      this.lastMessages = this.lastMessages.slice(Math.max(this.lastMessages.length - 50, 0));
    });
    this.logMessages.next('init');
    this.backendUrl = this.getWebsocketBackendUrl() + `?clientid=${clientID()}`;
    this.logMessages.next('init with ' + this.backendUrl);

    this.connect(undefined);
    this.startKeepAliveObservation();    
  }

  private connect(message?: string) {
    this.explicitClosed = false;
    this.websocket = new WebSocket(this.backendUrl);
    this.websocket.onopen = () => {
      this.connectedState = true;
      this.connected.next(this.connectedState);
      if (message) {
        this.sendMessage(message);
      }
      // for(let idx = 0; idx < localStorage.length; idx++) {
      //   this.logMessages.next('LocalStore Item ' + localStorage.key(idx) + " = " + localStorage.getItem(localStorage.key(idx)));
      // }
    };

    this.websocket.onclose = (evt: CloseEvent) => {
      this.close();
      switch (evt.code) {
        case 1001: this.logMessages.next('Going Away');
          break;
        case 1002: this.logMessages.next('Protocol error');
          if (!this.explicitClosed) {
            /// in case of an error with a loss of connection, we restore it
            this.reconnect();
          }
          break;
        case 1003: this.logMessages.next('Unsupported Data');
          if (!this.explicitClosed) {
            /// in case of an error with a loss of connection, we restore it
            this.reconnect();
          }
          break;
        case 1005: this.logMessages.next('No Status Rcvd');
          if (!this.explicitClosed) {
            /// in case of an error with a loss of connection, we restore it
            this.reconnect();
          }
          break;
        case 1006: this.logMessages.next('Abnormal Closure');
          if (!this.explicitClosed) {
            /// in case of an error with a loss of connection, we restore it
            this.reconnect();
          }
          break;
        case 1007: this.logMessages.next('Invalid frame payload data');
          if (!this.explicitClosed) {
            /// in case of an error with a loss of connection, we restore it
            this.reconnect();
          }
          break;
        case 1008: this.logMessages.next('Policy Violation');
          if (!this.explicitClosed) {
            /// in case of an error with a loss of connection, we restore it
            this.reconnect();
          }
          break;
        case 1009: this.logMessages.next('Message Too Big');
          if (!this.explicitClosed) {
            /// in case of an error with a loss of connection, we restore it
            this.reconnect();
          }
          break;
        case 1010: this.logMessages.next('Mandatory Ext.');
          if (!this.explicitClosed) {
            /// in case of an error with a loss of connection, we restore it
            this.reconnect();
          }
          break;
        case 1011: this.logMessages.next('Internal Server Error');
          if (!this.explicitClosed) {
            /// in case of an error with a loss of connection, we restore it
            this.reconnect();
          }
          break;
        case 1015: this.logMessages.next('TLS handshake');
          break;
        default:
      }
    };

    const sendMessageAck = (evt: MessageEvent) => {
      this.sendMessage(JSON.stringify(<MessageAck>{
        'type': 'MessageAck',
        'msg': evt.data
      }));
    };

    this.websocket.onmessage = (evt: MessageEvent) => {
      this.lstKeepAliveReceived = new Date().getTime();
      if (evt.data.startsWith('Connection established.')) {
        return;
      }
      if (evt.data === 'keepAlive') {
        sendMessageAck(evt);
        return;
      }
      try {
        const message = JSON.parse(evt.data);
        const type = message['type'];
        switch (type) {
          case 'MessageAck':
            console.log((message as MessageAck).msg);
            this.showMessage.next((message as MessageAck));
            break;
          default:
            if (!this.handleWebsocketMessage(message)) {
              console.log(message);
              this.logMessages.next('unknown message: ' + evt.data);
            }
        }
      } catch (e) {
        this.logMessages.next(e + ": " + evt.data);
      }
    };

    this.websocket.onerror = (e: ErrorEvent) => {
      this.close();
    };
  };
  
}