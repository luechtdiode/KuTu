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
import { LoadingController, Loading } from 'ionic-angular';

declare var location: any;

@Injectable()
export class BackendService extends WebsocketService {

  externalLoaderSubscription: Subscription;

  loggedIn = false;
  stationFreezed = false;
  captionmode = false;
  private loadingInstance: Loading;
  private loadingStack: number = 0;

  competitions: Wettkampf[];
  durchgaenge: string[];
  geraete: Geraet[];
  steps: number[];
  wertungen: WertungContainer[];
  newLastResults = new BehaviorSubject<NewLastResults>(undefined);
  askForUsername = new Subject<BackendService>();
  lastMessageAck: MessageAck;

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

  set currentUserName(username: string) {
    localStorage.setItem('current_username', username);
  }

  get currentUserName() {
    return localStorage.getItem('current_username');
  }

  getCurrentStation(): string {
    return localStorage.getItem('current_station')
     || this.competition + '/' + this.durchgang + '/' + this.geraet + '/' + this.step;
  }

  private lastJWTChecked = 0;

  constructor(public http: HttpClient, public loadingCtrl: LoadingController) {
    super();
    this.startLoading('App wird initialisiert. Bitte warten ...');
    this.externalLoaderSubscription = interval(1000).subscribe(latest => {
      const initWith = localStorage.getItem("external_load") || localStorage.getItem('current_station');
      if (initWith) {
        this.initWithQuery(initWith);
      } else {
        this.checkJWT();
      }
    });
    this.showMessage.subscribe(msg => {
      this.loadingStack = 0;
      this.resetLoading();
      this.lastMessageAck = msg;
    });
    this.resetLoading();
  }

  public resetLoading() {
    this.loadingStack -= 1;
    if (this.loadingInstance && this.loadingStack < 1) {
      this.loadingInstance.dismiss();
      this.loadingInstance = undefined;
      this.loadingStack = 0;
    }
  }

  public startLoading(content: string, observable?: Observable<any>) {    
    // console.log('starting ' + content + ' stack: ' + this.loadingStack);
    this.loadingStack += 1;
    if (!this.loadingInstance) {
      this.loadingInstance = this.loadingCtrl.create({
        content: content
      });
      this.loadingInstance.present();
    } else {
      this.loadingInstance.setContent(content);
    }
    if (observable) {
      observable.subscribe(() => this.resetLoading(), (err) => {});
    }
    return observable;
  }

  initWithQuery(initWith: string) {
    if (initWith && initWith.startsWith('c=') ) {
      this._step = 1;

      initWith.split('&').forEach(param => {
        const [key, value] = param.split('=')
        switch (key) {
          case 's':
            if (!this.currentUserName) {
              this.askForUsername.next(this);
            }
            localStorage.setItem('auth_token', value);
            this.checkJWT(value);
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
          case 'st':
            this._step = parseInt(value);
            break;
          case 'g':
            this._geraet = parseInt(value);
            localStorage.setItem('current_station', initWith);
            this.checkJWT();
            this.stationFreezed = true;
          default:
        }
      });
      this.externalLoaderSubscription.unsubscribe();
      localStorage.removeItem("external_load");
      this.startLoading('Bitte warten ...')
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
      this.resetLoading();
    }
  }

  standardErrorHandler = (err: HttpErrorResponse) => {
    console.log(err);
    this.wertungenLoading = false;
    const msgAck = <MessageAck>{
      msg : '<em>' + err.statusText + '</em><br>' + err.message,
      type : err.name
    };
    if (!this.lastMessageAck || this.lastMessageAck.msg !== msgAck.msg) {
      this.showMessage.next(msgAck);  
    }
  }

  private checkJWT(jwt?: string) {
    if (!jwt) {
      jwt = localStorage.getItem('auth_token');
    }
    if(!jwt) {
      this.loggedIn = false;
      return;
    }

    const now = new Date();
    const oneHourAgo = now.getTime() - 3600000;
    if ((!jwt || jwt === localStorage.getItem('auth_token')) && (oneHourAgo < this.lastJWTChecked)) {
      return;
    }

    this.startLoading('Berechtigungen werden überprüft. Bitte warten ...', this.http.options(backendUrl + 'api/isTokenExpired', { 
      observe: 'response', 
      responseType: 'text',
      headers: {
        'x-access-token': `${jwt}`
      }
    }).share()).subscribe((data) => {
      localStorage.setItem('auth_token', data.headers.get('x-access-token'));
      this.loggedIn = true;
    }, (err: HttpErrorResponse) => {
      console.log(err);
      if (err.status === 401) {
        localStorage.removeItem('auth_token');
        this.loggedIn = false;
        this.showMessage.next(<MessageAck> {
          msg: 'Die Berechtigung zum erfassen von Wertungen ist abgelaufen.',
          type:'Berechtigung'
        });
      } else {
        this.standardErrorHandler(err);
      }
    });
    this.lastJWTChecked = new Date().getTime();
  }

