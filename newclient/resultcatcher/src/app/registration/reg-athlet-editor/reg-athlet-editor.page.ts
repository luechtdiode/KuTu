import { Component, OnInit, NgZone } from '@angular/core';
import { NavController, AlertController } from '@ionic/angular';
import { ActivatedRoute } from '@angular/router';
import { BackendService } from 'src/app/services/backend.service';
import { AthletRegistration, ProgrammRaw } from 'src/app/backend-types';
import { toDateString } from '../../utils';
import { NgForm } from '@angular/forms';

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
  wkPgms: ProgrammRaw[];
  wettkampfId: number;
  clubAthletList: AthletRegistration[];
  clubAthletListCurrent: AthletRegistration[];
  // tslint:disable-next-line: variable-name
  private _selectedClubAthletId: number;

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
        this.backendService.loadAthletListForClub(this.wkId, this.regId).subscribe(regs => {
          this.clubAthletList = regs;
          if (this.athletId) {
            this.backendService.loadAthletRegistrations(this.wkId, this.regId).subscribe(regs => {
              this.clubAthletListCurrent = regs;
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
      });
    });
  }

  get selectedClubAthletId() {
    return this._selectedClubAthletId;
  }

  set selectedClubAthletId(id: number) {
    this._selectedClubAthletId = id;
    this.registration = this.clubAthletList.find(r => r.athletId === id);
  }

  needsPGMChoice(): boolean {
    const pgm = [...this.wkPgms][0];
    return !(pgm.aggregate == 1 && pgm.riegenmode == 2);
  }

  alter(athlet: AthletRegistration): number {
    const yearOfBirth = new Date(athlet.gebdat).getFullYear();
    const currentYear = new Date(Date.now()).getFullYear();
    return currentYear - yearOfBirth;
  }

  similarRegistration(a: AthletRegistration, b: AthletRegistration): boolean {
    return a.athletId === b.athletId ||
      a.name === b.name && a.vorname === b.vorname && a.gebdat === b.gebdat && a.geschlecht === b.geschlecht;
  }
  alternatives(athlet:AthletRegistration): AthletRegistration[] {
    return this.clubAthletListCurrent?.filter(cc => this.similarRegistration(cc, athlet) && cc.id != athlet.id) || [];
  }
  filterPGMsForAthlet(athlet: AthletRegistration): ProgrammRaw[] {
    const alter = this.alter(athlet);
    const alternatives = this.alternatives(athlet);
    return this.wkPgms.filter(pgm => {
      return (pgm.alterVon || 0) <= alter && (pgm.alterBis || 100) >= alter && alternatives.filter(a => a.programId === pgm.id).length === 0;
    });
  }

  editable() {
    return this.backendService.loggedIn;
  }

  updateUI(registration: AthletRegistration) {
    this.zone.run(() => {
      this.waiting = false;
      this.wettkampf = this.backendService.competitionName;
      this.registration = Object.assign({}, registration) as AthletRegistration;
      this.registration.gebdat = toDateString(this.registration.gebdat);
    });
  }

  isFormValid(): boolean {
    if(!this.registration?.programId && !this.needsPGMChoice()) {
      this.registration.programId = this.filterPGMsForAthlet(this.registration)[0]?.id;
    }
    return !!this.registration.gebdat &&
           !!this.registration.geschlecht &&
           this.registration.geschlecht.length > 0 &&
           !!this.registration.name &&
           this.registration.name.length > 0 &&
           !!this.registration.vorname &&
           this.registration.vorname.length > 0 &&
           !!this.registration.programId &&
           this.registration.programId > 0;

  }

  checkPersonOverride(reg: AthletRegistration) {
    if (reg.athletId) {
      const originalReg = [...this.clubAthletListCurrent, ...this.clubAthletList].find(r => r.athletId === reg.athletId);
      if (originalReg.geschlecht !== reg.geschlecht ||
          new Date(toDateString(originalReg.gebdat)).toJSON() !== new Date(reg.gebdat).toJSON()||
          originalReg.name !== reg.name ||
          originalReg.vorname !== reg.vorname) {
            return true;
          }
    }
    return false;
  }

  save(form: NgForm) {
    if(!form.valid) return;
    const reg = Object.assign({}, this.registration, {gebdat: new Date(form.value.gebdat).toJSON()});

    if (this.athletId === 0 || reg.id === 0) {

      if(!this.needsPGMChoice()) {
        this.filterPGMsForAthlet(this.registration).filter(pgm => pgm.id !== reg.programId).forEach(pgm => {
          this.backendService.createAthletRegistration(this.wkId, this.regId, Object.assign({}, reg, {programId: pgm.id}));
        });
      }

      this.backendService.createAthletRegistration(this.wkId, this.regId, reg).subscribe(() => {
        this.navCtrl.pop();
      });
    } else {
      let ask: boolean = this.checkPersonOverride(reg);
      if (ask) {
        const alert = this.alertCtrl.create({
          header: 'Achtung',
          subHeader: 'Person überschreiben vs korrigieren',
          message: 'Es wurden Änderungen an den Personen-Feldern vorgenommen. Diese sind ausschliesslich für Korrekturen zulässig. Die Identität der Person darf dadurch nicht geändert werden!',
          buttons: [
            {text: 'ABBRECHEN', role: 'cancel', handler: () => {}},
            {text: 'Korektur durchführen', handler: () => {
              this.backendService.saveAthletRegistration(this.wkId, this.regId, reg).subscribe(() => {
                this.navCtrl.pop();
              });
              }
            }
          ]
        });
        alert.then(a => a.present());
      } else {
        this.backendService.saveAthletRegistration(this.wkId, this.regId, reg).subscribe(() => {
          this.navCtrl.pop();
        });
      }
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
          if(!this.needsPGMChoice()) {
            this.clubAthletListCurrent
            .filter(reg => this.similarRegistration(this.registration, reg))
            .filter(reg => reg.id !== this.registration.id)
            .forEach(reg => {
              this.backendService.deleteAthletRegistration(this.wkId, this.regId, reg);
            });
          }
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
