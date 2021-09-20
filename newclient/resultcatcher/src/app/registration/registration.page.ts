import { Component, OnInit } from '@angular/core';
import { Wettkampf, ClubRegistration, SyncAction } from '../backend-types';
import { NavController, IonItemSliding, AlertController, ToastController } from '@ionic/angular';
import { BackendService } from '../services/backend.service';
import { BehaviorSubject, Subject, of, Observable } from 'rxjs';
import { ActivatedRoute } from '@angular/router';
import { debounceTime, distinctUntilChanged, map, filter, switchMap, share, take, tap } from 'rxjs/operators';

@Component({
  selector: 'app-registration',
  templateUrl: './registration.page.html',
  styleUrls: ['./registration.page.scss'],
})
export class RegistrationPage implements OnInit {


  sClubRegistrationList: ClubRegistration[];
  sFilteredRegistrationList: ClubRegistration[];
  sMyQuery: string;
  sSyncActions: SyncAction[] = [];

  tMyQueryStream = new Subject<any>();

  sFilterTask: () => void = undefined;

  busy = new BehaviorSubject(false);

  constructor(public navCtrl: NavController,
              private route: ActivatedRoute,
              public backendService: BackendService,
              public toastController: ToastController,
              private alertCtrl: AlertController) {
    if (! this.backendService.competitions) {
      this.backendService.getCompetitions();
    }
  }

  ngOnInit(): void {
    this.busy.next(true);
    const wkId = this.route.snapshot.paramMap.get('wkId');
    if (wkId) {
      this.competition = wkId;
    } else if (this.backendService.competition) {
      this.competition = this.backendService.competition;
    }
  }

  ionViewWillEnter() {
    this.getSyncActions();
  }

  get stationFreezed(): boolean {
    return this.backendService.stationFreezed;
  }

  set competition(competitionId: string) {
    if (!this.clubregistrations || competitionId !== this.backendService.competition) {
      this.busy.next(true);
      this.clubregistrations = [];

      this.backendService.getClubRegistrations(competitionId).subscribe(list => {
        this.getSyncActions();
        this.clubregistrations = list;
        this.busy.next(false);
        const pipeBeforeAction = this.tMyQueryStream.pipe(
          filter(event => !!event && !!event.target),
          map(event => event.target.value || '*'),
          debounceTime(300),
          distinctUntilChanged(),
          tap(event => this.busy.next(true)),
          switchMap(this.runQuery(list)),
          share()
        );

        pipeBeforeAction.subscribe(filteredList => {
          this.sFilteredRegistrationList = filteredList;
          this.busy.next(false);
        });
      });
    }
  }

  get competition(): string {
    return this.backendService.competition || '';
  }
  set clubregistrations(list: ClubRegistration[]) {
    this.sClubRegistrationList = list;
    this.reloadList(this.sMyQuery);
  }
  get clubregistrations() {
    return this.sClubRegistrationList;
  }
  get filteredStartList() {
    return this.sFilteredRegistrationList || this.sClubRegistrationList;
  }

  get isBusy(): Observable<boolean> {
    return this.busy;
  }

  getSyncActions() {
    this.backendService.loadRegistrationSyncActions().pipe(
      take(1)
    ).subscribe(sa => {
      this.sSyncActions = sa;
    });
  }

  getStatus(verein: ClubRegistration) {
    if (this.sSyncActions) {
      if (this.sSyncActions.find(a => a.verein.id === verein.id)) {
        return 'Abgleich ausstehend';
      } else {
        return 'Nachgeführt';
      }
    } else {
      return 'n/a';
    }
  }

