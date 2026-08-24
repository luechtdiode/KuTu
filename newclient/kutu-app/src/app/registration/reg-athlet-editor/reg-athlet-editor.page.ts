import { Component, OnInit, inject, ChangeDetectionStrategy, signal } from '@angular/core';
import { NavController, AlertController } from '@ionic/angular';
import { ActivatedRoute } from '@angular/router';
import { BackendService } from 'src/app/services/backend.service';
import { AthletRegistration, ProgrammRaw, TeamItem, Wettkampf } from 'src/app/backend-types';
import { toDateString } from '../../utils';
import { NgForm } from '@angular/forms';

@Component({
    selector: 'app-reg-athlet-editor',
    templateUrl: './reg-athlet-editor.page.html',
    styleUrls: ['./reg-athlet-editor.page.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class RegAthletEditorPage implements OnInit {
  navCtrl = inject(NavController);
  private route = inject(ActivatedRoute);
  backendService = inject(BackendService);
  private alertCtrl = inject(AlertController);

  /** Inserted by Angular inject() migration for backwards compatibility */
  constructor(...args: unknown[]);


  constructor() {
  }

  waiting = signal(false);
  registration = signal<AthletRegistration>(undefined);
  wettkampf = signal<string>(undefined);
  wettkampfFull = signal<Wettkampf>(undefined);
  regId: number;
  athletId: number;
  wkId: string;
  wkPgms = signal<ProgrammRaw[]>(undefined);
  teams = signal<TeamItem[]>(undefined);
  wettkampfId: number;
  clubAthletList = signal<AthletRegistration[]>(undefined);
  clubAthletListCurrent = signal<AthletRegistration[]>(undefined);
  // tslint:disable-next-line: variable-name
  private _selectedClubAthletId: number;

  ngOnInit() {
    this.waiting.set(true);
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
        this.wkPgms.set(pgms);
        this.backendService.loadTeamsListForClub(this.wkId, this.regId).subscribe(teams => {
          this.teams.set(teams.filter(tm => tm.name?.trim().length > 0));
          this.backendService.loadAthletListForClub(this.wkId, this.regId).subscribe(regs => {
            this.clubAthletList.set(regs);
              this.backendService.loadAthletRegistrations(this.wkId, this.regId).subscribe(regs => {
                this.clubAthletListCurrent.set(regs);
                if (this.athletId) {
                  this.updateUI(regs.find(athlet => athlet.id === this.athletId));
                } else {
                  this.updateUI({
                    id: 0,
                    vereinregistrationId: this.regId,
                    name: '',
                    vorname: '',
                    geschlecht: 'W',
                    gebdat: undefined,
                    programId: undefined,
                    team: 0,
                    registrationTime: 0
                  } as AthletRegistration);
                }
              });
          });
        });
      });
    });
  }

  get selectedClubAthletId() {
    return this._selectedClubAthletId;
  }

  set selectedClubAthletId(id: number) {
    this._selectedClubAthletId = id;
    this.registration.set(this.clubAthletList().find(r => r.athletId === id));
  }

  get teamrules() {
    return (this.wettkampfFull().teamrule || '').split(',');
  }

  needsPGMChoice(): boolean {
    const pgm = [...this.wkPgms()][0];
    return !(pgm.aggregate == 1 && pgm.riegenmode > 1);
  }

  alter(athlet: AthletRegistration): number {
    if (this.wettkampfFull().altersklassen?.trim().length > 0) {
      let timeDiff = Math.abs(new Date(this.wettkampfFull().datum).getTime() - new Date(athlet.gebdat).getTime());
      return Math.floor((timeDiff / (1000 * 3600 * 24))/365 + 0.25);
    } else {
      const yearOfBirth = new Date(toDateString(athlet.gebdat)).getFullYear();
      const wkYear = new Date(this.wettkampfFull().datum).getFullYear();
      return wkYear - yearOfBirth;
    }
  }

  similarRegistration(a: AthletRegistration, b: AthletRegistration): boolean {
    return (a.athletId > 0 && b.athletId > 0 && a.athletId === b.athletId) ||
      a.name === b.name && a.vorname === b.vorname && a.gebdat === b.gebdat && a.geschlecht === b.geschlecht;
  }
  alternatives(athlet:AthletRegistration): AthletRegistration[] {
    return this.clubAthletListCurrent()?.filter(cc => this.similarRegistration(cc, athlet) && (cc.id != athlet.id)) || [];
  }
  getAthletPgm(athlet: AthletRegistration) {
    return this.wkPgms().find(p => p.id === athlet.programId) || Object.assign({
      parent: 0
    }) as ProgrammRaw
  }
  filterPGMsForAthlet(athlet: AthletRegistration): ProgrammRaw[] {
    const alter = this.alter(athlet);
    const alternatives = this.alternatives(athlet);
    return this.wkPgms().filter(pgm => {
      return (pgm.alterVon || 0) <= alter &&
        (pgm.alterBis || 100) >= alter &&
        alternatives.filter(a =>
          a.programId === pgm.id ||
          this.getAthletPgm(a).parentId === pgm.parentId
        ).length === 0;
    });
  }
  teamsAllowed(): boolean {
    return this.wettkampfFull().teamrule?.length > 0 && this.wettkampfFull().teamrule !== 'Keine Teams';
  }

  mapTeam(teamId: number): string {
    return [...this.teams().filter(tm => tm.index == teamId).map(tm => {
      if (tm.index > 0) {
        return tm.name + ' ' + tm.index;
      } else return tm.name;
    }), ''][0];
  }

  editable() {
    return this.backendService.loggedIn();
  }

  updateUI(registration: AthletRegistration) {
    this.waiting.set(false);
    this.wettkampf.set(this.backendService.competitionName);
    this.wettkampfFull.set(this.backendService.currentCompetition());
    const reg = Object.assign({}, registration);
    reg.gebdat = toDateString(reg.gebdat);
    if (!reg.team) {
      reg.team = 0;
      reg.reserve = 0;
    }
    this.registration.set(reg);
  }

  patchRegistration(patch: Partial<AthletRegistration>) {
    this.registration.update(reg => Object.assign({}, reg, patch));
  }

  isFormValid(): boolean {
    const reg = this.registration();
    if(!reg?.programId && !this.needsPGMChoice()) {
      reg.programId = this.filterPGMsForAthlet(reg)[0]?.id;
    }
    return !!reg.gebdat &&
           !!reg.geschlecht &&
           reg.geschlecht.length > 0 &&
           !!reg.name &&
           reg.name.length > 0 &&
           !!reg.vorname &&
           reg.vorname.length > 0 &&
           !!reg.programId &&
           reg.programId > 0 &&
           (!this.teamsAllowed() || ((!!reg.team || reg.team === 0) && !isNaN(reg.team)));

  }

  checkPersonOverride(reg: AthletRegistration) {
    if (reg.athletId) {
      const originalReg = [...this.clubAthletListCurrent(), ...this.clubAthletList()].find(r => r.athletId === reg.athletId);
      if (originalReg.geschlecht !== reg.geschlecht ||
          new Date(toDateString(originalReg.gebdat)).toJSON() !== new Date(toDateString(reg.gebdat)).toJSON()||
          originalReg.name !== reg.name ||
          originalReg.vorname !== reg.vorname) {
            return true;
          }
    }
    return false;
  }
  
  mediaSource = signal<string>(undefined);
  onRemoveMedia(event) {
    this.backendService.deleteFile(this.wkId, this.regId, this.registration().id).subscribe({
      next: (response) => {
        console.log("File delete finished: response:", response);
        this.registration.update(reg => Object.assign({}, reg, { mediafile: undefined }));
        this.mediaSource.set(undefined);
        const EL_audio: any = document.querySelector("#myAudio");
        EL_audio.src = undefined;
        EL_audio.load();
      },
        error: this.backendService.standardErrorHandler
      }
    );
  }
  onFileChange(fileChangeEvent) {
    if (!fileChangeEvent) {
        this.backendService.downloadFile(this.wkId, this.regId, this.registration().id).subscribe((blob) => {
          this.mediaSource.set(URL.createObjectURL(blob));
          const EL_audio: any = document.querySelector("#myAudio");
          EL_audio.src = URL.createObjectURL(blob);
          EL_audio.load();
        })
    } else {
      // Get a reference to the file that has just been added to the input
      const audiofile = fileChangeEvent?.target?.files[0];
      // Create a form data object using the FormData API
      let formData = new FormData();
      // Add the file that was just added to the form data
      formData.append("mediafile", audiofile, audiofile.name);
      // POST formData to server using HttpClient
      this.backendService.uploadFile(this.wkId, this.regId, this.registration().id, formData).subscribe({
        next: (response) => {
          console.log("File upload finished: response:", response);
          this.registration.update(reg => Object.assign({}, reg, { mediafile: response.mediafile }));
          if (this.registration().id > 0) {
            this.backendService.downloadFile(this.wkId, this.regId, this.registration().id).subscribe((blob) => {
              this.mediaSource.set(URL.createObjectURL(blob));
              const EL_audio: any = document.querySelector("#myAudio");
              EL_audio.src = URL.createObjectURL(blob);
              EL_audio.load();
            })
          }
        },
          error: this.backendService.standardErrorHandler
        }
      );
    }
  }

  save(form: NgForm) {
    if(!form.valid) return;
    const reg = Object.assign({}, this.registration(), {
      gebdat: new Date(form.value.gebdat).toJSON(),
      team: form.value.team ? form.value.team : 0,
      reserve: form.value.reserve ? form.value.reserve : 0,
      athletId: this.registration().athletId > 0 ? this.registration().athletId : 0
    });

    if (this.athletId === 0 || reg.id === 0) {

      if(!this.needsPGMChoice()) {
        this.filterPGMsForAthlet(this.registration()).filter(pgm => pgm.id !== reg.programId).forEach(pgm => {
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
                this.clubAthletListCurrent()
                .filter(regg => this.similarRegistration(this.registration(), regg))
                .filter(regg => regg.id !== this.registration().id)
                .forEach(regg => {
                  const patchedreg = Object.assign({}, reg, {id: regg.id, registrationTime: regg.registrationTime, programId: regg.programId});
                  this.backendService.saveAthletRegistration(this.wkId, this.regId, patchedreg);
                });
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
      message: 'Hiermit wird die Anmeldung von ' + this.registration().name + ', ' + this.registration().vorname + ' am Wettkampf gelöscht.',
      buttons: [
        {text: 'ABBRECHEN', role: 'cancel', handler: () => {}},
        {text: 'OKAY', handler: () => {
          if(!this.needsPGMChoice()) {
            this.clubAthletListCurrent()
            .filter(reg => this.similarRegistration(this.registration(), reg))
            .filter(reg => reg.id !== this.registration().id)
            .forEach(reg => {
              this.backendService.deleteAthletRegistration(this.wkId, this.regId, reg);
            });
          }
          this.backendService.deleteAthletRegistration(this.wkId, this.regId, this.registration()).subscribe(() => {
            this.navCtrl.pop();
          });
          }
        },
      ]
    });
    alert.then(a => a.present());
  }
}
