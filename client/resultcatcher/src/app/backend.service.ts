import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { backendUrl } from './utils';
import { WertungContainer, Geraet, Wettkampf, Wertung, MessageAck, AthletWertungUpdated, DurchgangStarted, DurchgangFinished, FinishDurchgangStation } from './backend-types';
import { Observable } from 'rxjs/Observable';
import { Subject } from 'rxjs/Subject';
import { Subscription, BehaviorSubject } from 'rxjs';
import { tokenNotExpired } from 'angular2-jwt';
import { interval } from 'rxjs/observable/interval';
import { WebsocketService, encodeURIComponent2 } from './websocket.service';

declare var location: any;

@Injectable()
export class BackendService extends WebsocketService {

  externalLoaderSubscription: Subscription;

  loggedIn = false;
  stationFreezed = false;

  competitions: Wettkampf[];
  durchgaenge: string[];
  geraete: Geraet[];
  steps: number[];
  wertungen: WertungContainer[];

  private _competition: string = undefined;
  get competition(): string {
    return this._competition;
  }
  private _durchgang: string = undefined;
  get durchgang(): string {
    return this._durchgang;
  }
  private _geraet: number = undefined;
  get geraet(): number {
    return this._geraet;
  }
  private _step: number = undefined;
  get step(): number {
    return this._step;
  }

  constructor(public http: HttpClient) {
    super();
    this.externalLoaderSubscription = interval(1000).subscribe(latest => {
      this.loggedIn = this.checkJWT();
      const initWith = localStorage.getItem("external_load");
      if (initWith) {
        this.initWithQuery(initWith);
      } else {
        this.initWithQuery(localStorage.getItem('current_station'));
      }
    });
      
  }

  initWithQuery(initWith: string) {
    if (initWith && initWith.startsWith('c=') ) {
      initWith.split('&').forEach(param => {
        const [key, value] = param.split('=')
        switch (key) {
          case 's':
            localStorage.setItem('auth_token', value);
            this.loggedIn = this.checkJWT();
            break;
          case 'c':
            this._competition = value;
            break;
          case 'd':
            this._durchgang = value;
            break;
          case 'g':
            this._geraet = parseInt(value);
            localStorage.setItem('current_station', initWith);
            this.stationFreezed = true;
          default:
            this._step = 1;
      }});
      localStorage.removeItem("external_load");
      this.externalLoaderSubscription.unsubscribe();
      if (this._geraet) {
        this.getCompetitions();
        this.loadDurchgaenge();    
        this.loadGeraete();
        this.loadSteps();
        this.loadWertungen();
      } else if (this._competition) {
        this.getCompetitions();
        this.loadDurchgaenge();            
      }
    }
  }

  private checkJWT(): boolean {
    return tokenNotExpired('auth_token');
  }

  login(username, password) {
    const headers = new HttpHeaders();
    this.http.options(backendUrl + 'api/login', { 
      observe: 'response', 
      headers: headers.set('Authorization', 'Basic ' + btoa(username + ':' + password )),
      withCredentials: true,
      responseType: 'text'
    }).subscribe((data) => {
      console.log(data);
      localStorage.setItem('auth_token', data.headers.get('x-access-token'));
      this.loggedIn = this.checkJWT();
    }, (err) => {
      console.log(err);
      localStorage.removeItem('auth_token');
      this.loggedIn = this.checkJWT();
    });
  }
  
  unlock() {
    localStorage.removeItem('current_station');
    this.loggedIn = this.checkJWT();
    this.stationFreezed = false;
  }

  logout() {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('current_station');
    this.loggedIn = this.checkJWT();
    this.stationFreezed = false;
    this._competition = undefined;
    this._durchgang = undefined;
    this._geraet = undefined;
    this._step = undefined;    
    this.getCompetitions();
  }

  getCompetitions() {
    this.http.get<Wettkampf[]>(backendUrl + 'api/competition').subscribe((data) => {
      this.competitions = data;
    });
  }

  getDurchgaenge(competitionId: string) {
    if (this.durchgaenge != undefined && this._competition === competitionId) return;
    this.durchgaenge = [];
    this.geraete = undefined;
    this.steps = undefined;
    this.wertungen = undefined;
    
    this._competition = competitionId;
    this._durchgang = undefined;
    this._geraet = undefined;
    this._step = undefined;

    this.loadDurchgaenge();    
  }
  loadDurchgaenge() {
    this.http.get<string[]>(backendUrl + 'api/durchgang/' + this._competition).subscribe((data) => {
      this.durchgaenge = data;
    });
  }

  getGeraete(competitionId: string, durchgang: string) {
    if (this.geraete != undefined && this._competition === competitionId && this._durchgang === durchgang) return;
    this.geraete = [];
    this.steps = undefined;
    this.wertungen = undefined;

    this._competition = competitionId;
    this._durchgang = durchgang;
    this._geraet = undefined;
    this._step = undefined;

    this.loadGeraete();
  }
  loadGeraete() {
    this.http.get<Geraet[]>(backendUrl + 'api/durchgang/' + this._competition + '/' + encodeURIComponent2(this._durchgang)).subscribe((data) => {
      this.geraete = data;
    });
  }