  runQuery(list: ClubRegistration[]) {
    return (query: string) => {
      const q = query.trim();
      const result: ClubRegistration[] = [];

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

  reloadList(event: any) {
    this.tMyQueryStream.next(event);
  }

  itemTapped(club: ClubRegistration, slidingItem: IonItemSliding) {
    slidingItem.getOpenAmount().then(amount => {
        if (amount > 0) {
        slidingItem.close();
      } else {
        slidingItem.open('end');
      }
    });
  }

  isLoggedIn(club: ClubRegistration): boolean {
    return this.backendService.loggedIn && this.backendService.authenticatedClubId === club.id + '';
  }

  isLoggedInAsClub(): boolean {
    return this.backendService.loggedIn && !!this.backendService.authenticatedClubId;
  }

  logout(slidingItem: IonItemSliding) {
    if (slidingItem) {
      slidingItem.close();
    }
    const alert = this.alertCtrl.create({
      header: 'Achtung',
      // tslint:disable-next-line:max-line-length
      message: 'Für spätere Bearbeitungen an der Registrierung des Vereins am Wettkampf ist ein erneutes Login notwendig.',
      buttons: [
        {text: 'ABBRECHEN', role: 'cancel', handler: () => {}},
        {text: 'OKAY', handler: () => {
          this.backendService.clublogout();
          }
        },
      ]
    });
    alert.then(a => a.present());
  }

  delete(club: ClubRegistration, slidingItem: IonItemSliding) {
    slidingItem.close();
    const alert = this.alertCtrl.create({
      header: 'Achtung',
      // tslint:disable-next-line:max-line-length
      subHeader: 'Löschen der Vereins-Registrierung und dessen Anmeldungen am Wettkampf',
      message: 'Hiermit wird die Vereinsanmeldung am Wettkampf komplett gelöscht.',
      buttons: [
        {text: 'ABBRECHEN', role: 'cancel', handler: () => {}},
        {text: 'OKAY', handler: () => {
          this.backendService.deleteClubRegistration(this.competition, club.id);
          }
        },
      ]
    });
    alert.then(a => a.present());

  }
  async showPasswordResetSuccess() {
    const toast = await this.toastController.create({
      header: 'Passwort-Reset',
      message: 'Es wurde eine EMail mit dem Reset-Link versendet!',
      animated: true,
      position: 'middle',
      buttons: [
        {
          text: 'OK',
          role: 'cancel',
        }
      ]
    });
    toast.present();
  }

  login(club: ClubRegistration, slidingItem: IonItemSliding) {
    slidingItem.close();
    if (this.isLoggedIn(club)) {
      this.navCtrl.navigateForward(`registration/${this.competition}/${club.id}`);
    } else {
      const alert = this.alertCtrl.create({
        header: 'Vereins-Login',
        message: `Login für die Bearbeitung der Vereinsanmeldung am Wettkampf ${this.competitionName()}`,
        inputs: [
          {
            name: 'username',
            label: 'Vereinsname',
            placeholder: 'Vereinsname',
            value: `${club.vereinname} (${club.verband})`,
            type: 'text'
          },
          {
            name: 'pw',
            label: 'Passwort',
            placeholder: 'Passwort',
            type: 'password'
          }
        ],
        buttons: [
          {
            text: 'Abbrechen',
            role: 'cancel',
            handler: () => {
              console.log('Cancel clicked');
            }
          },
          {
            text: 'Login',
            handler: data => {
              if (data.pw && data.pw.trim().length > 0) {
                this.backendService.clublogin(club.id, data.pw).pipe(take(1)).subscribe((success) => {
                  this.navCtrl.navigateForward(`registration/${this.competition}/${club.id}`);
                }, (err) => {
                  if (err.status === 401) {
                    const resetAlert = this.alertCtrl.create({
                      header: 'Vereins-Login Fehlgeschlagen',
                      message: `Soll für diese Vereinsanmeldung am Wettkampf ${this.competitionName()} ein Passwort-Reset Vorgang gestartet werden?`,
                      inputs: [],
                      buttons: [
                        {
                          text: 'Abbrechen',
                          role: 'cancel',
                          handler: () => {
                            console.log('Cancel clicked');
                            return false;
                          }
                        },
                        {
                          text: 'Passwort-Reset',
                          handler: () => {
                            this.backendService.resetRegistration(club.id).pipe(take(1)).subscribe(() => {
                              this.showPasswordResetSuccess();
                            });
                            return true;
                          }
                        }
                      ]
                    });
                    resetAlert.then(a => {
                      a.present();
                    });
                  }
                });
                return true;
              } else {
                // invalid name
                return false;
              }
            }
          }
        ]
      });
      alert.then(a => {
        a.onkeypress = (ev: KeyboardEvent) => {
          if (ev.code === 'Enter') {
            (a.getElementsByClassName('ion-activatable')[1] as HTMLIonButtonElement).click();
          }
        };
        a.present();
      });
    }
  }

  createRegistration() {
    this.navCtrl.navigateForward(`registration/${this.competition}/${0}`);
  }

  getCompetitions(): Wettkampf[] {
    return this.backendService.competitions || [];
  }

  competitionName(): string {
    return this.backendService.competitionName;
  }

  filter(query: string) {
    const queryTokens = query.toUpperCase().split(' ');
    return (tn: ClubRegistration): boolean => {
      return query.trim() === '*' || queryTokens.filter(token => {
        if (tn.vereinname.toUpperCase().indexOf(token) > -1) {
          return true;
        }
        if (tn.verband.toUpperCase().indexOf(token) > -1) {
          return true;
        }
        if (tn.mail.toUpperCase().indexOf(token) > -1) {
          return true;
        }
        if (tn.respName.toUpperCase().indexOf(token) > -1) {
          return true;
        }
        if (tn.respVorname.toUpperCase().indexOf(token) > -1) {
          return true;
        }
      }).length === queryTokens.length;
    };
  }
}
