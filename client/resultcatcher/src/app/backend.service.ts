import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { backendUrl } from './utils';
import { WertungContainer, Geraet, Wettkampf, Wertung, MessageAck, AthletWertungUpdated, DurchgangStarted, DurchgangFinished, FinishDurchgangStation, NewLastResults } from './backend-types';
import { Observable } from 'rxjs/Observable';
import { Subject } from 'rxjs/Subject';
import { Subscription, BehaviorSubject } from 'rxjs';
//import { tokenNotExpired } from 'angular2-jwt';
import { interval } from 'rxjs/observable/interval';
import { WebsocketService, encodeURIComponent2 } from './websocket.service';

declare var location: any;

@Injectable()
export class BackendService extends WebsocketService {

  externalLoaderSubscription: Subscription;

  loggedIn = false;
  stationFreezed = false;
  captionmode = false;

  competitions: Wettkampf[];
  durchgaenge: string[];
  geraete: Geraet[];
  steps: number[];
  wertungen: WertungContainer[];
  newLastResults = new BehaviorSubject<NewLastResults>(undefined);

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

  private lastJWTChecked = 0;

  constructor(public http: HttpClient) {
    super();
    this.externalLoaderSubscription = interval(1000).subscribe(latest => {
      this.checkJWT();
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
            this.checkJWT();
            const cs = localStorage.getItem('current_station');
            if (cs) {
              this.initWithQuery(cs);
            }
            break;
          case 'c':
            this._competition = value;
            break;
          case 'ca':
            this._competition = undefined;
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
        }
      });
      this.externalLoaderSubscription.unsubscribe();
      localStorage.removeItem("external_load");
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

  private checkJWT() {
    if(!localStorage.getItem('auth_token')) {
      this.loggedIn = false;
      return;
    }

    const now = new Date();
    const oneHourAgo = now.getTime() - 3600000;
    if (oneHourAgo < this.lastJWTChecked) {
      return;
    }

    this.http.options(backendUrl + 'api/isTokenExpired', { 
      observe: 'response', 
      responseType: 'text'
    }).subscribe((data) => {
      localStorage.setItem('auth_token', data.headers.get('x-access-token'));
      this.loggedIn = true;
    }, (err: HttpErrorResponse) => {
      console.log(err);
      if (err.status === 401) {
        localStorage.removeItem('auth_token');
        this.loggedIn = false;
      }
      this.showMessage.next(<MessageAck> {
        msg: 'Die Berechtigung zum erfassen von Wertungen ist abgelaufen.',
        type:'Berechtigung'
      });
    });
    this.lastJWTChecked = new Date().getTime();
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
      this.loggedIn =true;
    }, (err) => {
      console.log(err);
      localStorage.removeItem('auth_token');
      this.loggedIn = false;
      this.showMessage.next(<MessageAck> {
        msg: 'Die Berechtigung zum erfassen von Wertungen ist nicht gültig oder abgelaufen.',
        type:'Berechtigung'
      });
    });
  }
  
  unlock() {
    localStorage.removeItem('current_station');
    this.checkJWT();
    this.stationFreezed = false;
    // this._competition = undefined;
    this._durchgang = undefined;
    this._geraet = undefined;
    this._step = undefined;    
    this.getCompetitions();
  }

  logout() {
    localStorage.removeItem('auth_token');
    this.unlock();
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
    let path = '';
    if (this.captionmode) {
      path = backendUrl + 'api/durchgang/' + this._competition + '/' + encodeURIComponent2(this._durchgang);  
    } else {
      path = backendUrl + 'api/durchgang/' + this._competition + '/geraete';  
    }
    const request = this.http.get<Geraet[]>(path).share();
    request.subscribe((data) => {
      this.geraete = data;
    });  

    return request;
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
    this.captionmode = true;
    this.disconnectWS(true);
    this.initWebsocket();
    this.http.get<WertungContainer[]>(backendUrl + 'api/durchgang/' + this._competition + '/' + encodeURIComponent2( this._durchgang) + '/' + this._geraet + '/' + this._step).subscribe((data) => {
      this.wertungen = data;
    });    
  }

  loadAlleResultate(): Observable<Geraet[]> {
    if (this._competition) {
      this.captionmode = false;
      this.disconnectWS(true);
      this.initWebsocket();
      return this.loadGeraete();
    } else {
      return Observable.of(this.geraete);
    }
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
    }, (err: HttpErrorResponse)=> {
      const msg = <MessageAck>{
        msg : err.statusText + ': ' + err.message,
        type : err.name
      };
      this.showMessage.next(msg);
      result.error(err);
    });    
    return result;
  }

  finishStation(competitionId: string, durchgang: string, geraetId: number, step: number): Observable<number[]> {
    const result = new Subject<number[]>();
    this.http.post<MessageAck>(backendUrl + 'api/durchgang/' + competitionId + '/finish', <FinishDurchgangStation>{
      type : "FinishDurchgangStation",
      wettkampfUUID : competitionId,
      durchgang : durchgang,
      geraet : geraetId,
      step : step
    })
    .subscribe((data) => {
      const nextSteps = this.steps.filter(s => s > this._step);
      if (nextSteps.length > 0) {
        this._step = nextSteps[0];
      } else {
        localStorage.removeItem('current_station');
        this.checkJWT();
        this.stationFreezed = false;
        this._step = 1;
      }
      this.loadWertungen();
      result.next(nextSteps);
    }, (err)=> {
    }); 
    return result.asObservable();   
  }  

  //// Websocket implementations

  private _activeDurchgangList: DurchgangStarted[] = [];
  get activeDurchgangList(): DurchgangStarted[] {
    return this._activeDurchgangList;
  }

  durchgangStarted = new BehaviorSubject<DurchgangStarted[]>([]);
  wertungUpdated = new Subject<AthletWertungUpdated>();
  
  protected getWebsocketBackendUrl(): string {
    let host = location.host;
    let path = location.pathname;
    const protocol = location.protocol === "https:" ? "wss:" : "ws:";
    let apiPath = "api/";
    if (this._durchgang && this.captionmode) {
      apiPath = apiPath + "durchgang/" + this._competition + "/" + encodeURIComponent2(this._durchgang) + "/ws";
    } else {
      apiPath = apiPath + "durchgang/" + this._competition + "/all/ws";
      // apiPath = apiPath + "competition/ws";
    }
    if (!host || host === '') {
      host = "wss://kutuapp.sharevic.net/";
    } else if (host.startsWith("localhost")) {
      host = "ws://localhost:5757/";
    } else {
      host = protocol + "//" + host + path;
    }

    return host + apiPath;
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
        this.wertungen = this.wertungen.map(w => {
          if (w.id === updated.wertung.athletId && w.wertung.wettkampfdisziplinId === updated.wertung.wettkampfdisziplinId ) {
            return Object.assign({}, w, {wertung: updated.wertung });
          } else {
            return w;
          }
        }); 
        this.wertungUpdated.next(updated);
        return true;

      case 'NewLastResults':
        this.newLastResults.next((message as NewLastResults));
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