import { Component, OnInit } from '@angular/core';
import { NavController, AlertController, IonItemSliding } from '@ionic/angular';
import { BackendService } from '../services/backend.service';
import { encodeURIComponent2 } from '../services/websocket.service';
import { Wettkampf, Geraet, WertungContainer } from '../backend-types';
import { map } from 'rxjs/operators';

@Component({
  selector: 'app-station',
  templateUrl: './station.page.html',
  styleUrls: ['./station.page.scss'],
})
export class StationPage implements OnInit  {

  durchgangopen = false;
  geraete: Geraet[] = [];

  constructor(public navCtrl: NavController, public backendService: BackendService,
              private alertCtrl: AlertController) {
    this.backendService.durchgangStarted.pipe(
      map(dgl =>
      dgl.filter(dg =>
        encodeURIComponent2(dg.durchgang) === encodeURIComponent2(this.backendService.durchgang)
        && dg.wettkampfUUID === this.backendService.competition).length > 0 ? true : false
    )).subscribe(dg => {
      this.durchgangopen = dg;
    });
  }

  ionViewWillEnter() {
    if (this.geraete.length > 0 && !this.backendService.captionmode) {
      this.backendService.activateCaptionMode();
    }
  }
  ngOnInit(): void {
    this.backendService.geraeteSubject.subscribe(geraete => {
      if (this.backendService.captionmode) {
        this.geraete = geraete;
      }
    });
  }

  durchgangstate() {
    const connected = this.backendService.isWebsocketConnected() ? (this.durchgangopen ? 'gestartet' : 'gesperrt') : 'offline';
    return connected;
  }
  get durchgangstateClass() {
    const classes = {
      open: this.backendService.isWebsocketConnected() && this.durchgangopen,
      closed: this.backendService.isWebsocketConnected() && !this.durchgangopen,
      offline: !this.backendService.isWebsocketConnected()
    };
    return classes;
  }

  set competition(competitionId: string) {
    if (!this.stationFreezed) {
      this.backendService.getDurchgaenge(competitionId);
    }
  }
  get competition(): string {
    return this.backendService.competition;
  }

  set durchgang(d: string) {
    if (!this.stationFreezed) {
      this.backendService.getGeraete(this.competition, d);
    }
  }
  get durchgang(): string {
    return this.backendService.durchgang;
  }

  set geraet(geraetId: number) {
    if (!this.stationFreezed) {
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

  get stationFreezed(): boolean {
    return this.backendService.stationFreezed;
  }
  isLoggedIn() {
    return this.backendService.loggedIn;
  }

  get nextStepCaption(): string {
    if (this.isLoggedIn()) {
      return 'Nächste Riege';
    } else {
      return 'Nächstes Gerät';
    }
  }

  get prevStepCaption(): string {
    if (this.isLoggedIn()) {
      return 'Vorherige Riege';
    } else {
      return 'Vorheriges Gerät';
    }
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
    this.backendService.nextGeraet().subscribe(step => {
      this.step = step;
      slidingItem.close();
    });
  }

  prevStep(slidingItem: IonItemSliding) {
    this.backendService.prevGeraet().subscribe(step => {
      this.step = step;
      slidingItem.close();
    });
  }

  get station() {
    if (this.isLoggedIn()) {
      return this.step + '. Riege ' + this.geraetName() + ', ' + this.durchgang;
    } else {
      return this.step + '. Gerät ' + this.geraetName() + ', ' + this.durchgang;
    }
  }

  finish() {
    const alert = this.alertCtrl.create({
      header: 'Achtung',
      message: 'Nach dem Abschliessen der Station "'
                + this.station
                + '" können die erfassten Wertungen nur noch im Wettkampf-Büro korrigiert werden.',
      buttons: [
        {text: 'Abbrechen', role: 'cancel', handler: () => {}},
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
  getCompetitions(): Wettkampf[] {
    return this.backendService.competitions;
  }
  competitionName() {
    if (!this.backendService.competitions) { return ''; }
    const candidate = this.backendService.competitions
      .filter(c => c.uuid === this.backendService.competition)
      .map(c => c.titel + '<br>' + (c.datum + 'T').split('T')[0].split('-').reverse().join('-'));

    if (candidate.length === 1) {
      return candidate[0];
    } else {
      return '';
    }
  }

  geraetName(): string {
    if (!this.geraete || this.geraete.length === 0) { return ''; }
    const candidate = this.geraete
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
    return this.geraete;
  }

  getSteps(): number[] {
    // if (! this.backendService.steps && this.backendService.geraet) {
    //   this.backendService.getSteps(this.competition, this.durchgang, this.geraet);
    //   /*.subscribe(steps => {
    //     this.backendService.getWertungen(this.competition, this.durchgang, this.geraet, 1);
    //   })
    //   */
    // }
    return this.backendService.steps;
  }

  getWertungen(): WertungContainer[] {
    return this.backendService.wertungen;
  }

}