  login(username, password) {
    const headers = new HttpHeaders();
    this.startLoading('Login wird verarbeitet. Bitte warten ...', this.http.options(backendUrl + 'api/login', { 
      observe: 'response', 
      headers: headers.set('Authorization', 'Basic ' + btoa(username + ':' + password )),
      withCredentials: true,
      responseType: 'text'
    }).share()).subscribe((data) => {
      console.log(data);
      localStorage.setItem('auth_token', data.headers.get('x-access-token'));
      this.loggedIn =true;
    }, (err) => {
      console.log(err);
      localStorage.removeItem('auth_token');
      this.loggedIn = false;
      if (err.status === 401) {
        localStorage.removeItem('auth_token');
        this.loggedIn = false;
        this.showMessage.next(<MessageAck> {
          msg: 'Die Berechtigung zum erfassen von Wertungen ist nicht gültig oder abgelaufen.',
          type:'Berechtigung'
        });
      } else {
        this.standardErrorHandler(err);
      }
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
    this.startLoading('Wettkampfliste wird geladen. Bitte warten ...', this.http.get<Wettkampf[]>(backendUrl + 'api/competition').share()).subscribe((data) => {
      this.competitions = data;
    }, this.standardErrorHandler);
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
    this.startLoading('Durchgangliste wird geladen. Bitte warten ...', this.http.get<string[]>(backendUrl + 'api/durchgang/' + this._competition).share()).subscribe((data) => {
      this.durchgaenge = data;
    }, this.standardErrorHandler);
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
    const request =  this.startLoading('Geräte zum Durchgang werden geladen. Bitte warten ...', this.http.get<Geraet[]>(path).share());
    request.subscribe((data) => {
      this.geraete = data;
    }, this.standardErrorHandler);
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
    this.startLoading('Stationen zum Gerät werden geladen. Bitte warten ...', this.http.get<string[]>(backendUrl + 'api/durchgang/' + this._competition + '/' + encodeURIComponent2(this._durchgang) + '/' + this._geraet).share()).subscribe((data) => {
      this.steps = data.map(step => parseInt(step));
      if (this._step === undefined || this.steps.indexOf(this._step) < 0) {
        this._step = 1;
        this.loadWertungen();
      }
    }, this.standardErrorHandler);
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

  wertungenLoading = false;
  loadWertungen() {
    // prevent denial of service fired from the step-slider
    if (this.wertungenLoading) {
      return;
    }
    this.captionmode = true;
    this.disconnectWS(true);
    this.initWebsocket();
    const lastStepToLoad = this._step;
    this.wertungenLoading = true;
    this.startLoading('Riegenteilnehmer werden geladen. Bitte warten ...', this.http.get<WertungContainer[]>(backendUrl + 'api/durchgang/' + this._competition + '/' + encodeURIComponent2( this._durchgang) + '/' + this._geraet + '/' + this._step).share()).subscribe((data) => {
      this.wertungenLoading = false;
      if (this._step !== lastStepToLoad) {
        this.loadWertungen();
      } else {
        this.wertungen = data;
      }
    }, this.standardErrorHandler);
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
    this.startLoading('Wertung wird gespeichert. Bitte warten ...', this.http.put<WertungContainer | MessageAck>(backendUrl + 'api/durchgang/' + competitionId + '/' + encodeURIComponent2(durchgang) + '/' + geraetId + '/' + step, wertung).share())
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
    this.startLoading('Station wird abgeschlossen. Bitte warten ...', this.http.post<MessageAck>(backendUrl + 'api/durchgang/' + competitionId + '/finish', <FinishDurchgangStation>{
      type : "FinishDurchgangStation",
      wettkampfUUID : competitionId,
      durchgang : durchgang,
      geraet : geraetId,
      step : step
    }).share())
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
    }, this.standardErrorHandler); 

    return result.asObservable();   
  }  

  nextStep(): number {
    const nextSteps = this.steps.filter(s => s > this._step);
    if (nextSteps.length > 0) {
      return nextSteps[0];
    } else {
      return 1;
    } 
  }

  prevStep(): number {
    const prevSteps = this.steps.filter(s => s < this._step);
    if (prevSteps.length > 0) {
      return prevSteps[prevSteps.length -1];
    } else {
      return this.steps[this.steps.length -1];
    } 
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
      host = (protocol + "//" + host + path).replace('index.html', '');
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

      case 'AthletWertungUpdatedSequenced':
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

      case 'AthletMovedInWettkampf':
      case 'AthletRemovedFromWettkampf':
        this.loadWertungen();  

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
