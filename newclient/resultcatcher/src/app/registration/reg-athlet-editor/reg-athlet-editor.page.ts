import { Component, OnInit, NgZone } from '@angular/core';
import { constructor } from 'color';
import { NavController, AlertController } from '@ionic/angular';
import { ActivatedRoute } from '@angular/router';
import { BackendService } from 'src/app/services/backend.service';
import { NewClubRegistration, ClubRegistration, AthletRegistration, ProgrammRaw } from 'src/app/backend-types';

@Component({
  selector: 'app-reg-athlet-editor',
  templateUrl: './reg-athlet-editor.page.html',
  styleUrls: ['./reg-athlet-editor.page.scss'],
})
export class RegAthletEditorPage implements OnInit {

  constructor(
    public navCtrl: NavController,
    private route: ActivatedRoute,
    public backendService: BackendService,
    private alertCtrl: AlertController,
    private zone: NgZone
    ) {
  }

  waiting = false;
  registration: AthletRegistration;
  wettkampf: string;
  regId: number;
  athletId: number;
  wkId: string;
  wettkampfId: number;
  private wkPgms: ProgrammRaw[];

  ngOnInit() {
    this.waiting = true;
    this.wkId = this.route.snapshot.paramMap.get('wkId');
    // tslint:disable-next-line: radix
    this.regId = parseInt(this.route.snapshot.paramMap.get('regId'));
    // tslint:disable-next-line: radix
    this.athletId = parseInt(this.route.snapshot.paramMap.get('athletId'));

    this.backendService.getCompetitions().subscribe(comps => {
      const wk = comps.find(c => c.uuid === this.wkId);
      // tslint:disable-next-line: radix
      this.wettkampfId = parseInt(wk.id);
      this.backendService.loadProgramsForCompetition(wk.uuid).subscribe(pgms => {
        this.wkPgms = pgms;
      });
      if (this.athletId) {
        this.backendService.loadAthletRegistrations(this.wkId, this.regId).subscribe(regs => {
          this.updateUI(regs.find(athlet => athlet.id === this.athletId));
        });
      } else {
        this.updateUI({
          id: 0,
          vereinregistrationId: this.regId,
          name: '',
          vorname: '',
          geschlecht: 'W',
          gebdat: undefined,
          programId: undefined,
          registrationTime: 0
        } as AthletRegistration);
      }
    });
  }

  editable() {
    return this.backendService.loggedIn;
  }

  updateUI(registration: AthletRegistration) {
    this.zone.run(() => {
      this.waiting = false;
      this.wettkampf = this.backendService.competitionName;
      this.registration = registration;
    });
  }

  save(newreg: AthletRegistration) {
    const reg = Object.assign({}, this.registration, {gebdat: new Date(newreg.gebdat).toJSON()});
    if (this.athletId === 0) {
      this.backendService.createAthletRegistration(this.wkId, this.regId, reg).subscribe(() => {
        this.navCtrl.pop();
      });
    } else {
      this.backendService.saveAthletRegistration(this.wkId, this.regId, reg).subscribe(() => {
        this.navCtrl.pop();
      });
    }
  }

  delete() {
    const alert = this.alertCtrl.create({
      header: 'Achtung',
      // tslint:disable-next-line:max-line-length
      subHeader: 'Löschen der Athlet-Anmeldung am Wettkampf',
      message: 'Hiermit wird die Anmeldung von ' + this.registration.name + ', ' + this.registration.vorname + ' am Wettkampf gelöscht.',
      buttons: [
        {text: 'ABBRECHEN', role: 'cancel', handler: () => {}},
        {text: 'OKAY', handler: () => {
          this.backendService.deleteAthletRegistration(this.wkId, this.regId, this.registration).subscribe(() => {
            this.navCtrl.pop();
          });
          }
        },
      ]
    });
    alert.then(a => a.present());
  }
}
