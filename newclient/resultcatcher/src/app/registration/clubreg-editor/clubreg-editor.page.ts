import { Component, OnInit, NgZone } from '@angular/core';
import { ClubRegistration, NewClubRegistration, SyncAction } from 'src/app/backend-types';
import { ActivatedRoute } from '@angular/router';
import { BackendService } from 'src/app/services/backend.service';
import { NavController, AlertController, ActionSheetController } from '@ionic/angular';
import { take } from 'rxjs/operators';
import { toDateString } from 'src/app/utils';
import { RegistrationResetPW } from '../../backend-types';

@Component({
  selector: 'app-clubreg-editor',
  templateUrl: './clubreg-editor.page.html',
  styleUrls: ['./clubreg-editor.page.scss'],
})
export class ClubregEditorPage implements OnInit {

  constructor(
    public navCtrl: NavController,
    private route: ActivatedRoute,
    public backendService: BackendService,
    private alertCtrl: AlertController,
    public actionSheetController: ActionSheetController,
    private zone: NgZone
    ) {
      if (! this.backendService.competitions) {
        this.backendService.getCompetitions();
      }
  }

  waiting = false;
  registration: ClubRegistration | NewClubRegistration;
  newRegistration: NewClubRegistration;
  changePassword: RegistrationResetPW;
  sSyncActions: string[] = [];
  wettkampf: string;
  regId: number;
  wkId: string;
  wettkampfId: number;

  ngOnInit() {
    this.waiting = true;
    this.wkId = this.route.snapshot.paramMap.get('wkId');
    // tslint:disable-next-line: radix
    this.regId = parseInt(this.route.snapshot.paramMap.get('regId'));
    this.backendService.getCompetitions().subscribe(comps => {
      // tslint:disable-next-line: radix
      this.wettkampfId = parseInt(comps.find(c => c.uuid === this.wkId).id);
      if (this.regId) {
        this.backendService.getClubRegistrations(this.wkId).subscribe(regs => {
          if (regs && regs.length > 0) {
            this.updateUI(regs.find(reg => reg.id === this.regId));
          }
        });
      } else {
        this.updateUI({mobilephone: '+417'} as NewClubRegistration);
      }
    });
  }

  async presentActionSheet() {
    let actionconfig: any = {};
    if ((this.registration as ClubRegistration).vereinId) {
      actionconfig = {
        header: 'Anmeldungen',
        cssClass: 'my-actionsheet-class',
        buttons: [{
          text: 'Athlet & Athletinnen',
          icon: 'list',
          handler: () => {
            this.editAthletRegistrations();
          }
        }, {
          text: 'Wertungsrichter',
          icon: 'glasses',
          handler: () => {
            this.editJudgeRegistrations();
          }
        }, {
          text: 'Kopieren aus anderem Wettkampf',
          icon: 'share',
          handler: () => {
            this.copyFromOthers();
          }
        }, {
          text: 'Registrierung löschen',
          role: 'destructive',
          icon: 'trash',
          handler: () => {
            this.delete();
          }
        }, {
          text: 'Abbrechen',
          icon: 'close',
          role: 'cancel',
          handler: () => {
            console.log('Cancel clicked');
          }
        }]
      };
    } else {
      actionconfig = {
        header: 'Anmeldungen',
        cssClass: 'my-actionsheet-class',
        buttons: [{
          text: 'Athlet & Athletinnen',
          icon: 'list',
          handler: () => {
            this.editAthletRegistrations();
          }
        }, {
          text: 'Wertungsrichter',
          icon: 'glasses',
          handler: () => {
            this.editJudgeRegistrations();
          }
        }, {
          text: 'Registrierung löschen',
          role: 'destructive',
          icon: 'trash',
          handler: () => {
            this.delete();
          }
        }, {
          text: 'Abbrechen',
          icon: 'close',
          role: 'cancel',
          handler: () => {
            console.log('Cancel clicked');
          }
        }]
      };
    }
    const actionSheet = await this.actionSheetController.create(actionconfig);
    await actionSheet.present();
  }

  editable() {
    return this.backendService.loggedIn;
  }

  ionViewWillEnter() {
    this.getSyncActions();
  }

  getSyncActions() {
    if (this.regId > 0) {
      this.backendService.loadRegistrationSyncActions().pipe(
        take(1)
      ).subscribe(sa => {
        this.sSyncActions = sa
        .filter(action => action.verein.id === (this.registration as ClubRegistration).id)
        .map(action => action.caption);
      });
    }
  }

  updateUI(registration: ClubRegistration | NewClubRegistration) {
    this.zone.run(() => {
      this.waiting = false;
      this.wettkampf = this.backendService.competitionName;
      this.registration = registration;
      if (this.regId === 0) {
         this.newRegistration = registration as NewClubRegistration;
      } else {
        this.changePassword = {
          id: this.regId,
          wettkampfId: registration.wettkampfId || this.wettkampfId,
          secret: '',
          verification: ''
        } as RegistrationResetPW;
      }
      if (this.registration.mail.length > 1) {
        this.backendService.currentUserName = this.registration.mail;
      }
    });
  }