  getSteps(competitionId: string, durchgang: string, geraetId: number) {
    if (this.steps != undefined && this._competition === competitionId && this._durchgang === durchgang && this._geraet === geraetId) return;
    this.steps = [];
    this.wertungen = undefined;

    this._competition = competitionId;
    this._durchgang = durchgang;
    this._geraet = geraetId;
    this._step = undefined;

    this.loadSteps();
  }

  loadSteps() {
    this.http.get<string[]>(backendUrl + 'api/durchgang/' + this._competition + '/' + encodeURIComponent2(this._durchgang) + '/' + this._geraet).subscribe((data) => {
      this.steps = data.map(step => parseInt(step));
      if (this._step === undefined) {
        this._step = 1;
        this.loadWertungen();
      }
    });
  }
  getWertungen(competitionId: string, durchgang: string, geraetId: number, step: number) {
    if (this.wertungen != undefined && this._competition === competitionId && this._durchgang === durchgang && this._geraet === geraetId && this._step === step) return;
    this.wertungen = [];

    this._competition = competitionId;
    this._durchgang = durchgang;
    this._geraet = geraetId;
    this._step = step;

    this.loadWertungen();
  }

  loadWertungen() {
    this.disconnectWS(true);
    this.initWebsocket();
    this.http.get<WertungContainer[]>(backendUrl + 'api/durchgang/' + this._competition + '/' + encodeURIComponent2( this._durchgang) + '/' + this._geraet + '/' + this._step).subscribe((data) => {
      this.wertungen = data;
    });    
  }

  isMessageAck(test: WertungContainer | MessageAck): test is MessageAck { return (test as MessageAck).type === 'MessageAck'}

  updateWertung(durchgang: string, step: number, geraetId: number, wertung: Wertung): Observable<WertungContainer> {
    const competitionId = wertung.wettkampfUUID;
    const result = new Subject<WertungContainer>();
    this.http.put<WertungContainer | MessageAck>(backendUrl + 'api/durchgang/' + competitionId + '/' + encodeURIComponent2(durchgang) + '/' + geraetId + '/' + step, wertung)
    .subscribe((data) => {
      if (!this.isMessageAck(data)) { 
        this.wertungen = this.wertungen.map(w => {
          if (w.id === data.id) {
            return data;
          } else {
            return w;
          }
        }); 
        result.next(data);
        result.complete();
      } else {
        const msg = data as MessageAck
        this.showMessage.next(msg);
        result.error(msg.msg);
      }
    }, (err)=> {
      this.showMessage.next(err);
      result.error(err);
    });    
    return result;
  }

  finishStation(competitionId: string, durchgang: string, step: number, geraetId: number) {
    this.http.post<MessageAck>(backendUrl + 'api/durchgang/' + competitionId + '/finish', JSON.stringify(<FinishDurchgangStation>{
      type : "FinishDurchgangStation",
      wettkampfUUID : competitionId,
      durchgang : durchgang,
      geraet : geraetId,
      step : step
    }))
    .subscribe((data) => {
      localStorage.removeItem('current_station');
      this.loggedIn = this.checkJWT();
      this.stationFreezed = false;
      const nextSteps = this.steps.filter(s => s > this._step);
      if (nextSteps.length > 0) {
        this._step = nextSteps[0];
      } else {
        this._step = 1;
      }
      this.loadWertungen();
    }, (err)=> {
    });    
  }  

  //// Websocket implementations

  private _activeDurchgangList: DurchgangStarted[] = [];
  get activeDurchgangList(): DurchgangStarted[] {
    return this._activeDurchgangList;
  }

  durchgangStarted = new BehaviorSubject<DurchgangStarted[]>([]);
  wertungUpdated = new Subject<AthletWertungUpdated>();
  
  protected getWebsocketBackendUrl(): string {
    const host = location.host;
    const path = location.pathname;
    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
    if (!host || host === '') {
      return "wss://kutuapp.sharevic.net/api/durchgang/" + this._competition + '/' + this._durchgang + '/ws';
    } else if (host.startsWith('localhost')) {
      return "ws://localhost:5757/api/durchgang/" + this._competition + '/' + this._durchgang + '/ws';
    } else {
      return protocol + "//" + host + path + "api/durchgang/" + this._competition + '/' + this._durchgang + '/ws';
    }
  }

  protected handleWebsocketMessage(message: any): boolean {
    const type = message['type'];
    switch (type) {
      case 'DurchgangStarted':
        this._activeDurchgangList = [...this.activeDurchgangList, (message as DurchgangStarted)]
        this.durchgangStarted.next(this.activeDurchgangList);
        return true;

      case 'DurchgangFinished':
        let finished = (message as DurchgangFinished)
        this._activeDurchgangList = this.activeDurchgangList.filter(d => d.durchgang !== finished.durchgang || d.wettkampfUUID !== finished.wettkampfUUID);
        this.durchgangStarted.next(this.activeDurchgangList);
        return true;

      case 'AthletWertungUpdated':
        let updated = (message as AthletWertungUpdated)
        this.wertungUpdated.next(updated);
        return true;

      case 'MessageAck':
        console.log((message as MessageAck).msg);
        this.showMessage.next((message as MessageAck));
        return true;

      default:
        return false;
    }
  };
}