import { Component, OnInit } from '@angular/core';
import { NavController, AlertController, IonItemSliding } from '@ionic/angular';
import { ActivatedRoute } from '@angular/router';
import { BackendService } from 'src/app/services/backend.service';
import { BehaviorSubject, Subject, of } from 'rxjs';
import { filter, map, debounceTime, distinctUntilChanged, share, switchMap, take, tap } from 'rxjs/operators';
import { ClubRegistration, ProgrammRaw, AthletRegistration, Wettkampf, SyncAction } from '../../backend-types';

@Component({
  selector: 'app-reg-athletlist',
  templateUrl: './reg-athletlist.page.html',
  styleUrls: ['./reg-athletlist.page.scss'],
})
export class RegAthletlistPage implements OnInit {
  busy = new BehaviorSubject(false);
  currentRegistration: ClubRegistration;
  currentRegId: number;
  wkPgms: ProgrammRaw[];
  tMyQueryStream = new Subject<any>();

  sFilterTask: () => void = undefined;
  sFilteredRegistrationList: Map<number, AthletRegistration[]>;
  sAthletRegistrationList: AthletRegistration[];
  sMyQuery: string;
  sSyncActions: SyncAction[] = [];

  constructor(public navCtrl: NavController,
              private route: ActivatedRoute,
              public backendService: BackendService,
              private alertCtrl: AlertController) {
      if (! this.backendService.competitions) {
      this.backendService.getCompetitions();
    }
  }

  ngOnInit(): void {
    this.busy.next(true);
    const wkId = this.route.snapshot.paramMap.get('wkId');
    // tslint:disable-next-line: radix
    this.currentRegId = parseInt(this.route.snapshot.paramMap.get('regId'));
    if (wkId) {
      this.competition = wkId;
    } else if (this.backendService.competition) {
      this.competition = this.backendService.competition;
    }
  }

  ionViewWillEnter() {
    this.refreshList();
  }

  getSyncActions() {
    this.backendService.loadRegistrationSyncActions().pipe(
      take(1)
    ).subscribe(sa => {
      this.sSyncActions = sa;
    });
  }

  getStatus(reg: AthletRegistration) {
    if (this.sSyncActions) {
      const action = this.sSyncActions.find(a => a.verein.id === reg.vereinregistrationId && a.caption.indexOf(reg.name) > -1 && a.caption.indexOf(reg.vorname) > -1)
      if (action) {
        return "pending (" + action.caption.substring(0, (action.caption + ":").indexOf(":")) + ")";
      } else {
        return "in sync";
      }
    } else {
      return "n/a";
    }
  }

  refreshList() {
    this.busy.next(true);
    this.currentRegistration = undefined;
    this.getSyncActions();
    this.backendService.getClubRegistrations(this.competition).pipe(
      filter(regs => !!regs.find(reg => reg.id === this.currentRegId))
      , take(1)).subscribe(regs => {
      this.currentRegistration = regs.find(reg => reg.id === this.currentRegId);
      console.log('ask athletes-list for registration');
      this.backendService.loadAthletRegistrations(this.competition, this.currentRegId).subscribe(athletRegs => {
        this.busy.next(false);
        this.athletregistrations = athletRegs;
        const pipeBeforeAction = this.tMyQueryStream.pipe(
          filter(event => !!event && !!event.target),
          map(event => event.target.value || '*'),
          debounceTime(300),
          distinctUntilChanged(),
          tap(event => this.busy.next(true)),
          switchMap(this.runQuery(athletRegs)),
          share()
        );

        pipeBeforeAction.subscribe(filteredList => {
          this.sFilteredRegistrationList = filteredList;
          this.busy.next(false);
        });
      });
    });
  }

  set competition(competitionId: string) {
    if (!this.currentRegistration || competitionId !== this.backendService.competition) {
      this.backendService.loadProgramsForCompetition(this.competition).subscribe(pgms => {
        this.wkPgms = pgms;
      });
    }
  }

  get competition(): string {
    return this.backendService.competition || '';
  }

  getCompetitions(): Wettkampf[] {
    return this.backendService.competitions || [];
  }

  competitionName(): string {
    return this.backendService.competitionName;
  }

