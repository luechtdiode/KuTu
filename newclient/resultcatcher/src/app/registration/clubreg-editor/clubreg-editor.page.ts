import { Component, OnInit, NgZone } from '@angular/core';
import { ClubRegistration, NewClubRegistration } from 'src/app/backend-types';
import { ActivatedRoute } from '@angular/router';
import { BackendService } from 'src/app/services/backend.service';
import { NavController, AlertController } from '@ionic/angular';

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
    private zone: NgZone
    ) { 
  }

  waiting = false;
  registration: ClubRegistration | NewClubRegistration;
  wettkampf: string;
  regId: number;
  wkId: string;
  wettkampfId: number;

  ngOnInit() {    
    this.waiting = true;
    this.wkId = this.route.snapshot.paramMap.get('wkId');
    this.regId = parseInt(this.route.snapshot.paramMap.get('regId'));
    this.backendService.getCompetitions().subscribe(comps => {
      this.wettkampfId = parseInt(comps.find(c => c.uuid === this.wkId).id);
      if (this.regId) {
        this.backendService.getClubRegistrations(this.wkId).subscribe(regs => {
          this.updateUI(regs.find(reg => reg.id === this.regId));
        })
      } else {
        this.updateUI({} as NewClubRegistration)
      }
    });
  }

  editable() {
    return this.backendService.loggedIn;
  }

  updateUI(registration: ClubRegistration | NewClubRegistration) {
    this.zone.run(() => {      
      this.waiting = false;
      this.wettkampf = this.backendService.competitionName
      this.registration = registration;
    });
  }

  save(registration: ClubRegistration | NewClubRegistration) {
    if (this.regId === 0) {
      let reg = <NewClubRegistration>{
        mail: registration.mail,
        mobilephone: registration.mobilephone,
        respName: registration.respName,
        respVorname: registration.respVorname,
        verband: registration.verband,
        vereinname: registration.vereinname,
        wettkampfId: registration.wettkampfId || this.wettkampfId,
        secret: (registration as NewClubRegistration).secret
      };      
      this.backendService.createClubRegistration(this.wkId, reg).subscribe(() => {
        this.navCtrl.pop();
      });
    } else {
      let reg = <ClubRegistration>{
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
      };
      this.backendService.saveClubRegistration(this.wkId, reg).subscribe(() => {
        this.navCtrl.pop();
      });
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
}
