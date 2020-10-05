import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { NavController, AlertController, IonItemSliding } from '@ionic/angular';
import { BehaviorSubject, Subject, of } from 'rxjs';
import { take, filter, map, debounceTime, distinctUntilChanged, tap, switchMap, share } from 'rxjs/operators';
import { ClubRegistration, ProgrammRaw, JudgeRegistration, SyncAction, Wettkampf, JudgeRegistrationProgramItem } from 'src/app/backend-types';
import { BackendService } from 'src/app/services/backend.service';

@Component({
  selector: 'app-reg-judgelist',
  templateUrl: './reg-judgelist.page.html',
  styleUrls: ['./reg-judgelist.page.scss'],
})
export class RegJudgelistPage  implements OnInit {
  private busy = new BehaviorSubject(false);

  private currentRegistration: ClubRegistration;
  private currentRegId: number;
  wkPgms: JudgeRegistrationProgramItem[];
  tMyQueryStream = new Subject<any>();

  sFilterTask: () => void = undefined;
  sFilteredRegistrationList: JudgeRegistration[];
  sJudgeRegistrationList: JudgeRegistration[];
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

  refreshList() {
    this.busy.next(true);
    this.currentRegistration = undefined;
    this.backendService.getClubRegistrations(this.competition).pipe(
      filter(regs => !!regs.find(reg => reg.id === this.currentRegId))
      , take(1)).subscribe(regs => {
      this.currentRegistration = regs.find(reg => reg.id === this.currentRegId);
      console.log('ask Judgees-list for registration');
      this.backendService.loadJudgeRegistrations(this.competition, this.currentRegId).subscribe(judgeRegs => {
        this.busy.next(false);
        this.judgeregistrations = judgeRegs;
        const pipeBeforeAction = this.tMyQueryStream.pipe(
          filter(event => !!event && !!event.target),
          map(event => event.target.value || '*'),
          debounceTime(300),
          distinctUntilChanged(),
          tap(event => this.busy.next(true)),
          switchMap(this.runQuery(judgeRegs)),
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
      this.backendService.loadJudgeProgramDisziplinList(this.competition).subscribe(pgms => {
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

  runQuery(list: JudgeRegistration[]) {
    return (query: string) => {
      const q = query.trim();
      const result: JudgeRegistration[] = [];

      if (q && list) {
        list.forEach(registration => {
          const filterFn = this.filter(q);
          if (filterFn(registration)) {
            result.push(registration);
          }
        });
      }

      return of(result);
    };
  }
  set judgeregistrations(list: JudgeRegistration[]) {
    this.sJudgeRegistrationList = list;
    this.reloadList(this.sMyQuery);
  }
  get judgeregistrations() {
    return this.sJudgeRegistrationList;
  }
  get filteredStartList() {
    return this.sFilteredRegistrationList || this.sJudgeRegistrationList || [];
  }
  reloadList(event: any) {
    this.tMyQueryStream.next(event);
  }

  itemTapped(reg: JudgeRegistration, slidingItem: IonItemSliding) {
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

  filter(query: string) {
    const queryTokens = query.toUpperCase().split(' ');
    return (tn: JudgeRegistration): boolean => {
      return query.trim() === '*' || queryTokens.filter(token => {
        if (tn.name.toUpperCase().indexOf(token) > -1) {
          return true;
        }
        if (tn.vorname.toUpperCase().indexOf(token) > -1) {
          return true;
        }
        if (tn.mobilephone.toUpperCase().indexOf(token) > -1) {
          return true;
        }
        if (tn.mail.toUpperCase().indexOf(token) > -1) {
          return true;
        }
        if (tn.comment.toUpperCase().indexOf(token) > -1) {
          return true;
        }
      }).length === queryTokens.length;
    };
  }

  createRegistration() {
    this.navCtrl.navigateForward(`reg-judgelist/${this.backendService.competition}/${this.currentRegId}/0`);
  }

  edit(judge: JudgeRegistration, slidingItem: IonItemSliding) {
    this.navCtrl.navigateForward(`reg-judgelist/${this.backendService.competition}/${this.currentRegId}/${judge.id}`);
    slidingItem.close();
  }

  delete(reg: JudgeRegistration, slidingItem: IonItemSliding) {
    slidingItem.close();
    const alert = this.alertCtrl.create({
      header: 'Achtung',
      // tslint:disable-next-line:max-line-length
      subHeader: 'Löschen der Wertungsrichter-Anmeldung am Wettkampf',
      message: 'Hiermit wird die Anmeldung von ' + reg.name + ', ' + reg.vorname + ' am Wettkampf gelöscht.',
      buttons: [
        {text: 'ABBRECHEN', role: 'cancel', handler: () => {}},
        {text: 'OKAY', handler: () => {
          this.backendService.deleteJudgeRegistration(this.backendService.competition, this.currentRegId, reg).subscribe(() => {
            this.refreshList();
            this.navCtrl.pop();
          });
          }
        },
      ]
    });
    alert.then(a => a.present());
  }
}