  runQuery(list: AthletRegistration[]) {
    return (query: string) => {
      const q = query.trim();
      const result: Map<number, AthletRegistration[]> = new Map();

      if (list) {
        list.forEach(registration => {
          const filterFn = this.filter(q);
          if (filterFn(registration)) {
            const existingItems = result.get(registration.programId) || [];
            result.set(registration.programId, [ ...existingItems, registration].sort((a, b)=>a.name.localeCompare(b.name)));
          }
        });
      }

      return of(result);
    };
  }
  set athletregistrations(list: AthletRegistration[]) {
    this.sAthletRegistrationList = list;
    this.runQuery(list)('*').subscribe(l => this.sFilteredRegistrationList = l);
    this.reloadList(this.sMyQuery || '*');
  }
  get athletregistrations() {
    return this.sAthletRegistrationList;
  }
  get filteredPrograms(): ProgrammRaw[] {
    return [...this.wkPgms.filter(pgm => this.sFilteredRegistrationList?.has(pgm.id))];
    //return [ ...this.sFilteredRegistrationList?.keys() ].map(id => this.wkPgms.filter(pgm => pgm.id==id)[0]);
  }
  
  mapProgram(programId: number) {
    return this.wkPgms.filter(pgm => pgm.id==programId)[0];
  }

  filteredStartList(programId: number) {
    return this.sFilteredRegistrationList?.get(programId) || this.sAthletRegistrationList || [];
  }
  reloadList(event: any) {
    this.tMyQueryStream.next(event);
  }

  itemTapped(reg: AthletRegistration, slidingItem: IonItemSliding) {
    slidingItem.getOpenAmount().then(amount => {
        if (amount > 0) {
        slidingItem.close();
      } else {
        slidingItem.open('end');
      }
    });
  }

  isLoggedInAsClub(): boolean {
    return this.backendService.loggedIn && this.backendService.authenticatedClubId === this.currentRegId + '';
  }

  isLoggedInAsAdmin(): boolean {
    return this.backendService.loggedIn && !!this.backendService.authenticatedClubId;
  }

  isLoggedIn(): boolean {
    return this.backendService.loggedIn;
  }

  filter(query: string) {
    const queryTokens = query.toUpperCase().split(' ');
    return (tn: AthletRegistration): boolean => {
      return query.trim() === '*' || queryTokens.filter(token => {
        if (tn.name.toUpperCase().indexOf(token) > -1) {
          return true;
        }
        if (tn.vorname.toUpperCase().indexOf(token) > -1) {
          return true;
        }
        if (this.wkPgms.find(pgm => tn.programId === pgm.id && pgm.name === token)) {
          return true;
        }
        if (tn.gebdat.indexOf(token) > -1) {
          return true;
        }
        if (token.length == 3 && tn.geschlecht.indexOf(token.substring(1,2)) > -1) {
          return true;
        }
      }).length === queryTokens.length;
    };
  }

  createRegistration() {
    this.navCtrl.navigateForward(`reg-athletlist/${this.backendService.competition}/${this.currentRegId}/0`);
  }

  edit(athlet: AthletRegistration, slidingItem: IonItemSliding) {
    this.navCtrl.navigateForward(`reg-athletlist/${this.backendService.competition}/${this.currentRegId}/${athlet.id}`);
    slidingItem.close();
  }

  delete(reg: AthletRegistration, slidingItem: IonItemSliding) {
    slidingItem.close();
    const alert = this.alertCtrl.create({
      header: 'Achtung',
      // tslint:disable-next-line:max-line-length
      subHeader: 'Löschen der Athlet-Anmeldung am Wettkampf',
      message: 'Hiermit wird die Anmeldung von ' + reg.name + ', ' + reg.vorname + ' am Wettkampf gelöscht.',
      buttons: [
        {text: 'ABBRECHEN', role: 'cancel', handler: () => {}},
        {text: 'OKAY', handler: () => {
          this.backendService.deleteAthletRegistration(this.backendService.competition, this.currentRegId, reg).subscribe(() => {
            this.refreshList();
          });
          }
        },
      ]
    });
    alert.then(a => a.present());
  }
}
