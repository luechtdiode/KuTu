import { Injectable } from '@angular/core';
import { WebsocketService, encodeURIComponent2, encodeURIComponent1 } from './websocket.service';
import { HttpClient, HttpHeaders, HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { LoadingController } from '@ionic/angular';
import { of, Subscription, BehaviorSubject, Subject, Observable } from 'rxjs';
import { share, map, switchMap} from 'rxjs/operators';
import { DurchgangStarted, Wettkampf, Geraet, WertungContainer, NewLastResults, StartList,
         MessageAck, AthletWertungUpdated, Wertung, FinishDurchgangStation,
         DurchgangFinished,
         ClubRegistration,
         NewClubRegistration,
         AthletRegistration,
         ProgrammRaw,
         RegistrationResetPW,
         SyncAction, JudgeRegistration, JudgeRegistrationProgramItem, BulkEvent, Verein, Score, ScoreLink, TeamItem, TeamList} from '../backend-types';
import { backendUrl } from '../utils';

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

    extractCompetitionLabel(wk: Wettkampf): string {
      return wk !!!== undefined ? wk.titel + ', am ' + (wk.datum + 'T').split('T')[0].split('-').reverse().join('-') : '';
    }

    get competitionName(): string {
      return this.extractCompetitionLabel(this.currentCompetition());
    }

    currentCompetition(): Wettkampf {
      if (!this.competition) { return undefined; }
      
      const candidate = this.competitions?.filter(c => c.uuid === this.competition);

      if (candidate?.length === 1) {
        return candidate[0];
      } else {
        return undefined;
      }
    }

    externalLoaderSubscription: Subscription;

    loggedIn = false;
    stationFreezed = false;
    captionmode = false;
    private loadingInstance: Promise<HTMLIonLoadingElement>;

    competitions: Wettkampf[];
    competitionSubject = new BehaviorSubject<Wettkampf[]>([]);
    durchgaenge: string[];

    geraete: Geraet[];
    geraeteSubject = new BehaviorSubject<Geraet[]>([]);
    steps: number[];
    wertungen: WertungContainer[];
    wertungenSubject = new BehaviorSubject<WertungContainer[]>([]);
    newLastResults = new BehaviorSubject<NewLastResults>(undefined);
    _clubregistrations = [];
    clubRegistrations = new BehaviorSubject<ClubRegistration[]>([]);
    clubTeams: TeamList = {};

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
        observable.subscribe({next: () => this.resetLoading(), error: (err) => this.resetLoading()});
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
            case 'rs':
              localStorage.setItem('auth_token', value);
              this.unlock();
              this.loggedIn = true;
              //this.checkJWT(value);
              console.log('club auth-token initialized');
              break;
            case 'rid':
              localStorage.setItem('auth_clubid', value);
              console.log('club id initialized', value);
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
        } else if (!!!this._competition || this._competition === 'undefined' && !!!localStorage.getItem('auth_clubid')) {
          console.log('initializing clubreg ...');
          this.getClubRegistrations(this._competition).subscribe(fin => finished.next(true));
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
      } else if (err.status === 404) {
        this.loggedIn = false;
        this.stationFreezed = false;
        this.captionmode = false;
        this._competition = undefined;
        this._durchgang = undefined;
        this._geraet = undefined;
        this._step = undefined;
        localStorage.removeItem('auth_token');
        localStorage.removeItem('current_competition');
        localStorage.removeItem('current_station');
        localStorage.removeItem('auth_clubid');
        this.showMessage.next({
          msg: 'Die aktuelle Einstellung ist nicht mehr gültig und wird zurückgesetzt.',
          type: 'Einstellung'
        } as MessageAck);
      } else {
        const msgAck = {
          title: err.statusText,
          msg : err.error,
          type : err.name
        } as MessageAck;
        this.showMessage.next(msgAck);
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
      }).pipe(share())).subscribe({
        next: (data) => {
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
        }, 
        error: (err: HttpErrorResponse) => {
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
        }
      });
      this.lastJWTChecked = new Date().getTime();
    }

    saveClubRegistration(competitionId: string, registration: ClubRegistration) {
      const save = this.startLoading('Vereins-Anmeldung wird gespeichert. Bitte warten ...',
        this.http.put<MessageAck>(backendUrl + 'api/registrations/' + competitionId + '/' + registration.id,
        registration
      ).pipe(share()));
      save.subscribe({
        next: (data) => {
          this._clubregistrations = [...this._clubregistrations.filter(r => r.id != registration.id), data];
          this.clubRegistrations.next(this._clubregistrations);
        }, 
        error: this.standardErrorHandler
      });
      return save;
    }

    saveClubRegistrationPW(competitionId: string, pwchange: RegistrationResetPW) {
      const save = this.startLoading('Neues Password wird gespeichert. Bitte warten ...',
        this.http.put<MessageAck>(backendUrl + 'api/registrations/' + competitionId + '/' + pwchange.id + "/pwchange",
        pwchange
      ).pipe(share()));
      save.subscribe({
        next: (data) => {
          this._clubregistrations = [...this._clubregistrations.filter(r => r.id != pwchange.id), data];
          this.clubRegistrations.next(this._clubregistrations);
        }, 
        error: this.standardErrorHandler
      });
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
      creater.subscribe({
        next: (data) => {
          this._clubregistrations = [...this._clubregistrations, data];
          this.clubRegistrations.next(this._clubregistrations);
        },
        error: this.standardErrorHandler
      });
      return creater;
    }

    deleteClubRegistration(competitionId: string, clubid: number) {
      const deleter = this.startLoading('Vereins-Anmeldung wird gelöscht. Bitte warten ...',
        this.http.delete(backendUrl + 'api/registrations/' + competitionId + '/' + clubid, {
          responseType: 'text'
        }
      ).pipe(share()));
      deleter.subscribe({
        next: (data) => {
          this.clublogout();
          this._clubregistrations = this._clubregistrations.filter(r => r.id != clubid);
          this.clubRegistrations.next(this._clubregistrations);
        }, 
        error: this.standardErrorHandler
      });
      return deleter;
    }

    loadWKPrograms(): Observable<ProgrammRaw[]> {
      const loader = this.startLoading('Programmliste wird geladen. Bitte warten ...',
        this.http.get<ProgrammRaw[]>(
          backendUrl + 'api/competition/programmlist'
          ).pipe(share()));

      loader.subscribe({
        error: this.standardErrorHandler
      });

      return loader;

    }

    loadProgramsForCompetition(competitionId: string) {
      const loader = this.startLoading('Programmliste zum Wettkampf wird geladen. Bitte warten ...',
        this.http.get<ProgrammRaw[]>(
          backendUrl + 'api/registrations/' + competitionId + '/programmlist'
          ).pipe(share()));

      loader.subscribe({
        error: this.standardErrorHandler
      });

      return loader;
    }
    
    loadTeamsListForClub(competitionId: string, clubid: number): Observable<TeamItem[]> {
      const path = 'api/registrations/' + competitionId + '/' + clubid + '/teams';
      if (this.clubTeams.path === path) {
        return of(this.clubTeams.teams);
      }
      const loader = this.startLoading('Teamliste zum Club wird geladen. Bitte warten ...',
        this.http.get<TeamItem[]>(
          backendUrl + path
          ).pipe(share()));

      loader.subscribe({
        next: (data) => {
          this.clubTeams = {
            "path" : path,
            "teams": data
          };
        }, 
        error: this.standardErrorHandler
      });

      return loader;
    }

    loadAthletListForClub(competitionId: string, clubid: number) {
      const loader = this.startLoading('Athletliste zum Club wird geladen. Bitte warten ...',
        this.http.get<AthletRegistration[]>(
          backendUrl + 'api/registrations/' + competitionId + '/' + clubid + '/athletlist'
          ).pipe(share()));

      loader.subscribe({
        next: (data) => {
        }, 
        error: this.standardErrorHandler
      });

      return loader;
    }

    loadAthletRegistrations(competitionId: string, clubid: number) {
      const loader = this.startLoading('Athletliste zum Club wird geladen. Bitte warten ...',
        this.http.get<AthletRegistration[]>(
          backendUrl + 'api/registrations/' + competitionId + '/' + clubid + '/athletes'
          ).pipe(share()));

      loader.subscribe({error: this.standardErrorHandler});

      return loader;
    }

    createAthletRegistration(competitionId: string, clubid: number, registration: AthletRegistration) {
      const loader = this.startLoading('Anmeldung wird gespeichert. Bitte warten ...',
        this.http.post<AthletRegistration>(
          backendUrl + 'api/registrations/' + competitionId + '/' + clubid + '/athletes',
          registration
          ).pipe(share()));

      loader.subscribe({error: this.standardErrorHandler});

      return loader;
    }

    saveAthletRegistration(competitionId: string, clubid: number, registration: AthletRegistration) {
      const loader = this.startLoading('Anmeldung wird gespeichert. Bitte warten ...',
        this.http.put<AthletRegistration>(
          backendUrl + 'api/registrations/' + competitionId + '/' + clubid + '/athletes/' + registration.id,
          registration
          ).pipe(share()));

      loader.subscribe({error: this.standardErrorHandler});

      return loader;
    }

    deleteAthletRegistration(competitionId: string, clubid: number, registration: AthletRegistration) {
      const loader = this.startLoading('Anmeldung wird gespeichert. Bitte warten ...',
        this.http.delete(
          backendUrl + 'api/registrations/' + competitionId + '/' + clubid + '/athletes/' + registration.id, {
            responseType: 'text'
          }
          ).pipe(share()));

      loader.subscribe({error: this.standardErrorHandler});

      return loader;
    }

    findCompetitionsByVerein(vereinId: number) {
      const loader = this.startLoading(
        'Es werden frühere Anmeldungen gesucht. Bitte warten ...',
        this.http.get<Wettkampf>(backendUrl + 'api/competition/byVerein/' + vereinId).pipe(share()));

      loader.subscribe({error: this.standardErrorHandler});

      return loader;
    }

    copyClubRegsFromCompetition(copyFromWk: Wettkampf, toCompetitionId: string, clubid: number) {
      const loader = this.startLoading('Anmeldung wird gespeichert. Bitte warten ...',
        this.http.put(
          backendUrl + 'api/registrations/' + toCompetitionId + '/' + clubid + '/copyfrom', copyFromWk, {
            responseType: 'text'
          }).pipe(share()));

      loader.subscribe({error: this.standardErrorHandler});

      return loader;
    }

    loadJudgeProgramDisziplinList(competitionId: string) {
      const loader = this.startLoading('Athletliste zum Club wird geladen. Bitte warten ...',
        this.http.get<JudgeRegistrationProgramItem[]>(
          backendUrl + 'api/registrations/' + competitionId + '/programmdisziplinlist'
          ).pipe(share()));

      loader.subscribe({error: this.standardErrorHandler});

      return loader;
    }

    loadJudgeRegistrations(competitionId: string, clubid: number) {
      const loader = this.startLoading('Wertungsrichter-Liste zum Club wird geladen. Bitte warten ...',
        this.http.get<JudgeRegistration[]>(
          backendUrl + 'api/registrations/' + competitionId + '/' + clubid + '/judges'
          ).pipe(share()));

      loader.subscribe({error: this.standardErrorHandler});

      return loader;
    }

    createJudgeRegistration(competitionId: string, clubid: number, registration: JudgeRegistration) {
      const loader = this.startLoading('Anmeldung wird gespeichert. Bitte warten ...',
        this.http.post<JudgeRegistration>(
          backendUrl + 'api/registrations/' + competitionId + '/' + clubid + '/judges',
          registration
          ).pipe(share()));

      loader.subscribe({error: this.standardErrorHandler});

      return loader;
    }

    saveJudgeRegistration(competitionId: string, clubid: number, registration: JudgeRegistration) {
      const loader = this.startLoading('Anmeldung wird gespeichert. Bitte warten ...',
        this.http.put<JudgeRegistration>(
          backendUrl + 'api/registrations/' + competitionId + '/' + clubid + '/judges/' + registration.id,
          registration
          ).pipe(share()));

      loader.subscribe({error: this.standardErrorHandler});

      return loader;
    }

    deleteJudgeRegistration(competitionId: string, clubid: number, registration: JudgeRegistration) {
      const loader = this.startLoading('Anmeldung wird gespeichert. Bitte warten ...',
        this.http.delete(
          backendUrl + 'api/registrations/' + competitionId + '/' + clubid + '/judges/' + registration.id, {
            responseType: 'text'
          }
          ).pipe(share()));

      loader.subscribe({error: this.standardErrorHandler});

      return loader;
    }

    clublogout() {
      this.logout();
    }

    utf8_to_b64( str: string ): string {
      return window.btoa(unescape(encodeURIComponent( str )));
    }

    b64_to_utf8( str: string ): string {
      return decodeURIComponent(escape(window.atob( str )));
    }

    clublist: Verein[] = [];

    getClubList() {
      if (!!this.clublist && this.clublist.length > 0) {
        return of(this.clublist);
      }
      const loader = this.startLoading('Clubliste wird geladen. Bitte warten ...',
        this.http.get<string[]>(backendUrl + 'api/registrations/clubnames').pipe(share()));

      loader.subscribe({
        next: (data) => {
          this.clublist = data;
        }, 
        error: this.standardErrorHandler
      });

      return loader;
    }

    resetRegistration(clubId: number) {
      const headers = new HttpHeaders();
      const obs: Observable<HttpResponse<String>> = this.http.options(
        backendUrl + 'api/registrations/' + this._competition + '/' + clubId + '/loginreset', {
          observe: 'response',
           headers: headers.set('Host', backendUrl),
          responseType: 'text'
        }
      ).pipe(share());
      const loader = this.startLoading('Mail für Login-Reset wird versendet. Bitte warten ...', obs);
      return loader;
    }

    clublogin(username, password) {
      this.clublogout();
      const headers = new HttpHeaders();
      const loader = this.startLoading('Login wird verarbeitet. Bitte warten ...',
        this.http.options(backendUrl + 'api/login', {
          observe: 'response',
          headers: headers.set('Authorization', 'Basic ' + this.utf8_to_b64(`${username}:${password}`)),
          withCredentials: true,
          responseType: 'text'
        }
      ).pipe(share()));
      loader.subscribe({
        next: (data) => {
          console.log(data);
          localStorage.setItem('auth_token', data.headers.get('x-access-token'));
          localStorage.setItem('auth_clubid', username);
          this.loggedIn = true;
        }, 
        error: (err) => {
          console.log(err);
          this.clublogout();
          this.resetLoading();
          if (err.status === 401) {
            localStorage.setItem('auth_token', err.headers.get('x-access-token'));
            this.loggedIn = false;
            /*this.showMessage.next({
              msg: 'Die Anmeldung ist nicht gültig oder abgelaufen.',
              type: 'Berechtigung'
            } as MessageAck);*/
          } else {
            this.standardErrorHandler(err);
          }
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
      loader.subscribe({
          next: (data) => {
            this.competitions = data;
            this.competitionSubject.next(data);
          }, 
          error: this.standardErrorHandler
        });
      return loader;
    }

    getClubRegistrations(competitionId: string) {
      this.checkJWT();
      if ((this._clubregistrations !== undefined && this._competition === competitionId) || this.isInitializing) {
        return this.loadClubRegistrations();
      }
      this.durchgaenge = [];
      this.clubTeams = {};
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
      if (!!!this._competition || this._competition === 'undefined') {
        return of([]);
      }
      const loader = this.startLoading('Clubanmeldungen werden geladen. Bitte warten ...',
        this.http.get<string[]>(backendUrl + 'api/registrations/' + this._competition).pipe(share()));

      loader.subscribe({
        next: (data) => {
          localStorage.setItem('current_competition', this._competition);
          this._clubregistrations = data;
          this.clubRegistrations.next(data);
        }, 
        error: this.standardErrorHandler
      });

      return loader;
    }

    loadRegistrationSyncActions(): Observable<SyncAction[]> {
      if (!!!this._competition || this._competition === 'undefined') {
        return of([]);
      }
      const loader = this.startLoading('Pendente An-/Abmeldungen werden geladen. Bitte warten ...',
        this.http.get<string[]>(backendUrl + 'api/registrations/' + this._competition + '/syncactions').pipe(share()));

      loader.subscribe({error: this.standardErrorHandler});

      return loader;
    }


    resetCompetition(competitionId) {
      console.log('reset data');
      this.durchgaenge = [];
      this.clubTeams = {};
      this._clubregistrations = [];
      this.clubRegistrations.next([]);
      this.geraete = undefined;
      this.geraeteSubject.next([]);
      this.steps = undefined;
      this.wertungen = undefined;
      this.wertungenSubject.next([]);

      this._competition = competitionId;
      this._durchgang = undefined;
      this._geraet = undefined;
      this._step = undefined;
    }

    getDurchgaenge(competitionId: string) {
      this.checkJWT();
      if ((this.durchgaenge !== undefined && this._competition === competitionId) || this.isInitializing) {
        return of(this.durchgaenge || []);
      }
      this.resetCompetition(competitionId);

      return this.loadDurchgaenge();
    }

    loadDurchgaenge(): Observable<string[]> {
      if (!!!this._competition || this._competition === 'undefined') {
        return of([]);
      }
      const loader = this.startLoading('Durchgangliste wird geladen. Bitte warten ...',
        this.http.get<string[]>(backendUrl + 'api/durchgang/' + this._competition).pipe(share()));

      const actualDg = this._durchgang;

      loader.subscribe({
        next: (data) => {
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
        },
        error: this.standardErrorHandler
      });

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

      this.captionmode = !!durchgang && this.durchgang !== 'undefined';

      return this.loadGeraete();
    }
    loadGeraete() {
      this.geraete = [];
      if (!!!this._competition || this._competition === 'undefined') {
        console.log('reusing geraetelist');
        return of([]);
      }
      console.log('renewing geraetelist');
      let path = '';
      if (this.captionmode && !!this._durchgang && this._durchgang !== 'undefined') {
        path = backendUrl + 'api/durchgang/' + this._competition + '/' + encodeURIComponent2(this._durchgang);
      } else {
        path = backendUrl + 'api/durchgang/' + this._competition + '/geraete';
      }
      const request =  this.startLoading('Geräte zum Durchgang werden geladen. Bitte warten ...',
        this.http.get<Geraet[]>(path).pipe(share()));
      request.subscribe({
        next: (data) => {
          this.geraete = data;
          this.geraeteSubject.next(this.geraete);
        }, 
        error: this.standardErrorHandler
      });
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
      stepsloader.subscribe({
        next: (data) => {
          this.steps = data.map(step => parseInt(step));
          if (this._step === undefined || this.steps.indexOf(this._step) < 0) {
            this._step = this.steps[0];
            this.loadWertungen();
          }
        }, 
        error: this.standardErrorHandler
      });

      return stepsloader;
    }

    loadSteps() {
      this.steps = [];
      if (!!!this._competition || this._competition === 'undefined' ||
          !!!this._durchgang || this._durchgang === 'undefined' ||
          this._geraet === undefined) {
        return of([]);
      }
      const request = this.startLoading('Stationen zum Gerät werden geladen. Bitte warten ...',
        this.http.get<string[]>(
          backendUrl + 'api/durchgang/' + this._competition + '/' + encodeURIComponent2(this._durchgang) + '/' + this._geraet
        ).pipe(share()));
      request.subscribe({
        next: (data) => {
          this.steps = data;
          if (this._step < data[0]) {
            this._step = data[0];
            this.loadWertungen();
          }
        }, 
        error: this.standardErrorHandler
      });
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
      if (this.wertungenLoading || this._geraet === undefined || this._step === undefined) {
        return of([]);
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

      loader.subscribe({
        next: (data) => {
          this.wertungenLoading = false;
          if (this._step !== lastStepToLoad) {
            this.loadWertungen();
          } else {
            this.wertungen = data;
            this.wertungenSubject.next(this.wertungen);
        }
        }, 
        error: this.standardErrorHandler
      });

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
      if (!competitionId || competitionId === 'undefined') {
        return of([]);
      }
      if (this._competition !== competitionId
        || this.captionmode
        || (!!!this.geraete || this.geraete.length === 0)
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

    isMessageAck(test: Wertung | WertungContainer | MessageAck): test is MessageAck { return (test as MessageAck).type === 'MessageAck' ||  !!(test as MessageAck).msg; }

    validateWertung(wertung: Wertung): Observable<Wertung> {
      const competitionId = wertung.wettkampfUUID;
      const result = new Subject<Wertung>();
      if (this.shouldConnectAgain()) {
        this.reconnect();
      }
      this.http.put<Wertung | MessageAck>(backendUrl + 'api/durchgang/' + competitionId + '/validate', wertung).pipe(share())
      .subscribe({
        next: (data) => {
          if (!this.isMessageAck(data)) {
            result.next(data);
            result.complete();
          } else {
            result.next(wertung);
            const msg = data as MessageAck;
            result.complete();
            this.showMessage.next(msg);
          }
        }, 
        error: this.standardErrorHandler
      });
      return result;
    }

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
      .subscribe({
        next: (data) => {
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
        }, 
        error: this.standardErrorHandler
      });
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
      .subscribe({
        next: (data) => {
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
        }, 
        error: this.standardErrorHandler
      });

      return result.asObservable();
    }

    nextStep(): number {
      if (this.steps == undefined) {
        return this._step
      }
      const nextSteps = this.steps.filter(s => s > this._step);
      if (nextSteps.length > 0) {
        return nextSteps[0];
      } else {
        return this.steps[0];
      }
    }

    prevStep(): number {
      if (this.steps == undefined) {
        return this._step
      }
      const prevSteps = this.steps.filter(s => s < this._step);
      if (prevSteps.length > 0) {
        return prevSteps[prevSteps.length - 1];
      } else {
        return this.steps[this.steps.length - 1];
      }
    }

    getPrevGeraet(): number {
      if (this.geraete == undefined) {
        return this._geraet;
      }

      let prevGeraeteIdx = this.geraete.indexOf(this.geraete.find(s => s.id === this._geraet)) - 1;
      if (prevGeraeteIdx < 0) {
        prevGeraeteIdx = this.geraete.length - 1;
      }
      return this.geraete[prevGeraeteIdx]?.id || this._geraet;
    }

    getNextGeraet(): number {
      if (this.geraete == undefined) {
        return this._geraet;
      }
      let nextGeraetIdx = this.geraete.indexOf(this.geraete.find(s => s.id === this._geraet)) + 1;
      if (nextGeraetIdx >= this.geraete.length) {
        nextGeraetIdx = 0;
      }
      return this.geraete[nextGeraetIdx]?.id || this._geraet;
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
        const actualStep = this._step;
        this._geraet = this.getNextGeraet();
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
        const actualStep = this._step;
        this._geraet = this.getPrevGeraet();
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

    getScoreList(path: string): Observable<Score> {
      if (this._competition) {
        const link = `${backendUrl}${path}`;
        return this.startLoading('Rangliste wird geladen. Bitte warten ...',
          this.http.get<Score>(link).pipe(share())
        );
      } else {
        return of(<Score>{});
      }
    }

    getScoreLists(): Observable<Map<any,ScoreLink>> {
      if (this._competition) {
        const link = `${backendUrl}api/scores/${this._competition}`;
        return this.startLoading('Ranglisten werden geladen. Bitte warten ...',
          this.http.get<Map<any,ScoreLink>>(link).pipe(share())
        );
      } else {
        return of(Object.assign({}));
      }
    }

    protected getWebsocketBackendUrl(): string {
      let host = location.host;
      const path = '/'; // location.pathname;
      const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
      let apiPath = 'api/';
      if (!!this._durchgang && this.captionmode) {
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
        case 'BulkEvent':
           return (message as BulkEvent).events.map( e => {
             return this.handleWebsocketMessage(e);
           }).reduce((a, b) => a && b);

        case 'DurchgangStarted':
          this._activeDurchgangList = [...this.activeDurchgangList, (message as DurchgangStarted)]
            .filter((value, index, self) => self.findIndex(ds => ds.durchgang === value.durchgang) === index);
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
