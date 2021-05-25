import { Injectable } from '@angular/core';
import { WebsocketService, encodeURIComponent2, encodeURIComponent1 } from './websocket.service';
import { HttpClient, HttpHeaders, HttpErrorResponse } from '@angular/common/http';
import { LoadingController } from '@ionic/angular';
import { interval, of, Subscription, BehaviorSubject, Subject, Observable } from 'rxjs';
import { share, map, switchMap} from 'rxjs/operators';
import { DurchgangStarted, Wettkampf, Geraet, WertungContainer, NewLastResults, StartList,
         MessageAck, AthletWertungUpdated, Wertung, FinishDurchgangStation,
         DurchgangFinished,
         ClubRegistration,
         NewClubRegistration,
         AthletRegistration,
         ProgrammRaw,
         SyncAction, JudgeRegistration, JudgeRegistrationProgramItem} from '../backend-types';
import { backendUrl } from '../utils';
import { ProgrammItem } from '../backend-types';

// tslint:disable:radix
// tslint:disable:variable-name

@Injectable({
  providedIn: 'root'
})
export class BackendService extends WebsocketService {

    get competition(): string {
      return this._competition;
    }
    get durchgang(): string {
      return this._durchgang;
    }
    get geraet(): number {
      return this._geraet;
    }
    get step(): number {
      return this._step;
    }

    set currentUserName(username: string) {
      localStorage.setItem('current_username', username);
    }

    get currentUserName() {
      return localStorage.getItem('current_username');
    }

    constructor(public http: HttpClient, public loadingCtrl: LoadingController) {
      super();
      this.showMessage.subscribe(msg => {
        this.resetLoading();
        this.lastMessageAck = msg;
      });
      this.resetLoading();
    }
    get activeDurchgangList(): DurchgangStarted[] {
      return this._activeDurchgangList;
    }

    get authenticatedClubId() {
      return localStorage.getItem('auth_clubid');
    }

    get competitionName(): string {
      if (!this.competitions) { return ''; }
      const candidate = this.competitions
        .filter(c => c.uuid === this.competition)
        .map(c => c.titel + ', am ' + (c.datum + 'T').split('T')[0].split('-').reverse().join('-'));

      if (candidate.length === 1) {
        return candidate[0];
      } else {
        return '';
      }
    }

    externalLoaderSubscription: Subscription;

    loggedIn = false;
    stationFreezed = false;
    captionmode = false;
    private loadingInstance: Promise<HTMLIonLoadingElement>;

    competitions: Wettkampf[];
    durchgaenge: string[];

    geraete: Geraet[];
    geraeteSubject = new BehaviorSubject<Geraet[]>([]);
    steps: number[];
    wertungen: WertungContainer[];
    wertungenSubject = new BehaviorSubject<WertungContainer[]>([]);
    newLastResults = new BehaviorSubject<NewLastResults>(undefined);
    _clubregistrations = [];
    clubRegistrations = new BehaviorSubject<ClubRegistration[]>([]);
    askForUsername = new Subject<BackendService>();
    lastMessageAck: MessageAck;

    private _competition: string = undefined;
    private _durchgang: string = undefined;
    private _geraet: number = undefined;
    private _step: number = undefined;

    private lastJWTChecked = 0;

    wertungenLoading = false;
    isInitializing = false;

    //// Websocket implementations

    private _activeDurchgangList: DurchgangStarted[] = [];

    durchgangStarted = new BehaviorSubject<DurchgangStarted[]>([]);
    wertungUpdated = new Subject<AthletWertungUpdated>();

    getCurrentStation(): string {
      return localStorage.getItem('current_station')
       || this.competition + '/' + this.durchgang + '/' + this.geraet + '/' + this.step;
    }

    public resetLoading() {
      if (this.loadingInstance) {
        this.loadingInstance.then(alert => alert.dismiss());
        this.loadingInstance = undefined;
      }
    }

