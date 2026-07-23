import { Injectable } from '@angular/core';
import { BehaviorSubject, Subject } from 'rxjs';
import { WebsocketService } from './websocket.service';
import { DurchgangStarted, DurchgangFinished, AthletWertungUpdated, BulkEvent, PlaybookStateUpdated } from '../backend-types';
import { clientID, formatCurrentMoment } from '../utils';

export interface DurchgangResetted {
  wettkampfUUID: string;
  durchgang: string;
  type: string;
}

export class AdminWebsocketService extends WebsocketService {
  private competitionUUID: string;
  private secret: string;

  durchgangStarted = new BehaviorSubject<DurchgangStarted[]>([]);
  durchgangStartedEvent = new Subject<DurchgangStarted>();
  durchgangFinished = new Subject<DurchgangFinished>();
  durchgangResetted = new Subject<DurchgangResetted>();
  wertungUpdated = new Subject<AthletWertungUpdated>();
  stepFinished = new Subject<void>();
  stationFinished = new Subject<void>();
  playbookStateUpdated = new Subject<PlaybookStateUpdated>();

  private _activeDurchgangList: DurchgangStarted[] = [];

  constructor(competitionUUID: string, secret: string) {
    super();
    this.competitionUUID = competitionUUID;
    this.secret = secret;
  }

  protected getWebsocketBackendUrl(): string {
    let host = location.host;
    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
    const apiPath = 'api/durchgang/' + this.competitionUUID + '/all/ws';
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
    this.logMessages.next('init');
    this.backendUrl = this.getWebsocketBackendUrl()
      + `?clientid=${encodeURIComponent(clientID())}&jwt=${encodeURIComponent(this.secret)}`;
    this.logMessages.next('init with ' + this.backendUrl);
    this.connect();
    this.startKeepAliveObservation();
  }

  protected handleWebsocketMessage(message: any): boolean {
    const type = message.type;
    switch (type) {
      case 'BulkEvent':
        return (message as BulkEvent).events.map(e => {
          return this.handleWebsocketMessage(e);
        }).reduce((a, b) => a && b, true);

      case 'DurchgangStarted':
        const started = message as DurchgangStarted;
        this._activeDurchgangList = [...this._activeDurchgangList, started]
          .filter((value, index, self) => self.findIndex(ds => ds.durchgang === value.durchgang) === index);
        this.durchgangStarted.next(this._activeDurchgangList);
        this.durchgangStartedEvent.next(started);
        return true;

      case 'DurchgangFinished':
        const finished = message as DurchgangFinished;
        this._activeDurchgangList = this._activeDurchgangList
          .filter(d => d.durchgang !== finished.durchgang || d.wettkampfUUID !== finished.wettkampfUUID);
        this.durchgangStarted.next(this._activeDurchgangList);
        this.durchgangFinished.next(finished);
        return true;

      case 'DurchgangResetted':
        const resetted = message as DurchgangResetted;
        this._activeDurchgangList = this._activeDurchgangList
          .filter(d => d.durchgang !== resetted.durchgang || d.wettkampfUUID !== resetted.wettkampfUUID);
        this.durchgangStarted.next(this._activeDurchgangList);
        this.durchgangResetted.next(resetted);
        return true;

      case 'AthletWertungUpdatedSequenced':
      case 'AthletWertungUpdated':
        this.wertungUpdated.next(message as AthletWertungUpdated);
        return true;

      case 'DurchgangStepFinished':
        this.stepFinished.next();
        return true;

      case 'DurchgangStationFinished':
        this.stationFinished.next();
        return true;

      case 'PlaybookStateUpdated':
        this.playbookStateUpdated.next(message as PlaybookStateUpdated);
        return true;

      default:
        return false;
    }
  }
}
