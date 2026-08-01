import { Subject } from 'rxjs';
import { WebsocketService } from './websocket.service';
import { RegistrationSyncUpdated } from '../backend-types';
import { clientID, formatCurrentMoment } from '../utils';

export class RegistrationWebsocketService extends WebsocketService {
  registrationSyncUpdated = new Subject<RegistrationSyncUpdated>();
  private wettkampfUUID: string;

  constructor(wettkampfUUID: string) {
    super();
    this.wettkampfUUID = wettkampfUUID;
  }

  protected getWebsocketBackendUrl(): string {
    let host = location.host;
    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
    const apiPath = 'api/registrations/' + this.wettkampfUUID + '/sync-ws';
    if (!host || host === '') {
      host = 'wss://kutuapp.sharevic.net/';
    } else {
      host = (protocol + '//' + host + '/').replace('index.html', '');
    }
    return host + apiPath;
  }

  public initWebsocket() {
    this.logMessages.subscribe(msg => {
      this.lastMessages.push(formatCurrentMoment(true) + ` - ${msg}`);
      this.lastMessages = this.lastMessages.slice(Math.max(this.lastMessages.length - 50, 0));
    });
    this.logMessages.next('init registration sync ws');
    this.backendUrl = this.getWebsocketBackendUrl()
      + `?clientid=${encodeURIComponent(clientID())}`;
    this.logMessages.next('init with ' + this.backendUrl);
    this.connect();
    this.startKeepAliveObservation();
  }

  protected handleWebsocketMessage(message: any): boolean {
    const type = message.type;
    switch (type) {
      case 'RegistrationSyncUpdated':
        this.registrationSyncUpdated.next(message as RegistrationSyncUpdated);
        return true;
      default:
        return false;
    }
  }
}