    public startLoading(content: string, observable?: Observable<any>) {
      // console.log('starting ' + content + ' stack: ' + this.loadingStack);
      this.resetLoading();
      this.loadingInstance = this.loadingCtrl.create({
        message : content
      });
      this.loadingInstance.then(alert => alert.present());
      if (observable) {
        observable.subscribe(() => this.resetLoading(), (err) => {});
      }
      return observable;
    }

    initWithQuery(initWith: string) {
      this.isInitializing = true;
      const finished = new BehaviorSubject<boolean>(false);
      if (initWith && initWith.startsWith('c=') ) {
        this._step = 1;

        initWith.split('&').forEach(param => {
          const [key, value] = param.split('=');
          switch (key) {
            case 's':
              if (!this.currentUserName) {
                this.askForUsername.next(this);
              }
              localStorage.setItem('auth_token', value);
              localStorage.removeItem('auth_clubid');
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
              break;
            default:
          }
        });
        // this.externalLoaderSubscription.unsubscribe();
        localStorage.removeItem('external_load');
        this.startLoading('Bitte warten ...');
        if (this._geraet) {
          this.getCompetitions().pipe(
            switchMap(() => this.loadDurchgaenge()),
            switchMap(() => this.loadGeraete()),
            switchMap(() => this.loadSteps()),
            switchMap(() => this.loadWertungen())
          ).subscribe(fin =>
            finished.next(true)
          );
        } else if (this._competition) {
          this.getCompetitions().pipe(
            switchMap(() => this.loadDurchgaenge())
          ).subscribe(fin => finished.next(true));
        }
      } else {
        finished.next(true);
      }

      finished.subscribe(fin => {
        if (fin) {
          this.isInitializing = false;
          this.resetLoading();
        }
      });

      return finished;
    }

    standardErrorHandler = (err: HttpErrorResponse) => {
      console.log(err);
      this.resetLoading();
      this.wertungenLoading = false;
      this.isInitializing = false;
      if (err.status === 401) {
        localStorage.removeItem('auth_token');
        this.loggedIn = false;
        this.showMessage.next({
          msg: 'Die Berechtigung zum erfassen von Wertungen ist abgelaufen.',
          type: 'Berechtigung'
        } as MessageAck);
      } else {
        const msgAck = {
          msg : '<em>' + err.statusText + '</em><br>' + err.message,
          type : err.name
        } as MessageAck;
        if (!this.lastMessageAck || this.lastMessageAck.msg !== msgAck.msg) {
          this.showMessage.next(msgAck);
        }
      }
    }

