import { Component } from '@angular/core';
import { NavController, AlertController, ItemSliding } from 'ionic-angular';
import { BackendService } from '../../app/backend.service';
import { Wettkampf, Geraet, WertungContainer } from '../../app/backend-types';
import { encodeURIComponent2 } from '../../app/websocket.service';


@Component({
  selector: 'page-station',
  templateUrl: 'station.html'
})
export class StationPage {

  durchgangopen = false;

  constructor(public navCtrl: NavController, public backendService: BackendService, private alertCtrl: AlertController) {
    this.backendService.durchgangStarted.map(dgl => 
      dgl.filter(dg => encodeURIComponent2(dg.durchgang) === encodeURIComponent2(this.backendService.durchgang) && dg.wettkampfUUID === this.backendService.competition).length > 0 ? true : false
    ).subscribe(dg => {
      this.durchgangopen = dg;
    });
  }
  durchgangstate() {
    const connected = this.backendService.isWebsocketConnected() ? (this.durchgangopen ? 'gestartet' : 'gesperrt') : ' (offline)';
    return connected;
  }
  set competition(competitionId: string) {
    if(!this.stationFreezed) {
      this.backendService.getDurchgaenge(competitionId);
    }
  }
  get competition(): string {
    return this.backendService.competition;
  }

  set durchgang(d: string) {
    if(!this.stationFreezed) {
      this.backendService.getGeraete(this.competition, d);
    }
  }
  get durchgang(): string {
    return this.backendService.durchgang;
  }

  set geraet(geraetId: number) {
    if(!this.stationFreezed) {
      this.backendService.getSteps(this.competition, this.durchgang, geraetId);
    }
  }
  get geraet(): number {
    return this.backendService.geraet;
  }

  set step(s) {
    this.backendService.getWertungen(this.competition, this.durchgang, this.geraet, s);
  }
  get step() {
    return this.backendService.step;
  }

  get stationFreezed(): Boolean {
    return this.backendService.stationFreezed;
  }
  isLoggedIn() {
    return this.backendService.loggedIn;
  }
  
  nextStep(slidingItem: ItemSliding) {
    this.step = this.backendService.nextStep();
    slidingItem.close();
  }

  prevStep(slidingItem: ItemSliding) {
    this.step = this.backendService.prevStep();
    slidingItem.close();
  }
  
  finish() {
    let station = this.geraetName() + '/Riege #' + this.step;
    let alert = this.alertCtrl.create({
      title: 'Achtung',
      subTitle: 'Nach dem Abschliessen der Station <em><font color="#ffa76d">"'
                + station
                + '"</font></em> können die erfassten Wertungen nur noch im Wettkampf-Büro korrigiert werden.',
      buttons: [
        {text: 'Abbrechen', role: 'cancel', handler: () => {}},
        {text: 'OKAY', handler: () => {
          const navTransition = alert.dismiss();
          this.backendService.finishStation(this.competition, this.durchgang, this.geraet, this.step)
          .subscribe(nextSteps => {
            if (nextSteps.length === 0) {
              navTransition.then(() => {
                this.navCtrl.pop();
              });
            }
          });
          return false;
        }
      }, 
    ]
    });
    alert.present();
  }
  getCompetitions(): Wettkampf[] {
    return this.backendService.competitions;
  }
  competitionName(): string {
    if (!this.backendService.competitions) return '';
    let candidate = this.backendService.competitions
      .filter(c => c.uuid === this.backendService.competition)
      .map(c => c.titel + ', am ' + (c.datum + 'T').split('T')[0].split('-').reverse().join("-"));

    if (candidate.length === 1) {
      return candidate[0];
    } else {
      return '';
    }
  }

  geraetName(): string {
    if (!this.backendService.geraete) return '';
    let candidate = this.backendService.geraete
      .filter(c => c.id === this.backendService.geraet)
      .map(c => c.name);

    if (candidate.length === 1) {
      return candidate[0];
    } else {
      return '';
    }
  }
  getDurchgaenge(): string[] {
    return this.backendService.durchgaenge;
  }

  getGeraete(): Geraet[] {
    return this.backendService.geraete;
  }
  
  getSteps(): number[] {
    if (! this.backendService.steps && this.backendService.geraet) {
      this.backendService.getWertungen(this.competition, this.durchgang, this.geraet, 1);
    }
    return this.backendService.steps;
  }  

  getWertungen(): WertungContainer[] {
    return this.backendService.wertungen;
  }
}