  savePWChange() {
    if (this.changePassword && this.changePassword.secret && this.changePassword.secret === this.changePassword.verification) {
      this.backendService.saveClubRegistrationPW(this.wkId, this.changePassword);
    }
  }

  save(registration: ClubRegistration | NewClubRegistration) {
    if (this.regId === 0) {
      const nereg = registration as NewClubRegistration;
      if (nereg.secret !== nereg.verification) {
        const alert = this.alertCtrl.create({
              header: 'Achtung',
              subHeader: 'Passwort-Verifikation',
              message: 'Das Passwort stimmt nicht mit der Verifikation überein. Beide müssen genau gleich erfasst sein.',
              buttons: [
                {text: 'OKAY', role: 'cancel', handler: () => {}},
              ]
            });
        alert.then(a => a.present());
      } else {
        const reg =  {
          mail: nereg.mail,
          mobilephone: nereg.mobilephone,
          respName: nereg.respName,
          respVorname: nereg.respVorname,
          verband: nereg.verband,
          vereinname: nereg.vereinname,
          wettkampfId: nereg.wettkampfId || this.wettkampfId,
          secret: nereg.secret
        } as NewClubRegistration;
        this.backendService.createClubRegistration(this.wkId, reg).subscribe((data) => {
          this.regId = data.id;
          this.registration = data;
          this.backendService.currentUserName = data.mail;
          this.presentActionSheet();
        });
      }
    } else {
      const reg =  {
        id: (this.registration as ClubRegistration).id,
        registrationTime: (this.registration as ClubRegistration).registrationTime,
        vereinId: (this.registration as ClubRegistration).vereinId,
        mail: registration.mail,
        mobilephone: registration.mobilephone,
        respName: registration.respName,
        respVorname: registration.respVorname,
        verband: registration.verband,
        vereinname: registration.vereinname,
        wettkampfId: registration.wettkampfId || this.wettkampfId,
      } as ClubRegistration;
      this.backendService.saveClubRegistration(this.wkId, reg);
    }
  }

  delete() {
    const alert = this.alertCtrl.create({
      header: 'Achtung',
      // tslint:disable-next-line:max-line-length
      subHeader: 'Löschen der Vereins-Registrierung und dessen Anmeldungen am Wettkampf',
      message: 'Hiermit wird die Vereinsanmeldung am Wettkampf komplett gelöscht.',
      buttons: [
        {text: 'ABBRECHEN', role: 'cancel', handler: () => {}},
        {text: 'OKAY', handler: () => {
          this.backendService.deleteClubRegistration(this.wkId, this.regId).subscribe(() => {
            this.navCtrl.pop();
          });
          }
        },
      ]
    });
    alert.then(a => a.present());
  }

  editAthletRegistrations() {
    this.navCtrl.navigateForward(`reg-athletlist/${this.backendService.competition}/${this.regId}`);
  }

  editJudgeRegistrations() {
    this.navCtrl.navigateForward(`reg-judgelist/${this.backendService.competition}/${this.regId}`);
  }

  copyFromOthers() {
    this.backendService.findCompetitionsByVerein((this.registration as ClubRegistration).vereinId).subscribe(list => {
      const selectOptions = list.map(wk => {
        return {
          type: 'radio',
          label: wk.titel + ' (' + toDateString(wk.datum) + ')',
          value: wk,
        };
      });
      this.alertCtrl.create({
        header: 'Anmeldungen kopieren',
        cssClass: 'my-optionselection-class',
        // tslint:disable-next-line:max-line-length
        // subHeader: 'Schritt #1',
        message: 'Aus welchem Wettkampf sollen die Anmeldungen kopiert werden?',
        inputs: selectOptions,
        buttons: [
          {text: 'ABBRECHEN', role: 'cancel', handler: () => {}},
          {text: 'OKAY', handler: (data) => {
            this.backendService.copyClubRegsFromCompetition(data, this.backendService.competition, (this.registration as ClubRegistration).id).subscribe(() => {
              this.getSyncActions();
              this.alertCtrl.create({
                header: 'Anmeldungen kopieren',
                // tslint:disable-next-line:max-line-length
                // subHeader: 'Schritt #2',
                message: 'Alle nicht bereits erfassten Anmeldungen wurden übernommen.',
                buttons: [
                  {text: 'OKAY', role: 'cancel', handler: () => {}},
                  {text: '> Athlet-Anmeldungen', handler: () => {
                    this.editAthletRegistrations();
                  }},
                  {text: '> WR Anmeldungen', handler: () => {
                    this.editJudgeRegistrations();
                  }}
                ]
              }).then(a => a.present());
            });
          }},
        ]
      }).then(a => a.present());
    });
  }
}