    public checkJWT(jwt?: string) {
      if (!jwt) {
        jwt = localStorage.getItem('auth_token');
      }
      if (!jwt) {
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
      }).pipe(share())).subscribe((data) => {
        localStorage.setItem('auth_token', data.headers.get('x-access-token'));
        this.loggedIn = true;
        if (!this.competitions || this.competitions.length === 0) {
          this.getCompetitions().subscribe(d => {
            if (this._competition) {
              this.getDurchgaenge(this._competition);
            }
          });
        } else if (this._competition) {
          this.getDurchgaenge(this._competition);
        }
      }, (err: HttpErrorResponse) => {
        console.log(err);
        if (err.status === 401) {
          localStorage.removeItem('auth_token');
          this.loggedIn = false;
          this.showMessage.next({
            msg: 'Die Berechtigung ist abgelaufen. Bitte neu anmelden',
            type: 'Berechtigung'
          } as MessageAck);
        } else {
          this.standardErrorHandler(err);
        }
      });
      this.lastJWTChecked = new Date().getTime();
    }

    saveClubRegistration(competitionId: string, registration: ClubRegistration) {
      const save = this.startLoading('Vereins-Anmeldung wird gespeichert. Bitte warten ...',
        this.http.put<MessageAck>(backendUrl + 'api/registrations/' + competitionId + '/' + registration.id,
        registration
      ).pipe(share()));
      save.subscribe((data) => {
        this._clubregistrations = [...this._clubregistrations.filter(r => r.id != registration.id), data];
        this.clubRegistrations.next(this._clubregistrations);
        }, this.standardErrorHandler);
      return save;
    }

    createClubRegistration(competitionId: string, registration: NewClubRegistration) {
      const creater = this.startLoading('Vereins-Anmeldung wird registriert. Bitte warten ...',
        this.http.post<ClubRegistration>(backendUrl + 'api/registrations/' + competitionId,
        registration, {
          observe: 'response',
        }
      ).pipe(
        map((data) => {
          console.log(data);
          localStorage.setItem('auth_token', data.headers.get('x-access-token'));
          localStorage.setItem('auth_clubid', data.body.id + '');
          this.loggedIn = true;
          return data.body;
        }),
        share()));
      creater.subscribe((data) => {
        this._clubregistrations = [...this._clubregistrations, data];
        this.clubRegistrations.next(this._clubregistrations);
        }, this.standardErrorHandler);
      return creater;
    }

    deleteClubRegistration(competitionId: string, clubid: number) {
      const deleter = this.startLoading('Vereins-Anmeldung wird gelöscht. Bitte warten ...',
        this.http.delete(backendUrl + 'api/registrations/' + competitionId + '/' + clubid, {
          responseType: 'text'
        }
      ).pipe(share()));
      deleter.subscribe((data) => {
        this.clublogout();
        this._clubregistrations = this._clubregistrations.filter(r => r.id != clubid);
        this.clubRegistrations.next(this._clubregistrations);
        }, this.standardErrorHandler);
      return deleter;
    }

    loadProgramsForCompetition(competitionId: string) {
      const loader = this.startLoading('Programmliste zum Wettkampf wird geladen. Bitte warten ...',
        this.http.get<ProgrammRaw[]>(
          backendUrl + 'api/registrations/' + competitionId + '/programmlist'
          ).pipe(share()));

      loader.subscribe((data) => {
      }, this.standardErrorHandler);

      return loader;
    }

    loadAthletListForClub(competitionId: string, clubid: number) {
      const loader = this.startLoading('Athletliste zum Club wird geladen. Bitte warten ...',
        this.http.get<AthletRegistration[]>(
          backendUrl + 'api/registrations/' + competitionId + '/' + clubid + '/athletlist'
          ).pipe(share()));

      loader.subscribe((data) => {
      }, this.standardErrorHandler);

      return loader;
    }

    loadAthletRegistrations(competitionId: string, clubid: number) {
      const loader = this.startLoading('Athletliste zum Club wird geladen. Bitte warten ...',
        this.http.get<AthletRegistration[]>(
          backendUrl + 'api/registrations/' + competitionId + '/' + clubid + '/athletes'
          ).pipe(share()));

      loader.subscribe((data) => {
      }, this.standardErrorHandler);

      return loader;
    }

    createAthletRegistration(competitionId: string, clubid: number, registration: AthletRegistration) {
      const loader = this.startLoading('Anmeldung wird gespeichert. Bitte warten ...',
        this.http.post<AthletRegistration>(
          backendUrl + 'api/registrations/' + competitionId + '/' + clubid + '/athletes',
          registration
          ).pipe(share()));

      loader.subscribe((data) => {
      }, this.standardErrorHandler);

      return loader;
    }

    saveAthletRegistration(competitionId: string, clubid: number, registration: AthletRegistration) {
      const loader = this.startLoading('Anmeldung wird gespeichert. Bitte warten ...',
        this.http.put<AthletRegistration>(
          backendUrl + 'api/registrations/' + competitionId + '/' + clubid + '/athletes/' + registration.id,
          registration
          ).pipe(share()));

      loader.subscribe((data) => {
      }, this.standardErrorHandler);

      return loader;
    }

    deleteAthletRegistration(competitionId: string, clubid: number, registration: AthletRegistration) {
      const loader = this.startLoading('Anmeldung wird gespeichert. Bitte warten ...',
        this.http.delete(
          backendUrl + 'api/registrations/' + competitionId + '/' + clubid + '/athletes/' + registration.id, {
            responseType: 'text'
          }
          ).pipe(share()));

      loader.subscribe((data) => {
      }, this.standardErrorHandler);

      return loader;
    }

    findCompetitionsByVerein(vereinId: number) {
      const loader = this.startLoading(
        'Es werden frühere Anmeldungen gesucht. Bitte warten ...',
        this.http.get<Wettkampf>(backendUrl + 'api/competition/byVerein/' + vereinId).pipe(share()));

      loader.subscribe((data) => {}, this.standardErrorHandler);

      return loader;
    }

    copyClubRegsFromCompetition(copyFromWk: Wettkampf, toCompetitionId: string, clubid: number) {
      const loader = this.startLoading('Anmeldung wird gespeichert. Bitte warten ...',
        this.http.put(
          backendUrl + 'api/registrations/' + toCompetitionId + '/' + clubid + '/copyfrom', copyFromWk, {
            responseType: 'text'
          }).pipe(share()));

      loader.subscribe((data) => {
      }, this.standardErrorHandler);

      return loader;
    }

    loadJudgeProgramDisziplinList(competitionId: string) {
      const loader = this.startLoading('Athletliste zum Club wird geladen. Bitte warten ...',
        this.http.get<JudgeRegistrationProgramItem[]>(
          backendUrl + 'api/registrations/' + competitionId + '/programmdisziplinlist'
          ).pipe(share()));

      loader.subscribe((data) => {
      }, this.standardErrorHandler);

      return loader;
    }

    loadJudgeRegistrations(competitionId: string, clubid: number) {
      const loader = this.startLoading('Wertungsrichter-Liste zum Club wird geladen. Bitte warten ...',
        this.http.get<JudgeRegistration[]>(
          backendUrl + 'api/registrations/' + competitionId + '/' + clubid + '/judges'
          ).pipe(share()));

      loader.subscribe((data) => {
      }, this.standardErrorHandler);

      return loader;
    }

    createJudgeRegistration(competitionId: string, clubid: number, registration: JudgeRegistration) {
      const loader = this.startLoading('Anmeldung wird gespeichert. Bitte warten ...',
        this.http.post<JudgeRegistration>(
          backendUrl + 'api/registrations/' + competitionId + '/' + clubid + '/judges',
          registration
          ).pipe(share()));

      loader.subscribe((data) => {
      }, this.standardErrorHandler);

      return loader;
    }

    saveJudgeRegistration(competitionId: string, clubid: number, registration: JudgeRegistration) {
      const loader = this.startLoading('Anmeldung wird gespeichert. Bitte warten ...',
        this.http.put<JudgeRegistration>(
          backendUrl + 'api/registrations/' + competitionId + '/' + clubid + '/judges/' + registration.id,
          registration
          ).pipe(share()));

      loader.subscribe((data) => {
      }, this.standardErrorHandler);

      return loader;
    }

    deleteJudgeRegistration(competitionId: string, clubid: number, registration: JudgeRegistration) {
      const loader = this.startLoading('Anmeldung wird gespeichert. Bitte warten ...',
        this.http.delete(
          backendUrl + 'api/registrations/' + competitionId + '/' + clubid + '/judges/' + registration.id, {
            responseType: 'text'
          }
          ).pipe(share()));

      loader.subscribe((data) => {
      }, this.standardErrorHandler);

      return loader;
    }

    clublogout() {
      this.logout();
    }

    clublogin(username, password) {
      this.clublogout();
      const headers = new HttpHeaders();
      const loader = this.startLoading('Login wird verarbeitet. Bitte warten ...',
        this.http.options(backendUrl + 'api/login', {
          observe: 'response',
          headers: headers.set('Authorization', 'Basic ' + btoa(username + ':' + password )),
          withCredentials: true,
          responseType: 'text'
        }
      ).pipe(share()));
      loader.subscribe((data) => {
        console.log(data);
        localStorage.setItem('auth_token', data.headers.get('x-access-token'));
        localStorage.setItem('auth_clubid', username);
        this.loggedIn = true;
      }, (err) => {
        console.log(err);
        this.clublogout();
        if (err.status === 401) {
          localStorage.removeItem('auth_token');
          this.loggedIn = false;
          this.showMessage.next({
            msg: 'Die Anmeldung ist nicht gültig oder abgelaufen.',
            type: 'Berechtigung'
          } as MessageAck);
        } else {
          this.standardErrorHandler(err);
        }
      });
      return loader;
    }

    unlock() {
      localStorage.removeItem('current_station');
      this.checkJWT();
      this.stationFreezed = false;
    }

    logout() {
      localStorage.removeItem('auth_token');
      localStorage.removeItem('auth_clubid');
      this.loggedIn = false;
      this.unlock();
    }

    getCompetitions() {
      const loader = this.startLoading('Wettkampfliste wird geladen. Bitte warten ...',
      this.http.get<Wettkampf[]>(backendUrl + 'api/competition').pipe(share()));
      loader.subscribe((data) => {
        this.competitions = data;
      }, this.standardErrorHandler);
      return loader;
    }

    getClubRegistrations(competitionId: string) {
      this.checkJWT();
      if ((this._clubregistrations !== undefined && this._competition === competitionId) || this.isInitializing) {
        return this.loadClubRegistrations();
      }
      this.durchgaenge = [];
      this._clubregistrations = [];
      this.geraete = undefined;
      this.steps = undefined;
      this.wertungen = undefined;

      this._competition = competitionId;
      this._durchgang = undefined;
      this._geraet = undefined;
      this._step = undefined;

      return this.loadClubRegistrations();
    }

    loadClubRegistrations(): Observable<ClubRegistration[]> {
      const loader = this.startLoading('Clubanmeldungen werden geladen. Bitte warten ...',
        this.http.get<string[]>(backendUrl + 'api/registrations/' + this._competition).pipe(share()));

      loader.subscribe((data) => {
        localStorage.setItem('current_competition', this._competition);
        this._clubregistrations = data;
        this.clubRegistrations.next(data);
      }, this.standardErrorHandler);

      return this.clubRegistrations;
    }

    loadRegistrationSyncActions(): Observable<SyncAction[]> {
      const loader = this.startLoading('Pendente An-/Abmeldungen werden geladen. Bitte warten ...',
        this.http.get<string[]>(backendUrl + 'api/registrations/' + this._competition + '/syncactions').pipe(share()));

      loader.subscribe((data) => {
      }, this.standardErrorHandler);

      return loader;
    }

    getDurchgaenge(competitionId: string) {
      this.checkJWT();
      if ((this.durchgaenge !== undefined && this._competition === competitionId) || this.isInitializing) {
        return of(this.durchgaenge || []);
      }
      this.durchgaenge = [];
      this._clubregistrations = [];
      this.clubRegistrations.next([]);
      this.geraete = undefined;
      this.steps = undefined;
      this.wertungen = undefined;

      this._competition = competitionId;
      this._durchgang = undefined;
      this._geraet = undefined;
      this._step = undefined;

      return this.loadDurchgaenge();
    }

    loadDurchgaenge(): Observable<string[]> {
      const loader = this.startLoading('Durchgangliste wird geladen. Bitte warten ...',
        this.http.get<string[]>(backendUrl + 'api/durchgang/' + this._competition).pipe(share()));

      const actualDg = this._durchgang;

      loader.subscribe((data) => {
        localStorage.setItem('current_competition', this._competition);
        this.durchgaenge = data;
        if (actualDg) {
          // const actualDgParts = actualDg.split('_');
          const candidates = this.durchgaenge.filter(dg => {
            const encodedDg = encodeURIComponent1(dg);
            return actualDg === dg
              || actualDg === encodedDg;
              // || (actualDgParts.filter(d => actualDg.indexOf(d) > -1).length === actualDgParts.length);
          });
          if (candidates.length === 1) {
            this._durchgang = candidates[0];
          }
        }
      }, this.standardErrorHandler);

      return loader;
    }

    getGeraete(competitionId: string, durchgang: string) {
      if ((this.geraete !== undefined && this._competition === competitionId && this._durchgang === durchgang) || this.isInitializing) {
        return of(this.geraete || []);
      }
      this.geraete = [];
      this.steps = undefined;
      this.wertungen = undefined;

      this._competition = competitionId;
      this._durchgang = durchgang;
      this._geraet = undefined;
      this._step = undefined;

      this.captionmode = true;

      return this.loadGeraete();
    }
    loadGeraete() {
      this.geraete = [];
      let path = '';
      if (this.captionmode && this._durchgang) {
        path = backendUrl + 'api/durchgang/' + this._competition + '/' + encodeURIComponent2(this._durchgang);
      } else {
        path = backendUrl + 'api/durchgang/' + this._competition + '/geraete';
      }
      const request =  this.startLoading('Geräte zum Durchgang werden geladen. Bitte warten ...',
        this.http.get<Geraet[]>(path).pipe(share()));
      request.subscribe((data) => {
        this.geraete = data;
        this.geraeteSubject.next(this.geraete);
      }, this.standardErrorHandler);
      return request;
    }

    getSteps(competitionId: string, durchgang: string, geraetId: number) {
      if ((this.steps !== undefined && this._competition === competitionId
        && this._durchgang === durchgang && this._geraet === geraetId) || this.isInitializing) {
          return of(this.steps || []);
      }
      this.steps = [];
      this.wertungen = undefined;

      this._competition = competitionId;
      this._durchgang = durchgang;
      this._geraet = geraetId;
      this._step = undefined;

      const stepsloader = this.loadSteps();
      stepsloader.subscribe((data) => {
        this.steps = data.map(step => parseInt(step));
        if (this._step === undefined || this.steps.indexOf(this._step) < 0) {
          this._step = this.steps[0];
          this.loadWertungen();
        }
      }, this.standardErrorHandler);

      return stepsloader;
    }

    loadSteps() {
      this.steps = [];
      const request = this.startLoading('Stationen zum Gerät werden geladen. Bitte warten ...',
        this.http.get<string[]>(
          backendUrl + 'api/durchgang/' + this._competition + '/' + encodeURIComponent2(this._durchgang) + '/' + this._geraet
        ).pipe(share()));
      request.subscribe((data) => {
        this.steps = data;
      }, this.standardErrorHandler);
      return request;
    }

    getWertungen(competitionId: string, durchgang: string, geraetId: number, step: number) {
      if ((this.wertungen !== undefined && this._competition === competitionId
        && this._durchgang === durchgang && this._geraet === geraetId && this._step === step) || this.isInitializing) { return; }
      this.wertungen = [];

      this._competition = competitionId;
      this._durchgang = durchgang;
      this._geraet = geraetId;
      this._step = step;

      this.loadWertungen();
    }

    activateCaptionMode() {
      if (!this.competitions) {
        this.getCompetitions();
      }
      if (!this.durchgaenge) {
        this.loadDurchgaenge();
      }
      if (!this.captionmode || !this.geraete) {
        this.captionmode = true;
        this.loadGeraete();
      }
      if (this.geraet && !this.steps) {
        this.loadSteps();
      }
      if (this.geraet) {
        this.disconnectWS(true);
        this.initWebsocket();
      }
    }

    loadWertungen() {
      // prevent denial of service fired from the step-slider
      if (this.wertungenLoading || !this._step) {
        return;
      }
      this.activateCaptionMode();
      const lastStepToLoad = this._step;
      this.wertungenLoading = true;
      const loader = this.startLoading('Riegenteilnehmer werden geladen. Bitte warten ...',
        this.http.get<WertungContainer[]>(
          backendUrl + 'api/durchgang/' + this._competition + '/'
          + encodeURIComponent2( this._durchgang) + '/' + this._geraet + '/' + this._step
        ).pipe(
          share()
        ));

      loader.subscribe((data) => {
        this.wertungenLoading = false;
        if (this._step !== lastStepToLoad) {
          this.loadWertungen();
        } else {
          this.wertungen = data;
          this.wertungenSubject.next(this.wertungen);
      }
      }, this.standardErrorHandler);

      return loader;
    }

    loadAthletWertungen(competitionId: string, athletId: number): Observable<WertungContainer[]> {
      this.activateNonCaptionMode(competitionId);
      const loader = this.startLoading('Wertungen werden geladen. Bitte warten ...',
        this.http.get<WertungContainer[]>(
          backendUrl + `api/athlet/${this._competition}/${athletId}`
        ).pipe(share()));

      return loader;
    }

    activateNonCaptionMode(competitionId: string): Observable<Geraet[]> {
      if (this._competition !== competitionId
        || this.captionmode
        || (competitionId && !this.isWebsocketConnected())
        ) {
        this.captionmode = false;
        this._competition = competitionId;
        this.disconnectWS(true);
        this.initWebsocket();
        return this.loadGeraete();
      } else {
        return of(this.geraete);
      }
    }

    loadStartlist(query: string): Observable<StartList> {
      if (this._competition) {
        if (query) {
          return this.startLoading('Teilnehmerliste wird geladen. Bitte warten ...',
            this.http.get<StartList>(backendUrl + 'api/report/' + this._competition + '/startlist?q=' + query).pipe(share())
          );
        } else {
          return this.startLoading('Teilnehmerliste wird geladen. Bitte warten ...',
            this.http.get<StartList>(backendUrl + 'api/report/' + this._competition + '/startlist').pipe(share())
          );
        }
      } else {
        return of();
      }
    }

    isMessageAck(test: WertungContainer | MessageAck): test is MessageAck { return (test as MessageAck).type === 'MessageAck'; }

    updateWertung(durchgang: string, step: number, geraetId: number, wertung: Wertung): Observable<WertungContainer> {
      const competitionId = wertung.wettkampfUUID;
      const result = new Subject<WertungContainer>();
      if (this.shouldConnectAgain()) {
        this.reconnect();
      }
      this.startLoading('Wertung wird gespeichert. Bitte warten ...',
        this.http.put<WertungContainer | MessageAck>(
          backendUrl + 'api/durchgang/' + competitionId + '/' + encodeURIComponent2(durchgang) + '/' + geraetId + '/' + step,
          wertung
      ).pipe(share()))
      .subscribe((data) => {
        if (!this.isMessageAck(data) && (data as WertungContainer).wertung) {
          let wertungFound = false;
          this.wertungen = this.wertungen.map(w => {
            if (w.wertung.id === (data as WertungContainer).wertung.id) {
              wertungFound = true;
              return data;
            } else {
              return w;
            }
          });
          this.wertungenSubject.next(this.wertungen);
          result.next(data);
          if (wertungFound) {
            result.complete();
          }
        } else {
          const msg = data as MessageAck;
          this.showMessage.next(msg);
          result.error(msg.msg);
          result.complete();
        }
      }, this.standardErrorHandler);
      return result;
    }

    finishStation(competitionId: string, durchgang: string, geraetId: number, step: number): Observable<number[]> {
      const result = new Subject<number[]>();
      this.startLoading('Station wird abgeschlossen. Bitte warten ...',
        this.http.post<MessageAck>(backendUrl + 'api/durchgang/' + competitionId + '/finish', {
        type : 'FinishDurchgangStation',
        wettkampfUUID : competitionId,
        durchgang,
        geraet : geraetId,
        step
      } as FinishDurchgangStation).pipe(share()))
      .subscribe((data) => {
        const nextSteps = this.steps.filter(s => s > step);
        if (nextSteps.length > 0) {
          this._step = nextSteps[0];
        } else {
          localStorage.removeItem('current_station');
          this.checkJWT();
          this.stationFreezed = false;
          this._step = this.steps[0];
        }
        this.loadWertungen().subscribe(wertungen => {
          result.next(nextSteps);
        });
      }, this.standardErrorHandler);

      return result.asObservable();
    }

    nextStep(): number {
      const nextSteps = this.steps.filter(s => s > this._step);
      if (nextSteps.length > 0) {
        return nextSteps[0];
      } else {
        return this.steps[0];
      }
    }

    prevStep(): number {
      const prevSteps = this.steps.filter(s => s < this._step);
      if (prevSteps.length > 0) {
        return prevSteps[prevSteps.length - 1];
      } else {
        return this.steps[this.steps.length - 1];
      }
    }

    nextGeraet(): Observable<number> {
      if (this.loggedIn) {
        const nextSteps = this.steps.filter(s => s > this._step);
        if (nextSteps.length > 0) {
          return of(nextSteps[0]);
        } else {
          return of(this.steps[0]);
        }
      } else {
        let nextGeraetIdx = this.geraete.indexOf(this.geraete.find(s => s.id === this._geraet)) + 1;
        if (nextGeraetIdx >= this.geraete.length) {
          nextGeraetIdx = 0;
        }
        const actualStep = this._step;
        this._geraet = this.geraete[nextGeraetIdx].id;
        return this.loadSteps().pipe(map(steps => {
          const nextSteps = steps.filter(s => s > actualStep);
          if (nextSteps.length > 0) {
            return nextSteps[0];
          } else {
            return this.steps[0];
          }
        }));
      }
    }

    prevGeraet(): Observable<number> {
      if (this.loggedIn) {
        const prevSteps = this.steps.filter(s => s < this._step);
        if (prevSteps.length > 0) {
          return of(prevSteps[prevSteps.length - 1]);
        } else {
          return of(this.steps[this.steps.length - 1]);
        }
      } else {
        let prevGeraeteIdx = this.geraete.indexOf(this.geraete.find(s => s.id === this._geraet)) - 1;
        if (prevGeraeteIdx < 0) {
          prevGeraeteIdx = this.geraete.length - 1;
        }

        const actualStep = this._step;
        this._geraet = this.geraete[prevGeraeteIdx].id;
        return this.loadSteps().pipe(map(steps => {
          const prevSteps = this.steps.filter(s => s < actualStep);
          if (prevSteps.length > 0) {
            return prevSteps[prevSteps.length - 1];
          } else {
            return this.steps[this.steps.length - 1];
          }
        }));
      }
    }

    protected getWebsocketBackendUrl(): string {
      let host = location.host;
      const path = '/'; // location.pathname;
      const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
      let apiPath = 'api/';
      if (this._durchgang && this.captionmode) {
        apiPath = apiPath + 'durchgang/' + this._competition + '/' + encodeURIComponent2(this._durchgang) + '/ws';
      } else {
        apiPath = apiPath + 'durchgang/' + this._competition + '/all/ws';
        // apiPath = apiPath + "competition/ws";
      }
      if (!host || host === '') {
        host = 'wss://kutuapp.sharevic.net/';
      // } else if (host.startsWith('localhost')) {
      //   host = 'ws://localhost:5757/';
      } else {
        host = (protocol + '//' + host + path).replace('index.html', '');
      }

      return host + apiPath;
    }

    protected handleWebsocketMessage(message: any): boolean {
      const type = message.type;
      switch (type) {
        case 'DurchgangStarted':
          this._activeDurchgangList = [...this.activeDurchgangList, (message as DurchgangStarted)];
          this.durchgangStarted.next(this.activeDurchgangList);
          return true;

        case 'DurchgangFinished':
          const finished = (message as DurchgangFinished);
          this._activeDurchgangList = this.activeDurchgangList
            .filter(d => d.durchgang !== finished.durchgang || d.wettkampfUUID !== finished.wettkampfUUID);
          this.durchgangStarted.next(this.activeDurchgangList);
          return true;

        case 'AthletWertungUpdatedSequenced':
        case 'AthletWertungUpdated':
          const updated = (message as AthletWertungUpdated);
          this.wertungen = this.wertungen.map(w => {
            if (w.id === updated.wertung.athletId && w.wertung.wettkampfdisziplinId === updated.wertung.wettkampfdisziplinId ) {
              return Object.assign({}, w, {wertung: updated.wertung });
            } else {
              return w;
            }
          });
          this.wertungenSubject.next(this.wertungen);
          this.wertungUpdated.next(updated);
          return true;

        case 'AthletMovedInWettkampf':
        case 'AthletRemovedFromWettkampf':
          this.loadWertungen();
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
    }
}
