import { Component, NgZone, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { NavController, AlertController } from '@ionic/angular';
import { JudgeRegistration, ProgrammRaw } from 'src/app/backend-types';
import { BackendService } from 'src/app/services/backend.service';
import { toDateString } from 'src/app/utils';

@Component({
  selector: 'app-reg-judge-editor',
  templateUrl: './reg-judge-editor.page.html',
  styleUrls: ['./reg-judge-editor.page.scss'],
})
export class RegJudgeEditorPage  implements OnInit {

  constructor(
    public navCtrl: NavController,
    private route: ActivatedRoute,
    public backendService: BackendService,
    private alertCtrl: AlertController,
    private zone: NgZone
    ) {
  }

  waiting = false;
  registration: JudgeRegistration;
  wettkampf: string;
  regId: number;
  judgeId: number;
  wkId: string;
  wkPgms: ProgrammRaw[];
  wettkampfId: number;
  // tslint:disable-next-line: variable-name
  private _selectedClubJudgeId: number;

  ngOnInit() {
    this.waiting = true;
    this.wkId = this.route.snapshot.paramMap.get('wkId');
    // tslint:disable-next-line: radix
    this.regId = parseInt(this.route.snapshot.paramMap.get('regId'));
    // tslint:disable-next-line: radix
    this.judgeId = parseInt(this.route.snapshot.paramMap.get('judgeId'));

    this.backendService.getCompetitions().subscribe(comps => {
      const wk = comps.find(c => c.uuid === this.wkId);
      // tslint:disable-next-line: radix
      this.wettkampfId = parseInt(wk.id);
      this.backendService.loadProgramsForCompetition(wk.uuid).subscribe(pgms => {
        this.wkPgms = pgms;
        if (this.judgeId) {
          this.backendService.loadJudgeRegistrations(this.wkId, this.regId).subscribe(regs => {
            this.updateUI(regs.find(judge => judge.id === this.judgeId));
          });
        } else {
          this.updateUI({
            id: 0,
            vereinregistrationId: this.regId,
            name: '',
            vorname: '',
            mobilephone: '+417',
            mail: '',
            comment: '',
            registrationTime: 0
          } as JudgeRegistration);
        }
      });
    });
  }

  editable() {
    return this.backendService.loggedIn;
  }

  updateUI(registration: JudgeRegistration) {
    this.zone.run(() => {
      this.waiting = false;
      this.wettkampf = this.backendService.competitionName;
      this.registration = registration;
    });
  }

  save(newreg: JudgeRegistration) {
    const reg = Object.assign({}, this.registration, newreg);
    console.log(reg);
    if (this.judgeId === 0 || reg.id === 0) {
      this.backendService.createJudgeRegistration(this.wkId, this.regId, reg).subscribe(() => {
        this.navCtrl.pop();
      });
    } else {
      this.backendService.saveJudgeRegistration(this.wkId, this.regId, reg).subscribe(() => {
        this.navCtrl.pop();
      });
    }
  }

  delete() {
    const alert = this.alertCtrl.create({
      header: 'Achtung',
      // tslint:disable-next-line:max-line-length
      subHeader: 'Löschen der Judge-Anmeldung am Wettkampf',
      message: 'Hiermit wird die Anmeldung von ' + this.registration.name + ', ' + this.registration.vorname + ' am Wettkampf gelöscht.',
      buttons: [
        {text: 'ABBRECHEN', role: 'cancel', handler: () => {}},
        {text: 'OKAY', handler: () => {
          this.backendService.deleteJudgeRegistration(this.wkId, this.regId, this.registration).subscribe(() => {
            this.navCtrl.pop();
          });
          }
        },
      ]
    });
    alert.then(a => a.present());
  }
}
