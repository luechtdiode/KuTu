import { Injectable } from '@angular/core';
import { Observable, BehaviorSubject, Subject, interval } from 'rxjs';
import { MessageAck } from '../backend-types';
import { formatCurrentMoment, clientID } from '../utils';
import { takeWhile } from 'rxjs/operators';

export function encodeURIComponent2(uri: string): string {
  return !!uri ? encodeURIComponent(uri.replace(/[,&.*+?/^${}()|[\]\\]/g, '_')) : '';
}

@Injectable({
  providedIn: 'root'
})
export abstract class WebsocketService {

  private identifiedState = false;
  private connectedState = false;
  private websocket: WebSocket;
  private backendUrl: string;
  private reconnectionObservable: Observable<number>;
  private explicitClosed = true;
  private reconnectInterval = 30000; // pause between connections
  private reconnectAttempts = 480; // number of connection attempts
  private lstKeepAliveReceived = 0;

  connected = new BehaviorSubject<boolean>(false);
  identified = new BehaviorSubject<boolean>(false);
  logMessages = new BehaviorSubject<string>('');
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
    this.lstKeepAliveReceived = 0;
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
    if (this.websocket) {
      this.websocket.onerror = undefined;
      this.websocket.onclose = undefined;
      this.websocket.onopen = undefined;
      this.websocket.onmessage = undefined;
      this.websocket.close();
    }
    this.websocket = undefined;
    this.identifiedState = false;
    this.lstKeepAliveReceived = 0;
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
  public shouldConnectAgain(): boolean {
    return !(this.isWebsocketConnected() || this.isWebsocketConnecting());
  }

  /// reconnection
  reconnect(): void {
    if (!this.reconnectionObservable) {
      this.logMessages.next('start try reconnection ...');
      this.reconnectionObservable = interval(this.reconnectInterval).pipe(
        takeWhile((v, index) => {
          return index < this.reconnectAttempts && !this.explicitClosed;
        }));
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
    this.disconnectWS();
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
                   if (!this.explicitClosed) {
            this.reconnect();
          }
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
      this.sendMessage(JSON.stringify({
        type: 'MessageAck',
        msg: evt.data
      } as MessageAck));
    };

    this.websocket.onmessage = (evt: MessageEvent) => {
      this.lstKeepAliveReceived = new Date().getTime();
      if (evt.data.startsWith('Connection established.')) {
        return;
      }
      if (evt.data === 'keepAlive') {
        // sendMessageAck(evt);
        return;
      }
      try {
        const jsonMessage = JSON.parse(evt.data);
        const type = jsonMessage.type;
        switch (type) {
          case 'MessageAck':
            console.log((jsonMessage as MessageAck).msg);
            this.showMessage.next((jsonMessage as MessageAck));
            break;
          default:
            if (!this.handleWebsocketMessage(jsonMessage)) {
              console.log(jsonMessage);
              this.logMessages.next('unknown message: ' + evt.data);
            }
        }
      } catch (e) {
        this.logMessages.next(e + ': ' + evt.data);
      }
    };

    this.websocket.onerror = (e: ErrorEvent) => {
      this.logMessages.next(e.message + ', ' + e.type);
      // this.close();
    };
  }

}
