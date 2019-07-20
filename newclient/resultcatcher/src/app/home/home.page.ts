import { Component, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { Wettkampf, Geraet } from '../../app/backend-types';
import { NavController, AlertController, IonItemSliding } from '@ionic/angular';
import { BackendService } from '../services/backend.service';
import { encodeURIComponent2 } from '../services/websocket.service';
import { map } from 'rxjs/operators';


@Component({
  selector: 'app-page-home',
  templateUrl: 'home.page.html'
})
export class HomePage {

  durchgangstate: string;
  durchgangopen = false;

  constructor(public navCtrl: NavController, public backendService: BackendService, private alertCtrl: AlertController) {
    this.backendService.getCompetitions();
    this.backendService.durchgangStarted.pipe(map(dgl =>
      dgl.filter(dg => encodeURIComponent2(dg.durchgang) === encodeURIComponent2(this.backendService.durchgang)
                       && dg.wettkampfUUID === this.backendService.competition).length > 0 ? true : false
    )).subscribe(dg => {
      this.durchgangstate = dg ? 'gestartet' : 'gesperrt';
      this.durchgangopen = dg;
    });
  }

  get isLocked(): boolean {
    return this.backendService.stationFreezed;
  }
  get isOpenAndActive() {
    return this.durchgangopen && this.backendService.isWebsocketConnected();
  }
  get isLoggedIn() {
    return this.backendService.loggedIn;
  }

  set competition(competitionId: string) {
    if (!this.isLocked) {
      this.backendService.getDurchgaenge(competitionId);
    }
  }
  get competition(): string {
    return this.backendService.competition || '';
  }

  set durchgang(d: string) {
    if (!this.isLocked) {
      this.backendService.getGeraete(this.competition, d);
    }
  }
  get durchgang(): string {
    return this.backendService.durchgang || '';
  }

  set geraet(geraetId: number) {
    if (!this.isLocked) {
      this.backendService.getSteps(this.competition, this.durchgang, geraetId);
    }
  }
  get geraet(): number {
    return this.backendService.geraet || -1;
  }

  set step(s) {
    this.backendService.getWertungen(this.competition, this.durchgang, this.geraet, s);
  }
  get step() {
    return this.backendService.step || -1;
  }

  itemTapped(slidingItem: IonItemSliding) {
    slidingItem.getOpenAmount().then(amount => {
        if (amount > 10 || amount < -10) {
        slidingItem.close();
      } else {
        slidingItem.open('end');
      }
    });
  }

  nextStep(slidingItem: IonItemSliding) {
    this.step = this.backendService.nextStep();
    slidingItem.close();
  }

  prevStep(slidingItem: IonItemSliding) {
    this.step = this.backendService.prevStep();
    slidingItem.close();
  }

  getCompetitions(): Wettkampf[] {
    return this.backendService.competitions;
  }
  competitionName(): string {
    if (!this.backendService.competitions) { return ''; }
    const candidate = this.backendService.competitions
      .filter(c => c.uuid === this.backendService.competition)
      .map(c => c.titel + ', am ' + (c.datum + 'T').split('T')[0].split('-').reverse().join('-'));

    if (candidate.length === 1) {
      return candidate[0];
    } else {
      return '';
    }
  }

  geraetName(): string {
    if (!this.backendService.geraete) { return ''; }
    const candidate = this.backendService.geraete
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
      this.backendService.getSteps(this.competition, this.durchgang, this.geraet);
    }
    return this.backendService.steps;
  }

  navToStation() {
    this.navCtrl.navigateForward('station');
  }

  unlock() {
    const station = this.competitionName() + '/' + this.durchgang + '/' + this.geraetName();
    const alert = this.alertCtrl.create({
      header: 'Info',
      message: 'Die Fixierung auf die Station "'
                + station
                + '" wird aufgehoben.<br>Du kannst dann die Station frei einstellen.',
      buttons: [
        {text: 'ABBRECHEN', role: 'cancel', handler: () => {}},
        {text: 'OKAY', handler: () => {
            this.backendService.unlock();
          }
        },
      ]
    });
    alert.then(a => a.present());
  }

  logout() {
    const alert = this.alertCtrl.create({
      header: 'Achtung',
      // tslint:disable-next-line:max-line-length
      subHeader: 'Nach dem Abmelden können keine weiteren Resultate mehr erfasst werden.',
      message: 'Für die Erfassung weiterer Resultate wird eine Neuanmeldung erforderlich.',
      buttons: [
        {text: 'ABBRECHEN', role: 'cancel', handler: () => {}},
        {text: 'OKAY', handler: () => {
            this.backendService.logout();
          }
        },
      ]
    });
    alert.then(a => a.present());
  }

  finish() {
    const station = this.geraetName() + '/Riege #' + this.step;
    const alert = this.alertCtrl.create({
      header: 'Achtung',
      message: 'Nach dem Abschliessen der Station "'
                 + station
                 + '" können die erfassten Wertungen nur noch im Wettkampf-Büro korrigiert werden.',
      buttons: [
        {text: 'ABBRECHEN', role: 'cancel', handler: () => {}},
        {text: 'OKAY', handler: () => {
            const navTransition = alert.then(a => a.dismiss());
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
    alert.then(a => a.present());
  }
}
